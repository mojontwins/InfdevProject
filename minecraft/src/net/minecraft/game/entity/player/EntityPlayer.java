package net.minecraft.game.entity.player;

import java.util.List;
import net.minecraft.game.IInventory;
import net.minecraft.game.entity.Entity;
import net.minecraft.game.entity.EntityLiving;
import net.minecraft.game.entity.misc.EntityItem;
import net.minecraft.game.entity.monster.EntityMonster;
import net.minecraft.game.entity.projectile.EntityArrow;
import net.minecraft.game.item.Item;
import net.minecraft.game.item.ItemArmor;
import net.minecraft.game.item.ItemStack;
import net.minecraft.game.world.World;
import net.minecraft.game.world.block.Block;
import net.minecraft.game.world.block.tileentity.TileEntityFurnace;
import net.minecraft.game.world.material.Material;
import util.MathHelper;

/**
 * The player: health, inventory, armour and score. Movement and the key/mouse
 * wiring live in the client subclass; what remains here is shared logic — the
 * camera bob, the armour-tapered damage pipeline and the death sequence.
 */
public class EntityPlayer extends EntityLiving {
	public InventoryPlayer inventory = new InventoryPlayer(this);
	public int score = 0;
	public float prevCameraYaw;
	public float cameraYaw;
	protected String username;
	/** Armour damage that did not divide evenly across the armour hopper; carried over to the next blow. */
	private int damageRemainder = 0;

	public EntityPlayer(World world) {
		super(world);
		this.setLocationAndAngles((double)world.spawnX + 0.5D, (double)world.spawnY, (double)world.spawnZ + 0.5D, 0.0F, 0.0F);
		this.yOffset = 1.62F;
		this.health = 20;
		this.fireResistance = 20;
		this.texture = "/char.png";
	}

	/** Teleports and sizes the player in at the world spawn point. */
	public final void preparePlayerToSpawn() {
		this.yOffset = 1.62F;
		this.setSize(0.6F, 1.8F);
		super.preparePlayerToSpawn();
		if(this.worldObj != null) {
			this.worldObj.playerEntity = this;
		}

		this.health = 20;
		this.deathTime = 0;
	}

	public void onLivingUpdate() {
		// Peaceful mode: regenerate one heart every few seconds.
		if(this.worldObj.difficultySetting == 0 && this.health < 20 && this.ticksExisted % 20 << 2 == 0) {
			this.heal(1);
		}

		for(ItemStack stack : this.inventory.mainInventory) {
			if(stack != null && stack.animationsToGo > 0) {
				--stack.animationsToGo;
			}
		}

		this.prevCameraYaw = this.cameraYaw;
		super.onLivingUpdate();
		// Head bob: sway sideways with horizontal speed and bounce on landings.
		float bob = MathHelper.sqrt_double(this.motionX * this.motionX + this.motionZ * this.motionZ);
		float rise = (float)Math.atan(-this.motionY * (double)0.2F) * 15.0F;
		if(bob > 0.1F) {
			bob = 0.1F;
		}

		if(!this.onGround || this.health <= 0) {
			bob = 0.0F;
		}

		if(this.onGround || this.health <= 0) {
			rise = 0.0F;
		}

		this.cameraYaw += (bob - this.cameraYaw) * 0.4F;
		this.cameraPitch += (rise - this.cameraPitch) * 0.8F;
		if(this.health > 0) {
			// Give every touching entity a chance to react to the player.
			List<Entity> nearby = this.worldObj.getEntitiesWithinAABBExcludingEntity(this, this.boundingBox.expand(1.0D, 0.0D, 1.0D));
			if(nearby != null) {
				for(Entity entity : nearby) {
					entity.onCollideWithPlayer(this);
				}
			}
		}

	}

	/** Shrinks the body to a crumpled corpse, drops the inventory and kicks away from the killer. */
	public final void onDeath(Entity killer) {
		this.setSize(0.2F, 0.2F);
		this.setPosition(this.posX, this.posY, this.posZ);
		this.motionY = (double)0.1F;
		if(this.username.equals("Notch")) {
			this.dropPlayerItemWithRandomChoice(new ItemStack(Item.apple, 1), true);
		}

		this.inventory.dropAllItems();
		if(killer != null) {
			this.motionX = (double)(-MathHelper.cos((this.attackedAtYaw + this.rotationYaw) * (float)Math.PI / 180.0F) * 0.1F);
			this.motionZ = (double)(-MathHelper.sin((this.attackedAtYaw + this.rotationYaw) * (float)Math.PI / 180.0F) * 0.1F);
		} else {
			this.motionX = this.motionZ = 0.0D;
		}

		this.yOffset = 0.1F;
	}

