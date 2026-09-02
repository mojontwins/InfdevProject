# Modern AI System — r1.2.5 Analysis and Port Plan

This document studies the r1.2.5 task-scheduler AI framework and lays out a
plan to replace the inline `updatePlayerActionState()` of every infdev mob
and animal with an equivalent list of `EntityAIBase` tasks. The same class
and field names are used everywhere to keep the mental model consistent.

> **Naming convention.** Throughout this document every r1.2.5 field, method
> and local variable that has a `field_NNNNN_x` or `func_NNNNN_x` name is
> rewritten as the descriptive name actually present in the *MCP* mapping of
> the time. The `r1.2.5 → meaningful` column in §1.1 makes the mapping
> explicit, so the r1.2.5 source can be read on its own after the table.

---

## 1. The framework

### 1.1 `EntityAIBase` — the task contract

```java
public abstract class EntityAIBase {
    private int mutexBits = 0;            // bitmask of mutex groups this task uses

    public abstract boolean shouldExecute();           // may this task start now?
    public boolean continueExecuting() { return this.shouldExecute(); }
    public boolean isContinuous()       { return true; } // survives equal-priority clash?
    public void    startExecuting()    { }             // called once on begin
    public void    resetTask()         { }             // called on end
    public void    updateTask()        { }             // called every tick while running

    public void setMutexBits(int mutexBits) { this.mutexBits = mutexBits; }
    public int  getMutexBits()              { return this.mutexBits; }
}
```

`isContinuous()` defaults to `true` so a running task can only be interrupted
by a strictly-higher-priority task whose mutex bits do not clash. Setting it
to `false` (as `EntityAIAttackOnCollide` does) means even an equal-priority
task can preempt it.

### 1.2 `EntityAITaskEntry` — the registered record

```java
class EntityAITaskEntry {
    public EntityAIBase action;        // the task instance
    public int          priority;       // the priority tier (lower wins)
    final EntityAITasks taskList;       // back-reference to the scheduler

    public EntityAITaskEntry(EntityAITasks taskList, int priority, EntityAIBase action) {
        this.taskList = taskList;
        this.priority = priority;
        this.action   = action;
    }
}
```

### 1.3 `EntityAITasks` — the scheduler

Two `ArrayList<EntityAITaskEntry>`:

| Field | Meaning |
|---|---|
| `taskEntries` | every registered task, in registration order |
| `executingTaskEntries` | tasks currently running this tick |

#### Method-by-method mapping

| r1.2.5 name | Meaningful name | Purpose |
|---|---|---|
| `addTask(int priority, EntityAIBase task)` | same | append a new entry to `taskEntries` |
| `onUpdateTasks()` | same | tick the scheduler (called every entity update) |
| `canUse(EntityAITaskEntry entry)` | was `func_46116_a` | "would this entry be allowed to start given current executing tasks?" |
| `areTasksCompatible(a, b)` | same | `(a.mutexBits & b.mutexBits) == 0` |

#### `canUse` algorithm (was `func_46116_a`)

For every other entry in `taskEntries`:

- if `entry.priority >= other.priority` (this entry does **not** strictly outrank
  it) AND `other` is currently executing AND `areTasksCompatible(entry, other)`
  is false → return `false` (mutex conflict).
- else if `other.priority` is strictly lower AND `other.action.isContinuous()`
  is false AND `other` is currently executing → return `false` (preemptable).

Otherwise → `true` (entry may run).

#### `onUpdateTasks()` algorithm

```
newList = empty list
for entry in taskEntries:
    if entry in executingTaskEntries:
        if !canUse(entry) or !entry.action.continueExecuting():
            entry.action.resetTask()
            executingTaskEntries.remove(entry)
    else:
        if canUse(entry) and entry.action.shouldExecute():
            newList.add(entry)
            executingTaskEntries.add(entry)

for entry in newList:                       // start
    entry.action.startExecuting()

for entry in executingTaskEntries:          // tick
    entry.action.updateTask()
```

So priority tiers are a strict ordering; two tasks that share a priority tier
must use disjoint mutex bits to coexist. The bitmask is just an int (binary
`0b0011` = 3 means bits 0 and 1), so up to 32 mutex groups exist.

### 1.4 `EntityAITarget` — the targeting base

`EntityAITarget extends EntityAIBase`. Adds:

| r1.2.5 name | Meaningful name | Type | Purpose |
|---|---|---|---|
| `taskOwner` | same | `EntityLiving` | the owning entity |
| `targetSearchRadius` | was `field_48379_d` | `float` | squared range, **the parameter is float but compared squared** |
| `shouldCheckSight` | was `field_48380_e` | `boolean` | if true, target must remain in line-of-sight |
| `nearbyOnly` | was `field_48383_a` | `boolean` | if true, only accept the target if a path can be made to it |
| `canTargetCountdown` | was `field_48381_b` | `int` | 0/1/2: 0 untried, 1 reachable, 2 unreachable |
| `reachabilityCheckCooldown` | was `field_48377_f` | `int` | ticks until next reachability check |
| `sightLossTimer` | was `field_48378_g` | `int` | ticks since target was last seen |

Constructor variants: `(EntityLiving, float, boolean)` and
`(EntityLiving, float, boolean, boolean)` (the second boolean is
`nearbyOnly`).

`continueExecuting()` re-checks each tick: target alive, in squared range,
and (if `shouldCheckSight`) still visible. After 60 ticks of lost sight the
task is dropped.

`resetTask()` clears `taskOwner.attackTarget`.

`isValidTarget(EntityLiving target, boolean includePlayers)` (was
`func_48376_a`) does the heavy filtering:
1. not null, not self, alive.
2. vertical-AABB overlap with owner.
3. `taskOwner.canAttackClass(target.getClass())` (was `func_48100_a`).
4. tameable guard: if owner is tamed, skip same-species tamed entities and skip the owner itself.
5. `EntityPlayer` creative-mode guard (only if `!includePlayers`).
6. home-distance check (`isWithinHomeDistance`).
7. line-of-sight (if `shouldCheckSight`).
8. reachability check (if `nearbyOnly`): every `10 + rand(5)` ticks, run
   `navigator.getPathToEntityLiving(target)` and accept only if the final
   path point is within 1.5 blocks.

