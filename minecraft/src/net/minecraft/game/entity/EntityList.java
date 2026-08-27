package net.minecraft.game.entity;

import com.mojang.nbt.NBTTagCompound;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.game.entity.animal.EntityPig;
import net.minecraft.game.entity.animal.EntitySheep;
import net.minecraft.game.entity.misc.EntityFallingSand;
import net.minecraft.game.entity.misc.EntityItem;
import net.minecraft.game.entity.misc.EntityTNT;
import net.minecraft.game.entity.monster.EntityCreeper;
import net.minecraft.game.entity.monster.EntityGiant;
import net.minecraft.game.entity.monster.EntityMonster;
import net.minecraft.game.entity.monster.EntitySkeleton;
import net.minecraft.game.entity.monster.EntitySpider;
import net.minecraft.game.entity.monster.EntityZombie;
import net.minecraft.game.entity.projectile.EntityArrow;
import net.minecraft.game.world.World;

public final class EntityList {
	private static Map<String, Class<? extends Entity>> stringToClassMapping = new HashMap<>();
	private static Map<Class<? extends Entity>, String> classToStringMapping = new HashMap<>();

	private static void addMapping(Class<? extends Entity> var0, String var1) {
		stringToClassMapping.put(var1, var0);
		classToStringMapping.put(var0, var1);
	}

	public static Entity createEntityFromNBT(NBTTagCompound var0, World var1) {
		Entity var2 = null;

		try {
			Class<? extends Entity> var3 = stringToClassMapping.get(var0.getString("id"));
			if(var3 != null) {
				var2 = (Entity)var3.getConstructor(new Class<?>[]{World.class}).newInstance(new Object[]{var1});
			}
		} catch (Exception var4) {
			var4.printStackTrace();
		}

		if(var2 != null) {
			var2.readFromNBT(var0);
		} else {
			System.out.println("Skipping Entity with id " + var0.getString("id"));
		}

		return var2;
	}

	public static String getEntityString(Entity var0) {
		return classToStringMapping.get(var0.getClass());
	}

	static {
		addMapping(EntityArrow.class, "Arrow");
		addMapping(EntityItem.class, "Item");
		addMapping(EntityPainting.class, "Painting");
		addMapping(EntityLiving.class, "Mob");
		addMapping(EntityMonster.class, "Monster");
		addMapping(EntityCreeper.class, "Creeper");
		addMapping(EntitySkeleton.class, "Skeleton");
		addMapping(EntitySpider.class, "Spider");
		addMapping(EntityGiant.class, "Giant");
		addMapping(EntityZombie.class, "Zombie");
		addMapping(EntityPig.class, "Pig");
		addMapping(EntitySheep.class, "Sheep");
		addMapping(EntityTNT.class, "PrimedTnt");
		addMapping(EntityFallingSand.class, "FallingSand");
	}
}
