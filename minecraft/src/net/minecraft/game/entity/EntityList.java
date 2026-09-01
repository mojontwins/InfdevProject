package net.minecraft.game.entity;

import com.mojang.nbt.NBTTagCompound;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.game.entity.animal.EntityCow;
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

/**
 * Registry that maps the string ids written into save files ("Mob", "PrimedTnt",
 * "FallingSand", ...) to their entity classes and back. The string is the source
 * of truth that must never change — it is what older saves carry.
 */
public final class EntityList {
	private static final Map<String, Class<? extends Entity>> STRING_TO_CLASS = new HashMap<>();
	private static final Map<Class<? extends Entity>, String> CLASS_TO_STRING = new HashMap<>();

	private static void addMapping(Class<? extends Entity> entityClass, String id) {
		STRING_TO_CLASS.put(id, entityClass);
		CLASS_TO_STRING.put(entityClass, id);
	}

	public static Entity createEntityFromNBT(NBTTagCompound tag, World world) {
		Entity entity = null;

		try {
			Class<? extends Entity> entityClass = STRING_TO_CLASS.get(tag.getString("id"));
			if (entityClass != null) {
				entity = entityClass.getConstructor(World.class).newInstance(world);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (entity != null) {
			entity.readFromNBT(tag);
		} else {
			System.out.println("Skipping Entity with id " + tag.getString("id"));
		}

		return entity;
	}

	public static String getEntityString(Entity entity) {
		return CLASS_TO_STRING.get(entity.getClass());
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
		addMapping(EntityCow.class, "Cow");
		addMapping(EntityTNT.class, "PrimedTnt");
		addMapping(EntityFallingSand.class, "FallingSand");
	}
}