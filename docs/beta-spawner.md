# Beta-Spawner: Eligible-Chunks Mob Spawning Plan

**Status:** Implemented (see README diary 2026-09-03)
**Branch:** `feature/modern-ai`

> **Naming note:** the plan below was written proposing a new `SpawnerAnimals`
> class replacing `MobSpawner`. During implementation the class **kept the name
> `MobSpawner`** (a better fit); it was rewritten in place as a static,
> eligible-chunks spawner rather than a fresh file. Everything else in the plan
> was followed as written.

## Problem

Infdev 20100420's `MobSpawner` dead-locks the world:

- The mob population climbs to the global cap of **100 monsters / 50 animals** and then freezes.
- Even when the player clears an area, the counter keeps reporting 100 and **no mobs appear** anywhere near the player.

Root causes:

1. **Global cap, not local.** `MobSpawner.tick()` compares a world-wide count
   (`EntityManager.monsterCount`) against the cap. Mobs spawned in distant,
   unloaded regions accumulate in the global total. Once the world has 100
   monsters *anywhere*, the spawner stops trying even where the player is.
2. **1 spawn attempt per tick.** Even when under the cap, only one entity can
   appear per world tick — much too slow to re-populate a cleared area.
3. **Sphere search, not chunk-scoped.** Candidates are picked within a ±128
   block sphere around the player, so spawns can land outside the loaded
   view and never be seen, padding the global counter.
4. **`EntityLiving.getCanSpawnHere()` mutates the entity.** It calls
   `setPosition(...)` as a test side effect, permanently moving the not-yet
   spawned entity before it is added to the world.

Reference behaviour (what we want to copy):

- **a1.1.2 `SpawnerAnimals`**: builds an `eligibleChunks` set from ±4 chunks
  per player (~81 chunks), tries **3 spawns/tick**, requires 24+ blocks from
  player and world spawn.
- **b1.7.3 `SpawnerAnimals.performSpawning`**: ±8 chunks per player (~289),
  a **per-creature-type cap that scales with the number of eligible chunks**
  (`baseCap * eligibleChunks / 256`), and **4-6 spawn attempts/tick**
  distributed over a per-chunk rate limit.

## Objective

Port the b1.7.3 eligible-chunks approach so mob density is **local to the
player's loaded area** and the cap **grows with the explored/loaded region**
instead of going global and stuck.

## Files

| File | Action |
|---|---|
| `minecraft/src/net/minecraft/game/world/MobSpawner.java` | **Rewritten** — kept the class name; became a static eligible-chunks spawner (`MobSpawner.performSpawning`) handling both monsters and animals |
| `minecraft/src/net/minecraft/game/world/World.java` | **Modify** — drive new spawner from `tick()`; drop `MobSpawner` fields |
| `minecraft/src/net/minecraft/game/entity/EntityLiving.java` | **Modify** — stop `getCanSpawnHere` from mutating the entity |

## New `MobSpawner` design

### Eligible chunk collection
```
eligibleChunks = {}                     // Set<ChunkCoordIntPair>, dedup'd
for each player in world.playerEntities:
    pcx = floor(player.posX / 16.0)
    pcz = floor(player.posZ / 16.0)
    radius = 4                          // ±4 chunks = 9x9 = 81 per player
    for dx in -radius..radius:
        for dz in -radius..radius:
            eligibleChunks.add(ChunkCoordIntPair(pcx + dx, pcz + dz))
```

### Per-creature-type cap
```
baseMonsters  = 70
baseAnimals   = 15
monsterChunks = eligibleChunks.size()
maxMonsters   = baseMonsters * monsterChunks / 256
maxAnimals    = baseAnimals  * monsterChunks / 256
```
- With a single player and 81 chunks: ~22 monsters, ~4 animals. Spawns stay
  near the player.
- As the render/loaded area grows the cap grows with it, so a well-travelled
  world never starves *locally* the way the fixed global cap did.