`isReachable(EntityLiving)` (was `func_48375_a`) is the helper used by
step 8 above.

### 1.5 The navigator

`PathNavigate` wraps `PathEntity` and exposes the r1.2.5 API on top of the
existing infdev `World.pathFinder` field. Field-by-field:

| r1.2.5 name | Meaningful name | Type | Purpose |
|---|---|---|---|
| `theEntity` | same | `EntityLiving` | the owner |
| `worldObj` | same | `World` | |
| `currentPath` | same | `PathEntity` | may be null |
| `speed` | same | `float` | the speed for the next setPath call |
| `pathSearchRange` | same | `float` | max search distance |
| `noSunPathfind` | same | `boolean` | skip nodes that can see the sky |
| `totalTicks` | same | `int` | how many ticks has this navigator existed |
| `ticksAtLastPos` | same | `int` | last tick the entity moved measurably |
| `lastPosCheck` | same | `Vec3D` | position the entity was at `ticksAtLastPos` ticks ago |
| `canPassOpenWoodenDoors` | same | `boolean` | default true |
| `canPassClosedWoodenDoors` | same | `boolean` | default false |
| `avoidsWater` | same | `boolean` | default false |
| `canSwim` | same | `boolean` | default false |

Public mutators are simply `setX(boolean)`. Getters worth listing:

| r1.2.5 name | Meaningful name |
|---|---|
| `getAvoidsWater()` | same |
| `getCanBreakDoors()` | same |

Path creation and movement:

| r1.2.5 name | Meaningful name | Purpose |
|---|---|---|
| `getPathToXYZ(double x, double y, double z)` | same | build a path to a static point |
| `tryMoveToXYZ(double x, double y, double z, float speed)` | same | wrapper that also calls `setPath` |
| `getPathToEntityLiving(EntityLiving)` | same | build a path to a moving entity |
| `tryMoveToEntityLiving(EntityLiving, float speed)` | same | wrapper |
| `setPath(PathEntity, float speed)` | same | adopt a path, returns `false` on null/empty |
| `getPath()` | same | getter for `currentPath` |
| `noPath()` | same | `currentPath == null || currentPath.isFinished()` |
| `clearPathEntity()` | same | `currentPath = null` |
| `onUpdateNavigation()` | same | call from the entity's tick |

Internally, every path-creation call delegates to one of
`worldObj.getEntityPathToXYZ(...)` or `worldObj.getPathEntityToEntity(...)`,
which already exist in infdev's `World` class. The path-following loop is
the meat of `onUpdateNavigation()`: it walks nodes that share the same y as
the entity, advances the path index, and short-circuits via
`isDirectPathBetweenPoints(...)` if a straight line ahead is walkable.

The helpers used by the path-following loop are `pathFollow()` (was a
private method), `canNavigate()` (was `canNavigate()`), `func_48657_k` (now
`isInLiquid()`), `getEntityPosition()` (now private), `getPathableYPos()`
(now private, with the swimming/lava branch), `removeSunnyPath()` (was
`removeSunnyPath`), `isDirectPathBetweenPoints` (was `isDirectPathBetweenPoints`),
`isSafeToStandAt` (was `isSafeToStandAt`), and `isPositionClear` (was
`isPositionClear`). All stay private with their original names — they are
algorithm primitives, not part of the public surface.

### 1.6 Helper classes

#### `EntityLookHelper`

| r1.2.5 name | Meaningful name | Type | Purpose |
|---|---|---|---|
| `entity` | same | `EntityLiving` | the owner |
| `deltaYaw` | was `field_46149_b` | `float` | max yaw turn per tick |
| `deltaPitch` | was `field_46150_c` | `float` | max pitch turn per tick |
| `isLooking` | was `field_46147_d` | `boolean` | whether a target is set |
| `posX`, `posY`, `posZ` | same | `double` | the target position |

Methods: `setLookPositionWithEntity(Entity, float, float)`,
`setLookPosition(double, double, double, float, float)`,
`onUpdateLook()` and the private `updateRotation(current, target, maxDelta)`.
The body is unchanged.

#### `EntityMoveHelper`

| r1.2.5 name | Meaningful name | Type | Purpose |
|---|---|---|---|
| `entity` | same | `EntityLiving` | the owner |
| `posX`, `posY`, `posZ` | same | `double` | the move target |
| `speed` | same | `float` | the move speed |
| `hasPath` | was `hasPath` | `boolean` | whether a target is set |

Methods: `setMoveTo(double, double, double, float)`,
`onUpdateMoveHelper()` and the private `func_48185_a` (now
`rotateAngleTowards`). Body unchanged.

#### `RandomPositionGenerator`

Static methods, all of which delegate to a private
`findRandomTargetBlock(EntityCreature, int rangeXZ, int rangeY, Vec3D bias)`:

| r1.2.5 name | Meaningful name | Purpose |
|---|---|---|
| `findRandomTarget(EntityCreature, int, int)` | same | sample 10 weighted random points, return best as `Vec3D` or null |
| `findRandomTargetBlockTowards(EntityCreature, int, int, Vec3D)` | same | bias the sample toward the given offset |
| `findRandomTargetBlockAwayFrom(EntityCreature, int, int, Vec3D)` | same | bias the sample away from the given offset |
| `randomPosition` (static field) | same | scratch vector reused by the `Towards` / `AwayFrom` helpers |

The body samples 10 points in a `[-XZ..XZ] × [-Y..Y] × [-XZ..XZ]` box,
scores each with `creature.getBlockPathWeight`, and keeps the best one. A
home-distance guard (`creature.isWithinHomeDistance`) is applied when the
creature has a home. The function variable `z5` becomes `found`, `i6`/`i7`/`i8`
become `bestX`/`bestY`/`bestZ`, `f9` becomes `bestScore`, `f15` becomes
`score`, `i12`/`i13`/`i14` become `sampleX`/`sampleY`/`sampleZ`.

### 1.7 The two task lists on `EntityLiving`

```java
protected EntityAITasks tasks       = new EntityAITasks();   // goal selector
protected EntityAITasks targetTasks = new EntityAITasks();   // target selector
```

