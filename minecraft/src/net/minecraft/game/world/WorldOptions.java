package net.minecraft.game.world;

import com.mojang.nbt.NBTTagCompound;

/**
 * The per-world generation preferences chosen when a world is created.
 *
 * <p>For now every flag is an unused placeholder that generation does not act
 * on yet: {@code generateStructures}, {@code generateCaves}, {@code winterMode}
 * and {@code generateBiomes} all default to false. The object is created fresh
 * on the world-selection screen, travels through {@link World} into the chunk
 * provider - whose constructor stores a reference future generation passes can
 * read - and persists in level.dat alongside the seed, so an existing world
 * keeps the options it was created with. Old worlds saved without the section
 * simply load every flag at its default.
 */
public class WorldOptions {
	/** Whether structures (villages, dungeons, ...) should be generated. */
	private boolean generateStructures;
	/** Whether caves should be carved into the terrain. */
	private boolean generateCaves;
	/** Whether the world should use the winter climate rules. */
	private boolean winterMode;
	/** Whether biomes should be calculated and applied to the terrain. */
	private boolean generateBiomes;

	/** Returns whether structures are requested for this world. */
	public final boolean isGenerateStructures() {
		return this.generateStructures;
	}

	/** Sets whether structures are requested for this world. */
	public final void setGenerateStructures(boolean generateStructures) {
		this.generateStructures = generateStructures;
	}

	/** Returns whether caves are requested for this world. */
	public final boolean isGenerateCaves() {
		return this.generateCaves;
	}

	/** Sets whether caves are requested for this world. */
	public final void setGenerateCaves(boolean generateCaves) {
		this.generateCaves = generateCaves;
	}

	/** Returns whether the winter climate is requested for this world. */
	public final boolean isWinterMode() {
		return this.winterMode;
	}

	/** Sets whether the winter climate is requested for this world. */
	public final void setWinterMode(boolean winterMode) {
		this.winterMode = winterMode;
	}

	/** Returns whether biomes are requested for this world. */
	public final boolean isGenerateBiomes() {
		return this.generateBiomes;
	}

	/** Sets whether biomes are requested for this world. */
	public final void setGenerateBiomes(boolean generateBiomes) {
		this.generateBiomes = generateBiomes;
	}

	/** Writes the four flags into the given tag (the level.dat "Data" section). */
	public final void writeToNBT(NBTTagCompound tag) {
		tag.setBoolean("GenerateStructures", this.generateStructures);
		tag.setBoolean("GenerateCaves", this.generateCaves);
		tag.setBoolean("WinterMode", this.winterMode);
		tag.setBoolean("GenerateBiomes", this.generateBiomes);
	}

	/**
	 * Reads the four flags from the given tag. Keys absent from old worlds save
	 * quietly as false, leaving every option on its default.
	 */
	public final void readFromNBT(NBTTagCompound tag) {
		this.generateStructures = tag.getBoolean("GenerateStructures");
		this.generateCaves = tag.getBoolean("GenerateCaves");
		this.winterMode = tag.getBoolean("WinterMode");
		this.generateBiomes = tag.getBoolean("GenerateBiomes");
	}
}