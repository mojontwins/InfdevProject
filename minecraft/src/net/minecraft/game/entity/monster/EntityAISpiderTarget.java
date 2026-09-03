package net.minecraft.game.entity.monster;

import net.minecraft.game.entity.EntityLiving;
import net.minecraft.game.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.game.entity.player.EntityPlayer;

/**
 * Spider-specific targeting: only hunts while it is dark enough
 * (brightness &lt; 0.5). In bright light it never acquires a target, and any
 * existing target is dropped.
 */
public class EntityAISpiderTarget extends EntityAINearestAttackableTarget {
	private final EntitySpider spider;

	public EntityAISpiderTarget(EntitySpider spider, Class<?> targetClass, float searchRadius, int chanceDivisor, boolean checkSight) {
		super(spider, targetClass, searchRadius, chanceDivisor, checkSight);
		this.spider = spider;
	}

	@Override
	public boolean shouldExecute() {
		if (this.spider.getEntityBrightness(1.0F) >= 0.5F) {
			return false;
		}
		return super.shouldExecute();
	}

	@Override
	public void updateTask() {
		if (this.spider.getEntityBrightness(1.0F) >= 0.5F) {
			this.spider.setAttackTarget(null);
			return;
		}
		super.updateTask();
	}
}
