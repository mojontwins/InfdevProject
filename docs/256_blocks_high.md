# Extending World Height to 256 Blocks

> **Status:** Implemented (branch `feature/256-world-height`).
> **Target version:** Infdev 20100420 (Java 8 refactor).

---

## Overview

The current world height is 128 blocks (y = 0 to 127), stored as a flat
`byte[16 * 16 * 128 = 32768]` per chunk.  This document describes how to
extend the world to 256 blocks (y = 0 to 255) while:

1. Preserving the random-block-tick rate (grass/crop/sapling growth unchanged).
2. Keeping memory usage at or below today's levels for partially-explored worlds.
3. Remaining performant at 16 subchunks (16 × 16 × 16) per chunk column.
4. Maintaining full backward compatibility with existing 128-high saves.

---

## Architecture

### Storage: lazy subchunks

Each chunk holds up to **16 subchunks** (16×16×16 each).  A subchunk is:

```
byte[]        blocks      — 4096 bytes
NibbleArray   data        — 2048 bytes (block metadata, 4 bits per cell)
NibbleArray   skyLight    — 2048 bytes
NibbleArray   blockLight  — 2048 bytes
```

Total per subchunk: **10 240 bytes** (4096 blocks + 3 × 2048 nibble planes).
(The earlier draft's "8 192 B" figure silently dropped the blockLight plane —
all four planes are materialized together with each subchunk.)

Subchunk index `s` covers world y = `s * 16` to `s * 16 + 15`.

| Subchunks | y range    | When allocated |
|-----------|------------|----------------|
| 0–7       | 0–127      | Always, on chunk load |
| 8–15      | 128–255    | Lazily, on first **write** above y=127 |

**Allocation is write-only.** A `null` subchunk is "implicit open air, fully
lit": reads against it return block 0 (air), metadata 0, skylight 15 and
blocklight 0 without allocating. Read-allocation was deliberately rejected —
`WorldRenderer` scans all 16 slabs per rebuild, so allocating on read would
materialize the entire top half on the first render pass and defeat laziness.

The flat arrays (`byte blocks[32768]`, `NibbleArray data`, `NibbleArray
skyLightMap`, `NibbleArray blockLightMap`) are removed from `Chunk` and
replaced with per-subchunk parallel arrays: `byte[16][] blocks`,
`NibbleArray[16] data / skyLightMap / blockLightMap`.

**Memory:** 8-eager subchunks = 8 × 10 240 B = 81 920 B (blocks + metadata +
lights) — matching today's single flat chunk exactly.  A fully built 16-subchunk
column is 163 840 B.  The lazy upper half costs nothing until a block is placed
there.

### Three independent vertical systems

The codebase already has three independent concepts of "16-block vertical
slice".  After the change all three stay 16-aligned and grow from 8 to 16:

| System              | Field / constant            | Before | After |
|---------------------|-----------------------------|--------|-------|
| Block + light storage | `Chunk.subchunks[]`         | —      | 16    |
| Entity buckets        | `Chunk.entities[]` (`ENTITY_SEGMENTS`) | 8 | **16** |
| Render layers        | `RenderGlobal.renderChunksTall` | 8   | **16** |

They are **independent structures** — no code-level coupling between them in
this version.  A future polish pass can extract a `Subchunk` class that owns
all four (blocks, lights, entities, renderer), but that is out of scope for
v1.

### Indexing

Subchunk-local cell index (shared by blocks and NibbleArrays):
```
index = (x << 8) | (z << 4) | yLocal
```
- `x, z ∈ [0, 15]` → 4 bits each
- `yLocal ∈ [0, 15]` → 4 bits
- Total: 12 bits per subchunk (fits in `int`, no overflow)

Chunk-level subchunk lookup:
```
subchunkIdx = y >> 4         // y / 16
yLocal      = y & 15         // y % 16
```

Entity segment index (already `entity.posY / 16` at `Chunk.java:482`):
unchanged — it naturally produces `[0, 15]` for y ∈ `[0, 255]`.

`TILE_Y_SHIFT` stays **10** — `packedTileKey = x + (y << 10) + (z << 10 << 10)`
is already collision-free for y ∈ `[0, 255]`, so no change is needed there.

---

## Performance

### Random block ticks

**Goal:** a crop/sapling/grass cell receives `updateTick` at the same
average rate as today.

The loop iterates **materialized** subchunks — 8 on fresh terrain — doing 10
LCG-local probes each, so a fresh chunk still performs exactly 80 probes (the
historical budget). Building upward materializes more subchunks and the probe
count scales with them, keeping the per-cell tick rate constant at any height:

|                    | Today        | Fresh terrain (8 subchunks) | Fully built (16 subchunks) |
|--------------------|--------------|-----------------------------|----------------------------|
| Cells per subchunk | 16 × 16 × 16 = 4 096 | 4 096 | 4 096 |
| Probes per chunk   | 80 (flat)   | 10 × 8 = 80                 | 10 × 16 = 160              |
| Pick probability   | 80 / 32 768 ≈ 0.244 % | 10 / 4 096 ≈ 0.244 % | 10 / 4 096 ≈ 0.244 % |
| Pick rate          | **identical** | **identical**            | **identical**              |

The tick loop in `World.updateBlocksAndPlayCaveSounds` iterates subchunks up to
`chunk.getSubchunkCount()`: for each present subchunk it performs 10 LCG-local
probes (`x = (lcg>>2) & 15`, `z = (lcg>>6) & 15`, `yLocal = (lcg>>10) & 15`),
and the world Y is `subchunkIdx * 16 + yLocal`. No global budget juggling.

### Memory

|                          | Today (128 flat) | 8 eager subchunks | 16 subchunks (fully built) |
|--------------------------|-------------------|-------------------|---------------------------|
| Blocks + metadata + lights / chunk | 32 768 + 16 384 + 16 384 + 16 384 = 81 920 B | 81 920 B | 163 840 B |
| Entities / chunk         | 8 × List overhead  | 8 × overhead     | 16 × overhead            |
| 17×17 loaded area        | ~46 MB            | ~46 MB           | ~47 MB                   |

**Result:** a fresh terrain chunk materializes exactly 8 subchunks — the same
81 920 B it always has — so new worlds cost no more than before. Building upward
or loading a saved top half adds 10 240 B per subchunk used. Memory is never
worse than today for equivalent coverage.

### Rendering

`renderChunksTall = 8` → `16`.  Each chunk column now has 16 `WorldRenderer`
instances instead of 8, each owning 3 GL call lists.

Mitigations:
- `WorldRenderer`'s existing empty-slab optimisation: a slab that contains no
  solid blocks compiles to an empty display list, so the all-air top half is
  cheap even at 16 layers. (A dedicated `skipRenderPass` flag was considered
  but not added — the empty-list behaviour already covers it.)
- Frustum culling discards the upper layers of distant chunks when looking
  horizontally.
- `RenderGlobal.markBlocksForUpdate` computes `chunkY = blockY >> 4` — the
  subchunk index maps 1:1 to the renderer index — but the resulting range is
  clamped into `[0, renderChunksTall - 1]` so neighbour-of-y=0 edits (which pass
  y = −1) and edits above the top layer cannot index out of bounds.

---

## Files that change

### `net.minecraft.game.world.chunk.NibbleArray`

- Index formula: `x << 8 | z << 4 | yLocal` (was `x << 11 | z << 7 | y`).
- Constructor called with `cellCount = 4096` (16 × 16 × 16) per subchunk.

### `net.minecraft.game.world.chunk.Chunk`

**Constants:**
- `SECTION_SIZE`: now `public static final int` = 16.
- `SECTION_HEIGHT`: `128` → `256` (also `public`, so the world and generators
  share the constant).
- `SUBCHUNK_COUNT` = `SECTION_HEIGHT / SECTION_SIZE` = 16.
- `ENTITY_SEGMENTS`: `8` → `16`.
- `X_SHIFT` / `Z_SHIFT` (block-plane indexing): replaced by subchunk-local
  `LOCAL_X_SHIFT` / `LOCAL_Z_SHIFT`.
- `TILE_Y_SHIFT`: **unchanged at 10** (see Indexing above).

**Fields:**
- `byte[] blocks` → `byte[][] blocks` — `[subchunkIdx][x<<8|z<<4|yLocal]`
- `NibbleArray data` → `NibbleArray[] data` (block metadata, 4 bits per cell)
- `NibbleArray skyLightMap` → `NibbleArray[] skyLightMap`
- `NibbleArray blockLightMap` → `NibbleArray[] blockLightMap`

**Methods:**
- `getBlockID(x, y, z)`: read-only; a `null` subchunk returns 0 (air). Never
  allocates.
- `setBlockID(x, y, z, id)`: allocates the subchunk on first write
  (`allocateSubchunk`, which also pre-fills a fully-lit skylight plane), then
  corrects the height/skylight via `relightBlock`.
