# De-hardcoding proposals — Infdev 20100420 (stage 1)

Survey of hardcoded behaviour in `minecraft/src` that would benefit from
extracting virtual methods onto `Block` (or related classes) so call sites
stop enumerating block IDs. All proposed refactors are byte-for-byte
behaviour-preserving and only reorganise where the knowledge lives.

Reviewed against the `minecraft/src` snapshot at the time of writing.
`minecraft/src_original` is the byte-identical reference and is **not**
touched.

---

## Done

| # | What | Commit |
|---|------|--------|
| #1 | `BlockFire` burn tables — `getEncouragementToFire()` / `getAbilityToCatchFire()` | `fa5b319` |
| #2 | `BlockSand.canFallBelow` — `canBeSubstituted()` | `53df16f` |
| #3 | `BlockCrops.updateTick` — `canGrowCrops(int metadata)` | `fa5b319` |
| #4 | `isBurning()` — `Block`, `BlockFire`, `EntityQueryService` | `57ff324` |
| #5 | `takesLightFromAbove()` — `Block`, `BlockStep`, `BlockFarmland`, `World`, `ChunkCache` | `57ff324` |
| #6 | `getAnimalPathBonus()` — `Block`, `BlockGrass`, `EntityAnimal` | `57ff324` |
| #7 | `EntityFallingSand.canBlockBePlacedAt` — `canBeSubstituted()` | `53df16f` |
| #8 | `hardenedBlock(int)` — `BlockFluid`, `BlockFlowing`/`BlockStationary` (lava) | `57ff324` |
| #9 | `BlockOreCoal` + `BlockOreDiamond` subclasses | `57ff324` |
| #11 | `REDSTONE_WIRE_ID` constant — `RenderBlockRedstoneWire` | `57ff324` |
| +new | `ItemBlock.onItemUse` click-on-flower fix — `canBeSubstituted()` | `53df16f` |
| +new | `Block.onSubstituted(World, int, int, int, int)` — replaced block drops its item; wired through `ItemBlock.onItemUse`, `EntityFallingSand.onUpdate`, `BlockFlowing.flowIntoBlock` (water drops, lava fizzes) | `974069c` |

---

## Remaining proposals

### #10 — `BlockCrops` — `canPlaceBlockAt` / `canBlockStay` duplicate the `canThisPlantGrowOnThisBlockID` check  *(LOW)*

**Where:** `minecraft/src/net/minecraft/game/world/block/BlockCrops.java:21-32`

```java
public final boolean canPlaceBlockAt(World world, int x, int y, int z) {
    return world.getBlockId(x, y - 1, z) == Block.tilledField.blockID;
}

public final boolean canBlockStay(World world, int x, int y, int z) {
    return (world.getBlockLightValue(x, y, z) >= 8 || world.canBlockSeeTheSky(x, y, z))
        && world.getBlockId(x, y - 1, z) == Block.tilledField.blockID;
}
```

**Why hardcoded:** Two methods duplicate the same id check that
`canThisPlantGrowOnThisBlockID` already encodes. Now that `canGrowCrops`
exists (proposal #3), this is even cleaner to fix.

**Refactor:**
```java
public final boolean canPlaceBlockAt(World world, int x, int y, int z) {
    Block below = world.getBlock(x, y - 1, z);
    return below != null && below.canGrowCrops(world.getBlockMetadata(x, y - 1, z));
}

public final boolean canBlockStay(World world, int x, int y, int z) {
    return (world.getBlockLightValue(x, y, z) >= 8 || world.canBlockSeeTheSky(x, y, z))
        && world.canPlantsGrowOn(x, y - 1, z);
}
```
Note: `canBlockStay` already uses `world.canPlantsGrowOn` through the existing
`world.canPlantsGrowOn` method, which dispatches through `canGrowPlants`. But
`canGrowPlants` returns `true` for dirt/grass, not specifically farmland. The
original check was `below == tilledField` so `canBlockStay` should use
`canGrowCrops` too.

**Status:** Not done.

---

### #12 — `BlockLeaves` adjacency table — `wood.blockID` / `leaves.blockID`  *(LOW)*

**Where:** `minecraft/src/net/minecraft/game/world/block/BlockLeaves.java:92`

```java
adjacency[PROBE_CURSOR[i]] = id == Block.wood.blockID   ?  0
                          : id == Block.leaves.blockID ? -2
                          :                              -1;
```

**Why hardcoded:** The 9×9×9 flood-fill for leaf decay classifies blocks by id:
log = 0, leaves = -2, other = -1.

**Refactor:** Add a virtual method to `Block`:
```java
public int leafDecayCategory() { return -1; }   // -1 = opaque wall
```
Override: `BlockLog` returns 0, `BlockLeaves` returns -2.

Low priority because tree mechanics rarely change.

**Status:** Not done.

---

### #13 — Block id literals in the middle-click dev tool  *(LOW)*

**Where:** `minecraft/src/net/minecraft/client/Minecraft.java:818-829`

```java
int blockId = this.theWorld.getBlockId(blockX, blockY, blockZ);
if(blockId == Block.grass.blockID)        blockId = Block.dirt.blockID;
if(blockId == Block.stairDouble.blockID)  blockId = Block.stairSingle.blockID;
if(blockId == Block.bedrock.blockID)      blockId = Block.stone.blockID;
```

**Why hardcoded:** A dev tool that gives the player a block on middle-click
substitutes grass→dirt, double-slab→single-slab, bedrock→stone because those
blocks aren't in the creative inventory.

**Refactor:**
```java
public Block devGiveSubstitution() { return null; }   // null = give self
```
Override: `BlockGrass` → `Block.dirt`, `BlockStep` (double slab) →
`Block.stairSingle`, `BlockBedrock` → `Block.stone`.

Low priority — purely a client-side dev tool.

**Status:** Not done.

---

### #14 — Slab single-slab flat-item rendering exception  *(LOW)*

**Where:** `minecraft/src/net/minecraft/client/render/entity/RenderItem.java:135`

```java
if(!Block.blocksList[itemStack.itemID].renderAsNormalBlock()
        && itemStack.itemID != Block.stairSingle.blockID) {
    scale = 0.5F;
}
```

**Why hardcoded:** Single slabs are excluded from the flat item rendering path
even though they don't render as a normal block.

**Refactor:**
```java
public boolean renderItemFlat() { return true; }
```
Override `false` on `BlockStep` (single slab). Low priority — purely a
renderer special case.

**Status:** Not done.

---

## Priority ordering suggested for the remaining proposals

1. **#10 (BlockCrops duplication)** — cosmetic cleanup, falls out of #3.
2. **#12 (leafDecayCategory)** — low value; defer to if/when tree mechanics change.
3. **#13 (devGiveSubstitution)** — client-side dev tool; low value.
4. **#14 (renderItemFlat)** — renderer; low value.