In `EntityLiving.onLivingUpdate()`:

```java
this.targetTasks.onUpdateTasks();   // 1. acquire / drop targets
this.tasks.onUpdateTasks();        // 2. run movement / action goals
this.navigator.onUpdateNavigation();// 3. step along the current path
this.getMoveHelper().onUpdateMoveHelper();  // 4. turn / move
this.getLookHelper().onUpdateLook();        // 5. turn head
```

`targetTasks` is exclusively for targeting: `EntityAIHurtByTarget` and
`EntityAINearestAttackableTarget`. `tasks` is for everything else
(movement, look, attack).

### 1.8 Existing tasks and their mutex bits

| Class | What it does | Mutex bits |
|---|---|---|
| `EntityAISwimming` | jump when in water/lava. Enables `navigator.canSwim` in the constructor. | 4 |
| `EntityAIWander` | sample 10 random positions weighted by `getBlockPathWeight`; 1-in-120 chance per tick; capped at `entityAge < 100`. | 1 |
| `EntityAIAttackOnCollide` | follow current `attackTarget` via `PathEntity`; melee strike when within `width * 2.0F` squared. `isContinuous() == false`. | 3 |
| `EntityAIArrowAttack` | ranged variant: stay close enough (≤ 10 m) to see target, then fire. | 3 |
| `EntityAIWatchClosest` | head-track nearest entity of given class. 2% chance per tick by default. Holds for `40 + rand(40)` ticks. | 2 |
| `EntityAILookIdle` | pick a random yaw, hold for `20 + rand(20)` ticks. 2% chance per tick. | 3 |
| `EntityAIHurtByTarget` | re-targets the entity stored in `getAITarget()` (the last aggressor). Optionally spreads the target to nearby same-type entities. | 1 |
| `EntityAINearestAttackableTarget` | scan AABB for the closest living entity of given class. `getClosestVulnerablePlayerToEntity` for `EntityPlayer`. Optional chance-divisor. | 1 |
| `EntityAICreeperSwell` | drives the creeper's swell state machine from a target. | 1 |

---

## 2. Infdev's current system

### 2.1 `EntityCreature.updatePlayerActionState()`

All creature AI lives in one method. Every tick it:

1. `playerToAttack == null` → call `findPlayerToAttack()` (overridden per
   subclass) → if found, build a path with `worldObj.pathFinder.createEntityPathTo(this, playerToAttack, 16.0F)`.
2. If a target exists and is alive: check line-of-sight and call
   `attackEntity(target, distance)`. `hasAttacked` prevents both attack
   and movement in the same tick.
3. If no attack happened: pick a random wander goal (200 samples, best score
   by `getBlockPathWeight`) and build a path with
   `worldObj.pathFinder.createEntityPathToXYZ(this, x, y, z, 16.0F)`.
4. Step along the current path: turn toward the next node, set `moveForward` to
   `moveSpeed`, set `isJumping = true` when the node is above.

### 2.2 `EntityMonster` additions

`EntityMonster extends EntityCreature` overrides:
- `findPlayerToAttack()` — returns the single player if within 16 m, visible,
  and not a sneaking player in dim light beyond 6 m.
- `attackEntity()` — melee strike when distance < 2.5 and vertically overlapping.
- `getBlockPathWeight()` — prefers dark areas (`0.5 - brightness`).
- `tryBurnInDaylight()` — ignites when exposed to sky in daylight.
- `attackEntityFrom()` — stores the attacker in `playerToAttack` for retaliation.
- `getCanSpawnHere()` — requires `blockLight <= rand.nextInt(8)`.

`EntityZombie` overrides `onLivingUpdate()` to call `tryBurnInDaylight()` and
`getDroppedItem()` to return feathers.

---

## 3. Per-mob port plan

For each mob the table shows:
- **infdev current behaviour** — what the existing `updatePlayerActionState()` does.
- **r1.2.5 task list** — the exact `addTask` calls from r1.2.5 (skipping
  villager/saddle/door/home/village features not present in infdev).
- **Modern equivalent** — the concrete tasks to register in the infdev port,
  using the meaningful names from §1.

### 3.1 Mob priority reference

The following priority tiers are used consistently across all mobs:

| Priority | Tier name | Typical contents |
|---|---|---|
| 0 | Highest | Swimmer (must interrupt everything to get out of water) |
| 1 | High | Panic, mate, tempt, follow-parent |
| 2 | Mid-high | Attack-on-collide, arrow attack, flee-sun |
| 3 | Mid | Wander, move-toward-target |
| 4 | Low | Watch-closest |
| 5 | Lowest | Look-idle |

Mutex bits are per §1.8. Target selector uses priority 1 for hurt-by-target and
priority 2 for nearest-attackable-target.

---

### 3.2 `EntityZombie`

**infdev current behaviour:**
- `findPlayerToAttack()`: within 16 m, visible, respects sneaking dim-light rule.
- `attackEntity()`: melee when < 2.5 m and vertically overlapping.
- `tryBurnInDaylight()`: called in `onLivingUpdate()`.
- `getBlockPathWeight()`: prefers dark (`0.5 - brightness`).

**r1.2.5 task list:**
```java
tasks.addTask(0, new EntityAISwimming(this));
tasks.addTask(1, new EntityAIBreakDoor(this));        // infdev: no doors
tasks.addTask(2, new EntityAIAttackOnCollide(this, EntityPlayer.class, moveSpeed, false));
tasks.addTask(3, new EntityAIAttackOnCollide(this, EntityVillager.class, moveSpeed, true)); // infdev: no villagers
tasks.addTask(4, new EntityAIMoveTwardsRestriction(this, moveSpeed)); // infdev: no home
tasks.addTask(5, new EntityAIMoveThroughVillage(this, moveSpeed, false)); // infdev: no villages
tasks.addTask(6, new EntityAIWander(this, moveSpeed));
tasks.addTask(7, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
tasks.addTask(7, new EntityAILookIdle(this));
targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
targetTasks.addTask(2, new EntityAINearestAttackableTarget(this, EntityPlayer.class, 16.0F, 0, true));
targetTasks.addTask(2, new EntityAINearestAttackableTarget(this, EntityVillager.class, 16.0F, 0, false)); // infdev: no villagers
```

