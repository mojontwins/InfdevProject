# Biome System Design (Infdev 20100420)

This document describes how the engine will be prepared to support biomes in the
future. Nothing here is implemented yet — it is the design to review and refine
before any code is written. The goal of this stage is to **establish the
plumbing**, not to add real biome variety. Only one biome exists today
(`BiomeGenInfdev`), and everything is wired so that adding real biomes later
means adding subclasses, not restructuring.

> Note on naming: this document uses the actual class name `ChunkProviderGenerate420`
> (the class the user sometimes calls `ChunkProviderGenerateInfdev420`). The public
> name is preserved per `AGENTS.md`.

---

## 1. The two abstractions

The design splits terrain variation into two cooperating pieces, mirroring how
later Minecraft versions separated "which biome is here" from "what a biome
looks like".

### 1.1 `BiomeProvider` (abstract)

Answers the question **"which biome is at a position?"** It has no terrain
state of its own.

```java
public abstract class BiomeProvider {
    /** The biome at a single world column (x, z). */
    public abstract BiomeGenerator getBiome(int x, int z);

    /**
     * A 2-D block of biomes covering [x0, x0+xSize) x [z0, z0+zSize).
     * Called with chunk origins and 16x16 sizes. The returned array is
     * indexed z-major: index = z * xSize + x.
     */
    public abstract BiomeGenerator[] getBiomes(int x0, int z0, int xSize, int zSize);

    /** Resolves a stored biome id (byte in the chunk) back to a BiomeGenerator. */
    public abstract BiomeGenerator getBiomeFromID(int id);
}
```

The array form exists because a chunk wants one contiguous lookup: when a chunk
is generated, `provideChunk` asks the provider for a whole 16×16 block at once,
so the chunk can persist it as a flat array (see §4).

### 1.2 `BiomeGenerator` (abstract)

Answers the question **"what terrain does this biome make?"** It owns the
surface replacement and decoration behavior for a biome.

```java
public abstract class BiomeGenerator {
    /** The byte id stored in the chunk. BiomeGenInfdev is id 0. */
    public abstract int getBiomeID();

    /** The surface block (defaults to Block.grass.blockID). */
    public int topBlock() { return Block.grass.blockID; }

    /** The block under the surface (defaults to Block.dirt.blockID). */
    public int fillerBlock() { return Block.dirt.blockID; }

    /**
     * Replaces the surface of one (x, z) column of a chunk.
     * The chunk-provider computes the per-column terrain noise and passes it in
     * so the biome does not own the noise generators (see §3.1).
     */
    public abstract void replaceBlocksForBiomeColumn(
        World world, Random rand,
        int chunkX, int chunkZ, int x, int z,
        byte[] blocks, int seaLevel,
        boolean sandBeach, boolean gravelBed, int dirtDepth);

    /** Drops the ore veins for the chunk this biome decorates. */
    public abstract void populateOres(World world, Random rand, int baseX, int baseZ);

    /** Places everything else (trees, …) for the chunk's center biome. */
    public abstract void decorate(World world, Random rand, int baseX, int baseZ, double treeNoise);
}
```

`topBlock`/`fillerBlock` are the canonical vanilla defaults (grass over dirt)
and are overridable by future biomes.

---

## 2. The two concrete classes

### 2.1 `BiomeGenInfdev` (the only `BiomeGenerator` today)

Reproduces exactly the surface and decoration behavior that
`ChunkProviderGenerate420` already performs today.

- `replaceBlocksForBiomeColumn` — the current surface/beach/gravel/dirt logic
  (grass above sea level, sand beaches, gravel beds, bare stone or water
  underwater), using the `sandBeach`, `gravelBed`, `dirtDepth` and `seaLevel`
  values passed in.
- `populateOres` — the current coal/iron/gold/diamond vein placement.
- `decorate` — the current tree placement.
- `topBlock` → `Block.grass.blockID`, `fillerBlock` → `Block.dirt.blockID`
  (unchanged from the defaults).

### 2.2 `BiomeProviderInfdev` (the only `BiomeProvider` today)

Always returns `BiomeGenInfdev`:

- `getBiome(x, z)` → `BiomeGenInfdev`
- `getBiomes(...)` → a block filled with `BiomeGenInfdev`
- `getBiomeFromID(id)` → `BiomeGenInfdev` for any id (only one biome is
  registered, id 0, so any stored id resolves to it).

---

## 3. Wiring into the chunk provider

### 3.1 Surface pass — `replaceBlocks`

Today `ChunkProviderGenerate420.replaceBlocks` walks every column, computes the
per-column surface noise from its own private `noiseGen4`/`noiseGen5` generators
(plus `rand`), and stamps the surface directly into the block array.

After the change:

- `ChunkProviderGenerate.provideChunk` fills the chunk's 16×16 biome array from
  the `BiomeProvider` (see §4) and then calls `replaceBlocks(chunkX, chunkZ,
  blocks, chunk)`.
- `ChunkProviderGenerate420.replaceBlocks` keeps its loop and keeps computing
  `sandBeach` / `gravelBed` / `dirtDepth` from **its own** `noiseGen4`/`noiseGen5`
  (these stay private to the provider), but for each column it looks up that
  column's biome from the chunk and delegates the actual block stamping to:

  ```java
  biome.replaceBlocksForBiomeColumn(worldObj, rand, chunkX, chunkZ, x, z,
                                    blocks, SEA_LEVEL, sandBeach, gravelBed, dirtDepth);
  ```

**Why the loop lives in the provider and not the base class:** the surface noise
generators are constructed in a load-bearing order inside `ChunkProviderGenerate420`'s
constructor. Removing them would change the seed stream and therefore the terrain
and tree placement. So the noise stays provider-owned and is **passed into** the
biome — a future biome can then decide to ignore it or vary its surface using the
same inputs. `ChunkProviderGenerate.replaceBlocks` remains an abstract stage.

### 3.2 Decoration — `populate`

`ChunkProviderGenerate420.populate(chunkProvider, chunkX, chunkZ)`:

1. Re-seeds the chunk `rand` (unchanged, so decoration stays reproducible).
2. Computes the chunk's base origin `baseX = chunkX << 4`, `baseZ = chunkZ << 4`.
3. Resolves the **center biome**: `getBiome(baseX + 8, baseZ + 8)`.
4. Calls `biome.populateOres(worldObj, rand, baseX, baseZ)`.
5. Calls `biome.decorate(worldObj, rand, baseX, baseZ, treeNoise)` where
   `treeNoise = mobSpawnerNoise.noiseGenerator(baseX * 0.05, baseZ * 0.05)`.

**Behavior fidelity:** the ore logic (steps that consume `rand` via
`nextInt`) runs first in `populateOres`, then the tree logic in `decorate` —
splitting sequentially preserves the exact order of `rand` consumption. The tree
count noise is computed by the provider (which still owns `mobSpawnerNoise`) and
passed in, following the same "pass the noise, don't give the biome the
generator" rule as the surface pass.

`decorate` then does `treeCount = (int)(treeNoise − rand.nextDouble())`, the
bonus tree roll, and places the trees — exactly the current behavior.

---

## 4. Per-chunk biome storage (Chunk)

Each `Chunk` stores the 16×16 biome grid as a flat `byte[]` of biome ids,
256 bytes, indexed z-major (`z << 4 | x`, matching the height map convention):

```java
private byte[] biomes = new byte[16 * 16];

public int getBiomeID(int x, int z) { return this.biomes[z << 4 | x] & 255; }
public void setBiome(int x, int z, int id) {
    this.biomes[z << 4 | x] = (byte) id;
    this.isModified = true;
}
```

### 4.1 Filling during generation

`ChunkProviderGenerate.provideChunk` fills the array before any terrain is
drawn:

```java
BiomeGenerator[] biomes = this.worldObj.worldType.getBiomeProvider()
        .getBiomes(chunkX << 4, chunkZ << 4, 16, 16);
for (int x = 0; x < 16; x++)
    for (int z = 0; z < 16; z++)
        chunk.setBiome(x, z, biomes[z << 4 | x].getBiomeID());
```

### 4.2 Persistence

The array is serialized with the rest of the chunk data:

