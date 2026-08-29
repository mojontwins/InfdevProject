package net.minecraft.game.entity.animal;

import com.mojang.nbt.NBTTagCompound;
import net.minecraft.game.entity.Entity;
import net.minecraft.game.entity.EntityLiving;
import net.minecraft.game.entity.misc.EntityItem;
import net.minecraft.game.world.World;
import net.minecraft.game.world.block.Block;

/**
 * A wooly animal: the very first melee blow shears it — releasing 1-3 grey
 * cloth with a lively little tumble — after which it stays wool-less for the
 * rest of its life.
 */
public class EntitySheep extends EntityAnimal {
	public boolean sheared = false;

	public EntitySheep(World world) {
		super(world);
		this.texture = "/mob/sheep.png";
		this.setSize(0.9F, 1.3F);
	}

	public final boolean attackEntityFrom(Entity attacker, int damage) {
		if(!this.sheared && attacker instanceof EntityLiving) {
			this.sheared = true;
			int clothCount = 1 + this.rand.nextInt(3);

			for(int cloth = 0; cloth < clothCount; ++cloth) {
				EntityItem clothItem = this.entityDropItem(Block.clothGray.blockID, 1, 1.0F);
				clothItem.motionY += (double)(this.rand.nextFloat() * 0.05F);
				clothItem.motionX += (double)((this.rand.nextFloat() - this.rand.nextFloat()) * 0.1F);
				clothItem.motionZ += (double)((this.rand.nextFloat() - this.rand.nextFloat()) * 0.1F);
			}
		}

		return super.attackEntityFrom(attacker, damage);
	}

	public final void writeEntityToNBT(NBTTagCompound compound) {
		super.writeEntityToNBT(compound);
		compound.setBoolean("Sheared", this.sheared);
	}

	public final void readEntityFromNBT(NBTTagCompound compound) {
		super.readEntityFromNBT(compound);
		this.sheared = compound.getBoolean("Sheared");
	}

	protected final String getLivingSound() {
		return "mob.sheep";
	}

	protected final String getHurtSound() {
		return "mob.sheep";
	}

	protected final String getDeathSound() {
		return "mob.sheep";
	}
}