**Modern equivalent (infdev):**
```java
tasks.addTask(0, new EntityAISwimming(this));
tasks.addTask(2, new EntityAIAttackOnCollide(
    this, EntityPlayer.class, this.moveSpeed, false));
tasks.addTask(3, new EntityAIWander(this, this.moveSpeed));
tasks.addTask(4, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
tasks.addTask(5, new EntityAILookIdle(this));
targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
targetTasks.addTask(2, new EntityAINearestAttackableTarget(
    this, EntityPlayer.class, 16.0F, 0, true));
```

**Notes:**
- `EntityAIBreakDoor`, `EntityAIMoveTwardsRestriction`, `EntityAIMoveThroughVillage`
  and the `EntityVillager` attack are omitted — infdev has no village system,
  no door-breaking, and no villagers.
- `tryBurnInDaylight()` is called from the skeleton's `onLivingUpdate()` in
  infdev. Zombies and skeletons inherit it from `EntityMonster`. In the modern
  AI this stays in `onLivingUpdate()` (it is not a task, it is a one-tick
  effect on `fire`). The day-flee behaviour (`EntityAIFleeSun`) is deferred.
- The `EntityPlayer.class` class literal is used directly; there is only one
  player type in infdev.

---

### 3.3 `EntitySkeleton`

**infdev current behaviour:**
- `onLivingUpdate()` calls `tryBurnInDaylight()` first, then the base tick.
- `findPlayerToAttack()`: same as monster (16 m, visible, respects sneaking rule).
- `attackEntity()`: ranged arrow every 30 ticks, within 10 m. Keeps aiming at the
  target between shots (`rotationYaw` tracks player). Calls `attackTime = 30`
  as cooldown.
- `getBlockPathWeight()`: same as monster.

**r1.2.5 task list:**
```java
tasks.addTask(0, new EntityAISwimming(this));
tasks.addTask(1, new EntityAIRestrictSun(this));      // infdev: deferred
tasks.addTask(2, new EntityAIFleeSun(this, moveSpeed)); // infdev: deferred
tasks.addTask(3, new EntityAIArrowAttack(this, moveSpeed, 1, 60));
tasks.addTask(4, new EntityAIWander(this, moveSpeed));
tasks.addTask(5, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
tasks.addTask(5, new EntityAILookIdle(this));
targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
targetTasks.addTask(2, new EntityAINearestAttackableTarget(this, EntityPlayer.class, 16.0F, 0, true));
```

**Modern equivalent (infdev):**
```java
tasks.addTask(0, new EntityAISwimming(this));
tasks.addTask(2, new EntityAIArrowAttack(this, this.moveSpeed, 1, 30));
tasks.addTask(3, new EntityAIWander(this, this.moveSpeed));
tasks.addTask(4, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
tasks.addTask(5, new EntityAILookIdle(this));
targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
targetTasks.addTask(2, new EntityAINearestAttackableTarget(
    this, EntityPlayer.class, 16.0F, 0, true));
```

**Notes:**
- `EntityAIRestrictSun` and `EntityAIFleeSun` are deferred (infdev skeletons
  only burn in daylight, they do not actively flee).
- The arrow attack's `rangedAttackID = 1` means "use `EntityArrow`". The
  constructor is `(EntityLiving host, float speed, int attackID, int
  maxAttackTime)`. The `maxAttackTime = 30` mirrors the infdev cooldown.
- `tryBurnInDaylight()` stays in `onLivingUpdate()` alongside the modern AI tick
  (it runs before the task scheduler).

---

### 3.4 `EntitySpider`

**infdev current behaviour:**
- `findPlayerToAttack()`: hunts only when brightness < 0.5 (dark). Within 16 m,
  visible.
- `attackEntity()`: if bright → give up (`playerToAttack = null`). If dark →
  within 2–6 m there is a 1-in-10 chance of a pounce (leap at target) if on
  ground. Otherwise falls through to `super.attackEntity()` (melee bite).
- `getBlockPathWeight()`: same as monster.

**r1.2.5 task list:**
```java
// r1.2.5 Spider has no tasks — it relies entirely on EntityMob base tasks.
// The only Spider-specific AI is the pounce in attackEntity() which is
// preserved as-is, and a ladder check for the renderer.
```

**Modern equivalent (infdev):**
```java
tasks.addTask(0, new EntityAISwimming(this));
tasks.addTask(2, new EntityAIAttackOnCollide(
    this, EntityPlayer.class, this.moveSpeed, false));
tasks.addTask(3, new EntityAIWander(this, this.moveSpeed));
tasks.addTask(4, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
tasks.addTask(5, new EntityAILookIdle(this));
targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
targetTasks.addTask(2, new EntityAINearestAttackableTarget(
    this, EntityPlayer.class, 16.0F, 0, true));
```

**Notes:**
- The `brightness < 0.5` hunt condition from `findPlayerToAttack()` is
  replicated by overriding `shouldExecute()` on a custom anonymous task or by
  leaving the base `EntityAINearestAttackableTarget` but adding a
  `EntityAISpiderLightSensor` custom task (priority 2, mutex 1) that sets
  `attackTarget = null` when it is too bright. The cleanest approach for
  infdev is to keep `findPlayerToAttack()` and let it gate the `attackTarget`
  field, which `EntityAIAttackOnCollide` reads via `getAttackTarget()`.
  This preserves the exact original behaviour.
- The pounce (`distance > 2 && distance < 6 && rand.nextInt(10) == 0 && onGround`)
  is preserved as an override of `updateTask()` in a custom `EntityAISpiderMelee`
  task (or kept directly in `attackEntity()` for now and moved later).
- Spider's `isOnLadder()` = `isCollidedHorizontally()` (used by renderer).
  In infdev there is no ladder block so this is always false.

---

### 3.5 `EntityCreeper`

**infdev current behaviour:**
- No target acquisition (`findPlayerToAttack()` is never called — `playerToAttack`
  stays null). The creeper has no active movement or wandering.
