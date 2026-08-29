package net.minecraft.game.entity.animal;

import net.minecraft.game.item.Item;
import net.minecraft.game.world.World;

/** The friendly, oinking food animal. */
public class EntityPig extends EntityAnimal {
	public EntityPig(World world) {
		super(world);
		this.texture = "/mob/pig.png";
		this.setSize(0.9F, 0.9F);
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