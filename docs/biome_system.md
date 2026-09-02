# Biome System Reference (Infdev 20100420)

This document describes the biome plumbing as implemented. It is **not** a
design proposal — it is a reference for readers and future work. The goal of this
stage was to establish the plumbing so that real biomes can be added later by
*adding subclasses*, not by restructuring. Today exactly one biome exists
(`BiomeGenInfdev`) and the world behaves exactly as before the refactor.

> Class naming: the codebase spells the concrete generator `ChunkProviderGenerate420`
> (not `...Infdev420`).

---

## 1. Overview

Terrain variation is split between two cooperating abstractions, mirroring how
later Minecraft versions separated "which biome is here" from "what a biome
looks like":

- **`BiomeProvider`** — answers *"which biome is at a position?"* It owns no
  terrain state; it only hands out `BiomeGenerator`s by world position.
- **`BiomeGenerator`** — answers *"what terrain does this biome make?"* It owns
  the surface replacement and decoration behaviour for one biome.

Data flow during chunk generation:

```
provideChunk(chunkX, chunkZ)
  1. biomeProvider.getBiomes(x0, z0, 16, 16)  -> BiomeGenerator[256]
  2. chunk.setBiome(x, z, id) per column        -> chunk.biomes byte[256]
  3. generateTerrain(...)                        -> raw stone/water volume
  4. replaceBlocks(...)                          -> per column, biome.replaceBlocksForBiomeColumn(...)
  5. chunk.generateHeightMap()
  (later) populate(...)                          -> center biome populateOres + decorate
```

The `BiomeProvider` lives on the `WorldType` (see §5), so every world type
selects both its generator and its biome distribution.

---

## 2. The abstractions

### 2.1 `BiomeProvider` (abstract)

`net.minecraft.game.world.biome.BiomeProvider`

```java
public abstract class BiomeProvider {
    public abstract BiomeGenerator getBiome(int x, int z);
    public abstract BiomeGenerator[] getBiomes(int x0, int z0, int xSize, int zSize);
    public abstract BiomeGenerator getBiomeFromID(int id);
}
```

- `getBiome(x, z)` — the biome for a single world column.
- `getBiomes(x0, z0, xSize, zSize)` — a 2-D block of biomes covering
  `[x0, x0+xSize) × [z0, z0+zSize)`. Called with chunk origins and `16x16`; the
  returned array is **z-major**: `index = z * xSize + x`. This block form exists
  so a chunk fetches its whole grid in one call and stores it flat.
- `getBiomeFromID(id)` — resolves a stored byte id (from a chunk's biome grid)
  back to the `BiomeGenerator` it names. This is the registry lookup used by
  `World.getBiome` and by the surface pass when a loaded chunk restores its grid.

### 2.2 `BiomeGenerator` (abstract)

`net.minecraft.game.world.biome.BiomeGenerator`

```java
public abstract class BiomeGenerator {
    public abstract int getBiomeID();

    public int topBlock()    { return Block.grass.blockID; }
    public int fillerBlock() { return Block.dirt.blockID;  }

    public abstract void replaceBlocksForBiomeColumn(
            World world, Random rand,
            int chunkX, int chunkZ, int x, int z,
            byte[] blocks, int seaLevel,
            boolean sandBeach, boolean gravelBed, int dirtDepth);

    public abstract void populateOres(World world, Random rand, int baseX, int baseZ);
    public abstract void decorate(World world, Random rand, int baseX, int baseZ, double treeNoise);
}
```

- `getBiomeID()` — the byte stored in the chunk's biome grid (`BiomeGenInfdev`
  is id `0`).
- `topBlock()` / `fillerBlock()` — the surface and sub-surface blocks. They
  default to the canonical grass-over-dirt pairing and are the extension point a
  future biome overrides to change its material (sand, snow, packed stone, …).
- `replaceBlocksForBiomeColumn(...)` — stamps one `(x, z)` column of a chunk's
  block buffer. It receives the **per-column terrain noise already computed by
  the provider** (`sandBeach`, `gravelBed`, `dirtDepth`, `seaLevel`) rather than
  owning noise generators (see §3.1).