- `updatePlayerActionState()`: manages the fuse state (`fuseState` and
  `timeSinceIgnited`) and calls the parent.
- `attackEntity()`: called by the parent's path-following when the player comes
  within range. Lights the fuse when distance < 3 m (or < 7 m if already lit).
  Explodes when `timeSinceIgnited == fuseTime` (30 ticks).
- `getBlockPathWeight()`: same as monster.

**r1.2.5 task list:**
```java
tasks.addTask(1, new EntityAISwimming(this));
tasks.addTask(2, new EntityAICreeperSwell(this));
tasks.addTask(3, new EntityAIAvoidEntity(this, EntityOcelot.class, 6.0F, 0.25F, 0.3F)); // infdev: no ocelots
tasks.addTask(4, new EntityAIAttackOnCollide(this, 0.25F, false));
tasks.addTask(5, new EntityAIWander(this, 0.2F));
tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
tasks.addTask(6, new EntityAILookIdle(this));
targetTasks.addTask(1, new EntityAINearestAttackableTarget(this, EntityPlayer.class, 16.0F, 0, true));
targetTasks.addTask(2, new EntityAIHurtByTarget(this, false));
```

**Modern equivalent (infdev):**
```java
tasks.addTask(0, new EntityAISwimming(this));
tasks.addTask(2, new EntityAICreeperSwell(this));
tasks.addTask(3, new EntityAIAttackOnCollide(this, EntityPlayer.class, 0.8F, false));
tasks.addTask(4, new EntityAIWander(this, 0.2F));
tasks.addTask(5, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
tasks.addTask(5, new EntityAILookIdle(this));
```

**Notes:**
- `EntityAICreeperSwell` is the key task: it drives `setCreeperState(1)` (lit fuse)
  when a target is within 3 m, and `setCreeperState(-1)` (idle) when the target
  is lost (> 7 m) or invisible. It calls `navigator.clearPathEntity()` on
  `startExecuting()`.
- `EntityAIAvoidEntity` (ocelot flee) is omitted — infdev has no ocelots.
- The `fuseTime = 30` and the explosion mechanic (`worldObj.createExplosion`)
  stay exactly as written. The `fuseProgress` renderer method is unchanged.
- The `dataWatcher` mechanism for the creeper state (`getCreeperState()` /
  `setCreeperState()`) is unchanged — it is not part of the AI framework.

---

### 3.6 `EntityGiant`

**infdev current behaviour:**
- Subclass of `EntityMonster`. No own AI methods.
- Constructor: `moveSpeed = 0.5`, `attackStrength = 50`, `health *= 10`,
  `setSize(width * 6, height * 6)`. Prefers bright spots (overrides
  `getBlockPathWeight()` to return `brightness - 0.5` — the opposite of
  monsters).
- Because it never passes the monster spawn check (`blockLight <= rand.nextInt(8)`)
  it can only be spawned via spawn egg.

**Modern equivalent (infdev):**
```java
tasks.addTask(0, new EntityAISwimming(this));
tasks.addTask(2, new EntityAIAttackOnCollide(
    this, EntityPlayer.class, this.moveSpeed, false));
tasks.addTask(3, new EntityAIWander(this, this.moveSpeed));
tasks.addTask(4, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
tasks.addTask(5, new EntityAILookIdle(this));
targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
targetTasks.addTask(2, new EntityAINearestAttackableTarget(
    this, EntityPlayer.class, 16.0F, 0, true));
```

**Notes:**
- Same as zombie but with `moveSpeed = 0.5` (set in the constructor).
- The `getBlockPathWeight()` override (brightness - 0.5) is the only thing
  differentiating it from a zombie. This is preserved as-is.
- No changes to `attackStrength`, health, or size — these are entity stats, not AI.

---

### 3.7 `EntityAnimal` (base class)

**infdev current behaviour:**
- `getBlockPathWeight()`: grass below scores 10; otherwise `brightness - 0.5`.
- `getCanSpawnHere()`: requires block light > 8 and the parent's spawn check.
- No active target acquisition or movement — passive.

**r1.2.5 task list:**
```java
// No tasks on the abstract base. Subclasses add them.
// r1.2.5 Animal has getBlockPathWeight = grass ? 10 : brightness - 0.5
// (same as infdev), and no wandering by default.
```

**Modern equivalent (infdev):**
```java
// EntityAnimal stays as-is — no tasks needed in the base class.
// The grass-pathfinding and light spawn check are preserved.
```

**Notes:**
- The r1.2.5 animal also has an `updateAITick()` method that decrements `inLove`
  when not breeding, and an `onLivingUpdate()` that manages `inLove` particles.
  These are breeding mechanics that infdev does not have — deferred.
- The `getBlockPathWeight()` and `getCanSpawnHere()` are the only behaviours
  in the infdev base class. These stay as-is.

---

### 3.8 `EntitySheep`

**infdev current behaviour:**
- No active AI. `getBlockPathWeight()` and `getCanSpawnHere()` from the base.
- Shears on first melee hit: drops 1–3 cloth, sets `sheared = true` forever.
- No wandering, no fleeing, no following.

**r1.2.5 task list:**
```java
tasks.addTask(0, new EntityAISwimming(this));
tasks.addTask(1, new EntityAIPanic(this, 0.38F));        // infdev: deferred (no panic trigger)
tasks.addTask(2, new EntityAIMate(this, f2));           // infdev: deferred (no breeding)
tasks.addTask(3, new EntityAITempt(this, 0.25F, Item.wheat.shiftedIndex, false)); // infdev: no wheat
tasks.addTask(4, new EntityAIFollowParent(this, 0.25F)); // infdev: deferred (no breeding)
tasks.addTask(5, new EntityAIEatGrass(this));            // infdev: deferred (no tall grass / grass eating)
tasks.addTask(6, new EntityAIWander(this, f2));
tasks.addTask(7, new EntityAIWatchClosest(this, EntityPlayer.class, 6.0F));
tasks.addTask(8, new EntityAILookIdle(this));
```

