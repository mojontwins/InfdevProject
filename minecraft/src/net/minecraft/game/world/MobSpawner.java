package net.minecraft.game.world;

import net.minecraft.game.entity.Entity;
import net.minecraft.game.entity.EntityLiving;
import net.minecraft.game.world.material.Material;
import util.IProgressUpdate;
import util.MathHelper;

public final class MobSpawner {
	private int maxSpawns;
	private Class<? extends EntityLiving> entityType;
	private Class<?>[] entities;

	public MobSpawner(int var1, Class<? extends EntityLiving> var2, Class<?>[] var3) {
		this.maxSpawns = var1;
		this.entityType = var2;
		this.entities = var3;
	}

	public final void onUpdate(World var1) {
		int var2 = var1.countEntities(this.entityType);
		if(var2 < this.maxSpawns) {
			this.performSpawning(var1, 1, var1.playerEntity, (IProgressUpdate)null);
		}

	}

	private int performSpawning(World var1, int var2, Entity var3, IProgressUpdate var4) {
		var2 = 0;
		int var30 = MathHelper.floor_double(var3.posX);
		int var5 = MathHelper.floor_double(var3.posZ);

		for(int var6 = 0; var6 <= 0; ++var6) {
			int var7 = var1.rand.nextInt(this.entities.length);
			int var8 = var30 + var1.rand.nextInt(256) - 128;
			int var9 = var1.rand.nextInt(128);
			int var10 = var5 + var1.rand.nextInt(256) - 128;
			if(!var1.isSolid(var8, var9, var10) && var1.getBlockMaterial(var8, var9, var10) == Material.air) {
				for(int var11 = 0; var11 < 6; ++var11) {
					int var12 = var8;
					int var13 = var9;
					int var14 = var10;

					for(int var15 = 0; var15 < 6; ++var15) {
						var12 += var1.rand.nextInt(6) - var1.rand.nextInt(6);
						var13 += var1.rand.nextInt(1) - var1.rand.nextInt(1);
						var14 += var1.rand.nextInt(6) - var1.rand.nextInt(6);
						if(var1.isSolid(var12, var13 - 1, var14) && !var1.isSolid(var12, var13, var14) && !var1.getBlockMaterial(var12, var13, var14).getIsLiquid() && !var1.isSolid(var12, var13 + 1, var14)) {
							float var16 = (float)var12 + 0.5F;
							float var17 = (float)var13 + 1.0F;
							float var18 = (float)var14 + 0.5F;
							if(var3 != null) {
								double var21 = (double)var16 - var3.posX;
								double var23 = (double)var17 - var3.posY;
								double var25 = (double)var18 - var3.posZ;
								double var27 = var21 * var21 + var23 * var23 + var25 * var25;
								if(var27 < 256.0D) {
									continue;
								}
							} else {
								float var31 = var16 - (float)var1.spawnX;
								float var22 = var17 - (float)var1.spawnY;
								float var33 = var18 - (float)var1.spawnZ;
								float var24 = var31 * var31 + var22 * var22 + var33 * var33;
								if(var24 < 256.0F) {
									continue;
								}
							}

							EntityLiving var32;
							try {
								var32 = (EntityLiving)this.entities[var7].getConstructor(new Class<?>[]{World.class}).newInstance(new Object[]{var1});
							} catch (Exception var29) {
								var29.printStackTrace();
								return var2;
							}

							var32.setLocationAndAngles((double)var16, (double)var17, (double)var18, var1.rand.nextFloat() * 360.0F, 0.0F);
							if(var32.getCanSpawnHere(var16, var17, var18)) {
								++var2;
								var1.spawnEntityInWorld(var32);
							}
						}
					}
				}
			}
		}

		return var2;
	}
}
