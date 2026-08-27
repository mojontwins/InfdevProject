package net.minecraft.game.world.block;

final class StepSoundGlass extends StepSound {
	StepSoundGlass(String stepSoundName, float volume, float pitch) {
		super(stepSoundName, 1.0F, 1.0F);
	}

	@Override
	public final String getBreakSound() {
		return "random.glass";
	}
}