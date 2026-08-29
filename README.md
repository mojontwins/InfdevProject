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