- `populateOres(...)` / `decorate(...)` — the two decoration phases invoked on
  the chunk's **center biome** (§3.2): ores first, then everything else (trees).

---

## 3. The concrete classes

### 3.1 `BiomeProviderInfdev` — the only `BiomeProvider`

`net.minecraft.game.world.biome.BiomeProviderInfdev` (final)

Always resolves to `BiomeGenInfdev.INSTANCE`:

- `getBiome(x, z)` → `BiomeGenInfdev.INSTANCE`
- `getBiomes(...)` → a `xSize * zSize` array filled with `BiomeGenInfdev.INSTANCE`
- `getBiomeFromID(id)` → `BiomeGenInfdev.INSTANCE` for any id (only one biome is
  registered, so any stored id, including unknown ones from a future save,
  resolves to the default world biome).

It is stateless, so a single shared instance is safe (the owning `WorldType` is
itself a shared singleton). This is the natural place a future noise-driven
distribution will appear: `getBiome`/`getBiomes` would consult a noise field and
return different `BiomeGenerator`s by position, and `getBiomeFromID` would use a
real id→biome table.

### 3.2 `BiomeGenInfdev` — the only `BiomeGenerator`

`net.minecraft.game.world.biome.BiomeGenInfdev` (final)

Reproduces byte-for-byte the surface and decoration behaviour that used to live
inline in `ChunkProviderGenerate420`. It is exposed as a stateless singleton
`INSTANCE` and reports `getBiomeID() == 0`.

**`replaceBlocksForBiomeColumn(...)`** — the exact top-down column walk that was
`ChunkProviderGenerate420.replaceBlocks`'s inner loop. It finds the first stone
cell below the air, picks the surface/filler blocks from the `dirtDepth` /
`sandBeach` / `gravelBed` state (plus `seaLevel` for the underwater-water rule)
and buries `dirtDepth` filler blocks under the cap. `topBlock()`/`fillerBlock()`
supply the grass/dirt defaults. The hardcoded `y >= 60 && y <= 65` beach band and
`y >= 63` cap line are preserved exactly.

**`populateOres(world, rand, baseX, baseZ)`** — the four ore passes (coal ×20,
iron ×10, gold and diamond on a chunk-local chance), moved verbatim.

**`decorate(world, rand, baseX, baseZ, treeNoise)`** — the tree line:
`treeCount = (int)(treeNoise − rand.nextDouble())`, the bonus tree roll
(`rand.nextInt(100) == 0`), and placement via `WorldGenBigTree`. `treeNoise` is
computed by the provider and passed in (see §3.2 below).

---

## 4. Loading the biome grid into a chunk

### 4.1 Filling during generation

`ChunkProviderGenerate.provideChunk` (base class) fills the grid before any
terrain is drawn:

```java
this.fillBiomeArray(chunk, chunkX, chunkZ);   // chunk.setBiome(x, z, id) per column
this.generateTerrain(chunkX, chunkZ, blocks);
this.replaceBlocks(chunkX, chunkZ, blocks, chunk);
```

`fillBiomeArray` calls `worldType.getBiomeProvider().getBiomes(chunkX << 4,
chunkZ << 4, 16, 16)` and stores one id per column. `BiomeProviderInfdev.getBiomes`
consumes **no** `Random`, so inserting this before `generateTerrain` does not
perturb the chunk's RNG stream.

### 4.2 Storage

`Chunk` holds the grid as a flat `byte[16*16]`, indexed z-major to match the
height map (`z << 4 | x`):

```java
private byte[] biomes = new byte[SECTION_SIZE * SECTION_SIZE];

public int  getBiomeID(int x, int z) { return this.biomes[z << HEIGHTMAP_Z_SHIFT | x] & 255; }
public void setBiome(int x, int z, int id) { this.biomes[z << HEIGHTMAP_Z_SHIFT | x] = (byte) id; this.isModified = true; }
```

### 4.3 Persistence