- **Save** (`writeChunkNBTData`): `nbtTag.setByteArray("Biomes", this.biomes)`.
- **Load** (`readChunkNBTData`): read `getByteArray("Biomes")`. NBT's
  `getByteArray` returns an empty (shared) array when the key is absent, so if
  `length != 256` the chunk initializes a fresh zero-filled `byte[256]` — all
  `BiomeGenInfdev` (id 0). This keeps **old save files fully backward
  compatible** (no `Biomes` tag → every column is `BiomeGenInfdev`, which is the
  only behavior the engine knows today).

---

## 5. World access & WorldType

### 5.1 `World.getBiome(x, z)`

Added to `World` (mirrors `getHeightValue`):

```java
public final BiomeGenerator getBiome(int x, int z) {
    Chunk chunk = this.getChunkFromChunkCoords(x >> 4, z >> 4);
    return this.worldType.getBiomeProvider().getBiomeFromID(chunk.getBiomeID(x & 15, z & 15));
}
```

It extracts the chunk at `(x>>4, z>>4)` and reads the biome at `(x&15, z&15)`
from the chunk's stored grid.

### 5.2 `BiomeProvider` is part of `WorldType`

`WorldType` grows a `BiomeProvider`:

```java
private final BiomeProvider biomeProvider;

public BiomeProvider getBiomeProvider() { return this.biomeProvider; }
```

The constructor gains a `BiomeProvider` parameter, and the single `WORLDTYPE_420`
registers `new BiomeProviderInfdev()`. Because `WorldType` instances are shared
singletons and `BiomeProviderInfdev` is stateless, a single shared instance is
safe and requires no factory ceremony. A future world type can register its own
`BiomeProvider`.

---

## 6. Package layout

New package `net.minecraft.game.world.biome` (additive — no existing package is
restructured):

```
net.minecraft.game.world.biome
├── BiomeProvider.java        (abstract)
├── BiomeProviderInfdev.java  (concrete — always BiomeGenInfdev)
├── BiomeGenerator.java       (abstract)
└── BiomeGenInfdev.java       (concrete — today's default behavior)
```

---

## 7. Files touched (summary)

| File | Change |
|------|--------|
| `biome/BiomeProvider.java` | **new** — abstract lookup interface |
| `biome/BiomeGenerator.java` | **new** — abstract surface/decorate contract |
| `biome/BiomeProviderInfdev.java` | **new** — always returns `BiomeGenInfdev` |
| `biome/BiomeGenInfdev.java` | **new** — today's default behavior |
| `world/WorldType.java` | add `BiomeProvider` field + accessor; constructor param |
| `world/chunk/Chunk.java` | add `biomes` byte array; accessors; save/load `Biomes` |
| `world/World.java` | add `getBiome(x, z)` |
| `world/terrain/ChunkProviderGenerate.java` | fill chunk biomes in `provideChunk`; thread `Chunk` into `replaceBlocks` |
| `world/terrain/ChunkProviderGenerate420.java` | `replaceBlocks` → delegate to biome; `populate` → center biome `populateOres`/`decorate` |
| `README.md` | diary entry |

---

## 8. Behavior fidelity checklist

1. **Surface noise** stays in `ChunkProviderGenerate420` — RNG construction order
   of `noiseGen1..mobSpawnerNoise` is untouched, so terrain/tree values do not
   change.
2. **Surface stamping** yields identical blocks: the exact sand/gravel/dirt/grass
   logic is moved verbatim into `BiomeGenInfdev.replaceBlocksForBiomeColumn`,
   given the same inputs it used to compute inline.
3. **Decoration** runs sequentially (`populateOres` then `decorate`), preserving
   the order of `rand` consumption; the tree-count noise is still read from the
   provider-owned `mobSpawnerNoise`.
4. **Old saves** load as all-`BiomeGenInfdev` — the only biome the engine has.

---

## 9. Future directions (out of scope today)

- Add real biomes as `BiomeGenerator` subclasses (desert/sand, forest, ocean…)
  by defaulting `topBlock`/`fillerBlock` and overriding `replaceBlocksForBiomeColumn`
  / `populateOres` / `decorate`.
- Give `BiomeProviderInfdev` a real distribution (noise-driven `getBiome`/`getBiomes`)
  instead of always returning `BiomeGenInfdev`.
- Honors `WorldOptions.generateBiomes` (currently an unused placeholder flag) to
  gate biome generation on a per-world basis.
