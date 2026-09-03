# ModelRenderer / ModelBox Port Plan

This document studies the r1.2.5 model framework and lays out a plan to port
the child-renderer + ModelBox system into the infdev 20100420 codebase.

The port keeps infdev's well-tested UV math (seam inset prevents texture
bleeding), but adopts r1.2.5's separation of `ModelBox` geometry, recursive
child rendering, eager display-list compilation, and the `ModelBase`-aware
constructor pattern.

> **Decisions settled during planning:**
> 1. **Constructor: non-breaking.** `ModelRenderer(int, int)` stays; delegates to new `ModelRenderer(ModelBase, int, int)`.
> 2. **`ModelBase.render()`: full r1.2.5 fidelity.** 7-arg form with `Entity` first; update all 9 model subclasses and `RenderLiving`.
> 3. **UV math: keep infdev inset.** `±0.0015625` / `±0.003125` seam inset stays.
> 4. **`GLAllocation`: added.** `generateDisplayLists(int)` helper used by `ModelRenderer`.
> 5. **Child renderers: migrate headwear/legs to children.** Zombie headwear, Sheep head/legs, Biped cloak/ears become real children.
> 6. **Compilation: eager.** Display list compiled inside the constructor on first `addBox`.

---

## 1. The framework

### 1.1 `ModelRenderer` — the part

`ModelRenderer` owns a list of `ModelBox` geometry, may have child renderers,
and renders via a display list.

```java
public class ModelRenderer {
    public float textureWidth = 64.0F;
    public float textureHeight = 32.0F;
    public List<ModelBox> cubeList = new ArrayList<>();
    public List<ModelRenderer> childModels;
    public final String boxName;
    private final ModelBase baseModel;
    public float rotationPointX, rotationPointY, rotationPointZ;  // promoted from private
    public float rotateAngleX, rotateAngleY, rotateAngleZ;        // already public
    public boolean mirror, showModel, isHidden;
    private int textureOffsetX, textureOffsetY;
    private boolean compiled;
    private int displayList;

    public ModelRenderer(ModelBase model, String boxName) {
        this.baseModel = model;
        this.boxName = boxName;
        model.boxList.add(this);
        this.setTextureSize(model.textureWidth, model.textureHeight);
    }

    public ModelRenderer(ModelBase model, int texX, int texY) {
        this(model, null);
        this.setTextureOffset(texX, texY);
    }

    // Backward-compat: 2-arg ctor delegates with baseModel=null
    public ModelRenderer(int texX, int texY) {
        this(null, null);
        this.setTextureOffset(texX, texY);
    }
}
```

**New methods:**

| Method | Purpose |
|---|---|
| `addChild(ModelRenderer)` | Lazily init `childModels`, append child |
| `addBox(String, float, float, float, int, int, int, float)` | Look up texture offset from `baseModel.modelTextureMap` |
| `addBox(...)` (fluent 6-arg and 7-arg overloads) | Returns `ModelRenderer` for chaining |
| `setTextureSize(int, int)` | Sets `textureWidth/Height`; fluent |
| `setTextureOffset(int, int)` | Sets offset; fluent |
| `renderWithRotation(float)` | Push/pop without compiling or recursing |
| `postRender(float)` | Apply transform only (for composite children) |
| `render(float)` | Eager compile + draw own boxes + recursive child render |

### 1.2 `ModelBox` — the geometry (new class)

A `ModelBox` is one cuboid inside a `ModelRenderer`. It owns the 8 corner
vertices and 6 quad faces.

```java
public class ModelBox {
    private final PositionTextureVertex[] vertexPositions;   // [8]
    private final TexturedQuad[] quadList;                  // [6]
    public final float posX1, posY1, posZ1;              // min corner
    public final float posX2, posY2, posZ2;              // max corner
    public String field_40673_g;                            // sub-box name

    public ModelBox(ModelRenderer renderer, int texX, int texY,
                    float x, float y, float z,
                    int width, int height, int depth,
                    float expansion) {
        // 1. Compute min/max corners, inflate by expansion
        // 2. Respect renderer.mirror (swap min/max X)
        // 3. Create 8 PositionTextureVertex
        // 4. Create 6 TexturedQuad using textureWidth/Height
        // 5. Flip faces if mirrored
    }

    public void render(Tessellator tess, float scale) {
        tess.startDrawingQuads();
        // normal via cross-product, draw 4 vertices per quad
        tess.draw();
    }

    public ModelBox func_40671_a(String name) { ... }
}
```

