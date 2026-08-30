package net.minecraft.game.world;

/**
 * Represents sky blocks and block variants in the world.
 * 
 * Contains two constants: SKY (with light value 15) and BLOCK (with light value 0).
 */
public enum EnumSkyBlock {
	Sky(15),
	Block(0);

	public final int defaultLightValue;

	private EnumSkyBlock(int lightValue) {
		this.defaultLightValue = lightValue;
	}
}