**Modern equivalent (infdev):**
```java
tasks.addTask(0, new EntityAISwimming(this));
tasks.addTask(3, new EntityAIWander(this, 0.23F));
tasks.addTask(4, new EntityAIWatchClosest(this, EntityPlayer.class, 6.0F));
tasks.addTask(5, new EntityAILookIdle(this));
```

**Notes:**
- Sheep in infdev are completely passive with no triggering condition. The wander
  task gives them ambient movement so the world feels alive.
- Shear-on-hit is preserved as `attackEntityFrom()` (not part of the AI framework).
- The `sheared` boolean and NBT are unchanged.

---

### 3.9 `EntityPig`

**infdev current behaviour:**
- No active AI. Purely passive. Can be saddled (saddle item does not exist in
  infdev, so `interact()` is a no-op for the saddle branch).

**r1.2.5 task list:**
```java
tasks.addTask(0, new EntityAISwimming(this));
tasks.addTask(1, new EntityAIPanic(this, 0.38F));
tasks.addTask(2, new EntityAIMate(this, f2));
tasks.addTask(3, new EntityAITempt(this, 0.25F, Item.wheat.shiftedIndex, false));
tasks.addTask(4, new EntityAIFollowParent(this, 0.28F));
tasks.addTask(5, new EntityAIWander(this, f2));
tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 6.0F));
tasks.addTask(7, new EntityAILookIdle(this));
```

**Modern equivalent (infdev):**
```java
tasks.addTask(0, new EntityAISwimming(this));
tasks.addTask(3, new EntityAIWander(this, 0.25F));
tasks.addTask(4, new EntityAIWatchClosest(this, EntityPlayer.class, 6.0F));
tasks.addTask(5, new EntityAILookIdle(this));
```

**Notes:**
- Same as sheep: give them ambient wandering movement.
- The `interact()` method's saddle branch is preserved as-is (no saddle item in
  infdev, so it never triggers).
- No breeding, no panic, no following.

---

### 3.10 `EntityCow`

**infdev current behaviour:**
- No active AI. Purely passive.
- `interact()`: right-click with empty bucket → milk bucket. Right-click with
  wheat → no-op (b1.7.3-style wheat-following is not present in infdev).

**r1.2.5 task list:**
```java
tasks.addTask(0, new EntityAISwimming(this));
tasks.addTask(1, new EntityAIPanic(this, 0.38F));
tasks.addTask(2, new EntityAIMate(this, 0.2F));
tasks.addTask(3, new EntityAITempt(this, 0.25F, Item.wheat.shiftedIndex, false));
tasks.addTask(4, new EntityAIFollowParent(this, 0.25F));
tasks.addTask(5, new EntityAIWander(this, 0.2F));
tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 6.0F));
tasks.addTask(7, new EntityAILookIdle(this));
```

**Modern equivalent (infdev):**
```java
tasks.addTask(0, new EntityAISwimming(this));
tasks.addTask(3, new EntityAIWander(this, 0.2F));
tasks.addTask(4, new EntityAIWatchClosest(this, EntityPlayer.class, 6.0F));
tasks.addTask(5, new EntityAILookIdle(this));
```

**Notes:**
- Same as pig and sheep: ambient wandering movement.
- The `interact()` milk mechanic is unchanged.

---

## 4. Files to create

New package: `net.minecraft.game.entity.ai`.

### 4.1 Core framework (3 files)

| File | Purpose |
|---|---|
| `EntityAIBase.java` | Abstract base: `shouldExecute()`, `continueExecuting()`, `startExecuting()`, `resetTask()`, `updateTask()`, `isContinuous()`, `mutexBits` + accessors. |
| `EntityAITaskEntry.java` | Package-private record: `action`, `priority`, back-reference to `EntityAITasks`. |
| `EntityAITasks.java` | Scheduler: `taskEntries` + `executingTaskEntries`, `addTask(priority, task)`, `onUpdateTasks()`, `canUse(entry)`, `areTasksCompatible(a, b)`. |

### 4.2 Navigation helpers (3 files)

| File | Purpose |
|---|---|
| `EntityLookHelper.java` | Smooth head-tracking. `setLookPositionWithEntity(Entity, deltaYaw, deltaPitch)` and `setLookPosition(x, y, z, deltaYaw, deltaPitch)`. `onUpdateLook()` eases `rotationYawHead` and `rotationPitch`. |
| `EntityMoveHelper.java` | Receives `setMoveTo(x, y, z, speed)` from `PathNavigate`. `onUpdateMoveHelper()` eases `rotationYaw` and sets `moveForward`. |
| `PathNavigate.java` | Wraps existing `World.pathFinder`. Exposes the full r1.2.5 API: `setAvoidsWater`, `setCanSwim`, `setBreakDoors`, `setEnterDoors`, `setAvoidSun`, `setSpeed`. `onUpdateNavigation()` calls the entity's `moveHelper`. All path creation delegates to existing `World` methods. |

### 4.3 Targeting (2 files)

| File | Purpose |
|---|---|
| `EntityAITarget.java` | Abstract extends `EntityAIBase`: `taskOwner`, `targetSearchRadius`, `shouldCheckSight`, `nearbyOnly`, `canTargetCountdown`, `reachabilityCheckCooldown`, `sightLossTimer`. Implements `continueExecuting()` (alive + in range + visible), `startExecuting()`, `resetTask()` (clears attack target). Provides `isValidTarget(EntityLiving, boolean)` filter and `isReachable(EntityLiving)` helper. |
| `EntityAINearestAttackableTarget.java` | Extends `EntityAITarget`: AABB scan sorted by distance, or `getClosestVulnerablePlayerToEntity` for `EntityPlayer`. `shouldExecute()` checks the chance-divisor. `startExecuting()` calls `taskOwner.setAttackTarget(...)`. |

### 4.4 Tasks (8 files)

