# InfdevProject

A RetroMCP (MCP) workspace for the Minecraft **Infdev 20100420** client (`inf-20100420`), originally released in 2010 and written against Java 5. This is a didactic project: the genuine 2010 source in `minecraft/src_original` is being modernized, cleaned up, and documented step by step into idiomatic Java 8 in `minecraft/src`.

## Diary

### 2026-08-27 — Baseline import

- Tracked the original 2010 Java 5 decompiled sources as the working snapshot (`minecraft/src`), byte-identical to the pristine `minecraft/src_original` reference.

### 2026-08-27 — Modernized block, tile entity, and NBT sources (committed)

- Rewrote `net.minecraft.game.world.block.*`, `net.minecraft.game.world.block.tileentity.*`, `com.mojang.nbt.*`, `World`, and `NBT` plumbing for Java 8: generics/diamond operator, try-with-resources, enums, clean naming and structure, without changing behavior.
- Fixed a modernization slip afterwards: restored the removed constructor parameters of `StepSoundGlass` and `StepSoundSand`.

### 2026-08-27 — Backported `EntityFallingSand` (committed)

- Brought sand/gravel falling-block physics in from a later version: `EntityFallingSand`, `RenderFallingSand`, `BlockSand` update handling, `World` per-chunk falling-block updates, and `ChunkProviderGenerate` wiring.

### 2026-08-27 — Modernized the render pipeline, world core, and item use (in progress, uncommitted)

- Rewrote the whole `net.minecraft.client.render.*` package for Java 8: the camera/frustum trio (`ClippingHelper*`, `Frustrum`), `EntityRenderer`, `RenderGlobal`, `RenderBlocks`, `WorldRenderer`, `Tessellator`, `RenderEngine`, sorters, entity renderers, and texture FX.
- Modernized the world core (`World`, `Chunk`, `MetadataChunkBlock`) and the remaining block/item classes.
- **Bugs found and fixed during the cleanup**:
  - Frustum matrix index bug in `ClippingHelperImplementation` — the projection matrix was indexed as a row block (`projectionMatrix[projectRow * 4 + k]`) instead of a column (`projectionMatrix[projectColumn + 4 * k]`); this was the root cause of chunks visibly disappearing/reappearing when turning or walking. Verified mathematically and in-game.
  - Far plane extended from a fixed 64-block distance to the full loaded grid (`farPlaneDistance = getGridWidth() << 4`) so terrain no longer pops out of the sky at the horizon.
  - Fog start/end now anchored to the grid half-extent (start `gridHalfExtent * 0.25F`, end `gridHalfExtent`) so fresh chunk generation materializes behind a fog wall instead of popping in.
  - `RenderGlobal.updateRenderers` pacing restored to the original `2500.0F` throttle so renderer recompiles are spread evenly across frames instead of lumped into one hitch.
- **New abstractions**: introduced `IBlockAccess` + `ChunkCache` so the block render path (`RenderBlocks`, `Block.getBlockBrightness/shouldSideBeRendered/getBlockTexture`) queries blocks through a lightweight accessor instead of `World` directly.
- **Item placement**: `Item.onItemUse` / `ItemBlock.onItemUse` / `Block.onBlockPlaced` now receive the exact hit position on the clicked face (`xWithinFace`, `yWithinFace`, `zWithinFace`), passed through from `Minecraft`'s ray trace.

A short diary entry is appended to this section after every code change.

### 2026-08-28 — render pipeline modernization + renderer bug fixes

- Modernized the full render package, world core, and item use to Java 8 (see work section 4).
- Fixed the frustum matrix index bug (root cause of disappearing/reappearing chunks), extended the far plane to the grid width, anchored fog to the grid half-extent, and restored the original renderer compile pacing — all verified correct in-game.

### 2026-08-28 — render package local-variable cleanup

- Renamed every decompiled `var#` local in `net.minecraft.client.render.*` (entity renderers, `RenderEngine`, `Tessellator`, `ItemRenderer`, texture FX, image download) to meaningful names, and collapsed decompiler noise (`var10001`-style copies) — behavior verified by re-reading against `src_original`.
- `ImageBufferDownload.setAreaTransparent/setAreaOpaque` now use their x/y parameters instead of the hardcoded coordinates that matched the only call site.
- Restored `TextureWaterFX`'s constructor to the pristine `super(Block.waterMoving.blockIndexInTexture)` (the working copy had drifted to `+1`).
- Verified each texture FX constructor (water `+32`, lava base, flame/gear indices) still matches `src_original` exactly.

### 2026-08-28 — Tessellator optimization + per-type block renderers

- Optimized `Tessellator`: the fixed-capacity vertex buffer that overflowed into an emergency mid-batch draw was replaced by a growable buffer (`ensureCapacity`, doubling from an 8 MB start), so geometry is never split mid-draw; stride/offset/UV ink now live behind named constants (`INTS_PER_VERTEX 8`, `BYTES_PER_VERTEX 32`, `UV_OFFSET 12`, `COLOR_OFFSET 20`, …). Public API unchanged.
- Added `Tessellator.getUseVBO()` / `setUseVBO(boolean)` with lazy VBO-ID allocation, and verified the `useVBO=true` path (per-`draw()` `glBufferDataARB(GL_STREAM_DRAW)` upload, pointer offsets 12/20/0, stride 32, ring of 10 buffers) stays coherent with the growable buffer.
- Refactored `RenderBlocks`: each block render type now lives in its own `BlockRenderHandler` implementation (`RenderBlockNormal`, `RenderBlockPlant`, `RenderBlockTorch`, `RenderBlockFire`, `RenderBlockFluid`, `RenderBlockLadder`, `RenderBlockCrops`) registered in the `BlockRenderType` enum, whose `BY_RENDER_TYPE` array keeps dispatch a single O(1) bounds-checked lookup — `renderBlockByRenderType`/`renderBlockOnInventory` are no longer a big if/else.
- Extracted shared constants (`NEIGHBOR_OFFSETS`, `SIDE_LIGHT`, `SIDE_NORMALS`, `TILE_MASK 15`, `TILE_ROW_MASK 240`, `TEX_ATLAS_SIZE 256.0F`, `TEX_TILE_INSET 15.99F`) and the six face emitters into the slim `RenderBlocks` engine; the fluid renderer also gained a genuine implementation from its `src_original` branch. All geometry and draw order re-verified against `src_original`; compile EXIT=0 and 253 classes deployed to `minecraft\output`.

### 2026-08-29 — centralized texture-atlas UV system + render-type dispatch