- **Save** (`Chunk.writeChunkNBTData`): `nbtTag.setByteArray("Biomes", this.biomes)`.
- **Load** (`Chunk.readChunkNBTData`): reads `getByteArray("Biomes")`. NBT's
  `getByteArray` returns a shared **empty** array when the key is absent, so if
  `length != 256` the chunk allocates a fresh zero-filled `byte[256]` — every
  cell becomes biome id `0` (`BiomeGenInfdev`). **Old save files without a
  `Biomes` tag therefore load as all-Infdev, exactly the only behaviour the
  engine knows today**, and are re-saved with the tag on the next write.

---

## 5. World access & WorldType wiring

### 5.1 `WorldType` owns the `BiomeProvider`

`WorldType` gained a final `BiomeProvider` field, an accessor, and a constructor
parameter:

```java
private final BiomeProvider biomeProvider;
public final BiomeProvider getBiomeProvider() { return this.biomeProvider; }
```

`WORLDTYPE_420` registers `new BiomeProviderInfdev()`:

```java
new WorldType(..., ChunkProviderGenerate420::new, new BiomeProviderInfdev())
```

### 5.2 `World.getBiome(x, z)`

Added to `World` (mirrors `getHeightValue`):

```java
public final BiomeGenerator getBiome(int x, int z) {
    Chunk chunk = this.getChunkFromChunkCoords(x >> 4, z >> 4);
    return this.worldType.getBiomeProvider().getBiomeFromID(chunk.getBiomeID(x & 15, z & 15));
}
```

It extracts the chunk at `(x >> 4, z >> 4)` and resolves the biome id stored at
the `(x & 15, z & 15)` cell through the world type's `BiomeProvider`.

---

## 6. Surface pass and decoration

### 6.1 Surface pass — `ChunkProviderGenerate420.replaceBlocks`

The concrete loop still lives in this class (not the base) because the
per-column surface noise generators are loaded there in a **load-bearing
construction order**:

```java
protected final void replaceBlocks(int chunkX, int chunkZ, byte[] blocks, Chunk chunk) {
    for (int x = 0; x < 16; ++x)
        for (int z = 0; z < 16; ++z) {
            double worldX = (double)((chunkX << 4) + x);
            double worldZ = (double)((chunkZ << 4) + z);
            boolean sandBeach = this.noiseGen4.generateNoiseOctaves(worldX * (1.0D/32.0D), worldZ * (1.0D/32.0D), 0.0D) + this.rand.nextDouble() * 0.2D > 0.0D;
            boolean gravelBed = this.noiseGen4.generateNoiseOctaves(worldZ * (1.0D/32.0D), 109.0134D, worldX * (1.0D/32.0D)) + this.rand.nextDouble() * 0.2D > 3.0D;
            int dirtDepth = (int)(this.noiseGen5.noiseGenerator(worldX * (1.0D/32.0D)*2.0D, worldZ * (1.0D/32.0D)*2.0D) / 3.0D + 3.0D + this.rand.nextDouble() * 0.25D);

            BiomeGenerator biome = this.worldObj.worldType.getBiomeProvider().getBiomeFromID(chunk.getBiomeID(x, z));
            biome.replaceBlocksForBiomeColumn(this.worldObj, this.rand, chunkX, chunkZ, x, z, blocks, SEA_LEVEL, sandBeach, gravelBed, dirtDepth);
        }
}
```

The provider computes the noise with its **own** `noiseGen4`/`noiseGen5` and
passes the values into the biome. This keeps the noise generator construction
order (and therefore the world seed stream) byte-identical, and lets a future
biome decide to ignore the shared inputs or vary its surface from them — without
ever owning noise state. `ChunkProviderGenerate.replaceBlocks` remains an
abstract stage (its signature now takes the `Chunk`).

### 6.2 Decoration — `ChunkProviderGenerate420.populate`