- `getBlockMetadata` / `getSavedLightValue` / `getBlockLightValue`: read-only;
  `null` subchunks return metadata 0 / skylight 15 / blocklight 0.
- `generateHeightMap()`: still one column at a time over the whole 0–255 span
  (the walk descends through the empty top half because air has opacity 0); the
  second cross-feed pass is unchanged.
- `relightBlock(x, y, z)`: unchanged logic; writes route through `setSkyLight`,
  which skips subchunks that are still implicit open sky.
- `addEntity(Entity)` / `removeEntityAtIndex(Entity, int)`: unchanged —
  `entity.posY / 16` already produces `[0, 15]`.
- `getSubchunkCount()`: number of materialized subchunks counting from the
  bottom (drives the random-tick loop).
- `writeChunkNBTData` / `readChunkNBTData`: see Save format below.

### `net.minecraft.game.world.World`

- All `y >= 128` / `y < 128` / `& 127` guards → `y >= SECTION_HEIGHT`,
  `y < SECTION_HEIGHT`, `& (SECTION_HEIGHT - 1)`.
- `updateBlocksAndPlayCaveSounds`: iterate subchunks with 10 probes each.
  LCG local extraction: `x = (lcg>>2) & 15`, `z = (lcg>>6) & 15`,
  `yLocal = (lcg>>10) & 15`; then add `subchunkIdx * 16` to get world y.
- `skylightSubtracted` formula: unchanged.

### `net.minecraft.game.world.ChunkCache`

- `y >= 128` guard → `y >= SECTION_HEIGHT`.  Subchunk lookups under the hood;
  no caller-visible API change.

### `net.minecraft.game.world.MetadataChunkBlock`

- `endY = Math.min(this.maxY, SECTION_HEIGHT - 1)`.

### `net.minecraft.game.world.biome.BiomeGenInfdev`

- **Unchanged.** The surface walk and ore-vein heights deliberately stay 128.
  `BiomeGenInfdev` writes into the flat 128-high generation buffer (block index
  `x<<11|z<<7|127`), so raising `placeOreVein`'s bound would consume extra RNG
  draws and change the RNG order — and the RNG order is load-bearing for world
  generation. Terrain is still produced only in the bottom 128 layers; the top
  half is writable by player action only.
- `WorldGenTrees` (sapling growth): the collision bound `checkY >= 128` and the
  ground bound `y >= 128 - trunkHeight - 1` become `SECTION_HEIGHT`-based, so a
  sapling on a platform at y ≈ 150 can grow its trunk into the new top half.

### `net.minecraft.game.world.chunk.ChunkProviderGenerate`

- `provideChunk` keeps its flat 32 768-byte (`new byte[-Short.MIN_VALUE]`)
  buffer, but the ordering is now explicit: the chunk is built empty (to hold
  the biome grid), `generateTerrain` / `replaceBlocks` fill the flat buffer
  (which writes column-major, `x << 11 | z << 7 | y`), and only then is
  `chunk.loadFlatBlocks(blocks)` called to slice the buffer into the bottom
  8 eager subchunks. (The first attempt sliced the buffer in the `Chunk`
  constructor — but that ran *before* the buffer was generated, so every chunk
  came out empty and the player fell to the lava at the world floor.) The upper
  8 subchunks stay `null` (air).

### `net.minecraft.game.world.terrain.ChunkProviderGenerate420`

- `blockIndex += 128` → `+= TERRAIN_HEIGHT` (a new `private static final = 128`).
  This is the terrain generation height — the flat up-sampled buffer is still
  128 high and its packing scheme (`<< 11` / `<< 7`) is tuned to it. It is
  unrelated to the new buildable `SECTION_HEIGHT`; the two must not be conflated.

### `net.minecraft.client.render.RenderGlobal`

- `renderChunksTall = 8` → `16`.  Comment at line 183 updated to 16x256x16.
- `markBlocksForUpdate`: `chunkY = blockY >> 4` (was `blockY % renderChunksTall`),
  with min/max Y clamped into `[0, renderChunksTall - 1]`.
- `maxBlockY = this.renderChunksTall` (line 175): unchanged — derived.

### `net.minecraft.game.world.MobSpawner`

- `SPAWN_VERTICAL_RANGE = SECTION_HEIGHT`.

---

## Save format

A `Height` tag distinguishes old 128-high saves from new 256-high saves.

### New format (per chunk)

