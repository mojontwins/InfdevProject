package net.minecraft.game.entity.animal;

import net.minecraft.game.entity.ai.EntityAISwimming;
import net.minecraft.game.entity.ai.EntityAIWander;
import net.minecraft.game.entity.ai.EntityAIWatchClosest;
import net.minecraft.game.entity.ai.EntityAILookIdle;
import net.minecraft.game.entity.player.EntityPlayer;
import net.minecraft.game.item.Item;
import net.minecraft.game.world.World;

/** The friendly, oinking food animal. */
public class EntityPig extends EntityAnimal {
	public EntityPig(World world) {
		super(world);
		this.texture = "/mob/pig.png";
		this.setSize(0.9F, 0.9F);

		this.tasks.addTask(0, new EntityAISwimming(this));
		this.tasks.addTask(1, new EntityAIWander(this, 0.25F));
		this.tasks.addTask(2, new EntityAIWatchClosest(this, EntityPlayer.class, 6.0F));
		this.tasks.addTask(3, new EntityAILookIdle(this));
	}

	protected final String getLivingSound() {
		return "mob.pig";
	}

	protected final String getHurtSound() {
		return "mob.pig";
	}

	protected final String getDeathSound() {
		return "mob.pigdeath";
	}

	protected final int getDroppedItem() {
		return Item.porkRaw.shiftedIndex;
	}
}
