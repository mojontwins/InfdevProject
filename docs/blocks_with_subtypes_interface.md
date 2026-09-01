# `IBlockWithSubtypes` and `ItemBlockWithSubtypes` — Design Document

## Problem

Blocks with sub-variants driven by metadata (flowers, mushrooms, saplings, cloth colours,
stone brick variants, etc.) are currently displayed as plain items — their icon does not
reflect the metadata stored in the `ItemStack`, and the inventory/renderer shows a
durability bar when `itemDamage != 0` even though `itemDamage` is a *variant selector*,
not a *durability counter*.

### Specific symptoms

1. **Wrong icon in inventory / HUD** — a yellow flower (damage=1) shows the red flower
   icon because `Item.getIconFromDamage()` takes no parameter and always returns the
   same `iconIndex` that was set once in the constructor.

2. **Wrong icon on dropped items** — `RenderItem.doRender` uses the same no-parameter
   call, so every variant of a subtyped block renders the same sprite.

3. **Spurious durability bar** — `RenderItem.renderItemOverlayIntoGUI` draws a red/green
   bar whenever `itemDamage > 0`. For subtyped items `itemDamage` is a variant index,
   not accumulated damage, so the bar makes no sense and clutters the display.

## Design Goals

1. Subtyped blocks look up their icon and tint *dynamically* from the stack's metadata.
2. The durability bar never appears on subtyped items.
3. Adding a new subtyped block requires **no new `Item*` subclass** — only a
   declaration in the block's static catalogue entry.
4. Non-subtyped blocks are unaffected.

---

## Core Concepts

### `hasSubTypes` — the bar gate

```
hasSubTypes == true   → never draw a durability bar (variant selector, not durability)
hasSubTypes == false  → normal durability logic: bar only if maxDamage > 0 && itemDamage > 0
```

This field lives on `Item`. The setter is `setHasSubTypes(boolean)`. The getter is
`getHasSubTypes()`. It defaults to `false`.

### Variant vs State — when to set `hasSubTypes`

| Metadata means... | Example | `hasSubTypes` |
|---|---|---|
| **Variant** — different but equivalent forms of the same item | Red flower vs yellow flower, oak sapling vs birch sapling, coloured cloth | `true` |
| **State** — a different condition of the same physical thing | Crop growth stage (0–7), farmland moisture level, furnace lit vs unlit | `false` |

Consequences of the distinction:

- Crops and farmland drop *seeds/wheat/dry farmland* when broken, not the block-as-item,
  so the variant question is moot for them. Their inventory icon shows the default
  side-2 texture via the plain `ItemBlock` constructor.
- Flowers and mushrooms drop the block-as-item (preserving their metadata), so they
  *must* show the correct variant icon.

### `IBlockWithSubtypes` — the block interface

A marker interface for blocks whose *icon* depends on metadata. Any `Block` subclass
that wants its `ItemBlock` to be swapped for a subtyped variant declares:

```java
public class BlockFlower extends Block implements IBlockWithSubtypes { ... }
```

The interface declares one method:

```java
public interface IBlockWithSubtypes {
    int getBlockTextureFromSideAndMetadata(int side, int metadata);
}
```

`Block` itself provides the default implementation (returning `blockIndexInTexture`), so
subclasses that already override `getBlockTextureFromSideAndMetadata` can simply add the
`implements` clause without adding any new code.

### `ItemBlockWithSubtypes` — the swapped-in item

When a block declares `implements IBlockWithSubtypes`, its `ItemBlock` is swapped for
`ItemBlockWithSubtypes` at registration time. This class:

- Sets `hasSubTypes = true` (the bar gate).
- Overrides `getIconFromDamage(int damage)` to call
  `block.getBlockTextureFromSideAndMetadata(2, damage)` — the icon is resolved at
  render time using the stack's actual metadata.
- Overrides `getColorFromDamage(int damage)` to call
  `block.getRenderColor(damage)` — tint is also metadata-sensitive.

```java
public final class ItemBlockWithSubtypes extends ItemBlock {

    public ItemBlockWithSubtypes(int itemID) {
        super(itemID);
        this.setHasSubTypes(true);
    }

    @Override
    public int getIconFromDamage(int damage) {
        return Block.blocksList[this.blockID].getBlockTextureFromSideAndMetadata(2, damage);
    }

    @Override
    public int getColorFromDamage(int damage) {
        return Block.blocksList[this.blockID].getRenderColor(damage);
    }
}
```

### `getIconFromDamage(int damage)` — parameterised icon lookup

