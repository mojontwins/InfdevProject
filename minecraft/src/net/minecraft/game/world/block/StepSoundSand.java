package net.minecraft.game.world.block;

final class StepSoundSand extends StepSound {
	StepSoundSand() {
		super("sand", 1.0F, 1.0F);
	}

	@Override
	public final String getBreakSound() {
		return "step.gravel";
	}
}