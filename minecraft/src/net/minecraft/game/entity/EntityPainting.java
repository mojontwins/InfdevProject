package net.minecraft.game.entity;

import com.mojang.nbt.NBTTagCompound;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.game.entity.misc.EntityItem;
import net.minecraft.game.item.Item;
import net.minecraft.game.item.ItemStack;
import net.minecraft.game.physics.AxisAlignedBB;
import net.minecraft.game.world.World;
import net.minecraft.game.world.material.Material;
import util.MathHelper;

/**
 * A painting hanging on a wall. It occupies a fixed set of full blocks (its
 * "backing"), keeps pointing at the wall it was placed on, breaks back into an
 * {@link EntityItem} if the backing is destroyed, and tips off the wall one
 * second after being placed somewhere the surface check no longer passes.
 */
public class EntityPainting extends Entity {
	/** How many ticks the painting has spent in an invalid spot before dropping off. */
	private int tickCounter;
	/** Facing as a compass style index: 0 = -Z, 1 = -X, 2 = +Z, 3 = +X. */
	public int direction;
	/** The block coordinates of the anchor point (one corner of the backing). */
	private int xPosition;
	private int yPosition;
	private int zPosition;
	/** The artwork currently on display. */
	public EnumArt art;

	public EntityPainting(World world) {
		super(world);
		this.tickCounter = 0;
		this.direction = 0;
		this.yOffset = 0.0F;
		this.setSize(0.5F, 0.5F);
	}

	/**
	 * Places a painting on a wall at the given block coordinates. Every art whose
	 * size fits the current wall surface becomes a candidate, and one is chosen
	 * at random.
	 */
	public EntityPainting(World world, int blockX, int blockY, int blockZ, int direction) {
		this(world);
		this.xPosition = blockX;
		this.yPosition = blockY;
		this.zPosition = blockZ;
		List<EnumArt> validArts = new ArrayList<>();
		for (EnumArt art : EnumArt.values()) {
			this.art = art;
			this.setDirection(direction);
			if (this.onValidSurface()) {
				validArts.add(art);
			}
		}
		if (validArts.size() > 0) {
			this.art = validArts.get(this.rand.nextInt(validArts.size()));
		}
		this.setDirection(direction);
	}

	/**
	 * Positions this painting against the wall it faces: the bounding box is
	 * half a block wide (the thickness) by the art's height, protruding one
	 * board's width (9/16 block) off the wall face.
	 */
	private void setDirection(int direction) {
		this.direction = direction;
		this.prevRotationYaw = this.rotationYaw = (float) (direction * 90);
		float halfWidth = (float) this.art.sizeX;
		float halfHeight = (float) this.art.sizeY;
		float halfDepth = (float) this.art.sizeX;
		if (direction != 0 && direction != 2) {
			halfWidth = 0.5F;
		} else {
			halfDepth = 0.5F;
		}

		halfWidth /= 32.0F;
		halfHeight /= 32.0F;
		halfDepth /= 32.0F;
		float centerX = (float) this.xPosition + 0.5F;
		float centerY = (float) this.yPosition + 0.5F;
		float centerZ = (float) this.zPosition + 0.5F;

		// Hover the canvas one board thickness in front of the wall face it points at.
		if (direction == 0) {
			centerZ -= 9.0F / 16.0F;
		}
		if (direction == 1) {
			centerX -= 9.0F / 16.0F;
		}
		if (direction == 2) {
			centerZ += 9.0F / 16.0F;
		}
		if (direction == 3) {
			centerX += 9.0F / 16.0F;
		}

		// Sweep the (half-width, zero or half depth) anchored footprint so the
		// canvas extends from its anchor corner.
		if (direction == 0) {
			centerX -= getArtSize(this.art.sizeX);
		}
		if (direction == 1) {
			centerZ += getArtSize(this.art.sizeX);
		}
		if (direction == 2) {
			centerX += getArtSize(this.art.sizeX);
		}
		if (direction == 3) {
			centerZ -= getArtSize(this.art.sizeX);
		}

		centerY += getArtSize(this.art.sizeY);
		this.setPosition((double) centerX, (double) centerY, (double) centerZ);
		this.boundingBox = new AxisAlignedBB((double) (centerX - halfWidth), (double) (centerY - halfHeight), (double) (centerZ - halfDepth), (double) (centerX + halfWidth), (double) (centerY + halfHeight), (double) (centerZ + halfDepth));
		this.boundingBox.maxX -= 0.00625F;
		this.boundingBox.maxY -= 0.00625F;
		this.boundingBox.maxZ -= 0.00625F;
	}

