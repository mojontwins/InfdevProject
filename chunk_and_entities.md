# Chunk Lifecycle and Entity Management

This document describes how Infdev 20100420 manages the world in memory: how chunks are loaded, cached, and evicted, and how entities are tracked, ticked, and cleaned up when their containing chunk leaves the active area.

---

## 1. Chunk Loading Pipeline

### The two-layer provider stack

World chunks are served by a two-layer provider chain:

```
World
  └── ChunkProviderLoadOrGenerate  (disk-backed cache + eviction)
        └── ChunkProviderGenerate  (procedural terrain generator)
              └── ChunkProviderGenerate420  (Infdev 20100420 terrain)
```

`World.getChunkFromChunkCoords()` delegates to `IChunkProvider.provideChunk()`, which routes through both layers.

### Layer 1 — `ChunkProviderGenerate`

The bottom layer builds terrain from fractal noise. It is **stateless**: every call to `provideChunk(chunkX, chunkZ)` re-generates the same terrain deterministically using the world's random seed. It never holds a chunk in memory.

```java
// ChunkProviderGenerate.provideChunk
public final Chunk provideChunk(int chunkX, int chunkZ) {
    this.rand.setSeed(chunkX * 341873128712L + chunkZ * 132897987541L);
    byte[] blocks = new byte[32768];
    Chunk chunk = new Chunk(this.worldObj, blocks, chunkX, chunkZ);
    this.generateTerrain(chunkX, chunkZ, blocks);   // stone/water volume
    this.replaceBlocks(chunkX, chunkZ, blocks);     // surface layer
    chunk.generateHeightMap();
    return chunk;
}
```

The three stages in detail:

1. **`initializeNoiseField`** — samples six-octave Perlin noise over a coarse 5×5×17 grid (one sample per 4-block corner of the chunk). Three octave banks are blended to produce a deterministic height value for each grid point.

2. **`generateTerrain`** — tri-linearly up-samples the coarse grid into the full 16×16×128 block array. Cells below sea level (64) become water; above become stone.

3. **`replaceBlocks`** — walks every 16×16 column top-down, carving the surface layer: sand on beaches, dirt under grass, gravel beds, bare stone under water.

Decoration (ore veins, trees) does **not** live here. It runs in `populate()`, deferred by the pipeline until all four neighbours of a chunk are generated, so ores and trees see a complete terrain.

### Layer 2 — `ChunkProviderLoadOrGenerate`

The top layer adds persistence and a fixed-size in-memory cache:

```java
private static final int CACHE_MASK = 31;          // 32 slots per axis
private static final int CACHE_BITS = 5;
private Chunk[] chunks = new Chunk[1024];          // 32×32 open-addressed ring
```

**Slot formula:** `slot = (chunkX & 31) | (chunkZ & 31) << 5`

Chunks are located by this slot key. A cache hit returns the cached chunk immediately. A cache miss triggers eviction of whatever occupies the slot.

---

## 2. Chunk Eviction (Unloading)

### When does eviction happen?

Chunks are evicted **only** through `provideChunk()` — there is no timer-based or distance-based eviction loop. Each call to `provideChunk()` may evict the current occupant of its slot if a different chunk is requested:

```java
public final Chunk provideChunk(int chunkX, int chunkZ) {
    int slot = chunkSlot(chunkX, chunkZ);
    if (!this.chunkExists(chunkX, chunkZ)) {
        if (this.chunks[slot] != null) {
            this.chunks[slot].unloadEntities();    // ← entities stripped from World
            this.saveChunk(this.chunks[slot]);      // ← written to c.x.z.dat
        }
        Chunk chunk = this.loadChunk(chunkX, chunkZ);
        if (chunk == null) {
            chunk = this.chunkProvider.provideChunk(chunkX, chunkZ);
        }
        this.chunks[slot] = chunk;
        if (chunk != null) {
            chunk.loadEntities();                   // ← entities rejoin World
        }
        ...
    }
    return this.chunks[slot];
}
```

