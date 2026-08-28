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