```
Level {
    xPos, zPos, LastUpdate,
    Height: Int = 256,              -- missing → legacy 128
    SubchunkMask: Short,             -- bit i set → subchunk i present (0x00FF for 8-eager)
    SubchunkBlocks: List[ByteArray(4096)],
    SubchunkData: List[ByteArray(2048)],        -- block metadata nibbles
    SubchunkSkyLight: List[ByteArray(2048)],
    SubchunkBlockLight: List[ByteArray(2048)],
    HeightMap: ByteArray(256), Biomes: ByteArray(256),
    TerrainPopulated, Entities, TileEntities
}
```

`SubchunkMask` encodes which subchunks are present (not just eager vs. lazy —
future-proofed for any sparse subchunk).  The four subchunk lists are parallel
and **mask-ordered**: each list holds exactly one element per set mask bit, in
ascending subchunk order (so a fresh terrain chunk has 8 entries, subchunks
0–7). `NBTTagList` cannot hold `null` entries, so the mask carries the
"present" set and the lists omit absent subchunks rather than leaving holes.

### Legacy migration

`readChunkNBTData` detects the format by the `Height` tag: `getInteger("Height")`
returns **0** when the tag is absent (see `NBTTagCompound.java:93`) and 128 for an
older save — either maps to the legacy flat path; `256` selects the subchunk
lists. The legacy path:

1. Read flat `Blocks` (32 768 B), `Data` (16 384 B), `SkyLight` (16 384 B),
   `BlockLight` (16 384 B) arrays.
2. Split each into subchunks 0–7: each subchunk gets its 4 096 / 2 048 B
   slice.
3. Upper subchunks 8–15 are `null` (air, fully lit).
4. If the height map or any skylight plane is missing (the existing
   `isValid()` / `hasSkyPlanes` path), regenerate light via `generateHeightMap` —
   pre-seeding the present subchunks' skylight to full-bright first.