The `unload100OldestChunks()` method (called from `World.tick()`) is a **no-op stub** in this version — both the generating layer and the cache layer return `false` and perform no actual eviction.

### What gets evicted?

Because the slot key uses only the **low 5 bits** of each coordinate, every 32×32 tile of chunk space maps to the same slot. The ring is effectively an LRU cache driven by hash collisions:

- As the player moves, newly needed chunks load into slots
- The previous occupant (if any) is evicted — saved to disk and its entities unloaded
- Chunks near the player are frequently re-requested and stay in the cache
- Chunks far from the player are evicted when their slot is needed for a closer chunk

### Why the 1024-slot ring?

32 × 32 = 1024 slots. The 5-bit coordinate folding means each slot represents one of 64 possible chunk coordinates (2^6 in each axis, masked to the 5-bit slot). This provides a reasonable working set for the area the player traverses without requiring a full world-length cache.

---

## 3. Entity Management

### The three entity stores

| Store | Type | Purpose |
|---|---|---|
| `World.loadedEntityList` | `ArrayList<Entity>` | Master list; iterated every tick in `levelEntities()` |
| `Chunk.entities[y >> 4]` | `List<Entity>[8]` | Per-chunk, per-vertical-segment entity lists for AABB queries |
| `World.loadedTileEntityList` | `ArrayList<TileEntity>` | Tile entities (furnaces, chests); updated every tick |

### How an entity joins the world

```java
// World.spawnEntityInWorld — called when an entity is created or respawned
public final void spawnEntityInWorld(Entity entity) {
    int chunkX = MathHelper.floor_double(entity.posX / 16.0D);
    int chunkZ = MathHelper.floor_double(entity.posZ / 16.0D);
    if (!this.chunkExists(chunkX, chunkZ)) {
        System.out.println("Failed to add entity " + entity);
    } else {
        this.getChunkFromChunkCoords(chunkX, chunkZ).addEntity(entity);  // → chunk entity list
        this.loadedEntityList.add(entity);                             // → master list
        this.updateEntityCountOnAdd(entity);                           // → monster/animal counters
        for (IWorldAccess access : this.worldAccesses) {
            access.obtainEntitySkin(entity);                           // → renderer textures
        }
    }
}
```

`Chunk.addEntity()` places the entity in one of eight vertical segments (each 16 blocks tall: `[0..15], [16..31], ..., [112..127]`), keyed by `y >> 4`.

### The per-tick entity loop — `levelEntities()`

Every tick, `World.tick()` calls `levelEntities()`, which iterates `loadedEntityList`:

```java
public final void levelEntities() {
    Entity player = this.playerEntity;
    double px = player.posX, py = player.posY, pz = player.posZ;

    for (int i = 0; i < this.loadedEntityList.size(); ++i) {
        Entity entity = this.loadedEntityList.get(i);

        if (!entity.isDead) {
            // ── Distance culling ──────────────────────────────────────
            double dx = entity.posX - px;
            double dy = entity.posY - py;
            double dz = entity.posZ - pz;
            if (dx*dx + dy*dy + dz*dz > ENTITY_VIEW_DISTANCE_SQ) {
                // Beyond ~45 blocks: skip onUpdate(), but advance timers.
                entity.ticksExisted++;
                if (entity instanceof EntityItem) {
                    ((EntityItem) entity).age++;   // despawn countdown
                }
                continue;
            }

            // ── Full update (nearby entities) ────────────────────────
            int oldChunkX = MathHelper.floor_double(entity.posX / 16.0D);
            int oldChunkY = MathHelper.floor_double(entity.posY / 16.0D);
            int oldChunkZ = MathHelper.floor_double(entity.posZ / 16.0D);

            entity.onUpdate();   // physics, AI, water, fire, etc.

            // ── Chunk migration ──────────────────────────────────────
            int newChunkX = MathHelper.floor_double(entity.posX / 16.0D);
            int newChunkY = MathHelper.floor_double(entity.posY / 16.0D);
            int newChunkZ = MathHelper.floor_double(entity.posZ / 16.0D);

            if (oldChunkX != newChunkX || oldChunkY != newChunkY || oldChunkZ != newChunkZ) {
                if (this.chunkExists(oldChunkX, oldChunkZ)) {
                    this.getChunkFromChunkCoords(oldChunkX, oldChunkZ)
                        .removeEntityAtIndex(entity, oldChunkY);
                }
                if (this.chunkExists(newChunkX, newChunkZ)) {
                    this.getChunkFromChunkCoords(newChunkX, newChunkZ)
                        .addEntity(entity);
                } else {
                    entity.isDead = true;   // entity wandered out of the world
                }
            }
        }

        // ── Cleanup dead entities ──────────────────────────────────────
        if (entity.isDead) {
            int deadChunkX = MathHelper.floor_double(entity.posX / 16.0D);
            int deadChunkZ = MathHelper.floor_double(entity.posZ / 16.0D);
            if (this.chunkExists(deadChunkX, deadChunkZ)) {
                this.getChunkFromChunkCoords(deadChunkX, deadChunkZ)
                    .removeEntityAtIndex(entity, MathHelper.floor_double(entity.posY / 16.0D));
            }
            this.loadedEntityList.remove(i--);
            this.updateEntityCountOnRemove(entity);   // monster/animal counters
            for (IWorldAccess access : this.worldAccesses) {
                access.releaseEntitySkin(entity);
            }
        }
    }

    // Tile entities (furnaces, etc.) — always ticked regardless of distance
    for (int i = 0; i < this.loadedTileEntityList.size(); ++i) {
        this.loadedTileEntityList.get(i).updateEntity();
    }
}
```

#### Distance culling — `ENTITY_VIEW_DISTANCE_SQ`

Entities farther than `sqrt(2048) ≈ 45` blocks from the player skip `onUpdate()`. This saves the expensive AI tick (200 random block samples + A* pathfinding for each creature) for distant mobs.

Timers are still advanced so that:
- `ticksExisted` keeps incrementing (life tracking)
- `EntityItem.age` reaches 6000 and triggers despawn even for far-away dropped items

The radius is stored as a squared value (`ENTITY_VIEW_DISTANCE_SQ = 2048.0D`) so the distance check is a single multiply-add without a `sqrt`.

#### Chunk migration

When an entity moves to a different chunk coordinate, it is removed from the old chunk's entity list and added to the new one. This keeps the per-chunk AABB queries (`Chunk.getEntitiesWithinAABBForEntity()`) accurate — the renderer and physics system only search chunks relevant to the player's vicinity.

If the destination chunk doesn't exist, the entity is marked dead.

#### Entity cleanup

When `entity.isDead` is `true` (set by the entity itself during `onUpdate()`, or by chunk migration failure), the entity is removed from its chunk's list, the master `loadedEntityList`, and the cached monster/animal counters. The renderer is notified to release the entity's texture.

### What `onUpdate()` does per entity type

| Type | Key work |
|---|---|
| `Entity` (base) | `ticksExisted++`, fire/lava damage, water swimming, fall distance |
| `EntityItem` | gravity, block-push-out of solid cells, pickup/despawn at age 6000 |
| `EntityCreature` | **200 random block samples**, A\* pathfinding to wander/target player |
| `EntityLiving` | movement input, health/damage, mounting |
| `EntityPlayerSP` | keyboard/mouse input forwarding |
| `EntityArrow` | physics, collision with blocks/entities |

The distance culling primarily benefits `EntityCreature` subclasses (animals, monsters), whose AI is the most computationally expensive.

---

## 4. The Connection: Chunk Unloading and Entity Cleanup

When a chunk is evicted from the 1024-slot ring cache (`ChunkProviderLoadOrGenerate.provideChunk()`):

