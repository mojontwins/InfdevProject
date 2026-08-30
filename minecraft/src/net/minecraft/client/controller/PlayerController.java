package net.minecraft.client.controller;

import net.minecraft.client.Minecraft;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.game.entity.player.EntityPlayer;
import net.minecraft.game.world.World;
import net.minecraft.game.world.block.Block;
import net.minecraft.game.world.block.StepSound;

public class PlayerController {
	// The base player controller: mediates player <-> world interaction (blast, breath, test/survival modes).
	protected final Minecraft mc;
	public boolean isInTestMode = false;

	public PlayerController(Minecraft mc) {
		this.mc = mc;
	}

	public void clickBlock(int x, int y, int z) {
		this.sendBlockRemoved(x, y, z);
	}

	// Removes the block at (x, y, z), plays the break sound and triggers block destroy callbacks.
	public boolean sendBlockRemoved(int x, int y, int z) {
		this.mc.effectRenderer.addBlockDestroyEffects(x, y, z);
		World world = this.mc.theWorld;
		Block block = Block.blocksList[world.getBlockId(x, y, z)];
		int metadata = world.getBlockMetadata(x, y, z);
		boolean removed = world.setBlockWithNotify(x, y, z, 0);
		if(block != null && removed) {
			// Play the block's break sound centred on the block, scaled from its volume/pitch.
			SoundManager sndManager = this.mc.sndManager;
			String breakSound = block.stepSound.getBreakSound();
			float soundX = (float)x + 0.5F;
			float soundY = (float)y + 0.5F;
			float soundZ = (float)z + 0.5F;
			StepSound stepSound = block.stepSound;
			float volume = (stepSound.stepSoundVolume + 1.0F) / 2.0F;
			stepSound = block.stepSound;
			sndManager.playSound(breakSound, soundX, soundY, soundZ, volume, stepSound.stepSoundPitch * 0.8F);
			block.onBlockDestroyedByPlayer(world, x, y, z, metadata);
		}

		return removed;
	}

	public void sendBlockRemoving(int x, int y, int z, int side) {
	}

	public void resetBlockRemoving() {
	}

	public void setPartialTime(float partialTick) {
	}

	public float getBlockReachDistance() {
		return 5.0F;
	}

	public void onUpdate() {
	}

	public boolean shouldDrawHUD() {
		return true;
	}

	public void onRespawn(EntityPlayer player) {
	}
}