The base `Item` class changes its existing no-parameter method to accept a damage:

```java
// Before
public final int getIconFromDamage()

// After
public final int getIconFromDamage(int damage) {
    return this.iconIndex;   // default: ignore metadata, return the fixed icon
}
```

All renderer call-sites are updated to pass `stack.itemDamage`:

| File | Method | Change |
|---|---|---|
| `RenderItem.java` | `renderItemIntoGUI` | `getIconFromDamage()` → `getIconFromDamage(itemStack.itemDamage)` (3 sites) |
| `RenderItem.java` | `doRender` | `getIconFromDamage()` → `getIconFromDamage(itemStack.itemDamage)` (1 site) |
| `ItemRenderer.java` | `renderItemInFirstPerson` | `getIconFromDamage()` → `getIconFromDamage(this.itemToRender.itemDamage)` (1 site) |

### `getColorFromDamage(int damage)` — parameterised tint lookup

A new method on `Item`, mirroring the existing `Block.getRenderColor(int)`:

```java
// Item.java
public int getColorFromDamage(int damage) {
    return 0xFFFFFF;   // default: no tint
}
```

`ItemBlockWithSubtypes` overrides it to delegate to the wrapped block's
`getRenderColor(int)`. Renderers call it and apply the result via `GL11.glColor3f`.

The 24-bit value packs RGB as `0xRRGGBB`. The unpack formula is:

```java
float r = ((color >> 16) & 0xFF) / 255.0F;
float g = ((color >>  8) & 0xFF) / 255.0F;
float b =  (color        & 0xFF) / 255.0F;
```

---

## File Changes

### New files (2)

#### `net/minecraft/game/world/block/IBlockWithSubtypes.java`
```java
package net.minecraft.game.world.block;

public interface IBlockWithSubtypes {
    int getBlockTextureFromSideAndMetadata(int side, int metadata);
}
```

#### `net/minecraft/game/item/ItemBlockWithSubtypes.java`
```java
package net.minecraft.game.item;

import net.minecraft.game.world.block.Block;
import net.minecraft.game.world.block.IBlockWithSubtypes;

public final class ItemBlockWithSubtypes extends ItemBlock {

    public ItemBlockWithSubtypes(int itemID) {
        super(itemID);
        this.setHasSubTypes(true);
    }

    @Override
    public int getIconFromDamage(int damage) {
        return Block.blocksList[this.blockID].getBlockTextureFromSideAndMetadata(2, damage);
    }

    @Override
    public int getColorFromDamage(int damage) {
        return Block.blocksList[this.blockID].getRenderColor(damage);
    }
}
```

---

### Modified files (6)

#### `net/minecraft/game/item/Item.java`

Changes to the field section (after `private int iconIndex;`):
```java
protected boolean hasSubTypes = false;
```

New methods (after `getIconFromDamage(int)`):
```java
public final boolean getHasSubTypes() {
    return this.hasSubTypes;
}

public final Item setHasSubTypes(boolean hasSubTypes) {
    this.hasSubTypes = hasSubTypes;
    return this;
}

public int getColorFromDamage(int damage) {
    return 0xFFFFFF;
}
```

Signature change:
```java
// Before
public final int getIconFromDamage()

// After
public final int getIconFromDamage(int damage) {
    return this.iconIndex;
}
```

#### `net/minecraft/game/world/block/Block.java`

**1. `setHasSubtypes()` setter** — added to the class, swaps the `ItemBlock` for the
subtyped variant at registration time:

```java
protected final Block setHasSubtypes() {
    Item.itemsList[this.blockID] = new ItemBlockWithSubtypes(this.blockID);
    return this;
}
```

This is called from the static catalogue (see 2 below), not from the constructor, so
that the decision to use a subtyped item is explicit at the catalogue site and does
not require a new constructor or factory method.

**2. Static catalogue changes** — `.setHasSubtypes()` chained onto variant blocks:

```java
// Before
public static final BlockFlower flowers = (BlockFlower)
    (new BlockFlower(37, 12)).setHardness(0.0F).setStepSound(soundGrassFootstep);
public static final BlockFlower mushrooms = (BlockFlower)
    (new BlockMushroom(38, 29)).setHardness(0.0F).setStepSound(soundGrassFootstep);
public static final Block sapling = (new BlockSapling(6, 15)).setHardness(0.0F).setStepSound(soundGrassFootstep);

// After
public static final BlockFlower flowers = (BlockFlower)
    (new BlockFlower(37, 12)).setHardness(0.0F).setStepSound(soundGrassFootstep).setHasSubtypes();
public static final BlockFlower mushrooms = (BlockFlower)
    (new BlockMushroom(38, 29)).setHardness(0.0F).setStepSound(soundGrassFootstep).setHasSubtypes();
public static final Block sapling = (new BlockSapling(6, 15)).setHardness(0.0F).setStepSound(soundGrassFootstep).setHasSubtypes();
```