### Per tick
```
// Hostiles (monster/animal split via the same routine, different pool + cap)
if world.countEntities(EntityMonster) <= maxMonsters:
    for attempt in 0..3:                     // ~4 spawn attempts / tick
        if rand.nextInt(10) != 0: continue   // per-chunk rate limiting
        chunk = random from eligibleChunks
        attemptSpawn(chunk, MonsterClasses)
if world.countEntities(EntityAnimal) <= maxAnimals:
    for attempt in 0..3:
        if rand.nextInt(10) != 0: continue
        chunk = random from eligibleChunks
        attemptSpawn(chunk, AnimalClasses)
```

### attemptSpawn
```
// Pick a random cell inside the chunk's column
x = chunk.chunkXPos * 16 + rand.nextInt(16)
y = rand.nextInt(128)                       // world height cap
z = chunk.chunkZPos * 16 + rand.nextInt(16)
// Validate the surface (same rules as today's spawner)
if isSolid(x,y,z) or material != air: return false
for jitter in 0..5:                         // reuse today's 6x6 jitter
    if validSurface(x + jit, y + jit, z + jit):
        px = x + 0.5 ; py = y + 1 ; pz = z + 0.5
        if closestPlayerDistance(px,py,pz) > 24.0 and
           distanceToWorldSpawn(px,py,pz)^2 >= 576.0:
            EntityLiving e = newEntity(pool)         // reflective World ctor
            e.setLocationAndAngles(px, py, pz, rand*360, 0)
            if e.getCanSpawnHere():                  // FIXED not to mutate
                world.spawnEntityInWorld(e)
                return true
return false
```

### Notes
- Reuse today's reflective entity creation (`getConstructor(World.class)`),
  the `isValidSurface` rule (solid below / air above / no liquid), and the
  world-spawn distance check.
- `EntityManager` cached counters are already correct (updated on
  `spawnEntityInWorld` and on dead-sweep), so read
  `world.countEntities(...)` / `getCachedEntityCount(...)` as-is.
- Keys for chunk coordinates need a small `ChunkCoordIntPair`-like value class
  (or just encode into a `long`). Decide in implementation — simplest is a
  `long` (x << 32 | z) in a `HashSet<Long>` for single-player.

## `EntityLiving.getCanSpawnHere` fix

Current (minecraft/src/.../EntityLiving.java:731):
```java
public boolean getCanSpawnHere(float x, float y, float z) {
    this.setPosition((double)x, (double)(y + this.height / 2.0F), (double)z);
    return this.worldObj.checkIfAABBIsClear1(this.boundingBox)
        && this.worldObj.getCollidingBoundingBoxes(this.boundingBox).size() == 0
        && !this.worldObj.getIsAnyLiquid(this.boundingBox);
}
```
Problem: `setPosition` permanently moves the not-yet-spawned entity to the
trial cell. If the check fails, the entity is dropped but its position was
mutated (and `rand`/seed effects may already have been applied).

Fix: snapshot the entity's position/rotation before the test and restore them
after, so the test is side-effect free regardless of its result.

## `World.tick()` wiring

Replace:
```java
this.monsterSpawner.tick();
this.animalSpawner.tick();
```
with:
```java
MobSpawner.performSpawning(this);
```
and remove the `monsterSpawner` / `animalSpawner` fields and their
constructor initialisation (lines ~46-48 and ~215-218).

## Tuning constants (initial)

- `SPAWN_RADIUS_CHUNKS = 4`
- `MAX_MONSTERS_BASE = 70`
- `MAX_ANIMALS_BASE = 15`
- `SPAWN_ATTEMPTS_PER_TICK = 3`
- `CHUNK_RATE_LIMIT = 10` (1-in-10 per chunk per tick)
- `MIN_PLAYER_DISTANCE = 24.0F`
- `MIN_WORLD_SPAWN_DISTANCE_SQ = 576.0F`
- `WORLD_HEIGHT = 128`

## Implementation order

1. Confirm no other caller references `MobSpawner` / `spawner.tick()`.
2. Rewrite `MobSpawner.java` (kept the name) as the static eligible-chunks spawner.
3. Update `World.tick()` + remove spawner fields and unused entity imports.
4. Fix `EntityLiving.getCanSpawnHere` mutation.
5. Full-tree `javac 1.8` compile (expect EXIT=0, no warnings).
7. Manual launch verification: mobs spawn near player, cap recharges once an
   area is cleared, no 100-stuck world.
