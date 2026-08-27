package net.minecraft.game.world.block.tileentity;

import com.mojang.nbt.NBTTagCompound;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.game.world.World;

public class TileEntity {
	private static Map<String, Class<? extends TileEntity>> nameToClassMap = new HashMap<>();
	private static Map<Class<? extends TileEntity>, String> classToNameMap = new HashMap<>();
	public World worldObj;
	public int xCoord;
	public int yCoord;
	public int zCoord;

	private static void addMapping(Class<? extends TileEntity> tileEntityClass, String name) {
		nameToClassMap.put(name, tileEntityClass);
		classToNameMap.put(tileEntityClass, name);
	}

	public void readFromNBT(NBTTagCompound tag) {
		this.xCoord = tag.getInteger("x");
		this.yCoord = tag.getInteger("y");
		this.zCoord = tag.getInteger("z");
	}

	public void writeToNBT(NBTTagCompound tag) {
		tag.setString("id", classToNameMap.get(this.getClass()));
		tag.setInteger("x", this.xCoord);
		tag.setInteger("y", this.yCoord);
		tag.setInteger("z", this.zCoord);
	}

	public void updateEntity() {
	}

	public static TileEntity createAndLoadEntity(NBTTagCompound tag) {
		TileEntity tileEntity = null;
		try {
			Class<? extends TileEntity> tileEntityClass = nameToClassMap.get(tag.getString("id"));
			if(tileEntityClass != null) {
				tileEntity = tileEntityClass.newInstance();
			}
		} catch (Exception exception) {
			exception.printStackTrace();
		}
		if(tileEntity != null) {
			tileEntity.readFromNBT(tag);
		} else {
			System.out.println("Skipping TileEntity with id " + tag.getString("id"));
		}
		return tileEntity;
	}

	static {
		addMapping(TileEntityFurnace.class, "Furnace");
		addMapping(TileEntityChest.class, "Chest");
	}
}