`Block.crops` and `Block.tilledField` are **not** given `.setHasSubtypes()` — they
are state-based, not variant-based.

**3. `IBlockWithSubtypes` default** — `Block` implicitly satisfies the interface via
its default `getBlockTextureFromSideAndMetadata`. No explicit `implements` needed on
`Block` itself; only subclasses that want the subtyped behaviour declare it.

#### `net/minecraft/game/world/block/BlockFlower.java`

```java
// Before
public class BlockFlower extends Block { ... }

// After
public class BlockFlower extends Block implements IBlockWithSubtypes { ... }
```

`BlockMushroom extends BlockFlower`, so it inherits the interface declaration
implicitly — no separate change needed for mushrooms.

#### `net/minecraft/game/world/block/BlockSapling.java`

```java
// Before
public class BlockSapling extends Block { ... }

// After
public class BlockSapling extends Block implements IBlockWithSubtypes { ... }
```

`BlockSapling` already overrides `getBlockTextureFromSideAndMetadata(int, int)`, so
only the interface declaration is added.

#### `net/minecraft/game/client/render/entity/RenderItem.java`

**`renderItemIntoGUI` — flat icon path (non-block items, ~lines 41–58):**

Compute tint colour before the tessellator draw, apply via `GL11.glColor3f`,
restore to white after. The restore can reuse the existing
`GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F)` call at line 91.

```java
// Before (around line 41)
if(itemStack.getItem().getIconFromDamage() >= 0) {
    GL11.glDisable(GL11.GL_LIGHTING);
    ...
    Tessellator tessellator = Tessellator.instance;
    tessellator.startDrawingQuads();
    tessellator.addVertexWithUV(...);
    ...
    tessellator.draw();
    GL11.glEnable(GL11.GL_LIGHTING);
}

// After
if(itemStack.getItem().getIconFromDamage(itemStack.itemDamage) >= 0) {
    GL11.glDisable(GL11.GL_LIGHTING);
    ...
    int tintColor = itemStack.getItem().getColorFromDamage(itemStack.itemDamage);
    float tr = ((tintColor >> 16) & 0xFF) / 255.0F;
    float tg = ((tintColor >>  8) & 0xFF) / 255.0F;
    float tb =  (tintColor        & 0xFF) / 255.0F;
    GL11.glColor3f(tr, tg, tb);
    Tessellator tessellator = Tessellator.instance;
    tessellator.startDrawingQuads();
    tessellator.addVertexWithUV(...);
    ...
    tessellator.draw();
    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);   // restore — already present at line 91
    GL11.glEnable(GL11.GL_LIGHTING);
}
```

**`renderItemOverlayIntoGUI` — damage bar gate (~line 76):**

```java
// Before
if(itemStack.itemDamage > 0) {

// After
if(itemStack.itemDamage > 0 && !itemStack.getItem().getHasSubTypes()) {
```

**`doRender` — flat icon branch (~lines 154–189):**

Pass `itemStack.itemDamage` to `getIconFromDamage`, compute tint before the loop,
apply via `GL11.glColor3f` before emitting quads, restore to white after the loop:

```java
// Before
int icon = itemStack.getItem().getIconFromDamage();

// After
int icon = itemStack.getItem().getIconFromDamage(itemStack.itemDamage);
...
int tintColor = itemStack.getItem().getColorFromDamage(itemStack.itemDamage);
float tr = ((tintColor >> 16) & 0xFF) / 255.0F;
float tg = ((tintColor >>  8) & 0xFF) / 255.0F;
float tb =  (tintColor        & 0xFF) / 255.0F;
GL11.glColor3f(tr, tg, tb);
// [tessellator draw]
GL11.glColor3f(1.0F, 1.0F, 1.0F);  // restore
```

#### `net/minecraft/game/client/render/ItemRenderer.java`

**`renderItemInFirstPerson` — flat icon path (~line 78):**

Pass `itemDamage` to `getIconFromDamage`. Compute tint, apply via
`GL11.glColor3f(r * brightness, g * brightness, b * brightness)` (preserve the
brightness multiplier from line 40), restore with `GL11.glColor4f(brightness,
brightness, brightness, 1.0F)` after the tessellator draw.

