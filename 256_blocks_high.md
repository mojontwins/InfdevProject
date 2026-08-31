# Extending World Height to 256 Blocks

> **Status:** Planned — not yet implemented.
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
byte[]   blocks       — 4096 bytes
NibbleArray skyLight  — 2048 bytes
NibbleArray blockLight — 2048 bytes
```

Subchunk index `s` covers world y = `s * 16` to `s * 16 + 15`.

| Subchunks | y range    | When allocated |
|-----------|------------|----------------|
| 0–7       | 0–127      | Always, on chunk load |
| 8–15      | 128–255    | Lazily, on first block read or write above y=127 |

The flat arrays (`byte blocks[32768]`, `NibbleArray skyLightMap`,
`NibbleArray blockLightMap`) are removed from `Chunk` and replaced with
the subchunk structure.

**Memory:** 8-eager subchunks = 8 × 6 144 B = ~49 152 B for blocks+lights,
matching today's ~49 152 B.  Lazy upper half costs nothing until used.

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

---

## Performance

### Random block ticks

**Goal:** a crop/sapling/grass cell receives `updateTick` at the same
average rate as today.

|                    | Today        | 16 subchunks, 10 probes/subchunk |
|--------------------|--------------|----------------------------------|
| Cells per subchunk | 16 × 16 × 16 = 4 096 | same |
| Probes per chunk   | 80 (flat)   | 10 × 16 = 160                    |
| Pick probability   | 80 / 32 768 ≈ 0.244 % | 10 / 4 096 ≈ 0.244 % |
| Pick rate          | **identical** | **identical** |

The tick loop in `World.updateBlocksAndPlayCaveSounds` iterates subchunks:
for each non-null subchunk it performs 10 LCG-local probes
(`x = (lcg>>2) & 15`, `z = (lcg>>6) & 15`, `yLocal = (lcg>>10) & 15`).
No global budget juggling; the budget scales automatically with the number of
present subchunks.

### Memory

|                          | Today (128 flat) | 8 eager subchunks | 16 subchunks (fully built) |
|--------------------------|-------------------|-------------------|---------------------------|
| Blocks + lights / chunk  | 32 768 + 32 768 = 65 536 B | 49 152 B | 98 304 B |
| Entities / chunk         | 8 × List overhead  | 8 × overhead     | 16 × overhead            |
| 17×17 loaded area        | ~37 MB            | ~19 MB            | ~37 MB                   |

**Result:** new worlds start at ~19 MB (lower than today).  Fully explored
worlds return to ~37 MB (parity).  Memory is never worse than today.

### Rendering

`renderChunksTall = 8` → `16`.  Each chunk column now has 16 `WorldRenderer`
instances instead of 8, each owning 3 GL call lists.

Mitigations:
- Subchunks that are entirely air set `skipRenderPass[0] = skipRenderPass[1] = true`
  on allocation, so the renderer skips GL upload for them entirely.
- Frustum culling discards the upper layers of distant chunks when looking
  horizontally.
- `RenderGlobal.markBlocksForUpdate` computes `chunkY = blockY >> 4` directly
  (no iteration needed) — the subchunk index maps 1:1 to the renderer index.

---

## Files that change

### `net.minecraft.game.world.chunk.NibbleArray`

- Index formula: `x << 8 | z << 4 | yLocal` (was `x << 11 | z << 7 | y`).
- Constructor called with `cellCount = 4096` (16 × 16 × 16) per subchunk.

### `net.minecraft.game.world.chunk.Chunk`

**Constants:**
- `SECTION_HEIGHT`: `128` → `256`
- `ENTITY_SEGMENTS`: `8` → `16`
- `X_SHIFT` / `Z_SHIFT`: removed (replaced by subchunk-local index)
- `TILE_Y_SHIFT`: `10` → `11` (tile key needs `1 << 11 = 2048` buckets)

**Fields:**
- `byte[] blocks` → `byte[][][] blocks` — `[subchunkIdx][x][z*16+yLocal]`
- `NibbleArray skyLightMap` → `NibbleArray[][] skyLightMap`
- `NibbleArray blockLightMap` → `NibbleArray[][] blockLightMap`
- `heightMap` sentinel: keep `-1` (signed byte; valid range 0–255 unchanged)

**Methods:**
- `getBlockID(x, y, z)`: branch on `subchunkIdx = y >> 4`, allocate lazy
  subchunk on read if `y >= 128 && subchunks[subchunkIdx] == null`.
- `setBlockID(x, y, z, id)`: same, allocate on write.
- `generateHeightMap()`: walk `subchunkIdx` from 15 down to 0, then within
  subchunk from 15 down to 0.
- `relightBlock(x, y, z)`: walk subchunks.
- `checkLight(x, y, z)`: unchanged logic, walks subchunks.
- `addEntity(Entity)` / `removeEntityAtIndex(Entity, int)`: unchanged —
  `entity.posY / 16` already produces `[0, 15]`.
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

- Surface walk: start at `SECTION_HEIGHT - 1` (255).  Ore veins up to
  `SECTION_HEIGHT` (256).

### `net.minecraft.game.world.chunk.ChunkProviderGenerate`

- `blocks` buffer stride: `SECTION_SIZE * SECTION_SIZE = 256` per subchunk.
  Allocate 8 subchunks, fill; leave upper 8 as `null` (air).

### `net.minecraft.game.world.terrain.ChunkProviderGenerate420`

- `blockIndex += SECTION_HEIGHT` (was literal 128) → `+= SECTION_SIZE`.

### `net.minecraft.client.render.RenderGlobal`

- `renderChunksTall = 8` → `16`.  Comment at line 183 updated.
- `markBlocksForUpdate`: `chunkY = blockY >> 4` (was `blockY % renderChunksTall`).
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
    SubchunkSkyLight: List[ByteArray(2048)],
    SubchunkBlockLight: List[ByteArray(2048)],
    HeightMap: ByteArray(256), Biomes: ByteArray(256),
    TerrainPopulated, Entities, TileEntities
}
```

`SubchunkMask` encodes which subchunks are present (not just eager vs. lazy —
future-proofed for any sparse subchunk).

### Legacy migration

`readChunkNBTData` when `Height` tag is missing or equals 128:

1. Read flat `Blocks` (32 768 B) into a temporary array.
2. Split into subchunks 0–7: each subchunk gets its 4 096 B slice.
3. Upper subchunks 8–15 are `null` (air).
4. Regenerate `SkyLight` / `BlockLight` via existing `checkLight` path.

Subsequent saves write the new format.  `Height` tag becomes permanent on
first re-save.

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
    byte[] blocks;                          // 4096 bytes
    NibbleArray data;                       // 2048 bytes (block metadata)
    NibbleArray skyLight;                   // 2048 bytes
    NibbleArray blockLight;                 // 2048 bytes
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