| File | What it does | Mutex |
|---|---|---|
| `EntityAISwimming.java` | Triggers jump when in water/lava. Calls `navigator.setCanSwim(true)` in constructor. | 4 |
| `EntityAIWander.java` | Calls `RandomPositionGenerator.findRandomTarget(entity, 10, 7)`. 1-in-120 chance, `entityAge < 100`. `startExecuting()` calls `navigator.tryMoveToXYZ(...)`. | 1 |
| `EntityAIAttackOnCollide.java` | Follows `attackTarget` via `PathEntity`, melee strike when within `width * 2.0` squared. `isContinuous() == false`. | 3 |
| `EntityAIArrowAttack.java` | Ranged: within 10 m, aim and fire every `maxAttackTime` ticks. `attackID = 1` means `EntityArrow`. | 3 |
| `EntityAIWatchClosest.java` | Head-track nearest entity of given class. 2% chance, holds for `40 + rand(40)` ticks. | 2 |
| `EntityAILookIdle.java` | Pick random yaw, hold for `20 + rand(20)` ticks. 2% chance. | 3 |
| `EntityAIHurtByTarget.java` | Extends `EntityAITarget`. Re-targets `taskOwner.getAITarget()`. Optionally spreads to nearby same-type entities. | 1 |
| `EntityAINearestAttackableTargetSorter.java` | `Comparator<EntityLiving>`: sorts by `entity.getDistanceSqToEntity(target)`. Used by `EntityAINearestAttackableTarget`. |
| `RandomPositionGenerator.java` | Static. `findRandomTarget(entity, rangeXZ, rangeY)` samples 10 weighted positions via `entity.getBlockPathWeight()`. |
| `EntityAICreeperSwell.java` | Drives creeper swell: lights fuse when target within 3 m, extinguishes when > 7 m or invisible. Calls `navigator.clearPathEntity()` on start. | 1 |

### 4.5 Entity wiring (modifications to 4 files)

| File | Change |
|---|---|
| `EntityLiving.java` | Add fields: `navigator` (`PathNavigate`), `lookHelper` (`EntityLookHelper`), `moveHelper` (`EntityMoveHelper`), `attackTarget` (`EntityLiving`), `entityAge` (already present as `livingSoundTime` — rename). Add getters/setters: `getNavigator()`, `getLookHelper()`, `getMoveHelper()`, `getAttackTarget()`, `setAttackTarget(EntityLiving)`, `getRNG()`. `isAIEnabled()` returns `false` initially (existing `updatePlayerActionState()` stays as fallback). In `onLivingUpdate()` add the task/navigation tick calls. |
| `EntityCreature.java` | Add `tasks` and `targetTasks`. Override `isAIEnabled()` returning `true`. `EntityCreature` opts into the new AI. The existing `updatePlayerActionState()` runs only when `!isAIEnabled()`. `getNavigator()` returns the new navigator. `getAttackTarget()`/`setAttackTarget()` delegating to the living field. |
| `EntityMonster.java` | Remove `findPlayerToAttack()` (replaced by `EntityAINearestAttackableTarget`). Remove `playerToAttack` field (replaced by `attackTarget` on `EntityLiving`). Keep `attackEntity()`, `getBlockPathWeight()`, `tryBurnInDaylight()`, `onLivingUpdate()` burn timer, `getCanSpawnHere()`. |
| `EntityZombie.java` | Strip all inline AI. Add task registrations in the constructor (see §3.2). |

---

## 5. Implementation stages

**Stage 1 — Framework skeleton** (no behaviour changes):
Create `EntityAIBase`, `EntityAITaskEntry`, `EntityAITasks`, `EntityAITarget`,
`EntityLookHelper`, `EntityMoveHelper`, `PathNavigate` (stub),
`EntityLiving` additions (`isAIEnabled()` returns `true`, adds fields,
calls tasks in `onLivingUpdate()`). At this stage no tasks are registered;
everything compiles and runs identically to before.

**Stage 2 — First tasks** (`EntityAISwimming`, `EntityAIWander`,
`EntityAIWatchClosest`, `EntityAILookIdle`). Register them in `EntityZombie`.
Verify movement and idle behaviour unchanged from the baseline.

**Stage 3 — Attack tasks** (`EntityAIAttackOnCollide`,
`EntityAIHurtByTarget`, `EntityAINearestAttackableTarget`). Register in
`EntityZombie`'s constructor. Remove `findPlayerToAttack()` override from
`EntityMonster`.

**Stage 4 — Remaining mobs**: Skeleton (`EntityAIArrowAttack`),
Creeper (`EntityAICreeperSwell`), Spider, Giant, and the animals (ambient
wander).

**Stage 5 — Deferred**: Breeding, panic, tempt, follow-parent, mate, eat-grass
(animals); door-breaking, sun-flee, village movement (monsters). These require
features that infdev does not have and can be layered on later.

---

## 6. Key differences to keep in mind

### 6.1 Target vs. entity field names

| r1.2.5 | Infdev |
|---|---|
| `attackTarget` field on `EntityLiving` | `playerToAttack` field on `EntityCreature` |
| `setAttackTarget(EntityLiving)` / `getAttackTarget()` | direct field access |
| `getAITarget()` — last entity that hurt this one | not present yet (add to `EntityLiving`) |

### 6.2 Navigator vs. path entity

| r1.2.5 | Infdev |
|---|---|
| `PathNavigate navigator` field | `pathToEntity` field directly on `EntityCreature` |
| `navigator.tryMoveToXYZ(x, y, z, speed)` | `pathToEntity = world.pathFinder.createEntityPathToXYZ(...)` + manual path-following in `updatePlayerActionState()` |
| `navigator.noPath()` | `pathToEntity == null || pathToEntity.isFinished()` |

`PathNavigate` delegates to the existing infdev `World.pathFinder` for all path
creation and wraps the path-following loop that currently lives in
`EntityCreature.updatePlayerActionState()`. The existing `PathFinder` and
`PathEntity` are reused unchanged.

### 6.3 Home / village features

r1.2.5 zombies have `EntityAIMoveTwardsRestriction` (stay near spawn) and
`EntityAIMoveThroughVillage`. Infdev has no village system and no home
position. These are deferred to Stage 5.

### 6.4 Door interaction

`EntityAIBreakDoor` and `EntityAIDoorInteract` require door-block
interaction methods that infdev does not have. Deferred.

### 6.5 Breeding and taming

Infdev animals have no breeding, no baby entities, no wheat-following, and no
taming. All breeding-related tasks (`EntityAIMate`, `EntityAIFollowParent`,
`EntityAITempt`, `EntityAIPanic`) are deferred.