- New `util` package centralizing all atlas math so a texture resize is a two-number change: `TextureAtlas` (enum `TERRAIN(256,256)` / `ITEMS(256,256)`, the `width`/`height` constructor args; static `TILE 16`, `TILE_INSET 0.01F`, `tileSpan 15.99F`), `AtlasTexel` (pixel origins), `TexelScale` (pixel→normalized), `AtlasUV` (full-tile and pixel-rect quad bounds), `AtlasUVBounds` (v scaled by the block's `minY/maxY` so thin blocks stretch their tile). RenderBlocks' own atlas constants are gone.
- Migrated every terrain/items UV site onto the util system: the six `RenderBlocks` face emitters (full-tile via `AtlasUV`, side faces via `AtlasUVBounds` + a real `mirrorTexture` flag), all block handlers (crops, ladder wall/rail/repeater/vine, lily pad, redstone wire strips, fire rows, ladder parity columns, torch head, lever pad), item icons in `ItemRenderer`/`RenderItem` (per-atlas choice `TERRAIN`/`ITEMS`) plus the on-fire overlays, entity fire quads in `Render`, and the digging/water particles (`EntityFX`, `EntityDiggingFX`, jitter folded into the atlas-relative offsets) — requested so particles follow an atlas resize too. Phone-0.006px deviation on particle spans accepted for unification.
- Recreated `BlockRenderType` with the full era table (0–15, 18, 20, 23; redstone wire shares slot 5 with the ladder panel, ladder registered last so it wins) and ported `RenderBlockBed` (type 14) from Beta 1.7.3 with the `BlockBed` metadata helpers inlined (`direction = meta & 3`, `foot = meta & 8`) — beds postdate this build, so it is registered ahead of the expansion stage. In-game types actually used (0,1,2,3,5,6) all dispatch correctly.
- Fixed `RenderBlockPane.canConnect` to test the actual neighbour cell (`getBlockId(x, y, z±1)` / `(x±1, y, z)`) instead of the broken `getBlockY` call, and fixed the door handler's negative-tile mirror to toggle `mirrorTexture` (the flag the side emitters actually read) instead of `flipTexture`.
- Full-tree compile EXIT=0 (272 classes in `minecraft\output`) and the client boots to the Infdev main menu cleanly; run also surfaced a pre-existing classpath quirk (trailing newline present in an external build script) that broke `deobfuscated.jar` resource lookups (`/misc/gear.png`), now avoided.

### 2026-08-29 — EntityLiving hierarchy cleanup

- Examined every entity extending `EntityLiving` (`EntityCreature`, `EntityPlayer`, the animals Pig/Sheep and the monsters Creeper/Zombie/Skeleton/Spider/Giant, plus the client's `EntityPlayerSP` and the `RenderLiving`/`RenderCreeper` consumers) and refactored the whole hierarchy:
  - **Readable names**: all decompiled `var#` locals renamed (creature pathing/wander block, player drop/armour/damage pipeline, skeleton bow shot, spider pounce, `EntityPlayerSP` NBT `var10002`-style copies). Cryptic fields renamed and documented: `newPosZ/newRotationYaw/newRotationPitch` → `prevLimbSwing/limbSwing/limbSwingPitch` (updated in `RenderLiving`), `creeper.c()` → `EntityCreeper.getFuseProgress(float)` (updated in `RenderCreeper`, both call sites), private `creeperState` → `fuseState`.
  - **Dead code removed**: write-only `EntityLiving.rotationYawHead` (original also had a long-gone `prevRotationYawHead`), the `setSize` final passthrough, `EntityPlayer.unusedMiningCooldown` (zero usages), and pure-`super` NBT overrides in `EntityAnimal`, `EntityPig`, `EntityCreeper`, `EntitySkeleton`, `EntitySpider`, `EntityMonster`, `EntityPlayer` (kept in `EntitySheep` "Sheared" and `EntityPlayerSP` inventory/score — the only real payloads).
  - **Shared helpers** extracted in `EntityLiving`: `playSound(String)` (volume 1.0, ±0.1 random pitch jitter, used by living/hurt/death sites), `moveInFluid(float damping)` (0.8 water / 0.5 lava, with the `posY` snapshot the fluid-borne check needs), `wrapAngleTo180(float)` + angle-unwrap loop; and `EntityMonster.tryBurnInDaylight()` reused by Zombie and Skeleton (their duplicated daylight-fire block). Float-vs-double `atan2`→yaw formula kept inline per class on purpose (different intermediate precision).
  - **Java 8**: collision loops in `EntityLiving`/`EntityPlayer` now `forEach`/enhanced-for; the player inventory animation and armor scan use enhanced-for (indexed where a slot must be nulled). Hot numeric loops (200-sample wander, path-node walk) stayed loops.
  - **Behavior preserved**: integer/float math, RNG draw order (even the three no-op `Math.random()` draws in the `EntityLiving` constructor are kept so the shared RNG stream stays aligned), the spider's "airborne mid-range roll does nothing" quirk, and the player's armour 25-hopper with remainder carry-over. `super.isDead` writes normalized to `this.isDead`.
  - Full-tree compile EXIT=0 (272 classes); client launched and ran 45 s with no exceptions across the entity tick path (world-generated main-menu boot).

### 2026-08-29 — Base `Entity` and remaining entities cleanup

- Rewrote the base class `Entity` (`net.minecraft.game.entity.Entity`): the movement/collision solver got documented, meaningful locals (`startX/startZ`, `wantedX/Y/Z`, `startBox`, `colliders`, `touchDown`, `sideResultX/Y/Z`, `steppedBox`, …); the private `unknownBool` became `keepMovingOnCollide` with its dead-but-faithful branches explained; unused arg names (`unusedDeltaY`, `amount`, `partialTick`) are kept and documented. `moveEntity` stays on plain enhanced-for loops — the game's hottest path — with a javadoc note on that choice.
- Rewrote `EntityList` (maps → `STRING_TO_CLASS`/`CLASS_TO_STRING`, reflection collapsed to `getConstructor(World.class).newInstance(world)`), `EnumArt` (named fields/ctor args), `EntityPainting` (art-placement loop, `setDirection` geometry, `onValidSurface` now a stream `noneMatch`, NBT art lookup via `Arrays.stream(...).filter(...).findFirst()`), `EntityTNT` (named vars, the post-decrement `fuse--` semantics kept and commented), `EntityItem` (opaque-cube face-selection rewritten with named `openFaceMinusX/…` + `pushDirection`/`pushSpeed`, the 6-way push kept as a `switch`), `EntityFallingSand` (already modernized; added class docs).
- Rewrote `InventoryPlayer` with `IntStream.range(...).filter(...).findFirst().orElse(-1)` slot scanners and a fully named `storePartialItemStack` merge/drop split; kept the public (misnamed) `getFirstEmptyStack` API used by the client. `EntityArrow` now finds its closest target with `stream().filter().map(EntityHit).min(Comparator.comparingDouble(...))`, ray-trace/BVH intercept, unpacked `inGround`/block-pin logic, named smoothing/drag locals.
- Preserved every public field read by renderers (`EntityItem.item/age/hoverStart`, `EntityTNT.fuse`, `EntityArrow.arrowShake`, `EntityPainting.art/direction`) and the exact save-format writes (byte-vs-short quirks, `& 255` reads). Full-tree compile EXIT=0 (272 classes); client launched and ran 45 s with no exceptions — the only startup log noise is the known environmental `Stopping!`/`Failed to add entity` window-close at the main menu seen before this cleanup.

### 2026-08-29 — Item package modernization

- Rewrote all 16 `net.minecraft.game.item` classes to idiomatic Java 8 with javadoc throughout. `Item` is now self-documenting: the register-order-as-id convention (block items own slots 0–255; custom items live at `256 + order`) is explained in the class doc, the 67-entry static catalogue uses a `register(Item, int)` helper (atlas sprite applied, item returned for chaining) with named `Item.XXX` fields, the `diamod` misspelling is kept and commented as public API, and the private stale field setter/`shouldPassSneakingClick` removed as unreached.
- **Reuse**: new `Item.neighbourAcrossFace(side, x, y, z)` helper computes the cell stepped into through a face, now shared by `ItemBlock` and `ItemFlintAndSteel` (their duplicated side→offset chains collapsed); `ItemSword`, `ItemSpade`, `ItemAxe`, `ItemPickaxe`, `ItemHoe` all forward their real constructor args instead of swallowing them (e.g. `super(26)`/`super(39)`/`super(3)`/`super(5)`/`super(65)` hardcodes gone).
- **Tidy-ups**: `ItemTool` fields made `final` (the dead `= 4.0F` initializer dropped, stats now computed in one place — `maxDamage = 32 << tier` with the diamond `<<= 1`, `efficiency = (float)((tier+1)<<1)`, `damage = base + tier`); `getStrVsBlock` uses `Stream.of(...).anyMatch`; `ItemAxe`'s misnamed static array field renamed; `ItemStack` constructors lost the redundant `this.stackSize = 0;` noise; `ItemPickaxe.canHarvestBlock`'s nested ternary is now early returns ordered obsidian→diamond/gold→steel→rock; `ItemPainting`'s side→facing chain is a `switch`; `ItemHoe` lost its single-iteration `for(var9 = 0; var9 <= 0; ++var9)` decompiler loop (direct spawn, RNG draw order `nextInt(8)` → two `nextFloat` preserved).
- **Behavior preserved exactly**: `ItemBlock` keeps the 9-arg placement signature and the `&&`-binds-tighter-than-`||` replaceable-material check, `blockID - 256` construction (`Block.java:434`) matching `itemID + 256` recovery, `ItemArmor`'s `*`-before-`<<` durability (`maxDamageArray[type] * 3 << tier`), `ItemFood` healing via `player.heal(healAmount)`, float-form vs double-form sound coordinates where the original used each, and `ItemSoup` returning the fresh `new ItemStack(Item.bowlEmpty)`.
- Full-tree compile EXIT=0 (272 classes in `minecraft\output`).

### 2026-08-29 — Metadata-aware tool effectiveness

- `ItemTool.getStrVsBlock` now matches on block *and* block state: `blocksEffectiveAgainst` became an `ItemStack[]` whose `itemDamage` field holds the required metadata, with `-1` meaning "any". Matches are `entry.itemID == block.blockID && (entry.itemDamage == -1 || entry.itemDamage == metadata)`; a `blockStack(Block, int)` helper builds the list entries.
- Threaded metadata through the digging path: `Item` and `ItemTool` gained `getStrVsBlock(Block, int metadata)` (old 1-arg forms kept as delegating shims), `Block.blockStrength(EntityPlayer, int metadata)` was added (the 1-arg delegates with `-1`), and `PlayerControllerSP.clickBlock`/`sendBlockRemoving` now fetch `world.getBlockMetadata(...)` so the tool speed tables see the real state. Dropped a no-op `1.0F *` multiplier along the way.
- Existing entries converted to metadata `-1`, and the obvious gaps filled: `ItemAxe` gained the crafting table (`workbench`); `ItemPickaxe` gained `brick`, `obsidian`, and both furnace variants (all `Material.rock` — notably fix, the diamond pickaxe could previously *harvest* obsidian but never dug it at tool speed); `ItemSpade` gained `tilledField` (farmland, `Material.ground` like dirt).
- Full-tree compile EXIT=0 (272 classes in `minecraft\output`).

### 2026-08-29 — Centralized armor pipeline on `EntityLiving` + mob armor capacity

- Moved the armor system out of `EntityPlayer` onto `EntityLiving` (r1.2.5 architecture): every living being now has `armorInventory[4]` (index 0 = boots, 3 = helmet, matching `renderPass`/container slots) with `getArmorInSlot`/`setArmorInSlot` accessors, `getTotalArmorValue()` (durability-attenuated rating), `damageArmor(int)` (wear + drop broken pieces), and `applyArmorCalculations(attacker, damage)` — the 25-point hopper with the un-dividable remainder banked in `armorDamageCarryover`. `attackEntityFrom` applies armor only when `attacker != null`, so environmental harm (fall, fire/lava, drowning) bypasses protection exactly as in r1.2.5.
- The player's armor stays in `InventoryPlayer`: `EntityPlayer` now overrides the four hooks to delegate there (and no-ops the equipment NBT hook since the inventory list already persists slots 100–103). `InventoryPlayer.getPlayerArmorValue` renamed to the r1.2.5 name `getTotalArmorValue`, with the wear logic moved in as `damageArmor(int)`; `GuiIngame`/`ItemArmor` javadoc updated. The player's difficulty scaling and 0-damage early-out are untouched.
- **Mobs can carry armor**: `EntityLiving` gains `mightSpawnArmored()` (returns false — a hook subclasses may override) and `addRandomArmor()` (difficulty-scaled piece count, material weighted leather→diamond). `MobSpawner` rolls armor only when the hook says true, so no mob spawns armored today by design. Worn armor drops on death (with its damage intact) via `onDeath`, and is persisted in an `"Armor"` NBT list (`writeEquipmentToNBT`/`readEquipmentFromNBT`, player overrides to no-op).
- **Shared armor rendering**: the four-pass overlay moved from `RenderPlayer` into `RenderLiving` (the two `ModelBiped` armor fittings + the `/armor/*` texture table), reading `getArmorInSlot(3 - renderPass)` — so players, zombies, skeletons and giants all render worn armor through the one inherited `shouldRenderPass`. `RenderPlayer` dropped its private copy; spiders/sheep keep their own pass overrides.
- Behavior notes (r1.2.5-faithful): fully-absorbed combat blows now still trigger the hurt animation (was: silent return), and fall/fire/lava/drown are no longer armor-reduced.
- Full-tree compile EXIT=0 (272 classes); client launched and booted clean (60 s, LWJGL/OpenAL up, no exceptions).

### 2026-08-29 — Recipe & physics packages cleaned up

- **Recipe package**: the three near-identical tiered loops (`RecipesArmor`, `RecipesTools`, `RecipesWeapons`) are now thin data tables over a shared `RecipesTiered` base that owns the registration loop (material tier × shape), with the `'#'`-handle variant passed as a stick by tools/weapons. `RecipesIngots` uses typed parallel `Block[]`/`Item[]` arrays. The `bowlSoup`/`chest`/… ad-hoc recipes in `CraftingManager` switched from `new Object[]{...}` noise to natural varargs, and the symbol→id decoding got a small `ingredientId` helper.
- `RecipeSorter` is gone: `CraftingManager` sorts with `Comparator.comparingInt(CraftingRecipe::getRecipeArea).reversed()` (same descending-by-area, stable); the now-unused size methods/empty decompiler stubs `RecipesCrafting` and `RecipesFood` (and `CraftingRecipe.b()` → `getRecipeArea()`) were deleted. `findMatchingRecipe` is a one-liner stream. The tiered set names were standardized to `addRecipe` (was `RecipesTools.addRecipes`).
- **Behavior preserved exactly** — verified by a runtime probe on the compiled output: still `69 recipes` on startup, identical registration order within the (stable) biggest-first sort, spot-checked grids/stacks such as both mushroom-soup orders, the 3×3 chest ring, gold-apple ring, bow `#X` curve and `arrow` stacks, and the Infdev quirk of chain armor smelted from literal `Block.fire` (kept, commented).
- **Physics package**: `Vec3D` got its three identical `getIntermediateWith*` bodies collapsed into one axis-parameterized helper (threshold kept as `(double)1.0E-7F`, exact interpolation order); `AxisAlignedBB`'s six-plane `calculateIntercept` tail is now a `planePoints[]`/`sideHits[]` loop with a single `isPointOnPlane` check, the ternaries in the three `calculate*Offset` and `intersectsWith` were flattened, and every method gained javadoc. The face codes stay 0 = −Y, 1 = +Y, 2 = −Z, 3 = +Z, 4 = −X, 5 = +X (documented in `MovingObjectPosition`, which also names its hit-type constants). The `IllegalArgumentException("NOOOOOO!")` guard in `expand` is kept verbatim.
- Clean rebuild nag: an incremental compile into the existing `minecraft\output` left a stale `CraftingManager` (missing `RecipesTiered`) that crashed the first smoke run; deleting `output` and rebuilding fixed it. Compile EXIT=0, recipe probe confirmed, client launched and booted clean.

### 2026-08-29 — Recipe & smelting framework (`IRecipe`, `ShapedRecipe`, `ShapelessRecipe`, `FurnaceRecipes`)

- Introduced the `IRecipe` interface (Beta-style, typed against the client-only `InventoryCrafting` craft matrix). `CraftingRecipe` became `ShapedRecipe implements IRecipe` and now takes **`ItemStack` inputs**: ingredients match by item id and damage, with a damage of `-1` as the "match any" wildcard (blocks become `-1` ingredients automatically). Matching is generalized to the matrix's real width/height, so the 2×2 survival grid and the 3×3 crafting table both work — previously only the 3×3 was handled.
- Added `ShapelessRecipe` (beta `ShapelessRecipes`-style, count-aware unordered matching with the same `-1` wildcard) and `CraftingManager.addShapelessRecipe`. No recipe in this version is shapeless, so the framework ships unused — the mushroom soup stays faithfully registered as two mirrored shaped recipes.
- `InventoryCrafting` now carries real width/height and exposes `getStackInRowAndColumn`, and `ItemStack` gained a `copy()`. `findMatchingRecipe` takes the `InventoryCrafting` itself (the GUIs dropped their manual int-array projections), and the recipe list is sorted via `IRecipe.getRecipeSize`.
- New `FurnaceRecipes` singleton replaces `TileEntityFurnace.smeltItem(...)`: recipes pair a complete input `ItemStack` with a complete output `ItemStack`, damage `-1` on the input matches any damage (exact matches win). The six original smelts (ores → ingots/diamond, sand → glass, raw pork → cooked pork, cobblestone → stone) are registered in code; fuel burning logic is untouched.
- Verified on the compiled output: still `69 recipes` with the identical content/order (probe re-checked every grid, incl. the fire-smelted chain armor), plus functional match checks for 3×3/2×2 grids, `-1` wildcards, shapeless matching, and the furnace table. Client booted clean.

### 2026-08-29 — Sneaking port (Hold-Shift to sneak)

- **Early-alpha sneak dynamic** (a1.1.2-faithful): Left Shift (`KeyBinding("Sneak", 42)`, inserted at index 5 of `GameSettings.keyBindings`) toggles `MovementInput.sneak`, which halves the intended step `moveForward`/`moveStrafe` by the classic `* 0.3` multiplier while held, restores them on release, and still lets the player jump mid-sneak.
- `Entity.isSneaking()` now exists: false on the base class, and overridden `final` in `EntityPlayerSP` to report `movementInput.sneak`.
- **Ledge protection**: `Entity.moveEntity` clamps `motionX`/`motionZ` while grounded-and-sneaking if the block under the next step has no ground (the two `0.05D`-stepping `while` loops that retract motion until a collider exists, with the step-up guard preserved via `wantedX = motionX`/`wantedZ = motionZ` iteration clauses) — matches the alpha solver exactly, so sneak-walking near edges no longer topples off.
- **Mob sight concealment** in `EntityMonster.findPlayerToAttack`: a sneaking player is ignored as a target when more than 6 blocks away (`distanceSq > 36.0D`) *and* standing in light level < 7 — moving or leaving the dark lets monsters notice you again. Acquisition-only; existing keep-target/16-m-LOS rules untouched. (Deliberately not copied from the a1.1.2 sources; the rule is implemented per the requested spec.)
- Visual crouch pose/`ySize` sink intentionally left out — this change is behavior-only.
- Verified with a runtime probe on the compiled output (15/15 checks: key = 42, bindings now 12, the 0.3× walk/strafe multiply and release restore, jump-while-sneak, the real `GameSettings`/`MovementInputFromOptions` wiring, base `Entity.isSneaking()` false, and live mob-targeting: control vs sneaking targets at the light/distance boundary). Full-tree compile EXIT=0 (272 classes); client booted clean with no exceptions attributable to the change (the `Stopping!`/`Failed to add entity` main-menu log noise is the pre-existing environmental artifact this project already documents).

### 2026-08-29 — chunk & material packages cleaned up

- **`Chunk`** — rewrote with documented, meaningful names and a class doc explaining the parallel storage planes: the `blocks` byte array, the three 4-bit `NibbleArray` planes (metadata, skylight, blocklight), the per-column `heightMap`, the 8 vertical entity buckets and the packed tile-entity map. The skylight/height machinery (`generateHeightMap`, `relightBlock`, `updateSkylight_do`, `checkSkylightNeighborHeight`) is explained step by step — how the first pass walks each column down to its highest opaque block, how the second pass cross-feeds neighbour columns to shade cliff faces, and how `setBlockID` re-seeds a column on edit. Heat-path loops (entity AABB queries, block marching) deliberately stay raw/enhanced-`for` like the original; a few index-dense loops are annotated.
- **Height-map bug fixed (crash/save-corruption root cause)**: `relightBlock` walked down through *any* block whose opacity was non-zero as if it were transparent when re-descending from the current height, but several blocks have multi-step `lightOpacity` (water 3, leaves 1). Starting from the current surface, a column could drill straight through a partially-opaque block to the cave floor and record a bogus height/skylight, which a later save wrote out and a later boot then failed to spawn on (the `Failed to add entity` / empty-chunk symptoms). The surface is now found at the first non-zero-opacity block, matching how `setBlockID` seeds the column. Also `getBlockLightValue` clamps the subtracted skylight into `[0,15]` (safe; the original's or-representation could leak negatives). No new empty-chunk failures in fresh-world boots.
- **`ChunkProviderLoadOrGenerate`** — named cache ring (`chunkSlot`, `CACHE_MASK`/`CACHE_BITS`, `FILE_BUCKET_MASK`), documented the disk layout (`c.x.z.dat` under base-36 subfolders), the open-addressed cache and the worldgen fan-out: a fresh chunk is populated only once its (NE, N, E) neighbourhood exists, and the three neighbour corners are re-checked as their own squares complete. The dead `emptyList`/`unload100OldestChunks` stub (it always returned false and never evicted) is gone; the generating layer's eviction is still delegated to. `saveChunk`/`loadChunk` use try-with-resources.
- **`IChunkProvider`** — interface parameters renamed and each method documented (the chunk-pipeline contract: exists / provide / populate / save / evict).
- **`NibbleArray`** — the bit-twiddling get/set indexed with named constants (`NIBBLE_BITS`, `NIBBLE_MASK`) and the 2-nibbles-per-byte, cell-index `(x<<11 | z<<7 | y) >> 1` packing explained; `isValid` annotated as the "plane missing" check.
- **`Material` package** — each material class got javadoc; the 16 material constants are annotated. Base methods documented with their real semantics: `isSolid()` is "has a collision box / can be stood on", `getIsSolid()` is the distinct "forms an opaque body with a top" used by fire/​splash/pathing (deliberately different for liquids — water is non-solid but still a surface), `getCanBlockGrass()`, and the fluid helper `liquidSolidCheck()`. Kept every public method name and all override behavior; only the genuinely duplicate null-op stanzas were consolidated.
- **`BlockFluid`** — the earlier cleanup had left calls to the removed private `liquidSolidCheck(...)` helper as bare statements; replaced with the equivalent `world.getBlockMaterial(...).liquidSolidCheck()` receiver-style calls so the fluid flow/splash path compiles and runs again as originally intended.
- Full-tree compile EXIT=0 (276 classes); client launched and booted clean on a fresh world (60 s, LWJGL/OpenAL up, no Java exceptions, no `Failed to add` / `Wrong location` in stdout).

### 2026-08-29 — terrain (`world.terrain`) cleanup + a real height-map crash fix

- **`ChunkProviderGenerate`** — the whole worlds-gen provider rewritten with documented, meaningful locals and a three-stage class doc (coarse 5×5×17 height field → tri-linear up-sample to full 16×16×128 fill → per-column surface pass for grass/sand/gravel/dirt/water). Named constants for the grid (`GRID_WIDTH 5`, `GRID_HEIGHT 17`, `GRID_DEPTH 5`, `NOISE_ARRAY_SIZE 425`, `SEA_LEVEL 64`). Decompiler noise removed (dead `boolean var5` re-assignments, `var10003/var10005`, the `var71 = this` alias). The two throw-away noise generators in the constructor are deliberately kept and annotated — they still advance the shared `Random` stream, so removing them would silently change every later generator's seed.
- **`generate`/`noise` subpackages** — `WorldGenerator` gained a documented abstract contract (+ `setScale` javadoc); `WorldGenMinable`'s rotated-ellipsoid ore vein is named/documented (angle, two centre heights, sine-bulged per-step radius, `radius = radiusHalf` reuse of the equal `var28`/`var30`); the three noise classes fully renamed and documented — `NoiseGeneratorPerlin`'s `smoothStep` fade curve extracted into one static helper, its gradient/lerp arithmetic annotated, and the grid-sampling `populateNoiseArray` restructured (the `var10001` write-fold, the y-lattice-gradient cache made explicit); `NoiseGeneratorOctaves`'s infdev scaling (`+= value/frequency; frequency /= 2`) kept verbatim and explained.
- **`WorldGenBigTree`** — the decompiler mess (nested `label134`/`while(true)` loops, `var47/var57/var38` self-aliases, `var10001` swaps) rewritten into two named line-draw helpers (`placeBlockLine`/`checkBlockLine`), a branch-collection pass and a leaf-disc pass, with the whole crown/branch algorithm documented. The dead third argument to `placeBlockLine` (ignored in the original, which always placed wood; both call sites passed `17`) was dropped, and the raw block ids are now `Block.wood/Block.leaves.blockID`.
- **Real crash bug fixed in `Chunk` (from the previous chunk cleanup)**: `generateHeightMap`/`relightBlock`/`getHeightValue` indexed the 16×16 `heightMap` with the block-plane `Z_SHIFT = 7`, overrunning the 256-cell array (`z<<7|x` reaches 1935) and throwing `ArrayIndexOutOfBoundsException` the moment any world terrain was generated — the client could not actually produce a world. Fixed with a dedicated `HEIGHTMAP_Z_SHIFT = 4` (index `z×16 + x`, as in the original `<< 4`), added as its own constant with a comment warning against reusing `Z_SHIFT` here. (The earlier boot checks evidently never populated enough of a world to trip it.)
- **Behavior verified end-to-end against `src_original`**: a headless probe drove the real `World`/`ChunkProviderLoadOrGenerate`/`ChunkProviderGenerate` (+ populate: ores and trees) over 49 chunks with a fixed seed and hashed every block + height — original and refactored output were byte-for-byte identical; the Perlin/Octaves noise generators matched across ~3 M samples. Full-tree compile EXIT=0 (276 classes).

### 2026-08-29 — fixed the quadrant-biased ore bug in `WorldGenMinable`

- **Real historical bug found and fixed.** In this Infdev build (and several later alpha/beta versions) the ore-vein placement loop in `WorldGenMinable.generate` bracketed its `vx/vy/vz` sweeps with a plain `(int)` cast: `for(vx = (int)(centreX - radiusHalf); vx <= (int)(centreX + radiusHalf); ...)`. Java's `(int)` truncates toward zero, so a vein whose centre sits on a negative coordinate loses a cell (e.g. `(int)(-12.2)` is `-12` while `(int)(+12.2)` is `12`), which squashes that vein and makes fewer ore blocks appear wherever the terrain samples the negative end of an axis — i.e. every quadrant with any negative sign, worst when both x and z are negative.
- **The fix** changes those three bounds to floor: `(int) Math.floor(centreX - radiusHalf)` / `...+ radiusHalf`. This leaves positive-coordinate veins byte-for-byte unchanged and mirrors a negative-centre vein exactly onto its positive-centre twin. An on-disk comparison over a fresh symmetric region showed the previously under-served `X- Z-` quadrant (and the two mixed-negative quadrants) rising to parity while the `X+ Z+` quadrant stayed bit-identical: gold `{673, 815, 630, 703}` → `{779, 860, 685, 703}` and diamond `{176, 207, 140, 158}` → `{208, 218, 144, 158}` across three fixed seeds.
- **Isolated proof** (drove the vein loop on a uniform all-stone grid with the real `populate` RNG seeding): before the fix one quadrant placed ~2.80 M ore blocks vs ~3.26 M in the others (a ~14% deficit); after flooring, all four quadrants land within ~0.2% of each other. Full-tree compile EXIT=0 (276 classes).

### 2026-08-30 — client package naming/documentation sweep + enum-driven GameSettings

- Renamed the decompiler's `varN` parameters/locals to meaningful context names and added teaching comments across the entire `net.minecraft.client` tree (62 files beyond render): root client classes, `controller`, `effect`, `gui` + `gui/container`, `model`, `player`, and `sound`. Preserved every public class/field/method name and all semantics; left `render/*` (already documented) untouched. Full-tree javac 1.8 compile EXIT=0.
- **Rewrote `GameSettings` option handling** on a data-driven `GameOption` enum: each constant declares its options.txt key, `OptionType` (BOOLEAN / INTEGER / FLOAT), display name, optional level labels, and lambda getters/setters over the value fields plus an optional "on changed" side effect (`onSoundOptionsChanged` for music/sound, `refreshTextures` for anaglyph). This replaces the nine separate `if`s for applying options, the nested-ternary label builder, and the load/save if-chains with a single lookup loop.
  - Adding an option is now: declare one enum constant (+ a backing field if new); `numberOfOptions` derives from `GameOption.values().length` and save/load/display/cycling dispatch automatically.
  - Integer options wrap through their level count generically (replaces the fixed `& 3`), float 0-1 options step by `FLOAT_OPTION_STEP` and clamp to `[0,1]`; the public int-based API (`getKeyBinding(int)`, `setOptionFloatValue(int,int)`, `getOptionDisplayString(int)`, `setKeyBinding(int,int)`, `numberOfOptions`, all value fields) is unchanged and call sites in `GuiOptions`/`Minecraft` still work.

### 2026-08-30 — path package rewrite + block/terrain documentation pass

- **`world.path` rewritten**: `Pathfinder`, `Path`, `PathEntity`, `PathPoint` fully renamed and javadoc'd — the A* walk (binary-heap sift-up/down in `Path`, `getSafePoint`/`openPoint`/`getVerticalOffset` in `Pathfinder`, the partial-path fallback to the closest point, and the `stepUp` 1-block-climb rule). Public API and behavior preserved exactly: `createEntityPathTo(Entity, Entity, float)` keeps its (unused, hardcoded 16.0F) range param, candidate gathering order, node costs, and the `IllegalStateException("OW KNOWS!")` heap guard kept verbatim; a dead unreachable branch in `getSafePoint` was removed as behavior-identical. Decompiler-only locals (`varN`) are all gone.
- **`world.block` javadoc sweep**: class docs added to the remaining block subclasses (fluid moving/still/source, sponge, slab, chest, furnace, container, torch, fire, crops, farmland, leaves/breakable, stone/ore, TNT/gears, bookshelf/workbench/log, glass/sand/gravel, flower/sapling/mushroom, dirt/grass) and the `StepSound` family + `world.block.tileentity`. Genuine quirks are kept and annotated rather than "fixed": the fluid behavior suppressors in `BlockFlowing`/`BlockStationary` (verified meaningful against `src_original`), the half-finished sponge water-absorption stub, the never-firing `BlockFluid.randomDisplayTick` (`nextInt(128) == -1` cannot occur), `flow()` always returning false, the inert `getBlockMaterial(...).liquidSolidCheck()` probe reads, the furnace reporting the chest's "Chest" name and its `"Lit: ..."` println, and the chest's 36-vs-27 slot array.
- **`ChunkProviderGenerate`**: annotated the tri-linear height-field up-sample and the per-column surface pass (index layout, sand/gravel/dirt thresholds); corrected a misleading `BlockFluid.update` comment left over from the earlier sweep.
- Full-tree javac 1.8 compile EXIT=0.

### 2026-08-30 — `ChunkProviderGenerate` split into an abstract pipeline + `ChunkProviderGenerate420`

- Reworked the terrain provider into the classic staged architecture. `ChunkProviderGenerate` is now an **abstract** class owning the `World`/`Random` state and the fixed `provideChunk` template: re-seed the per-chunk RNG, set up the block buffer, then delegate to `generateTerrain(chunkX, chunkZ, blocks)` (which itself calls `initializeNoiseField(chunkX, chunkZ)` to produce the coarse 5x5x17 height field), `replaceBlocks(chunkX, chunkZ, blocks)` (the per-column surface pass), and finally regenerate the height map. The old monolithic `provideChunk` body is gone — each stage is now a named, overridable hook.
- The actual generation moved to a new concrete `ChunkProviderGenerate420` subclass (the generator for the `inf-20100420` world format), which `World` now instantiates. It keeps the noise generator fields, the fidelity-critical construction order (the throw-away `Random`-stream advance retained), and all the numeric upsample/surface loops with their index-dense inline comments.
- `populate` (ores + trees) stays the third stage but is **not** called from `provideChunk`: the chunk pipeline (`ChunkProviderLoadOrGenerate`) invokes it once per chunk after its settlement square exists. Hoisting it into `provideChunk` would decorate each chunk twice and before its neighbours are ready, changing terrain output.
- **Java 8**: `populate` drives the fixed-count ore passes and the tree line with `IntStream.range(...).forEach(...)` (ordered, same RNG draw sequence), plus a shared `placeOreVein` helper; `WorldGenMinable` instances are now created once per ore type (stateless across `generate` calls, so reuse is behavior-identical). The hot generation loops deliberately stay classical loops per the project's performance convention.
- **Behavior verified end-to-end**: headless probe compiled both the pre-refactor tree (`3b7bc82`) and the new tree, drove `provideChunk` + `populate` over 49 chunks at a fixed seed and hashed every block id + column height — both outputs hash identically (`1612640944359336683`), including ores, trees and the affected height maps. Full-tree javac 1.8 compile EXIT=0.

### 2026-08-30 — `WorldOptions`: per-world generation preferences (placeholder)

- New `net.minecraft.game.world.WorldOptions` with the four generation flags `generateStructures`, `generateCaves`, `winterMode` and `generateBiomes` — all defaulting to false and unused by generation for now — plus `isXxx`/`setXxx` accessors and `writeToNBT`/`readFromNBT` (NBT keys `GenerateStructures`/`GenerateCaves`/`WinterMode`/`GenerateBiomes`, following the codebase's all-caps NBT method convention used by `Entity`/`TileEntity`/`ItemStack`).
- `GuiSelectWorld.selectWorld` now builds a fresh all-default `WorldOptions` and hands it to `Minecraft.startWorld(String, WorldOptions)`, which passes it into the `World` constructor.
- `World` keeps a public `worldOptions` field: the private constructor receives it, re-reads it from level.dat's `Data` section when loading an existing world (absent keys → false, so pre-existing saves load unchanged), and writes it back via `saveWorld`. The chunk provider is now built as `ChunkProviderGenerate420(this, randomSeed, worldOptions)`.
- `ChunkProviderGenerate` (and its `ChunkProviderGenerate420` subclass) constructors accept and store a `WorldOptions` in a `protected final` field, ready for future generation passes to consume.
- **Verified**: full-tree javac 1.8 compile EXIT=0; reused the headless probe (updated to the new reflected ctor signatures) — worldgen hash on a 7x7 region still `1612640944359336683`, so terrain output is unchanged. A second probe round-tripped a `winterMode=true` world through level.dat (written to disk, read back as true on reload, `generateStructures` still false).

### 2026-08-30 — `WorldType` abstraction + `WORLDTYPE_420`

- New immutable `net.minecraft.game.world.WorldType` describes a world family: the base sky/cloud/fog palette (`0xRRGGBB` longs), the cloud layer height, and the chunk generator that builds the terrain. The generator is created through a stored `ChunkProviderFactory` functional interface — `WORLDTYPE_420` passes the constructor reference `ChunkProviderGenerate420::new` — so adding a type only declares the class. Types register in a `worldTypes` array; `fromId(String)` looks one up by id (null when unknown) and `getWorldTypes()` hands out a defensive copy.
- `WORLDTYPE_420` ("WORLDTYPE_420") carries the previously hardcoded atmosphere values: sky `10079487`, cloud `16777215`, fog `11587839`, cloud height `120` (the constant baked into the cloud-plane renderer).
- `World` gained a public `worldType` field and a `World(File, String, WorldOptions, WorldType)` constructor; the private constructor stores the passed type, overrides it from level.dat's `"WorldType"` id when loading an existing world (absent/unknown id → keep the default, so old saves load as `WORLDTYPE_420`), applies the palette from the resolved type, and builds the chunk provider via `worldType.createChunkProvider(this, randomSeed, worldOptions)` instead of instantiating `ChunkProviderGenerate420` directly. `saveWorld` writes `"WorldType"` with `worldType.getId()`.
- `GuiSelectWorld.selectWorld` now passes `WorldType.WORLDTYPE_420` through `Minecraft.startWorld(String, WorldOptions, WorldType)` as the creation-time default. `RenderGlobal` cloud-plane height now reads `worldObj.worldType.getCloudHeight()` (still 120 → rendering unchanged).
- **Verified**: full-tree javac 1.8 compile EXIT=0 (277 sources); worldgen hash still `1612640944359336683`; probe round-trip writes `"WorldType" = "WORLDTYPE_420"` to level.dat and reloads it, with winterMode and palette values confirmed.

### 2026-08-30 — `EnumSkyBlock`, `Explosion`, `NextTickListEntry` cleanup

- Added javadoc comments to `EnumSkyBlock` (sky/block light value variants), `Explosion` (placeholder documentation), and `NextTickListEntry` (scheduled block tick entry with x/y/z position, block ID, and scheduled time).
- Renamed constructor parameters from decompiled `var#` to descriptive `x/y/z/blockID` names across `NextTickListEntry` and `EnumSkyBlock` constructor for clarity.
- All public API names preserved exactly; behavior unchanged.

### 2026-08-30 — orientation blocks face the player at placement

- `BlockChest` and `BlockFurnace` previously picked their front from the surrounding opaque blocks (`BlockFurnace.onBlockAdded`/`setDefaultDirection` chased the nearest open direction; `BlockChest` derived a per-frame facing from its walls in `getBlockTexture`). They now **always face the player who placed them**, no matter what surrounds the block.
- New shared helper `Block.getPlayerFacing(World, x, y, z)`: the front side (metadata 2..5) points back at the player's position relative to the block centre (`+X → 5, -X → 4, +Z → 3, -Z → 2`; X wins on a tie). It deliberately uses position, not the player's viewing yaw (the b1.7.3 reference used yaw, which orients the front *away* from a player looking at the block). Falls back to the +Z default when no player exists.
- `BlockFurnace.onBlockPlaced` stores the facing in the block metadata and the old surroundings-based `onBlockAdded`/`setDefaultDirection` were removed — important because `Chunk.getChunkBlockTileEntity` lazily re-invokes `onBlockAdded` when a container is materialized, which would otherwise wipe saved facings on chunk load.
- `BlockChest.onBlockPlaced` likewise stores the facing. A lone chest keeps it: the single-chest branch of `getBlockTexture` now reads the stored metadata (2..5, with legacy metadata 0 falling back to the historical +Z default 3). The double-chest branches are untouched — a chest connected to a neighbour still derives its door alignment from the corner walls and ignores the stored facing.
- **Verified with a headless probe** on the compiled output: `getPlayerFacing` returns 5/4/3/2 for player at +X/−X/+Z/−Z, X wins the tie, 3 with no player; a furnace placed with the player to its +X ends with metadata 5 and stays 5 after the player moves; a lone chest placed with the player to its −X ends with metadata 4 and renders its front sprite on the facing side and the back on the opposite; a legacy chest (metadata 0) renders front on +Z; two adjacent chests still exercise the double-chest branches (side textures 26/41). Full-tree compile EXIT=0.

### 2026-08-30 — Flowing water & lava (b1.7.3 flow engine backported)

- Water and lava now genuinely flow. `BlockFluid` was rewritten as the shared base of the two liquid roles, `BlockFlowing` gained the full b1.7.3 flow algorithm, and `BlockStationary` became the settle-and-wakeup role instead of a can-flow-scan stub. The block metadata is now the *flow decay* (0 = source, 1–7 = levels downstream, `+8` = falling column), matching later builds.
- **`BlockFlowing.updateTick`** (ported, renamed, commented): re-breathes each cell's decay from its four horizontal neighbours (`getSmallestFlowDecay`), dries up cells whose cheapest route is exhausted, lets a falling column above feed through without decay limits, regenerates a source when a water cell rests on solid ground (or another source) with two adjacent sources, slows lava (one extra level per step plus a 3-in-4 hold-position roll), then pours one decay-step down any open cell below or, grounded, fans out along the cheapest A*-costed sideways directions (`calculateFlowCost`/`getOptimalFlowDirections`). Quiet cells `settleToStill`.
- **`BlockStationary`**: `wakeToMoving` (the b1.7.3 `setNotStationary`) re-arms a still cell on a neighbour change; still lava keeps a slow smoulder tick that walks upward and ignites flammables in open air — flammability read from `Block.fire`'s spread table since this build has no `Material.getBurning`.
- **Lava/water hardening** (`BlockFluid.checkForHarden`): a lava source meeting water becomes obsidian (no crying variant here), flowing lava (decay 1–4) becomes cobblestone, with the fizz/smoke `triggerLavaMixEffects`. The old harden-to-stone logic is gone.
- **Liquid rendering**: `BlockFluid.getRenderType()` now returns 4 and `RenderBlockFluid` lowers the surface by the decay (`BlockFluid.getPercentAir`, applied to water *and* lava — previously the helper only treated water and used `metadata/9` instead of `(decay+1)/9`). Items still fall back to their flat icon, so water/lava held in the hand is unaffected.
- **Support plumbing**: `Chunk.setBlockIDWithMetadata` (the old `setBlockID` zeroes the metadata nibble, which would veil flow state for a frame), `World.setBlockAndMetadata` / `setBlockAndMetadataWithNotify`, a real `markBlocksDirty` box (with `markBlockNeedsUpdate`, and `markBlocksDirtyVertical` now delegating), and `World.setBlockMetadataWithNotify` upgraded from a silent write to render-mark + neighbour-notify (which also fixes crops/farmland failing to re-render on growth). Removed the dead `Block.isBlockContainer[fluid] = true` line.
- **Verified with a headless probe** on the compiled output (16/16 checks): a source settles to still water (meta 0) and a flat 1-deep sealed plate spreads to its four neighbours at decay 1 and its diagonals at decay 2 (the faithful boxed-cell fan-out); a falling column keeps the `+8` flag on the way down and settles `9/8`; lava hardens to obsidian next to a source and to cobblestone next to flowing lava. Full-tree compile EXIT=0 (276 classes).

### 2026-08-30 — Fixed digging particles rendering tilted (atlas-refactor regression)

- `EntityDiggingFX.renderParticle` had been mangled during the texture-atlas UV centralization: its four vertex positions mixed a random **texture-jitter texel count** and a **UV coordinate** into the X/Z coordinates instead of the billboard's two pitch-rotation components (`surfU`/`surfV`, i.e. `pitchYawCos`/`pitchYawSin`). Each digging/crack particle therefore flew at a random tilted angle rather than facing the camera — the effect this also surfaced when breaking the newly flowing water. Restored the position cross-terms to `surfU`/`surfV` (matching the base `EntityFX.renderParticle` and the pristine `src_original`), kept the correct 4×4 crack sub-tile UV math, and fixed a latent `posY - posY` (should be `prevPosY`) interpolation typo in the same override. Full-tree compile EXIT=0.

### 2026-08-30 — Leaf decay (b1.7.3) + fast/fancy leaf toggle

- **Leaves now decay** (ported from beta 1.7.3). `BlockLog.onBlockRemoval` stamps `BlockLeaves.DECAY_CHECK_BIT` (metadata bit 8) onto every leaf within a 4-block box when a log is chopped. A flagged leaf then runs, on its next tick, a connectivity test: it classifies a 9×9×9 neighbourhood (logs = distance 0, leaves = passable, anything else = wall) and flood-fills outward up to four leaf-to-leaf steps. A leaf within four hops of a surviving log keeps its place (the mark is cleared); an orphaned leaf drops its sapling and vanishes.
- **Optimized for the per-tick hot path**: the scratch grid is a flat, padded `int[]` (`11³`, one-cell pad so every ±1 neighbour stride stays in bounds — no per-cell bounds checks), the neighbour strides are a constant LUT (`NEIGHBOR_STRIDES`), and the world offset + grid cursor of each of the 729 probe cells is precomputed once at class-load (`PROBE_DX/DY/DZ/CURSOR`). The scan/flood write a 5 KB grid instead of the reference's 128 KB sparse array, and `quantityDropped`/`idDropped` (1/10 sapling) are unchanged. Named `removeLeaves` = drop + air.
- **Fast/fancy leaf rendering** (`BlockLeavesBase`): `isOpaqueCube()` now follows the graphics level (transparent "fancy" by default, opaque "fast" when toggled), `setGraphicsLevel(boolean)` swaps between them and moves the texture to the opaque tile one past the transparent one. Left **off** — leaves keep the fancy translucent look by default.
- **Verified with a headless probe** (16/16 checks): chopping a lone log flags the nearby leaves (and not a leaf 6 blocks away), and those flagged leaves decay to air on tick while the untouched leaf stays; a chain of leaves between two logs is flagged when one log is chopped but survives on the remaining log (mark cleared); and the fast/fancy toggle flips `isOpaqueCube` and the texture index and restores cleanly. Full-tree compile EXIT=0 (276 classes).

### 2026-08-30 — Mob spawner O(1) entity counter + Explosion class refactor

- **Explosion class refactored** from b1.7.3: all explosion logic (block destruction via ray-casting, entity damage/knockback, particles) moved from `World.createExplosion` into `Explosion.explode()` + `Explosion.applyEffects()`. `World.createExplosion` now delegates cleanly. All locals renamed and documented; removed the unused `isFlaming`/`ExplosionRNG` fields from b1.7.3.
- **`MobSpawner` counter caching**: `World` now maintains `monsterCount` and `animalCount` as private fields, updated incrementally at every entity-list mutation site (`spawnEntityInWorld`, `updateEntities` death-removal, `addLoadedEntities`, `unloadEntities`). `MobSpawner` now calls `world.getCachedEntityCount(entityClass)` — a single `isAssignableFrom` if/else dispatch returning the O(1) counter instead of scanning `loadedEntityList` every tick. The four helper sites use `instanceof` checks for `EntityMonster`/`EntityAnimal`, so the counter never desyncs as long as entity type hierarchies don't change. `countEntities` kept for general-purpose use. Full-tree compile EXIT=0 (283 classes).

### 2026-08-30 — MobSpawner refactor: named constants + extracted helpers

- Split the monolithic `performSpawning` into focused helpers with descriptive names and Javadoc: `findSpawnPosition` (random base + jittered surface search), `isValidSurface` (solid below, air above, no liquid), `isFarEnoughFrom` (16-block exclusion radius), `createEntity` (reflection instantiation), `trySpawn` (orchestrates validation + armor). Magic numbers replaced with named constants: `SPAWN_HORIZONTAL_SPREAD (128)`, `SPAWN_VERTICAL_RANGE (128)`, `MIN_SPAWN_DISTANCE_SQ (256.0D)`, `MAX_JITTER_HORIZONTAL (5)`, `MAX_JITTER_VERTICAL (1)`, `SURFACE_ATTEMPTS (6)`, `JITTER_ATTEMPTS (6)`. Fields renamed `maxSpawns/entityType/entityClasses`. Removed dead `var2` shadowing. Eliminated the meaningless outer `for(var6 = 0; var6 <= 0; ++var6)` — replaced with a plain scoped block.

### 2026-08-30 — Move MobSpawner ownership from PlayerControllerSP into World

- `MobSpawner` instances were owned by `PlayerControllerSP` and ticked via `playerController.onUpdate()` — a misplaced responsibility: spawning is world game-logic, not player-input logic, and it ran before `world.tick()` instead of inside it.
- `World` now owns `monsterSpawner` and `animalSpawner`, initialized in the private constructor.
- `World.tick()` calls both spawner `tick()` methods (after the player re-attach safety check, before scheduled block ticks).
- `MobSpawner` constructor now takes the owning `World` and stores it in a final field, so the per-tick method is just `tick()` with no `World` parameter.
- All `MobSpawner` helpers drop the redundant `World` parameter and use `this.world` instead.
- `PlayerControllerSP.onUpdate()` reduced to updating `curBlockDamage`; the two `MobSpawner` fields and their 8 entity imports are gone. Full-tree compile EXIT=0 (283 classes).

### 2026-08-30 — Skeleton arrow freeze investigation + defensive null-guards

- Investigated a reported game freeze triggered when a skeleton fires an arrow at the player (`EntitySkeleton.attackEntity` → `EntityArrow` spawn + `setArrowHeading`). Traced the full call path: arrow tick, block ray-trace, entity collision, `onCollideWithPlayer`, item pickup — no infinite loop or unbounded recursion found in the source.
- Added defensive null-guards to prevent silent NPE crashes: `World.playSoundAtEntity` and `World.playSoundEffect` now guard against a null `playerEntity`, and `EntityArrow.onUpdate` guards against a null `worldObj`.
- Added a silent try/catch around the per-entity `onUpdate()` call in `World.levelEntities` so one misbehaving entity cannot freeze the entire tick loop.
- Added `ARROW_DEBUG` flag (`false` by default) in `EntityArrow` that prints a one-time position log when an arrow has been in the air for > 200 ticks — useful for diagnosing stuck arrows without any runtime overhead when off.
- **Root cause of in-game freeze at `playSoundAtEntity`**: `SoundPool.nameToSoundPoolEntriesMapping` was a plain `HashMap`, concurrently mutated by the background `ThreadDownloadResources` daemon (`addSound`) while the game thread read it via `getRandomSoundFromSoundPool`. The download thread held the map lock while doing `file.toURI().toURL()` (and potentially blocking on HTTP network I/O if minecraft.net was slow/unreachable), causing the game thread to block on the same lock inside `getRandomSoundFromSoundPool` — a classic producer-consumer deadlock. Fixed by replacing the synchronized wrapper with `ConcurrentHashMap` (lock-free reads on the map) and synchronizing only on the per-entry `ArrayList` for list operations, with `putIfAbsent` for race-free list creation. `getRandomSoundFromSoundPool` now never blocks on the map; it returns `null` (silent no-op) if the sound isn't registered yet, which is safe since `SoundManager.playSound` already handles a null entry gracefully. Compile EXIT=0.
- **NBT `IndexOutOfBoundsException` on world load**: `Entity.readFromNBT` was reading `Pos`/`Motion`/`Rotation` NBT lists via `tagAt(0/1/2)` without any size check. When `NBTTagCompound.getTagList` returns a fresh empty `NBTTagList()` (because the key is missing from a partial/corrupt save), the very first `tagAt(0)` threw `java.lang.IndexOutOfBoundsException: Index: 0, Size: 0` and aborted world load. Fixed by reading each component with a `tagCount() > N` guard, defaulting to `0.0D`/`0.0F`. `NBTTagList.tagAt` now also throws a descriptive message on out-of-bounds so any future caller surfaces a meaningful stack. Compile EXIT=0.
- **Disabled the legacy `ThreadDownloadResources`**: the daemon was hitting the long-dead `http://www.minecraft.net/resources/` list, leaving 169-byte HTML redirect pages on disk in place of real sound files (e.g. `newsound/random/bow.ogg`). When `getRandomSoundFromSoundPool("random.bow")` returned the broken entry, `SoundManager.playSound` would pass the bogus URL to OpenAL, which would then block trying to decode a non-audio file as an OGG stream. With the launchwrapper already providing every indexed asset under `game/assets/objects/` via the `asset://` protocol, the legacy downloader has nothing useful to do. `run()` is now an empty body, and the stale `bow.ogg` was deleted; missing sounds now silently no-op (which `SoundManager.playSound` already supports). Compile EXIT=0.

### 2026-08-30 - Drop launchwrapper, add Start.java bootstrap with proxy-aware sound download

- Removed the launchwrapper as the entry point. The wrapper was exclusively a sound-download mechanism for this version (textures, fonts, and all other resources come from the classpath via jars/minecraft.jar); with its origin server dead, the wrapper was producing duplicate 404 console lines every run and was making the freeze-fixing download-thread re-fetch same-broken files.
- New entry point: 
et.minecraft.client.Start (replaces org.mcphackers.launchwrapper.Launch in conf/version.json and minecraft/Client.launch). The new bootstrap parses the full launchwrapper CLI surface (--username, --session, --uuid, --gameDir, --assetsDir, --assetIndex, --accessToken, --userProperties, --userType, --versionType, --skinProxy, --fullscreen, --server, --port, --loadmap_user, --loadmap_id) into system properties, then constructs a MinecraftApplet and starts the game thread. MinecraftApplet.getAppletParam falls back to those system properties so the applet's argument-parsing works without an HTML container.
- MinecraftApplet.init() is now NullPointerException-safe: getParameter(name) and getDocumentBase() can both throw when constructed outside a browser; both paths are wrapped in try/catch so the applet starts cleanly via main().
- ThreadDownloadResources rewritten: walks game/resources/ recursively and registers every real .ogg (>= 512 bytes), then attempts to download any known-missing sound (
andom.bow) via HttpURLConnection honouring -Dhttp.proxyHost / -Dhttp.proxyPort. With -Dhttp.proxyHost=betacraft.uk -Dhttp.proxyPort=11702, requests are routed through the betacraft mirror; without it, the direct connection is used. Both modes fall back gracefully on failure (sound silently no-ops, no freeze).
- New SoundPool.hasSound(name) and SoundManager.hasSound(name) accessors let the downloader detect which sounds are missing before attempting a fetch.
- New minecraft/Start.bat for one-click launch on Windows (locates JDK 8, builds the classpath, runs 
et.minecraft.client.Start with the same args the launchwrapper used to receive). Commented-out JVM-args block shows how to enable the betacraft proxy.
- Compile EXIT=0 (285 classes, including Start.class, Start.class, MinecraftApplet.class, ThreadDownloadResources.class). Verified end-to-end: Start.main() -> pplet.init() -> startMainThread() -> Minecraft.run() -> Display.create() reaches the LWJGL init stage cleanly (the only failure in a headless environment is Parent.isDisplayable(), expected when no display is available).


### 2026-08-30 — Distance-based entity culling with timer advancement

- **Distance culling in World.levelEntities()**: entities farther than sqrt(ENTITY_VIEW_DISTANCE_SQ) ≈ 45 blocks from the player now skip onUpdate(). This saves the most expensive per-tick work — EntityCreature's 200 random block samples and A* pathfinding — for distant mobs. The check is a single squared-distance comparison (6 flops per entity, negligible overhead).
- **Timer advancement for distant entities**: to keep despawn timers accurate, 	icksExisted++ is always incremented and, for EntityItem, ge++ is also advanced. Dropped items still despawn at 6000 ticks even when far from the player.
- **ENTITY_VIEW_DISTANCE_SQ = 2048.0D** is a public static final in World, so the radius can be tuned without code changes. Tile entities (furnaces, chests) are always ticked regardless of distance.
- **loadedEntityList.removeAll() remains O(N²)**: the 
emoveAll(entities) call in World.unloadEntities() is unchanged — a future improvement would mark distant entities dead and defer removal to the next levelEntities() pass, eliminating the quadratic scan.
- New chunk_and_entities.md in the project root documents the full chunk lifecycle (two-layer provider pipeline, the 1024-slot ring cache, eviction via hash collision), the entity management system (loadedEntityList, per-chunk entity buckets, levelEntities() tick loop), the distance-culling design, and the chunk→entity cleanup connection.
- Compile EXIT=0 (286 classes); pushed to master.

### 2026-08-31 — First-frame lighting flash fix

- Diagnosed the "fully lit for one frame" flash when loading a night-time world: `World.skylightSubtracted` was hard-initialised to `0` in the constructor, so the first rendered frame used daytime ambient brightness; the value only updated to the saved world's correct value when `World.tick()` ran for the first time.
- Added `World.computeSkylightSubtracted()` which mirrors the day-factor formula from `tick()`, and call it right after `worldTime` is read from `level.dat` (`World.java:167`) so the renderer picks the correct ambient value on the very first frame.

### 2026-08-31 — Entity migration optimization: entity-side chunk cache + mark-dead unload

- **Entity-side chunk cache**: added ddedToChunk, chunkCoordX, chunkCoordY, chunkCoordZ fields to Entity. Migration now reads the entity's cached coords instead of recomputing oldChunkX/Y/Z as local variables every tick. The entity itself is the authoritative record of which chunk it belongs to. B1.7.3 brought this pattern; Infdev didn't have it.
- **Chunk.addEntity**: now sets entity.addedToChunk = true and the three chunkCoord* fields directly — the "Wrong location!" recomputation and check were removed (the entity's own cached coords are authoritative).
- **Chunk.removeEntityAtIndex**: now clears entity.addedToChunk = false after removal. The contains(entity) defensive scan was removed — ddedToChunk is trusted as the authoritative guard.
- **World.levelEntities()**: migration now compares against entity-cached coords (!entity.addedToChunk || entity.chunkCoordX != newX || …). No more 6 loor(pos/16) operations as a local pre-snapshot before onUpdate(). Dead-entity cleanup also uses entity.addedToChunk as the guard (instead of always querying the chunk cache), avoiding a redundant chunkExists call.
- **World.unloadEntities()** (chunk eviction path): replaced loadedEntityList.removeAll(entities) (O(M×N)) with a mark-dead approach — sets isDead = true on each entity, defers removal from loadedEntityList to the existing levelEntities() cleanup loop. Renderer textures still released immediately via worldAccesses loop.
- chunk_and_entities.md updated with the new migration design, the mark-dead chunk eviction flow, and updated performance notes.
- Full-tree compile EXIT=0 (286 classes); pushed to master.

### 2026-08-31 - Fix white/unclosable window on close (X button)

- Root cause of "window goes white and doesn't close on X, task manager required": a JVM-exit deadlock between the AWT EDT and the LWJGL display teardown. The old Start.java installs a shutdown hook (AWT-shutdown) that calls frame.dispose(). At exit, that hook performs an EventQueue.invokeAndWait() back onto the AWT EDT - which is itself stuck tearing down the LWJGL canvas (WComponentPeer.hide). System.exit() then blocks forever waiting for that hook, leaving a white, unclosable window and a live process.
- Start.java windowClosing now runs applet.shutdown() on a dedicated worker thread (never the EDT): it flips Minecraft.running so the game loop unwinds itself through its finally -> shutdownMinecraftApplet() (saves world, closes sound/input/display), then System.exit(0) halts the JVM cleanly.
- Removed the frame.dispose() shutdown hook entirely - it was the deadlock. The OS reclaims the window when the JVM halts.
- Start.bat classpath fixed: bin now comes first (so freshly-compiled .class files shadow stale ones in jars\deobfuscated.jar) and the library jars are listed explicitly because the * wildcard does not expand on Windows java.
- Verified end-to-end with a WM_CLOSE window message: process now exits cleanly and the log shows the graceful "Stopping! -> SoundSystem shutting down" path (previously it hung after that point).

### 2026-08-31 — Split `World` into focused helper collaborators (5-step refactor)

`World.java` had grown into a god class holding every quasi-independent subsystem as private fields plus inline loops. Over five approved, separately-committed and in-game-verified steps, each cohesive subsystem was extracted into its own `final`, package-private class in `net.minecraft.game.world`, constructed inline in `World`'s constructor and placed among the collaborators at the top of `World`'s field block. `World` keeps a thin public delegate for each, so every external caller (`Chunk`, `Minecraft`, `RenderGlobal`, `MobSpawner`) is unchanged. All classes carry javadoc; behavior (integer math, RNG draw order, save format) is byte-for-byte preserved.

- **Step 1 — `AtmosphereCalculator`** (static util): the sky/cloud/fog colour logic and the `getSkyColor`/`getCloudColor`/`getFogColor` computation moved out of `World`.
- **Step 2 — `BlockTickScheduler`** (instance): owns the scheduled-block-tick queue and its update pass; `World.tick()` delegates to `blockTickScheduler.updateTicks()`. The decompiled `unloadedEntityList` field was renamed to the honest `pendingBlockTicks`.
- **Step 3 — `LightingManager`** (instance): owns the `lightingToUpdate` queue plus the tuning constants (`MAX_LIGHTING_BOXES_PER_CALL 100000`, `MAX_LIGHTING_QUEUE_SIZE 1000000`, `LIGHTING_QUEUE_DRAIN_SIZE 500000`). `World` keeps `scheduleLightingUpdate`, `updatingLighting`, `lightUpdatesNeeded` as public delegates (called from `Chunk`, `Minecraft`).
- **Step 4 — `EntityQueryService`** (static util): seven static AABB/block queries (`getCollidingBoundingBoxes`, `getEntitiesWithinAABBExcludingEntity`, `checkIfAABBIsClear1`, `getIsAnyLiquid`, `isBoundingBoxBurning`, `isMaterialInBB`, `getBlockDensity`) moved off `World`. `chunkExists` was widened from `private` to package-private for this.
- **Step 5 — `EntityManager`** (instance): owns `loadedEntityList`, `monsterCount`, `animalCount` and the whole entity lifecycle (`spawnEntityInWorld`, `levelEntities`, `addLoadedEntities`, `unloadEntities`, `getLoadedEntityList`, `contains`, `getMonsterCount`/`getAnimalCount`, `getCachedEntityCount`, `countEntities`, plus the private add/remove count bookkeeping). `worldAccesses` widened to package-private so it can drive the `obtainEntitySkin`/`releaseEntitySkin` notifications; `World.tick()` uses `entityManager.contains(this.playerEntity)`. The tile-entity update loop stays in `levelEntities()` but reads `World.loadedTileEntityList` (still a public field mutated by `Chunk`). Per-frame `levelEntities()` is invoked by `Minecraft` (not by `World.tick()`).
- **TimSort crash fix** (root-caused during this work, committed first as `78e53e3`): the 2010 `RenderSorter`/`EntitySorter` comparators returned only `±1`, violating the `Comparator` contract and throwing `IllegalArgumentException "Comparison method violates its general contract!"` from TimSort at `RenderGlobal.updateRenderers`. Both were rewritten as consistent three-way comparators (`Float.compare(d2, d1)`; `EntitySorter` → `d1<d2 ? -1 : d1>d2 ? 1 : 0`), identical ordering for distinct values, so no sort order changed.
- Full-tree javac 1.8 compile EXIT=0 throughout; the client was launched and smoke-tested after each step (the comparator fix ~2 min, steps 2–5 each ~30–90 s clean with no exceptions, clean exit via the X button). All five steps + the comparator fix are committed to master.

### 2026-08-31 — Prepared the engine for biomes

- Added a `net.minecraft.game.world.biome` package with the two abstractions and their only concrete pair: `BiomeProvider`/`BiomeProviderInfdev` (which biome is at a position) and `BiomeGenerator`/`BiomeGenInfdev` (what terrain that biome makes).
- `Chunk` now stores a per-chunk 16×16 biome-id grid (`byte[256]`, z-major), persisted as a `Biomes` NBT tag; old saves without the tag load as all-Infdev (id 0).
- `ChunkProviderGenerate.provideChunk` fills the biome grid from the world type's `BiomeProvider` before the surface pass; `WorldType` gained a `BiomeProvider` and `getBiomeProvider()`; `World.getBiome(x, z)` reads a column's biome from its chunk.
- Surface pass (`ChunkProviderGenerate420.replaceBlocks`) now delegates each column's stamping to the column's biome, passing the terrain noise (beach/gravel/dirt depth) in; `populate` resolves the center biome and calls its `populateOres` then `decorate`.
- `BiomeGenInfdev` reproduces the previous default behavior verbatim (RNG construction order and draw order unchanged), so terrain, ores and trees are identical to before; approval + detailed design documented in `biome_system.md`.

### 2026-08-31 — Fixed sound registry key mismatch (LWJGL "No codec found")

- `ThreadDownloadResources.registerFromFolder` was passing `assetName = "newsound/random/click"` to `addSound`, which then replaced `/` with `.` → key `"newsound.random.click"`. The game looks up `"random.click"` (no category prefix). Fixed by stripping the `<category>/` prefix from `assetPath` before registering, so `newsound/random/click.ogg` produces the key `"random.click"` matching the engine's pool calls.
- `SoundPoolEntry.soundName` was stored as `fullName` (with the original slash: `"random/click"`), and `LibraryLWJGLOpenAL` uses that string as the audio-file identifier for codec lookup. It couldn't find the `.ogg` codec from `"random/click"` because there was no `.` before the extension. Now stores `soundName` (the slash-normalized key: `"random.click"`), so LWJGL can extract `.ogg` and find the codec.
- `SoundPool.addSound` now appends the real file extension (from `file.getName()`) to the pool key when constructing the `SoundPoolEntry.soundName`. So a file `click.ogg` registered as `"random/click"` ends up with `soundName = "random.ogg"` — LWJGL sees the `.ogg` extension, finds the `CodecJOrbis` codec, and the sound plays. The map key stays `"random"` (the digit-stripped, slash-normalized bare key), so lookups from the engine are unchanged. Compile EXIT=0.

### 2026-09-01 — Extract `Block.itemStackDropped(int, Random)`

Extracted the `ItemStack` construction out of `dropBlockAsItemWithChance` into a new overridable `itemStackDropped(int metadata, Random rand)` method on `Block`. The default implementation calls `idDropped` and `damageDropped` — behaviour is byte-for-byte identical for every existing block. The refactor enables subclasses to attach extra data (enchantments, NBT, display name) to their drops without touching the drop-chance or entity-spawning logic. It also correctly handles blocks that randomise their item id on each call: the stack is now built per loop iteration instead of once per block.

### 2026-09-01 — Backport `WorldGenTrees` (small oak-style tree)

Added `WorldGenTrees` — the small oak tree generator from a1.1.2 that was the default tree choice before large variants appeared. The a1.1.2 source used one-letter variable names throughout and had no comments; this backport:
- Keeps the algorithm byte-for-byte identical (collision column walk, ground conversion, leaf sphere with radius-2 disc bottom, radius-1 disc top, trunk fill).
- Replaced `var8 == Block.grass.blockID || var8 == Block.dirt.blockID` with `world.canPlantsGrowOn(x, y-1, z)` so any future plantable block is automatically a valid tree base.
- Renamed every variable to a meaningful name (`trunkHeight`, `canPlace`, `checkX/Y/Z`, `discRadius`, etc.).
- Added a class-level overview and per-phase inline comments explaining the collision, ground and placement passes.

### 2026-09-01 — Extract plant-support check to a extensible Block hook

Introduced `Block.canGrowPlants(int metadata)` returning `false` by default. `BlockDirt` and `BlockGrass` now override it to return `true`, so any new dirt/grass variants can opt in by doing the same.

Added `World.getBlock(x, y, z)` (null-safe shortcut for `Block.blocksList[id]`) and `World.canPlantsGrowOn(x, y, z)` (true when the block exists and `block.canGrowPlants(metadata)` is true). All plant placement checks in `BlockFlower.canPlaceBlockAt`, `BlockFlower.canBlockStay` and `WorldGenBigTree` now call this single method instead of hard-coding `grass || dirt`. The two subclasses that need different rules override the two methods directly:
- `BlockMushroom` places on any opaque block (no longer tied to `canGrowPlants`).
- `BlockCrops` places and stays only on farmland (its own override, unchanged behaviour).

Removing the dead `protected canThisPlantGrowOnThisBlockID` from `BlockFlower` freed `canPlaceBlockAt` to be non-final, so mushroom and crop subclasses can override it.

### 2026-09-01 — Add b1.7.3 mushroom spread

Mushrooms now spread in dim light exactly like b1.7.3: a 1 % chance per random tick tries to clone the mushroom into a random neighbour air cell (±1 xz, ±1 y) that passes `canBlockStay`. The new mushroom inherits the original's metadata so brown stays brown and red stays red. The `canBlockStay` light threshold was tightened from 13 to 12 (`getBlockLightValue ≤ 12`) to match the b1.7.3 reference.

### 2026-09-01 — Consolidate flowers and mushrooms into metadata-driven block ids

- Replaced the four separate block ids (`plantYellow` 37, `plantRed` 38, `mushroomBrown` 39, `mushroomRed` 40) with two consolidated blocks: `Block.flowers` (id 37) and `Block.mushrooms` (id 38). The variant now lives in the block metadata: flower metadata 0 = red / 1 = yellow; mushroom metadata 0 = brown / 1 = red.
- `BlockFlower` gained a `metadataToTexture` lookup array and `getBlockTextureFromSideAndMetadata` maps metadata → atlas tile: flower `{12, 13}`, mushroom `{29, 28}`. Sapling locks to tile 15. Render type 1 now shows the correct variant in the world and in the inventory (the inventory preview previously passed metadata `-1`).
- World decoration (`BiomeGenInfdev.decorate`) now spawns plants exactly like the a1.1.2 reference: two patches of yellow flowers, red flowers with 50 % chance, brown mushrooms with 25 %, red mushrooms with 12.5 %, each placing the consolidated block with the right metadata. Added `WorldGenFlowers` (a1.1.2 algorithm: 64 scatter attempts in a 16-block box over an air cell that `canBlockStay` accepts).
- Crafting: mushroom soup now requires one brown (metadata 0) and one red (metadata 1) mushroom, in either order. `Session.registeredBlocksList` lists the two consolidated blocks instead of the four variants.
- Drops: added the `Block.damageDropped(int metadata)` hook so consolidated blocks drop an item carrying their variant metadata (base class still drops damage-0); flowers/mushrooms pass metadata through, saplings/crops override back to 0.
- Removed the `getRenderColor` per-variant tint misuse; the plant tiles already carry baked-in colours, and per-variant selection is purely a texture-index swap.

### 2026-09-01 — Use `ItemStack.itemDamage` as block metadata when placing

- `ItemBlock.onItemUse` now reads the stack's `itemDamage` and writes it to the world as the block's metadata via `World.setBlockAndMetadataWithNotify` (replacing the old `setBlockWithNotify` call), so a damaged block item places with the matching sub-variant — e.g. a damaged cloth item places the matching colour swatch, and a damaged stair item places the matching orientation.
- The damage-to-metadata translation goes through a new overridable `ItemBlock.getMetadata(int damage)` hook (default: pass-through). Sub-classes can override it to remap damage to a different nibble without changing the public item damage stored in the `ItemStack`. Mirrors the r1.2.5 pattern.

### 2026-09-01 — Wire up a real "Fancy graphics" option

- Added a `FANCY_GRAPHICS` row in the options screen (id 6) that calls `BlockLeavesBase.setGraphicsLevel(boolean)` for every registered leaf block, so toggling the option flips leaves between translucent merged-face "fancy" mode and opaque "fast" mode. The block class already had the two-mode logic; it just had no caller.
- Toggling also calls `Minecraft.refreshRenderers()` which delegates to `RenderGlobal.updateAllRenderers()`, setting `needsUpdate = true` on every lit chunk renderer. Each chunk rebuilds its compiled OpenGL display list on the next render tick, so the leaf change is visible immediately rather than only on chunks that happen to reload.
- Apply on load too (`GameSettings` constructor runs `applyFancyGraphics(fancyGraphics)` after `loadOptions()`) so a saved `fancyGraphics:false` in `options.txt` actually takes effect on game start.
- Fixed the existing `VIEW_BOBBING` row: it was bound to the `fancyGraphics` field (a copy/paste bug from the cleanup), so toggling "View bobbing" did nothing on its own. Gave `viewBobbing` its own `boolean` field and updated the three `EntityRenderer` sites that gate `setupViewBobbing` (lines 381, 560, 574) to check the new field.
- Renumbered the remaining option ids (`ANAGLYPH=7`, `LIMIT_FRAMERATE=8`, `DIFFICULTY=9`); saves are not affected because `loadOptions` matches by save key.

### 2026-09-01 — Block colour tint hook (`getRenderColor`) and metadata-aware inventory renderer

- Added `Block.getRenderColor(int metadata)` returning `0xFFFFFF` (white / no tint) as the base implementation. Subclasses that need a per-metadata colour (e.g. cloth) can override this; the renderer reads it automatically.
- Colour is packed as `0xRRGGBB` (R = high byte, B = low byte, matching `Tessellator.setColorOpaque_F` / a1.1.2 convention).
- Extended `BlockRenderHandler.renderBlockOnInventory` with an `int metadata` parameter, propagated through `RenderBlocks` and all four handlers that produce inventory previews (`RenderBlockNormal`, `RenderBlockCrops`, `RenderBlockPlant`, `RenderBlockTorch`).
- Updated callers to thread metadata through: `ItemRenderer` and `RenderItem` now extract `itemStack.itemDamage`; `RenderTNT` passes `0`; `RenderFallingSand` passes `0` (the entity has no metadata field in 2010 NBT).
- The in-world cube renderer (`RenderBlockNormal.renderBlock`) was also updated to apply the `getRenderColor` tint per-face using the block's actual world metadata, so future colour-aware blocks will tint in-world too.

### 2026-09-02 — Subtyped block items: `IBlockWithSubtypes` + `ItemBlockWithSubtypes`

Subtyped blocks (flowers, mushrooms, saplings) now display and tint correctly
in the inventory, the held item, dropped items, and break particles.

- `Item.hasSubTypes` (`false` by default) — when `true`, the durability bar is
  never drawn and `getIconFromDamage(int)` / `getColorFromDamage(int)` may
  return different values per metadata. New `getHasSubTypes()` / `setHasSubTypes()`
  helpers.
- `Item.getIconFromDamage()` is now `getIconFromDamage(int damage)`, returning
  `this.iconIndex` by default. `Item.getColorFromDamage(int)` returns `0xFFFFFF`
  by default.
- New `IBlockWithSubtypes` marker interface in `net.minecraft.game.world.block`.
  `Block` already provides the default `getBlockTextureFromSideAndMetadata`, so
  subclasses only need to add `implements IBlockWithSubtypes`. `BlockFlower`
  declares it; `BlockMushroom` and `BlockSapling` inherit it via `extends BlockFlower`.
- New `ItemBlockWithSubtypes extends ItemBlock` (constructed in `Block`'s
  static init) sets `hasSubTypes=true` and overrides `getIconFromDamage` /
  `getColorFromDamage` to delegate to the wrapped block's metadata-sensitive
  methods. Blocks whose class implements the interface are automatically wired
  to it — no per-block catalogue change needed.
- `RenderItem` and `ItemRenderer` pass `itemDamage` to `getIconFromDamage` and
  apply tint via `getColorFromDamage` (preserving the brightness multiplier in
  the first-person hand). The damage-bar overlay in
  `RenderItem.renderItemOverlayIntoGUI` is gated on `!getHasSubTypes()`.
- `EffectRenderer` passes the block's metadata to `EntityDiggingFX`, which now
  uses `block.getBlockTextureFromSideAndMetadata(2, metadata)` for the crack
  sprite and `block.getRenderColor(metadata)` for the particle tint (the
  hard-coded `0.6F` dark gray is gone).
- `docs/blocks_with_subtypes_interface.md` documents the design.

### 2026-09-02 — BlockSapling: bit 3 (& 8) is the growth-state flag

`BlockSapling.updateTick` no longer increments metadata 0→15. It now uses bit 3
(`& 8`) as the "ready to grow" state, matching b1.7.3: the first tick that meets
the growth conditions sets the bit, the next tick attempts to generate the tree.
The other metadata bits are left untouched so future sapling subtypes (bits 4–7
in b1.7.3) can keep their type across growth.

The class Javadoc documents the metadata layout and the future-subtype contract:
`getBlockTextureFromSideAndMetadata` and any other variant-aware method must mask
out bit 3 once subtypes are introduced (e.g. `metadata & 0xF0` if subtypes live
in the upper nibble as in b1.7.3).

### 2026-09-02 — Organised static Block and Item lists by id, grouped by tens

`Block.java` and `Item.java` static catalogue sections are now:

- Ordered strictly by id (ascending).
- Grouped in blocks of 10 with a blank line after every group.
- `Block.java` has a `// ID NNN` placeholder on its own line for every
  unoccupied id (0, 21–34, 36, 39–40, 52–53, 63–255), so free slots are
  obvious at a glance. `Item.java` has no gaps (ids 0–71 are all occupied)
  and ends with `// ID 72 .. ID 1023: free` to mark the open range.

This makes it easy to scan which ids are free and where to insert new blocks/items.

### 2026-09-02 — Add `ItemBucket` from a1.1.2 (water, lava, milk)

Backported from `minecraft_a1.1.2/src/net/minecraft/src/ItemBucket.java`.

- `ItemBucket extends Item`: the field `isFull` stores the block id to place
  (0 = empty, 8 = waterMoving, 10 = lavaMoving, -1 = milk).
  `onItemRightClick` handles all four cases via a single ray trace:

  - Empty bucket + hitting a source block of water / lava → picks it up,
    clears the cell, plays `liquid.water` / `liquid.lava`, returns the
    water / lava bucket.
  - Water / lava bucket + hitting a block face → resolves the neighbour
    cell using `neighbourAcrossFace` (the same helper used by `ItemBlock`
    and `ItemFlintAndSteel`), places the fluid if the neighbour is empty
    or non-solid, plays `liquid.water` / `random.fizz`, returns the empty
    bucket.
  - Empty bucket + hitting a cow entity → milks it, returns the milk bucket.
  - Milk bucket + hitting any block face → consumes the bucket, returns
    the empty bucket (the milk bucket cannot be refilled from a cow —
    a1.1.2 semantics preserved).

- `Item.java`: the four bucket fields are now `ItemBucket` instances:
  `bucketEmpty` (id 68, isFull=0), `bucketWater` (id 70, isFull=8),
  `bucketLava` (id 71, isFull=10), `bucketMilk` (id 69, isFull=-1).
  Item ids 67 (leather) and 68–69 are unchanged from the previous commit.

- `EntityCow.interact` updated: the milking path is now handled by
  `ItemBucket`; the stub is retained but noted as unused.

Full-tree `javac -source 1.8 -target 1.8` EXIT=0, 295 files.

### 2026-09-02 — Backport `EntityCow` from a1.1.2

Cows now exist in the world. Backported from `minecraft_a1.1.2/src/net/minecraft/src/EntityCow.java`.

- `EntityCow extends EntityAnimal`: `texture = "/mob/cow.png"`, `setSize(0.9F, 1.3F)`, `getLivingSound` / `getHurtSound` / `getDeathSound` set to `mob.cow` / `mob.cowhurt` / `mob.cowhurt`. `getDroppedItem()` returns `Item.leather.shiftedIndex` (the existing `EntityLiving.onDeath` scatter is 0-2 drops). The `interact` method is a stub (no right-click handler wired in this codebase yet).
- `ModelCow extends ModelBase` (not `ModelQuadruped`): the working-src `ModelRenderer.rotationPoint*` fields are private and `ModelQuadruped.render` is final, so the cow's extra parts (horns, udders) cannot piggy-back on the base class. The legs are constructed inline with the same offsets the a1.1.2 reference would have produced via the in-place `--leg1.rotationPointX` mutations.
- `RenderCow extends RenderLiving`: thin class — `RenderLiving` already does the work; the subclass exists so the renderer registry can name a model and shadow size.
- `Item.java` adds `leather` (id 67), `bucketEmpty` (id 68), `bucketMilk` (id 69) at the end of the register sequence (so no existing ids shift). These are needed for the cow's drop (`leather`) and the stub milking interaction (`bucketEmpty` / `bucketMilk`).
- `EntityList` adds `addMapping(EntityCow.class, "Cow")`. The save-format string is new and so does not collide with anything in older worlds.
- `World.animalSpawner` adds `EntityCow.class` to the spawnable-animals array.
- `RenderManager` adds `new RenderCow(new ModelCow(), 0.7F)`.

Full-tree `javac -source 1.8 -target 1.8` EXIT=0, 294 files.

### 2026-09-02 — `Block.canBeSubstituted()` and the click-on-flower fix (#2, #7, and a new bug)

Added a virtual method on `Block`:
```java
public boolean canBeSubstituted() {
    return this == Block.fire
        || this.blockMaterial == Material.water
        || this.blockMaterial == Material.lava;
}
```

A block is *substitutable* when the player meaningfully "puts a block there" — i.e. the cell can take a new block, and the existing one disappears without conflict. The default covers fire, water, lava. `BlockFlower` (covers flowers, mushrooms, saplings, crops via the hierarchy) overrides to return `true`. Solid decorations like cogs default to `false`, so they stay opaque to placement.

Three call sites are routed through it:

- **`BlockSand.canFallBelow`** (proposal #2): the static helper previously listed air, fire, water, lava by id and material. Now `block == null || block.canBeSubstituted()`.
- **`EntityFallingSand.canBlockBePlacedAt`** (proposal #7): the `instanceof BlockFlower` check and the four water/lava id checks collapse to `existingBlock == null || existingBlock.canBeSubstituted()`.
- **`ItemBlock.onItemUse`** (new behaviour, this commit): right-clicking a flower (or any substitutable block) with a block item now places the new block **in the clicked cell, replacing the flower**, instead of placing in the cell across the clicked face. Before the fix, the player had to right-click *next to* a flower to place a block on its cell — the bug is in vanilla Infdev 2010 too.

`canPlaceBlockAt` is still called on the new block at the target cell, so the placement rules (e.g. need a plant-supporting block below) are not bypassed. The "where" and the "is the cell clear" questions are now both answered by the same hook, with no enumeration of block ids or materials at the call sites.

Full-tree `javac -source 1.8 -target 1.8` compile passed (`EXIT: 0`).

### 2026-09-02 — Six more de-hardcoding passes (#4, #5, #6, #8, #9, #11)

A batch of small, focused refactors that move id checks onto the block
hierarchy. Each is byte-for-byte behaviour-preserving and follows the same
pattern: a virtual method on the base class, an override on the specific
block(s) that need special behaviour, and a redirect at the call site.

- **#4 `Block.isBurning()`** — default is `blockMaterial == Material.lava`,
  overridden in `BlockFire` to return `true`. Replaces the
  3-id OR chain in `EntityQueryService.isBoundingBoxBurning`
  (`Block.fire`, `Block.lavaMoving`, `Block.lavaStill`) with
  `block.isBurning()`. Same semantics, one virtual dispatch.

- **#5 `Block.takesLightFromAbove()`** — default `false`, overridden in
  `BlockStep` (only when `!doubleSlab`) and `BlockFarmland`. Replaces the
  duplicated `id == stairSingle || id == tilledField` check in
  `World.getBlockLightValue_do` and `ChunkCache.getLightValueExt` with
  `block.takesLightFromAbove()`. The two call sites are now identical
  one-liners.

- **#6 `Block.getAnimalPathBonus()`** — default `0.0F`, overridden in
  `BlockGrass` to `10.0F`. `EntityAnimal.getBlockPathWeight` now asks the
  block below instead of checking `Block.grass.blockID`. The 10.0F
  tuning constant moves to where it belongs (the block that earns it).

- **#8 `BlockFluid.hardenedBlock(int decay)`** — protected method on the
  fluid base that returns `null` for water and (for lava) the right block:
  obsidian for source (decay 0), cobblestone for flowing (decay 1-4).
  `checkForHarden` no longer hardcodes `Block.obsidian.blockID` or
  `Block.cobblestone.blockID`; it just calls `this.hardenedBlock(decay)`.

- **#9 `BlockOreCoal` / `BlockOreDiamond`** — two new subclasses that
  override `idDropped` to return `Item.coal.shiftedIndex` and
  `Item.diamod.shiftedIndex` respectively. `Block.oreCoal` and
  `Block.oreDiamond` are now declared with the new subclasses; the base
  `BlockOre.idDropped` simplifies to `return this.blockID`. The
  `this.blockID == Block.oreCoal.blockID` ternary chain is gone.

- **#11 `RenderBlockRedstoneWire.REDSTONE_WIRE_ID`** — extracted the
  five occurrences of the magic literal `55` into a single named
  constant at the top of the file, with a javadoc note explaining
  it shares the slot with the cog/gears block in this version.

Full-tree `javac -source 1.8 -target 1.8` compile passed (`EXIT: 0`).

### 2026-09-02 — Add `Block.canGrowCrops(int metadata)` hook

De-hardcoded `BlockCrops.updateTick`: the 3×3 neighbourhood scan of farmland used to read `world.getBlockId(tileX, y-1, tileZ) == Block.tilledField.blockID` nine times per random tick and used the metadata inline. Replaced with a virtual method on `Block`:
```java
public boolean canGrowCrops(int metadata) { return false; }
```
Override `true` on `BlockFarmland` only — distinct from `canGrowPlants` (which is true for dirt/grass) because crops must grow on tilled soil specifically, not on any plant-supporting block.

`BlockCrops.updateTick` now reads:
```java
Block below = world.getBlock(tileX, y - 1, tileZ);
int belowMeta = world.getBlockMetadata(tileX, y - 1, tileZ);
if(below != null && below.canGrowCrops(belowMeta)) { ... }
```

Full-tree `javac -source 1.8 -target 1.8` compile passed (`EXIT: 0`).

### 2026-09-02 — Add `Block.getLightValue(int metadata)` hook

Added a per-metadata light-value hook to `Block`, mirroring the r1.2.5 pattern. Default returns `Block.lightValue[blockID]`, so existing blocks keep their behaviour byte-for-byte; subclasses (e.g. a future glowing-mushroom variant reusing `Block.mushrooms` with a reserved metadata) can override it to return a per-metadata brightness.

Direct reads of the static `Block.lightValue[]` array were routed through the new method, with a null check on the looked-up `Block` instance:
- `MetadataChunkBlock.relightBlock` — now uses `block.getLightValue(world.getBlockMetadata(x, y, z))` with a `Block.blocksList[blockID] != null` guard.
- `RenderBlockDoor` (3 call sites) — uses the cached `metadata` from the door's own `blockAccess.getBlockMetadata` call.
- `RenderBlockNormal.neighborBrightness` — calls `block.getLightValue(renderBlocks.blockAccess.getBlockMetadata(...))` per side.
- `RenderBlockTorch`, `RenderBlockLever`, `RenderBlockRepeater` — use their cached `metadata` directly.

`World.computeLightAt` is intentionally left as `Block.lightValue[blockID]` — it runs in a hot inner loop with only the id in hand, and there is no per-block instance or metadata there. A 256×16 LUT was considered and rejected for now: the static array is already a single load, the shift+or indirection of `(id << 4) | meta` is not a measurable win, and the real value of the hook is the override path, not the lookup. The LUT can be added later if profiling shows it matters.

Full-tree `javac -source 1.8 -target 1.8` compile passed (`EXIT: 0`).

### 2026-09-02 — Add `ItemStack.stackTagCompound` (per-stack NBT payload)

Ported from r1.2.5: `ItemStack` now carries an optional `NBTTagCompound stackTagCompound` field, saved alongside the rest of the stack and read back on load. Behaviour:
- `writeToNBT` emits the tag under the `"tag"` key only when non-null.
- The `ItemStack(NBTTagCompound)` constructor and the new in-place `readFromNBT` method both read it back, with the tag field left as `null` if the key is absent.
- `splitStack` and `copy` clone the tag (deep copy via `NBTTagCompound.copy()`) so the original and the split/copy don't share a mutable payload.
- `hasTagCompound` / `getTagCompound` / `setTagCompound` helpers mirror r1.2.5.

To support deep copies, `NBTBase` now has an abstract `copy()` method with concrete implementations in every tag type (recursive for `NBTTagList` and `NBTTagCompound`, value copy for byte arrays). The `NBTTagList` tag type is copied correctly so nested lists-of-lists work.

`toString` was updated to include the tag presence. Behaviour is unchanged for stacks that don't set a tag.

### 2026-09-02 — `Block.onSubstituted()` — substituted blocks drop themselves

Added a new virtual method on `Block`:
```java
public void onSubstituted(World world, int x, int y, int z, int metadata) {
}
```

The default is no-op: fire, water, lava vanish silently when replaced. `BlockFlower` overrides to call `dropBlockAsItem`, so a yellow-flower block drops a yellow-flower item (the existing `damageDropped` chain makes the metadata follow the variant). The call site is responsible for reading the cell's `metadata` *before* the new block is written.

Three call sites are wired through it:

- `ItemBlock.onItemUse` — when a player right-clicks a substitutable block with a block item, the new block writes into the same cell. Now the existing block's `onSubstituted` is called first, so clicking a flower with a stone item drops a flower. Clicking a flower with another flower drops a flower. Clicking fire / water / lava drops nothing.
- `EntityFallingSand.onUpdate` — when a sand entity settles into a cell that contains a non-null, non-blocked block, `onSubstituted` is called before the sand writes itself. Sand falling onto a flower drops a flower item. Sand falling onto fire drops nothing. Sand falling into air is unchanged.
- `BlockFlowing.flowIntoBlock` — water flowing over a block calls `occupied.onSubstituted(...)`. Lava is unchanged: the `triggerLavaMixEffects` branch (fizz, no drop) stays in place because the lava's "destroy" effect is a property of the lava, not of the displaced block. This preserves the existing vanilla asymmetry: water breaks a flower; lava fizzes it away.

Behavioural cross-check (matches modern vanilla expectations):

| Path | Drop? |
|---|---|
| Player right-clicks flower with stone | yes (flower) |
| Player right-clicks flower with flower | yes (flower) |
| Sand falls onto flower | yes (flower) |
| Sand falls onto fire | no |
| Water flows onto flower | yes (flower) |
| Water flows onto fire | no |
| Lava flows onto flower | no (fizz) |

Verified by full-tree `javac -source 1.8 -target 1.8` compile (EXIT=0, 291 files).

### 2026-09-02 — 256-block world height + legacy-aware level select screen

Extended the buildable world height from 128 to 256 blocks (spec in
`docs/256_blocks_high.md`). Blocks 0–127 stay exactly as before; the top half
(128–255) starts empty and fills only where the player builds.

- **Lazy subchunk storage.** Each chunk is now 16 subchunks of 16×16×16. The
  flat `blocks`/`data`/`skyLightMap`/`blockLightMap` arrays were replaced with
  per-subchunk parallel arrays (`byte[16][]` + `NibbleArray[16]` × 3). The
  bottom 8 subchunks materialize on chunk load; the top 8 are allocated
  **write-only** on the first block placed at/above y=128. Reads against a
  `null` subchunk return "open air, fully lit" (block 0, sky 15, block 0)
  without allocating — which is what keeps the fresh-world footprint identical
  to the old 128-high flat chunk (81 920 B).
- **Save format v2.** New chunks write `Height=256`, a `SubchunkMask` short (one
  bit per present subchunk) and parallel `SubchunkBlocks/Data/SkyLight/
  BlockLight` byte-array lists (mask-ordered). Legacy saves (no `Height`, or
  `Height ≤ 128`) load unchanged: the flat 128-high arrays are sliced into the
  8 lower subchunks and upgraded on the first re-save. Old builds cannot read
  the new format.
- **Lighting & heightmap** walk the full 0–255 span; `setBlockID` clamps the
  relight step to `SECTION_HEIGHT - 1`; `RenderGlobal.markBlocksForUpdate`
  clamps the subchunk index into bounds so neighbour-of-y=0 edits (y = −1)
  and edits above the top slab cannot index out of range.
- **Random block ticks** keep the historical per-cell rate: the loop probes each
  *materialized* subchunk 10 times, so fresh terrain still gets 80 probes/chunk
  and the rate is constant at any height. `blocksToTickPerFrame` became
  `tickProbesPerSubchunk = 10`.
- **Generation stays 128.** `ChunkProviderGenerate420` produces terrain only in
  the bottom 128 layers (`TERRAIN_HEIGHT = 128`, renamed from the magic 128);
  `BiomeGenInfdev`'s ore/decoration heights are untouched (RNG order
  load-bearing). `WorldGenTrees` was relaxed to grow into the new top half so a
  sapling on a platform can grow normally.
- **Level select screen** now shows the real on-disk folder size of each save
  (sum of file lengths under `saves/WorldN/`) instead of the never-set
  `SizeOnDisk` tag, and appends `(OLD)` to worlds still in the legacy 128-high
  format (no `SubchunkMask` in their chunk files).
- Full-tree `javac -source 1.8 -target 1.8` compile EXIT=0.

### 2026-09-02 — Fix empty-world/fall-to-lava after the 256-height change

A fresh world came up empty and the player fell straight to the lava at the
floor. Root-caused to the flat-buffer → subchunk conversion in `Chunk`:

- **Wrong slice timing.** `provideChunk` built the `Chunk` from the flat
  32 768-byte generator buffer *before* `generateTerrain`/`replaceBlocks` had
  filled it, so every chunk's subchunks held stale (all-air) data. `provideChunk`
  now builds the chunk empty, fills the flat buffer, then calls
  `Chunk.loadFlatBlocks(blocks)` — a dedicated method — after generation.
- **Wrong layout remap.** The first slicing copied the flat buffer as a
  contiguous byte slice, but the generator's buffer is *column-major*
  (`x << 11 | z << 7 | y`, y-contiguous per column), while a subchunk packs
  cells `x << 8 | z << 4 | yLocal`. `sliceFlatBlockPlane` /
  `sliceFlatNibblePlane` now re-map every cell value into the subchunk layout.
  The same correction was applied to the legacy-save path (`readFlatPlanes`),
  so old 128-high saves both load and re-save correctly.
- The `Chunk(World, byte[], chunkX, chunkZ)` constructor was replaced by a
  public empty `Chunk(World, chunkX, chunkZ)` plus `loadFlatBlocks`, so
  construction no longer couples to the not-yet-generated buffer.

Verified headless: a generated chunk has ~17 245 non-air blocks across the
bottom four surface bands (sane heights, `getSubchunkCount=8`); placing a block
at y=200 materializes subchunk 12 and survives an NBT write/read round-trip;
legacy flat chunks re-map byte-for-byte to the original column-major layout.
Full-tree `javac -source 1.8 -target 1.8` compile EXIT=0.

### 2026-09-02 — F3 debug HUD with redesigned info

Add an in-game debug overlay toggled by the **F3** key (press again to turn it
off), wired like the existing F5/F11 toggles and gated on the same `showFPS`
setting (which itself remains an options-screen toggle, persisted to options.txt).

The overlay was redesigned into a compact read-out:

- **Header / memory** — `Minecraft Infdev (fps, chunk updates)` on the left,
  `Used: X% (NN MB) of XXXX MB` right-aligned (used = allocated − free).
- **Entities** — `Entities: A:Animal | M:Mob | O:Other | T:Total`, using the
  cached monster/animal counters plus the total list size (other = total − A − M).
- **Position** — `Pos: X Y Z (N 273)`: integer coords plus the compass facing
  (derived from `rotationYaw * 4 / 360`) and the integer yaw.
- **Time** — `Time: HH:MM`: 24 virtual hours to the day (24000 ticks) with the
  +6000 dawn offset, so worldTime 6000 reads 06:00.

Supporting change: `World.getSeed()` now exposes the exact generation seed
(`randomSeed`), shown right-aligned on the entities line (`Seed: …`).
Full-tree `javac -source 1.8 -target 1.8` compile EXIT=0.

### 2026-09-02 — Modernized horizontal friction to the a1.1.2/b1.7.3 model

Modernize `EntityLiving` horizontal movement to the per-block **slipperiness**
model used by a1.1.2/b1.7.3.

- `Block` gains a `slipperiness` field (default `0.6F`, the value used for every
  current block) — the hook a later ice-like block would use to let the player
  slide further.
- `EntityLiving.onLivingUpdate` replaces the legacy fixed-damping pass (hard
  `0.91`/`0.6` multipliers) with the reference model: `friction =
  Block.blocksList[block at feet].slipperiness * 0.91F`, ground acceleration
  `0.1F * (0.16277136F / friction³)`, then `motionX/Z *= friction`. The two
  models are numerically identical at `slipperiness = 0.6` (both give ~0.546
  per-tick ground friction and `0.1F` ground accel), so this is a behavior-neutral
  normalization that also keeps ladder support: added an inert `isOnLadder()`
  stub (Infdev has no ladder block yet) wired exactly like the reference
  (fall reset, descent clamp, climb on horizontal collision). `Entity.fallDistance`
  was widened to `protected` (as in a1.1.2) so the climb block compiles.

Verified with a headless probe against the compiled classes: real block
`slipperiness` is `0.6F`, the friction/accel equivalence holds within float
error, and the pillar-cell predicate only matches the feet cell. Full-tree
`javac -source 1.8 -target 1.8` compile EXIT=0.
