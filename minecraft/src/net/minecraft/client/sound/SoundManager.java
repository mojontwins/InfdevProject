package net.minecraft.client.sound;

import java.io.File;
import net.minecraft.client.GameSettings;
import net.minecraft.game.entity.EntityLiving;
import paulscode.sound.SoundSystem;
import paulscode.sound.SoundSystemConfig;
import paulscode.sound.codecs.CodecJOrbis;
import paulscode.sound.codecs.CodecWav;
import paulscode.sound.libraries.LibraryLWJGLOpenAL;
import util.MathHelper;

public final class SoundManager {
	// Manages the audio engine: loads the OpenAL library, registers sounds/music and plays them.
	private SoundSystem sndSystem;
	private SoundPool soundPoolSounds = new SoundPool();
	private SoundPool soundPoolMusic = new SoundPool();
	private int playedSoundsCount = 0;
	private GameSettings options;
	private boolean loaded = false;

	public final void loadSoundSettings(GameSettings settings) {
		this.options = settings;
		if(!this.loaded && (settings.sound || settings.music)) {
			this.tryToSetLibraryAndCodecs();
		}

	}

	// Set up the sound library and codecs; temporarily disables sound in the settings so no events fire mid-setup.
	private void tryToSetLibraryAndCodecs() {
		try {
			boolean wasSound = this.options.sound;
			boolean wasMusic = this.options.music;
			this.options.sound = false;
			this.options.music = false;
			this.options.saveOptions();
			SoundSystemConfig.addLibrary(LibraryLWJGLOpenAL.class);
			SoundSystemConfig.setCodec("ogg", CodecJOrbis.class);
			SoundSystemConfig.setCodec("wav", CodecWav.class);
			this.sndSystem = new SoundSystem();
			this.options.sound = wasSound;
			this.options.music = wasMusic;
			this.options.saveOptions();
		} catch (Throwable error) {
			System.err.println("error linking with the LibraryJavaSound plug-in");
		}

		this.loaded = true;
	}

	public final void onSoundOptionsChanged() {
		if(!this.loaded && (this.options.sound || this.options.music)) {
			this.tryToSetLibraryAndCodecs();
		}

		// Stop the looping background music if music is turned off.
		if(!this.options.music) {
			this.sndSystem.stop("BgMusic");
		}

	}

	public final void closeMinecraft() {
		if(this.loaded) {
			this.sndSystem.cleanup();
		}

	}

	public final void addSound(String soundName, File file) {
		this.soundPoolSounds.addSound(soundName, file);
	}

	public final void addMusic(String soundName, File file) {
		this.soundPoolMusic.addSound(soundName, file);
	}

	// Place the 3D audio listener at the (interpolated) position/orientation of an entity.
	public final void setListener(EntityLiving entity, float partialTick) {
		if(this.loaded && this.options.sound) {
			if(entity != null) {
				float pitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTick;
				float yaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTick;
				double posX = entity.prevPosX + (entity.posX - entity.prevPosX) * (double)partialTick;
				double posY = entity.prevPosY + (entity.posY - entity.prevPosY) * (double)partialTick;
				double posZ = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * (double)partialTick;
				float cosYaw = MathHelper.cos(-yaw * ((float)Math.PI / 180.0F) - (float)Math.PI);
				partialTick = MathHelper.sin(-yaw * ((float)Math.PI / 180.0F) - (float)Math.PI);
				yaw = MathHelper.cos(-pitch * ((float)Math.PI / 180.0F));
				pitch = MathHelper.sin(-pitch * ((float)Math.PI / 180.0F));
				float fwdX = -partialTick * yaw;
				float fwdZ = -cosYaw * yaw;
				partialTick = -partialTick * pitch;
				cosYaw = -cosYaw * pitch;
				this.sndSystem.setListenerPosition((float)posX, (float)posY, (float)posZ);
				this.sndSystem.setListenerOrientation(fwdX, pitch, fwdZ, partialTick, yaw, cosYaw);
			}
		}
	}

	public final void playSound(String soundName, float x, float y, float z, float volume, float pitch) {
		if(this.loaded && this.options.sound) {
			SoundPoolEntry entry = this.soundPoolSounds.getRandomSoundFromSoundPool(soundName);
			if(entry != null && volume > 0.0F) {
				this.playedSoundsCount = (this.playedSoundsCount + 1) % 256;
				String sourceName = "sound_" + this.playedSoundsCount;
				float range = 16.0F;
				// Louder sounds project to a larger distance.
				if(volume > 1.0F) {
					range = 16.0F * volume;
				}

				this.sndSystem.newSource(volume > 1.0F, sourceName, entry.soundUrl, entry.soundName, false, x, y, z, 2, range);
				this.sndSystem.setPitch(sourceName, pitch);
				// Volume is clamped to 1 for the engine after computing range.
				if(volume > 1.0F) {
					volume = 1.0F;
				}

				this.sndSystem.setVolume(sourceName, volume);
				this.sndSystem.play(sourceName);
			}

		}
	}

	public final void playSoundFX(String soundName, float x, float y) {
		if(this.loaded && this.options.sound) {
			SoundPoolEntry entry = this.soundPoolSounds.getRandomSoundFromSoundPool(soundName);
			if(entry != null) {
				this.playedSoundsCount = (this.playedSoundsCount + 1) % 256;
				String sourceName = "sound_" + this.playedSoundsCount;
				this.sndSystem.newSource(false, sourceName, entry.soundUrl, entry.soundName, false, 0.0F, 0.0F, 0.0F, 0, 0.0F);
				this.sndSystem.setPitch(sourceName, 1.0F);
				this.sndSystem.setVolume(sourceName, 0.25F);
				this.sndSystem.play(sourceName);
			}

		}
	}
}
