# Dehardcoding trees

Infdev only has one tree type: Oak. Depending on the version, oak saplings may
produce two different trees (WorldGenTrees, WorldGenBigTree). Future versions have
more tree types, with different leaf types and wood types, that produce different
saplings. All the logic was hardcoded.

## EnumTreeType

`EnumTreeType` is a Java enum that manages the registered tree variants.

### Fields (per enum constant)

- `leaves`   — `BlockState` of the leaf block for this tree.
- `wood`     — `BlockState` of the log block for this tree.
- `sapling`  — `BlockState` of the corresponding sapling block.
- `name`     — Human-readable name.
- `needsFourSaplings` — `true` if this tree grows from a 2x2 cluster (e.g. spruce,
  birch in later versions); `false` for single-sapling trees.

### Constructors

- `EnumTreeType(String name, BlockState leaves, BlockState wood, BlockState sapling,
  boolean needsFourSaplings)` — full form.
- `EnumTreeType(String name, BlockState leaves, BlockState wood, BlockState sapling)` —
  delegates to the full constructor with `needsFourSaplings = false`.

### Methods

- `WorldGenerator getGenerator(Random rand)` — produces the appropriate
  `WorldGenerator` for this tree. OAK returns a big tree 1-in-10, otherwise a
  normal tree; new variants can override freely.
- `static EnumTreeType findTreeTypeFromLeaves(int blockID, int metadata)` — looks
  up the tree that owns those leaf cells. Falls back to OAK if no entry matches.
- `static EnumTreeType findTreeTypeFromSapling(int blockID, int metadata)` — looks
  up the tree type for that sapling. Falls back to OAK.
- `static EnumTreeType findTreeTypeFromWood(int blockID, int metadata)` — looks up
  the tree type for that log block. Falls back to OAK.

All three `find*` lookups are O(1) via `HashMap<BlockState, EnumTreeType>`.

### Current variants

Only `OAK` is defined:

```java
OAK("oak",
    new BlockState(Block.leaves,   BlockLeaves.OAK),
    new BlockState(Block.wood,     BlockLog.OAK),
    new BlockState(Block.sapling,  BlockSapling.OAK));
```

`getGenerator` for OAK returns `WorldGenBigTree` 10 % of the time, otherwise
`WorldGenTrees`.

## Wiring / automation

### BlockLeaves — sapling drop

When a leaf block is broken it must drop its corresponding sapling variant (e.g.
oak leaves drop oak saplings). `BlockLeaves` overrides `itemStackDropped` (not
`idDropped`) to look up the tree type from the leaf's block id and metadata and
return the `ItemStack` from `treeType.getSapling()`.

```java
// BlockLeaves
@Override
public ItemStack itemStackDropped(int metadata, Random random) {
    EnumTreeType tree = EnumTreeType.findTreeTypeFromLeaves(this.blockID, metadata);
    BlockState sapling = tree.getSapling();
    return new ItemStack(sapling.getBlockID(), 1, sapling.getMetadata());
}
```

### BlockSapling — growing

`BlockSapling` stores the tree variant in the upper nibble of its metadata
(bits 4–7; bit 3 is the "ready to grow" flag). `updateTick` accumulates light
and time and calls `growTree` when the conditions are met.

`growTree` is a public method so it can also be called externally (Bonemeal,
dispenser, future API):

```java
public final void growTree(World world, int x, int y, int z, Random rand) {
    int saplingId = world.getBlockId(x, y, z);
    int meta = world.getBlockMetadata(x, y, z) & 0xF0;  // strip growth flag

    EnumTreeType tree = EnumTreeType.findTreeTypeFromSapling(saplingId, meta);
    WorldGenerator worldGen = tree.getGenerator(rand);

    if (tree.getNeedsFourSaplings()) {
        // 2x2 placement check — only for multi-sapling trees (spruce, birch, etc.)
        // ...
    } else {
        world.setTileNoUpdate(x, y, z, 0);
        if (worldGen == null || !worldGen.generate(world, rand, x, y, z)) {
            world.setTileNoUpdate(x, y, z, saplingId);
            world.setBlockMetadataWithNotify(x, y, z, meta);
        }
    }
}
```

`damageDropped` on `BlockSapling` returns `metadata & 0xF7` so that a broken
sapling drops itself with bit 3 cleared, preserving the subtype (bits 4-7) while
forcing it to re-accumulate light/time.

### BlockLog

`BlockLog` holds the `OAK` constant and the leaf-decay flagging logic is
unchanged.

## Subtype constants

Each block class carries its own subtype constant:

| Class          | Constant | Current value | Purpose                        |
|----------------|----------|---------------|--------------------------------|
| `BlockLeaves`  | `OAK`    | `0`           | Leaf variant index              |
| `BlockLog`     | `OAK`    | `0`           | Log/tree-ring variant index     |
| `BlockSapling` | `OAK`    | `0`           | Sapling/tree-type variant index |

Future tree variants add new constants and a new `EnumTreeType` entry, then
register the leaf/log/sapling block states in the three lookup maps inside the
enum's static initializer.