### 6.6 Persistence of existing mechanics

The following are **not** part of the AI framework and are **not** replaced by
tasks:
- The fuse state machine on `EntityCreeper` (managed directly in the entity,
  driven by `EntityAICreeperSwell`).
- The shearing mechanic on `EntitySheep` (`attackEntityFrom`).
- Milking on `EntityCow` (`interact`).
- The skeleton arrow cooldown (`attackTime = 30` in `EntityAIArrowAttack`).
- Daylight burning on zombies and skeletons (`tryBurnInDaylight()` in
  `onLivingUpdate`).
- Spawn-light check (`getCanSpawnHere`).
- The `dataWatcher` state for creeper fuse (`getCreeperState` /
  `setCreeperState`).

---

## 7. How the new framework relates to the existing inline methods

### 7.1 The naming split: infdev vs. r1.2.5

The old method is called `updatePlayerActionState` in infdev (and
`updateEntityActionState` in r1.2.5). The two names refer to the same concept.

### 7.2 Does the new framework run by itself?

**Yes, once `isAIEnabled()` returns `true`.** The new `EntityAITasks` framework
is fully self-contained and does not call the old `updatePlayerActionState()`
under any code path. Each task is a small, composable object that reads from and
writes to the entity's public movement fields (`moveForward`, `moveStrafing`,
`isJumping`, `rotationYaw`, `rotationYawHead`, `attackTarget`) — the same
fields the old inline method writes.

However, the old `updatePlayerActionState()` is still **called** in the entity's
tick loop unless it is explicitly disabled. The infdev `EntityLiving.onLivingUpdate()`
at line 558 unconditionally calls `this.updatePlayerActionState()` for every
living entity. After migration, `EntityCreature` will override `isAIEnabled()` to
return `true`, and `onLivingUpdate()` will be modified (in Stage 1 of the
implementation plan) to gate the old call behind `!isAIEnabled()` — the same
pattern used in r1.2.5:

```java
// r1.2.5 EntityLiving.onLivingUpdate() (~line 1014)
} else if(this.isClientWorld()) {
    if(this.isAIEnabled()) {
        this.updateAITasks();          // new framework (tasks + targetTasks)
    } else {
        this.updateEntityActionState(); // old inline AI (player / client entities)
        this.rotationYawHead = this.rotationYaw;
    }
}
```

In r1.2.5, the new framework and the old method are **mutually exclusive per
tick**: either the task scheduler runs, or the inline method runs, never both.
The boolean guard `isAIEnabled()` is the only switch.

### 7.3 The old method is still live in r1.2.5

This is a subtle but important point. In r1.2.5, `EntityCreature` still
overrides `updateEntityActionState()` and it still does real work (target
acquisition, path following, wander). The new tasks run in *addition* to it,
not *instead* of it. Both systems drive the same movement fields concurrently.
This means:

- `EntityCreature.updateEntityActionState()` in r1.2.5 is **not deprecated**
  and should not be deleted — it is the base creature behaviour that tasks build
  on top of.
- The plan document's §3 sections (the "Modern equivalent" snippets) intentionally
  do **not** reproduce everything from the r1.2.5 `updateEntityActionState()`
  inline method — that method's body is preserved in `EntityCreature` and
  continues to run under `!isClientWorld()`.

### 7.4 Can the old methods be deprecated after migration?

**Yes, but only for subclasses that fully migrate to the task system.**

The old methods that override `updatePlayerActionState` in infdev are:

| Class | What it does | Can deprecate? |
|---|---|---|
| `EntityLiving` (base) | Idle random strafe/walk for non-creatures | No — `EntityPlayer` needs it (player drives `moveForward`/`moveStrafing` directly; `isAIEnabled()` stays `false` for `EntityPlayer`). |
| `EntityCreature` | Inline creature AI: target, attack, path-follow, wander | **Yes** once all `EntityCreature` subclasses (zombie, skeleton, spider, creeper, giant) register their tasks. The method body becomes dead code under `isAIEnabled() == true`. Mark `@Deprecated` with javadoc: *"Only runs when `isAIEnabled() == false`; set to `true` in the constructor to use the task framework instead."* |
| `EntityCreeper` | Fuse state (`fuseState`, `timeSinceIgnited`) management | **Yes** — the fuse state machine stays in the entity (called from `onLivingUpdate`); the `updatePlayerActionState` body becomes a no-op stub. |
| `EntityMonster` | **No override** of `updatePlayerActionState` — it only overrides `findPlayerToAttack`, `attackEntity`, `getBlockPathWeight`, `tryBurnInDaylight`. Those methods are replaced by tasks or stay as entity-level methods. No deprecation needed here. |

The deprecation pattern for `EntityCreature` and `EntityCreeper` after migration:

```java
/**
 * @deprecated inline AI only; set {@code isAIEnabled() = true} in the
 * constructor to use the task framework ({@link #tasks} / {@link #targetTasks}).
 * This method is called only when {@code isAIEnabled() == false}.
 */
@Deprecated
@Override
protected void updatePlayerActionState() {
    // Empty: behaviour moved to tasks.
}
```

`EntityPlayer` is never touched — it keeps `isAIEnabled() == false` (the default)
and relies on `updatePlayerActionState()` for the base idle strafe when no input
is being given.

### 7.5 Summary

- The new `EntityAITasks` framework is **self-contained** and drives all AI without
  any call to `updatePlayerActionState()`.
- `isAIEnabled()` is the gate: `true` → new framework runs; `false` → old
  inline method runs. Both are never active simultaneously.
- After full migration, `EntityCreature.updatePlayerActionState()` and
  `EntityCreeper.updatePlayerActionState()` can be marked `@Deprecated` and made
  into empty stubs — their behaviour is now in tasks.
- `EntityLiving.updatePlayerActionState()` stays **un-deprecated** because
  `EntityPlayer` depends on it.
- `EntityMonster` has no `updatePlayerActionState` override and needs none;
  its AI methods (`findPlayerToAttack`, `attackEntity`, etc.) are replaced by
  task wiring or stay as entity-level helpers.