	/** Half of a 32-pixel canvas is exactly 0.5; smaller paintings sit flush at their anchor. */
	private static float getArtSize(int size) {
		return size == 32 ? 0.5F : (size == 64 ? 0.5F : 0.0F);
	}

	public final void onUpdate() {
		if (this.tickCounter++ == 100 && !this.onValidSurface()) {
			this.tickCounter = 0;
			super.isDead = true;
			this.worldObj.spawnEntityInWorld(new EntityItem(this.worldObj, this.posX, this.posY, this.posZ, new ItemStack(Item.painting)));
		}
	}

	/**
	 * The painting stays mounted only while its bounding box is not obstructed,
	 * the whole backing strip is made of solid material, and no other painting
	 * already occupies the same spot.
	 */
	public final boolean onValidSurface() {
		if (!this.worldObj.getCollidingBoundingBoxes(this.boundingBox).isEmpty()) {
			return false;
		} else {
			int paintingsWide = this.art.sizeX / 16;
			int paintingsTall = this.art.sizeY / 16;
			int anchorX = this.xPosition;
			int anchorZ = this.zPosition;
			if (this.direction == 0 || this.direction == 2) {
				anchorX = MathHelper.floor_double(this.posX - (double) ((float) this.art.sizeX / 32.0F));
			} else {
				anchorZ = MathHelper.floor_double(this.posZ - (double) ((float) this.art.sizeX / 32.0F));
			}

			int anchorY = MathHelper.floor_double(this.posY - (double) ((float) this.art.sizeY / 32.0F));

			for (int row = 0; row < paintingsWide; ++row) {
				for (int column = 0; column < paintingsTall; ++column) {
					Material backing;
					if (this.direction != 0 && this.direction != 2) {
						backing = this.worldObj.getBlockMaterial(this.xPosition, anchorY + column, anchorZ + row);
					} else {
						backing = this.worldObj.getBlockMaterial(anchorX + row, anchorY + column, this.zPosition);
					}

					if (!backing.isSolid()) {
						return false;
					}
				}
			}

			List<Entity> nearbyEntities = this.worldObj.getEntitiesWithinAABBExcludingEntity(this, this.boundingBox);
			return nearbyEntities.stream().noneMatch(nearby -> nearby instanceof EntityPainting);
		}
	}

	public final boolean canBeCollidedWith() {
		return true;
	}

	public final boolean attackEntityFrom(Entity entity, int damage) {
		super.isDead = true;
		this.worldObj.spawnEntityInWorld(new EntityItem(this.worldObj, this.posX, this.posY, this.posZ, new ItemStack(Item.painting)));
		return true;
	}

	public final void writeEntityToNBT(NBTTagCompound tag) {
		tag.setByte("Dir", (byte) this.direction);
		tag.setString("Motive", this.art.title);
		tag.setInteger("TileX", this.xPosition);
		tag.setInteger("TileY", this.yPosition);
		tag.setInteger("TileZ", this.zPosition);
	}

	public final void readEntityFromNBT(NBTTagCompound tag) {
		this.direction = tag.getByte("Dir");
		this.xPosition = tag.getInteger("TileX");
		this.yPosition = tag.getInteger("TileY");
		this.zPosition = tag.getInteger("TileZ");
		String motive = tag.getString("Motive");
		this.art = Arrays.stream(EnumArt.values()).filter(art -> art.title.equals(motive)).findFirst().orElse(null);

		if (this.art == null) {
			this.art = EnumArt.Kebab;
		}

		this.setDirection(this.direction);
	}
}