```java
// Before
AtlasUV.calc(this.itemToRender.getItem().getIconFromDamage(), itemAtlas);

// After
AtlasUV.calc(this.itemToRender.getItem().getIconFromDamage(this.itemToRender.itemDamage), itemAtlas);
// [before tessellator.startDrawingQuads()]
int tintColor = this.itemToRender.getItem().getColorFromDamage(this.itemToRender.itemDamage);
float tr = ((tintColor >> 16) & 0xFF) / 255.0F * brightness;
float tg = ((tintColor >>  8) & 0xFF) / 255.0F * brightness;
float tb =  (tintColor        & 0xFF) / 255.0F * brightness;
GL11.glColor3f(tr, tg, tb);
// [tessellator draw]
GL11.glColor3f(brightness, brightness, brightness);  // restore with brightness
```

---

## Block Classification Reference

| Block | Metadata means | `implements IBlockWithSubtypes` | `.setHasSubtypes()` | Notes |
|---|---|---|---|---|
| `BlockFlower` (red/yellow) | variant | yes | yes | drops the block-as-item, variant must survive |
| `BlockMushroom` (brown/red) | variant | inherited via `extends BlockFlower` | yes | same as above |
| `BlockSapling` (oak/birch) | variant | yes | yes | no subtypes yet, but the slot is reserved |
| `BlockCloth` / wool | variant | yes (when added) | yes | 16 colours; `BlockCloth.getRenderColor` returns cloth palette |
| `BlockCrops` | **state** (growth stage) | no | no | drops seeds/wheat, not the block |
| `BlockFarmland` | **state** (moisture) | no | no | drops dry farmland, not the block |
| Any plain block (stone, dirt, …) | none | no | no | plain `ItemBlock`, fixed icon, no bar |

---

## Colour Restore Strategy

Three render methods modify `GL11` colour state. Each uses a local restore:

| Method | Apply | Restore |
|---|---|---|
| `RenderItem.renderItemIntoGUI` | `GL11.glColor3f(tr, tg, tb)` | `GL11.glColor4f(1,1,1,1)` — already at line 91 |
| `RenderItem.doRender` | `GL11.glColor3f(tr, tg, tb)` | `GL11.glColor3f(1,1,1)` after loop |
| `ItemRenderer.renderItemInFirstPerson` | `GL11.glColor3f(r*b, g*b, b*b)` | `GL11.glColor3f(b,b,b)` — preserves brightness |

---

## Extending the System

To add a new subtyped block (e.g., coloured wool, stone brick variants):

1. Make the block's class implement `IBlockWithSubtypes` (if it already overrides
   `getBlockTextureFromSideAndMetadata`, only the `implements` clause is needed).
2. Optionally override `getRenderColor(int)` on the block if the variants have
   different tint colours (e.g. cloth palette). Return `0xFFFFFF` for no tint.
3. Add `.setHasSubtypes()` to the block's static catalogue entry.
4. If the block is a new class (not `BlockFlower`/`BlockSapling`), no `Item*` class
   is needed — `ItemBlockWithSubtypes` handles all of them automatically.

---

## Open Questions

1. **Block subclasses that override `getBlockTextureFromSideAndMetadata` for rendering
   but should *not* be subtyped** — currently none exist. The rule is: if the block
   drops itself as an item with preserved metadata, it needs subtypes; if it drops a
   different item or nothing, it does not. This is enforced by the explicit
   `.setHasSubtypes()` call at the catalogue site, not by the class hierarchy.

2. **`BlockSapling`** — no subtypes exist in this version, but the slot is reserved.
   Adding the interface and `.setHasSubtypes()` now means future sapling types
   (birch, spruce) would automatically work with no further code changes.

3. **`BlockCrops` / `BlockFarmland` inventory icon** — these retain the plain
   `ItemBlock` with a fixed icon from the constructor. Their `getBlockTextureFromSide`
   (side 2 = the side face) is used, not a metadata-sensitive variant. This is
   acceptable because these blocks do not drop as items. A future optimisation could
   add `ItemBlockWithSubtypes` for visual completeness, but it is out of scope.

4. **`RenderItem.doRender` tint vs brightness** — the world-drop renderer currently has
   no brightness multiplier for the flat-icon path. The tint is applied directly.
   Whether this matches the intended appearance for coloured cloth items in the world
   (vs. on the ground) should be verified against b1.7.3 behaviour when cloth is
   added.