1. **`chunk.unloadEntities()`** is called — iterates the chunk's 8 vertical entity segments and calls `World.unloadEntities()` on each
2. **`World.unloadEntities()`** removes every entity from `loadedEntityList` and the monster/animal counters, and notifies world accesses
3. **`saveChunk()`** serializes the chunk to `c.x.z.dat` on disk

```java
// Chunk.unloadEntities
public final void unloadEntities() {
    this.worldObj.loadedTileEntityList.removeAll(this.chunkTileEntityMap.values());
    for (List<Entity> segment : this.entities) {
        this.worldObj.unloadEntities(segment);
    }
}

// World.unloadEntities
public final void unloadEntities(List<Entity> entities) {
    this.loadedEntityList.removeAll(entities);        // ← O(N²) for large lists
    entities.forEach(this::updateEntityCountOnRemove);
    for (IWorldAccess access : this.worldAccesses) {
        for (Entity e : entities) {
            access.releaseEntitySkin(e);
        }
    }
}
```

### Important: entities stay in the world even if their chunk is unloaded

The ring cache eviction saves entities to disk, but **entities are only truly removed from `loadedEntityList` during `levelEntities()`** — when the dead flag is set, or when `unloadEntities()` is called. The chunk-local entity lists are per-segment `ArrayList`s; they do not prevent the master list from growing.

The 1024-slot ring caps the number of loaded chunks, which indirectly caps the number of entities in `loadedEntityList`, because entities are added to the master list only when their chunk is loaded, and removed when the chunk is evicted.

---

## 5. Performance Considerations

### What scales well

- **Chunk generation** — deterministic, stateless, no global state
- **Chunk cache** — fixed 1024 slots, O(1) slot lookup, O(1) eviction
- **Per-chunk AABB queries** — only the 8–9 chunks covering an AABB are searched

### What does not scale well

- **`loadedEntityList.removeAll()`** — O(N²) on entity list size. Every chunk eviction scans the entire entity list. For worlds with thousands of entities, this is the most expensive part of chunk unloading. A mark-and-sweep approach (mark entities dead, defer removal to `levelEntities()`) would eliminate this.
- **`levelEntities()` full iteration** — all entities in `loadedEntityList` are visited every tick, even distant ones. The distance culling gates `onUpdate()` but not the loop overhead. A spatial index (e.g. grid-based bucketing keyed by chunk coordinate) would allow skipping entire chunks worth of entities.
- **Entity AI for distant mobs** — the 200-sample wander pathfinding runs even for creatures the player will never see. The distance culling gates this, but entities between 45 and 256 blocks away still receive a full AI tick.
- **`unload100OldestChunks()` is a no-op** — there is no timer-based age eviction. The only eviction happens through cache collisions in `provideChunk()`. A true age-based unloading pass (which the method name implies) would help cap memory use for long play sessions.

---

## 6. Glossary

| Term | Definition |
|---|---|
| **Chunk** | A 16×128×16 block volume. The unit of world storage, generation, and loading. |
| **Slot** | A 32×32 open-addressed hash ring slot in `ChunkProviderLoadOrGenerate`. |
| **Entity** | Any non-block game object with a position: players, mobs, items, arrows, TNT. |
| **Tile entity** | A block-scoped state object: furnace, chest, sign. Persisted inside the chunk file. |
| **Chunk migration** | Moving an entity from one chunk's entity list to another when it crosses a chunk boundary. |
| **`loadedEntityList`** | The master flat list of all active entities in the world tick. |
| **`levelEntities()`** | The per-tick loop that updates all entities, handles chunk migration, and cleans up dead ones. |
| **Distance culling** | Skipping `onUpdate()` (but not timer advancement) for entities beyond `ENTITY_VIEW_DISTANCE_SQ`. |
| **Populate** | The deferred decoration pass (ores, trees) that runs once all four neighbours of a chunk exist. |