Subsequent saves write the new format.  `Height` becomes permanent on first
re-save, so a world is upgraded in place; a legacy world that has not been
re-saved since the change still reads as old format (see the level-select
screen's `(OLD)` marker).

---

## Implementation order

1. **`NibbleArray`** — index formula update to subchunk-local.
2. **`Chunk`** — subchunk storage, `getBlockID`/`setBlockID`, `heightMap`,
   lazy allocation, `readChunkNBTData`/`writeChunkNBTData` with migration,
   `checkLight` over subchunks.
3. **`ChunkProviderGenerate`** + **`ChunkProviderGenerate420`** — generate
   into 8 eager subchunks; upper 8 `null`.
4. **`World`** — guards, random tick loop over subchunks.
5. **`ChunkCache`**, **`MetadataChunkBlock`**, **`BiomeGenInfdev`**,
   **`MobSpawner`** — constant references.
6. **`RenderGlobal`** — `renderChunksTall = 16`, `chunkY = blockY >> 4`.
7. **Manual verification:**
   - New world: place a block at y = 200, verify it renders and persists.
   - Load an existing 128-high save, fly to y = 140, verify it's air.
   - Plant a sapling and crop; compare growth rate with the 128-height baseline.
   - Save/reload round-trip of a world that has blocks above y = 127.

---

## What is intentionally out of scope

- **Server / headless build:** the refactor keeps `net.minecraft.game.world`
  free of `net.minecraft.client.render` dependencies.  This constraint remains.
- **World generation above y = 127:** terrain generation continues to produce
  terrain in the bottom 128 layers.  The upper half is only writable by player
  action until a future change adds upper-terrain generation.
- **Chunk unloading optimisation:** subchunks that are entirely air could skip
  their renderer and entity bucket allocation.  This is a future win on top of
  the lazy allocation already described.

---

## Appendix: `SubChunkStorage` class — pros and cons

A true `SubChunkStorage` class would own the base-Y offset and all four
data arrays for one 16×16×16 slab:

```java
class SubChunkStorage {
    int baseY;                              // world y of subchunk bottom
    byte[] blocks;                           // 4096 bytes
    NibbleArray data;                        // 2048 bytes (block metadata)
    NibbleArray skyLight;                    // 2048 bytes
    NibbleArray blockLight;                  // 2048 bytes
    boolean isEmpty;                        // all blocks are air (computed)
}
```

### Pros

| # | Point |
|---|-------|
| 1 | **Cache locality.** Processing a full subchunk (all 4 096 block lookups during a render rebuild or light propagation walk) touches one object with four contiguous arrays. With separate `blocks[][][]` / `skyLightMap[][]` / etc., the hot inner loop must pointer-chase across three separate jagged-array dimensions. |
| 2 | **Single serialization unit.** `writeChunkNBTData` iterates the subchunk list and calls one method per subchunk. The NBT list of byte arrays maps 1:1 to the subchunk list — no separate `SubchunkMask` needed if we use a `null` entry to mean "absent". |
| 3 | **`baseY` simplifies coordinate math.** Every accessor method (`getBlockId`, `setBlockId`, etc.) on the subchunk receives only the local `(x, yLocal, z)` — world-y is always implicit via `baseY`. The `Chunk` level only needs `y >> 4` to pick the right subchunk and `y & 15` to pass to the subchunk. The alternative — passing full world `(x, y, z)` into `Chunk` and re-extracting the parts at each layer — is fine but slightly noisier. |
| 4 | **Empty subchunk singleton.** A `SubChunkStorage` where `blocks == EMPTY_BYTES` can be replaced with a shared `EMPTY` singleton. Any read from it returns `0` (air) instantly; writes allocate a real instance. This makes the `null` check in `Chunk.getBlockId` a `== EMPTY` check — same cost, more semantically meaningful. |
| 5 | **Single place for the all-air check.** `isEmpty` is computed once when the subchunk is first created (or lazily on first fill) and stored on the object. The renderer can read it directly rather than scanning the block array. Future: dirt-cheap `skipRenderPass` for subchunks with no opaque blocks. |
| 6 | **Extensibility.** Adding per-subchunk flags (e.g. `hasOpaqueBlocks`, `requiresLightingUpdate`, `isDirty`) attaches cleanly to the class without touching `Chunk`'s field layout. |
| 7 | **Natural boundary for future changes.** If a later version wants 3D noise per subchunk, or per-subchunk heightmaps, or compression, the class is the obvious home. |
| 8 | **Simpler `Chunk` code.** `Chunk` becomes a thin container — one `SubChunkStorage[16]` array plus metadata (`heightMap`, `biomes`, etc.). All block/light logic lives on the subchunk. `Chunk`'s methods shrink to two lines: index into the array, delegate to the subchunk. |

### Cons

| # | Point |
|---|-------|
| 1 | **Bigger initial refactor.** `Chunk` must be rewritten to delegate instead of holding the arrays directly. In the v1 plan (three independent arrays inside `Chunk`), `Chunk` grows three new dimensions and the arrays stay in-place. With `SubChunkStorage`, `Chunk` loses the arrays and gains a list of subchunk objects — more code to write and more ground to cover in a single step. |
| 2 | **Indirection on every block access.** The hot path — `Chunk.getBlockId` → subchunk array → `SubChunkStorage.getBlockId` — is one extra dereference. On a machine with shallow branch prediction and warm L1 cache this is negligible, but the renderer's inner `y` loop (`WorldRenderer.updateRenderer` line 160) calls `chunk.getBlockId` 16 × 16 × 16 × visibleChunks times per frame. A method-call overhead × 4096 per subchunk per frame is measurable. Mitigation: make `SubChunkStorage` a `final` class with `final` fields; the JIT can devirtualise and inline aggressively. |
| 3 | **NBT format coupling.** If `SubChunkStorage` owns its serialization, changing the internal layout of a subchunk (e.g. adding compression, or a future `NibbleArray` replacement) forces a `Height` / format version bump. With separate flat arrays in `Chunk`, adding a `SubchunkMask` to the `Chunk` level is the only required change. |
| 4 | **Two-level null check.** v1 plan: `if (subchunks[idx] == null) allocate()`. With `SubChunkStorage`: `if (subchunk.isEmpty)` for the singleton, or `if (subchunk == EMPTY)` for a singleton approach. Both are cheap, but the semantics differ from `null == no subchunk`. The empty-singleton approach requires `SubChunkStorage` to be immutable after construction — any write to an `EMPTY` subchunk must allocate a new real instance. |
| 5 | **`Chunk`–`SubChunkStorage` circular dependency risk.** If `SubChunkStorage` ever needs to call back into `Chunk` (e.g. for light propagation that spans subchunk boundaries), the circular dependency must be resolved carefully. In the v1 plan all light propagation is on `Chunk` anyway, so there is no risk. |

### Recommendation

**Do it in v2, not v1.** The `SubChunkStorage` class is the cleaner long-term design, but v1's three independent arrays minimises churn and gets the 256-height feature working with the smallest possible diff. Once the subchunk model is proven in production, extract `SubChunkStorage` as a clean extraction refactor — the kind of class that naturally emerges from the three parallel arrays already being 16-aligned. The v1 plan is specifically designed so that this extraction is a mechanical transformation, not a redesign.