### 1.3 `TexturedQuad` — the face (port)

Added to the existing infdev class:

| Addition | Description |
|---|---|
| `public int nVertices` | Set to `vertexPositions.length` in constructor |
| `public boolean invertNormal` | New field |
| `public TexturedQuad(vertices, uMin, vMin, uMax, vMax, textureWidth, textureHeight)` | 6-arg constructor: parameterized UV math with **infdev seam inset preserved** |
| `public void draw(Tessellator, float)` | Moved here from `ModelRenderer.render()` |
| `public void flipFace()` | Reverse vertex order for mirroring |

The seam inset is preserved (decision #3):

```java
float u1 = (float)uMin / textureWidth + 0.0015625F;
float u2 = (float)uMax / textureWidth - 0.0015625F;
float v1 = (float)vMin / textureHeight + 0.003125F;
float v2 = (float)vMax / textureHeight - 0.003125F;
```

### 1.4 `GLAllocation` (new class)

```java
public class GLAllocation {
    public static int generateDisplayLists(int count) {
        return GL11.glGenLists(count);
    }
}
```

### 1.5 `ModelBase` — extended

```java
public class ModelBase {
    public List<ModelRenderer> boxList = new ArrayList<>();
    public Map<String, TextureOffset> modelTextureMap = new HashMap<>();
    public int textureWidth = 64;
    public int textureHeight = 32;
    public float onGround;
    public boolean isRiding;
    public boolean isChild = true;

    // CHANGED: 7-arg signature with Entity first
    public void render(Entity entity, float limbSwing, float limbSwingAmount,
                       float ageInTicks, float netHeadYaw, float headPitch,
                       float scaleFactor) { ... }

    public void setTextureOffset(String name, int x, int y) {
        this.modelTextureMap.put(name, new TextureOffset(x, y));
    }

    public TextureOffset getTextureOffset(String name) {
        return this.modelTextureMap.get(name);
    }

    public void setRotationAngles(...) { ... }
    public void setLivingAnimations(EntityLiving, float, float, float) { ... }
}
```

### 1.6 `TextureOffset` (new class)

```java
public class TextureOffset {
    public final int textureOffsetX;
    public final int textureOffsetY;
    public TextureOffset(int x, int y) { ... }
}
```

---

## 2. Infdev's current system

- `ModelRenderer` has **no `cubeList`**, **no `childModels`**, **no `textureWidth/Height`**,
  and a 2-arg constructor only.
- `ModelBase` is **empty**. `render()` takes **6 args** (no Entity).
- **52 call sites** across 9 model files: `new ModelRenderer(texX, texY)` then
  `part.addBox(...)` then `part.setRotationPoint(...)`.
- `RenderLiving` calls `mainModel.render(headPitch, ..., 1.0F)` (6 args).

---

## 3. Per-model port plan

### 3.1 All 9 model classes

1. Update `render()` to 7-arg form with `Entity` first.
2. For `ModelZombie`/`ModelSkeleton`/`ModelBiped`: migrate `bipedHeadwear`/`bipedCloak`/`bipedEars` to children of the appropriate parent.
3. For `ModelSheep`/`ModelSheepWool`: migrate head, legs, and body to a hierarchical child system.
4. Other models (`Creeper`, `Spider`, `Pig`, `Cow`, `Quadruped`): signature update only, no child migration.

### 3.2 Child migration example (`ModelZombie`)

```java
// Before:
this.bipedHead = new ModelRenderer(0, 0);
this.bipedHead.addBox(-4F, -8F, -4F, 8, 8, 8, 0F);
this.bipedHeadwear = new ModelRenderer(32, 0);
this.bipedHeadwear.addBox(-4F, -8F, -4F, 8, 8, 8, 0.5F);
// render: both rendered independently
this.bipedHead.render(1.0F);
this.bipedHeadwear.render(1.0F);

// After:
this.bipedHead = new ModelRenderer(this, 0, 0);
this.bipedHead.addBox(-4F, -8F, -4F, 8, 8, 8, 0F);
this.bipedHeadwear = new ModelRenderer(this, 32, 0);
this.bipedHeadwear.addBox(-4F, -8F, -4F, 8, 8, 8, 0.5F);
this.bipedHead.addChild(this.bipedHeadwear);
// render: only the root
this.bipedHead.render(1.0F);  // headwear renders automatically inside
```

**Important:** child `rotationPoint` is now relative to the parent pivot. The
headwear's `rotationPoint` must be `(0, 0, 0)` since its position is
already expressed relative to the head's pivot.

---

## 4. Files to create / modify

### 4.1 New files (3)

| File | Purpose |
|---|---|
| `client/render/GLAllocation.java` | `generateDisplayLists(int)` static wrapper |
| `client/model/ModelBox.java` | 8-vertex / 6-quad geometry unit |
| `client/model/TextureOffset.java` | Two-int named offset holder |

### 4.2 Files to modify (5)

| File | Change |
|---|---|
| `client/model/ModelRenderer.java` | New fields, new constructors, `addChild`, fluent setters, `ModelBox`-based `addBox`, `renderWithRotation`, `postRender`, recursive `render()`, eager `compileDisplayList` via `GLAllocation` |
| `client/model/TexturedQuad.java` | Add `nVertices`, `invertNormal`, 6-arg constructor, `draw(Tessellator, float)`, `flipFace()` |
| `client/model/ModelBase.java` | New fields (`boxList`, `modelTextureMap`, `textureWidth/Height`, `onGround`, `isRiding`, `isChild`), new methods, **7-arg `render(Entity, ...)`** |
| `client/render/entity/RenderLiving.java` | Update `mainModel.render(...)` to pass `Entity` as first arg |
| All 9 model subclasses | Update `render()` to 7-arg; migrate children where applicable |

### 4.3 Files not touched

`PositionTextureVertex` — only its constructor visibility changes (private → public for the 3-arg copy constructor).

---

## 5. Implementation stages

Each stage compiles and renders identically to before.

**Stage 1 — `GLAllocation` and `TextureOffset`.**
Trivial new classes from r1.2.5. No callers yet.

**Stage 2 — `ModelBox`.**
Port verbatim from r1.2.5, parameterized on `ModelRenderer`.

**Stage 3 — `TexturedQuad` extensions.**
Add `nVertices`, `invertNormal`, 6-arg constructor (keep infdev seam inset),
`draw(Tessellator, float)`, `flipFace()`. Existing 2-arg constructor stays.

**Stage 4 — `ModelRenderer` core.**
Add all new fields, all new constructors (backward-compat 2-arg stays),
`addChild`, fluent setters, fluent `addBox` overloads, `renderWithRotation`,
`postRender`, recursive `render()`, eager `compileDisplayList` via
`GLAllocation`.

**Stage 5 — `ModelBase` extensions.**
Add new fields and `setTextureOffset`/`getTextureOffset`. Do NOT change
`render()` signature yet.

**Stage 6 — `render(Entity, ...)` signature change.**
Change `ModelBase.render()` to 7-arg with `Entity` first. Update all 9 model
overrides. Update `RenderLiving` call site.

**Stage 7 — Child renderer migration.**
Migrate zombie headwear, sheep parts, biped cloak/ears one model at a time.
7×7 visual probe (4 angles × 4 mob types) after each migration.

---

## 6. Risks and mitigations

| Risk | Mitigation |
|---|---|
| Child migration re-anchor errors (child `rotationPoint` becomes relative to parent) | Careful review of each migrated part; r1.2.5 reference used as ground truth |
| Display-list staleness when `rotationPoint` mutated after `addBox` | `render()` re-compiles if `!compiled` |
| UV seam inset lost if `TexturedQuad` 6-arg constructor ported incorrectly | Explicit test: compare pixel diff of a known box face before/after; must be zero |
| 7-arg `render()` signature breaks `RenderLiving` call site | Single call site update; grep confirms only `RenderLiving` consumes it |

---

## 7. Verification

A headless render probe:
1. Compiles the full tree.
2. Creates a `ModelZombie`, `ModelSheep`, `ModelSpider`, `ModelCreeper`.
3. Calls `mainModel.render(entity, 0, 0, 0, 0, 0, 0, 1.0F)` for each.
4. Hashes the Tessellator vertex output (not pixels — tessellation is deterministic).

After each stage, the hash must be unchanged. Any hash change after stage 4 or
beyond is a regression to investigate before proceeding.