	public final void dropPlayerItem(ItemStack item) {
		this.dropPlayerItemWithRandomChoice(item, false);
	}

	/**
	 * Drops an item in front of the player (largely matching the camera aim),
	 * or scatters it randomly when {@code scatter} is set (e.g. from Notch's
	 * apple on death).
	 */
	public final void dropPlayerItemWithRandomChoice(ItemStack item, boolean scatter) {
		if(item != null) {
			EntityItem entityItem = new EntityItem(this.worldObj, this.posX, this.posY - (double)0.3F, this.posZ, item);
			entityItem.delayBeforeCanPickup = 40;
			float speed;
			float angle;
			if(scatter) {
				speed = this.rand.nextFloat() * 0.5F;
				angle = this.rand.nextFloat() * (float)Math.PI * 2.0F;
				entityItem.motionX = (double)(-MathHelper.sin(angle) * speed);
				entityItem.motionZ = (double)(MathHelper.cos(angle) * speed);
				entityItem.motionY = (double)0.2F;
			} else {
				entityItem.motionX = (double)(-MathHelper.sin(this.rotationYaw / 180.0F * (float)Math.PI) * MathHelper.cos(this.rotationPitch / 180.0F * (float)Math.PI) * 0.3F);
				entityItem.motionZ = (double)(MathHelper.cos(this.rotationYaw / 180.0F * (float)Math.PI) * MathHelper.cos(this.rotationPitch / 180.0F * (float)Math.PI) * 0.3F);
				entityItem.motionY = (double)(-MathHelper.sin(this.rotationPitch / 180.0F * (float)Math.PI) * 0.3F + 0.1F);
				speed = this.rand.nextFloat() * (float)Math.PI * 2.0F;
				angle = 0.02F * this.rand.nextFloat();
				entityItem.motionX += Math.cos((double)speed) * (double)angle;
				entityItem.motionY += (double)((this.rand.nextFloat() - this.rand.nextFloat()) * 0.1F);
				entityItem.motionZ += Math.sin((double)speed) * (double)angle;
			}

			this.worldObj.spawnEntityInWorld(entityItem);
		}
	}

	/** True when the currently selected item can break the given block. */
	public final boolean canHarvestBlock(Block block) {
		if(block.blockMaterial != Material.rock && block.blockMaterial != Material.iron) {
			return true;
		} else {
			ItemStack stack = this.inventory.getStackInSlot(this.inventory.currentItem);
			return stack != null ? Item.itemsList[stack.itemID].canHarvestBlock(block) : false;
		}
	}

	public void displayChestGUI(IInventory inventory) {
	}

	public void displayWorkbenchGUI() {
	}

	public void onItemPickup(Entity item) {
	}

	protected final float getEyeHeight() {
		return 0.12F;
	}

	/**
	 * Damage for the player: scaled by the difficulty, softened by armour on a
	 * 25-point hopper (with the odd remainder carried over), and nullified
	 * while the red hearts flash is still up. Returns true when the blow landed.
	 */
	public final boolean attackEntityFrom(Entity attacker, int damage) {
		this.entityAge = 0;
		if(this.health <= 0) {
			return false;
		} else if((float)this.heartsLife > (float)this.heartsHalvesLife / 2.0F) {
			// Still inside the post-hit grace window: the blow is ignored entirely.
			return false;
		} else {
			if(attacker instanceof EntityMonster || attacker instanceof EntityArrow) {
				// Hostile damage scales with the difficulty setting.
				if(this.worldObj.difficultySetting == 0) {
					damage = 0;
				}

				if(this.worldObj.difficultySetting == 1) {
					damage = damage / 3 + 1;
				}

				if(this.worldObj.difficultySetting == 3) {
					damage = damage * 3 / 2;
				}
			}

			// Armour absorbs a fraction: every protected point shaves one point
			// from a 25-scale hopper, and whatever did not divide evenly is kept.
			int hopper = 25 - this.inventory.getPlayerArmorValue();
			hopper = damage * hopper + this.damageRemainder;

			for(int slot = 0; slot < this.inventory.armorInventory.length; ++slot) {
				ItemStack armorStack = this.inventory.armorInventory[slot];
				if(armorStack != null && armorStack.getItem() instanceof ItemArmor) {
					armorStack.damageItem(damage);
					if(armorStack.stackSize == 0) {
						this.inventory.armorInventory[slot] = null;
					}
				}
			}

			damage = hopper / 25;
			this.damageRemainder = hopper % 25;
			if(damage == 0) {
				return false;
			} else {
				return super.attackEntityFrom(attacker, damage);
			}
		}
	}

	public void displayFurnaceGUI(TileEntityFurnace tileEntityFurnace) {
	}
}