```java
public final void populate(IChunkProvider chunkProvider, int chunkX, int chunkZ) {
    this.rand.setSeed((long) chunkX * 318279123L + (long) chunkZ * 919871212L);
    int baseX = chunkX << 4;
    int baseZ = chunkZ << 4;

    BiomeGenerator center = this.worldObj.worldType.getBiomeProvider().getBiome(baseX + 8, baseZ + 8);
    center.populateOres(this.worldObj, this.rand, baseX, baseZ);

    double treeNoise = this.mobSpawnerNoise.noiseGenerator(baseX * 0.05D, baseZ * 0.05D);
    center.decorate(this.worldObj, this.rand, baseX, baseZ, treeNoise);
}
```

- Re-seeds the chunk RNG (unchanged, so decoration stays reproducible).
- Resolves the **center biome** at `(baseX + 8, baseZ + 8)` (the chunk's middle
  column) and calls its two phases: `populateOres` then `decorate`.
- The tree-count noise is read from the provider-owned `mobSpawnerNoise` and
  passed into `decorate`, following the same "pass the noise, don't hand the
  biome the generator" rule as the surface pass.

---

## 7. Package layout

New (additive) package `net.minecraft.game.world.biome`:

```
net.minecraft.game.world.biome
├── BiomeProvider.java        (abstract)
├── BiomeProviderInfdev.java  (final — always BiomeGenInfdev)
├── BiomeGenerator.java       (abstract)
└── BiomeGenInfdev.java       (final — today's default behaviour, singleton INSTANCE)
```

Existing packages are untouched except for the additions listed in §8.

---

## 8. Files touched

| File | Change |
|------|--------|
| `net/minecraft/game/world/biome/BiomeProvider.java` | new — abstract lookup interface |
| `net/minecraft/game/world/biome/BiomeGenerator.java` | new — abstract surface/decorate contract |
| `net/minecraft/game/world/biome/BiomeProviderInfdev.java` | new — always returns `BiomeGenInfdev` |
| `net/minecraft/game/world/biome/BiomeGenInfdev.java` | new — today's default behaviour |
| `net/minecraft/game/world/WorldType.java` | added `BiomeProvider` field + `getBiomeProvider()`, constructor param; `WORLDTYPE_420` passes `new BiomeProviderInfdev()` |
| `net/minecraft/game/world/chunk/Chunk.java` | added `biomes` byte[256] grid, `getBiomeID`/`setBiome`; save/load `Biomes` NBT tag with all-Infdev fallback |
| `net/minecraft/game/world/World.java` | added `getBiome(x, z)` |
| `net/minecraft/game/world/terrain/ChunkProviderGenerate.java` | `provideChunk` fills the biome grid; `replaceBlocks` signature now takes the `Chunk` |
| `net/minecraft/game/world/terrain/ChunkProviderGenerate420.java` | `replaceBlocks` delegates each column to the biome; `populate` delegates to the center biome's `populateOres`/`decorate` |
| `README.md` | diary entry |

---

## 9. Behaviour fidelity

1. **Surface noise** stays in `ChunkProviderGenerate420` — the `noiseGen1..mobSpawnerNoise`
   RNG construction order is untouched, so terrain/tree values are unchanged.
2. **Surface stamping** yields identical blocks: the sand/gravel/dirt/grass logic
   moved verbatim into `BiomeGenInfdev.replaceBlocksForBiomeColumn`, given the
   same inputs it used to compute inline.
3. **Decoration** runs sequentially (`populateOres` then `decorate`), preserving
   the order of `rand` consumption; the tree-count noise is still read from
   `mobSpawnerNoise`.
4. **Old saves** load as all-`BiomeGenInfdev` — the only biome the engine has.

Verified by a full-tree `javac -source 1.8 -target 1.8` compile (EXIT 0).

---

## 10. Extending later (out of scope today)

- **A real biome** — add a `BiomeGenerator` subclass overriding `topBlock()` /
  `fillerBlock()` (different material) and/or `replaceBlocksForBiomeColumn`,
  `populateOres`, `decorate`; give it a unique `getBiomeID()`.
- **A real distribution** — make `BiomeProviderInfdev.getBiome`/`getBiomes`
  noise-driven and `getBiomeFromID` map a table of registered ids to
  `BiomeGenerator`s.
- **Respect `WorldOptions.generateBiomes`** — currently an unused placeholder
  flag; gate biome generation on it per world if desired.
