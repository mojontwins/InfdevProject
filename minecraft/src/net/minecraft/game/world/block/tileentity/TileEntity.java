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

	private static void addMapping(Class<? extends TileEntity> var0, String var1) {
		nameToClassMap.put(var1, var0);
		classToNameMap.put(var0, var1);
	}

	public void readFromNBT(NBTTagCompound var1) {
		this.xCoord = var1.getInteger("x");
		this.yCoord = var1.getInteger("y");
		this.zCoord = var1.getInteger("z");
	}

	public void writeToNBT(NBTTagCompound var1) {
		var1.setString("id", classToNameMap.get(this.getClass()));
		var1.setInteger("x", this.xCoord);
		var1.setInteger("y", this.yCoord);
		var1.setInteger("z", this.zCoord);
	}

	public void updateEntity() {
	}

	public static TileEntity createAndLoadEntity(NBTTagCompound var0) {
		TileEntity var1 = null;

		try {
			Class<? extends TileEntity> var2 = nameToClassMap.get(var0.getString("id"));
			if(var2 != null) {
				var1 = var2.newInstance();
			}
		} catch (Exception var3) {
			var3.printStackTrace();
		}

		if(var1 != null) {
			var1.readFromNBT(var0);
		} else {
			System.out.println("Skipping TileEntity with id " + var0.getString("id"));
		}

		return var1;
	}

	static {
		addMapping(TileEntityFurnace.class, "Furnace");
		addMapping(TileEntityChest.class, "Chest");
	}
}
