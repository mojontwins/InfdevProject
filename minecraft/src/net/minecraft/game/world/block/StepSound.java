package net.minecraft.game.world.block;

public class StepSound {
	private final String stepSoundName;
	public final float stepSoundVolume;
	public final float stepSoundPitch;

	public StepSound(String stepSoundName, float stepSoundVolume, float stepSoundPitch) {
		this.stepSoundName = stepSoundName;
		this.stepSoundVolume = stepSoundVolume;
		this.stepSoundPitch = stepSoundPitch;
	}

	public String getBreakSound() {
		return "step." + this.stepSoundName;
	}

	public final String getStepSound() {
		return "step." + this.stepSoundName;
	}
}