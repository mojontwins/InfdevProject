package net.minecraft.game.world.block;

/**
 * A block's footstep/break sound words, e.g. {@code step.stone} /
 * {@code step.wood}. Small subclasses ({@link StepSoundGlass},
 * {@link StepSoundSand}) only special-case the break variant - a mismatch the
 * original shipped with.
 */
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