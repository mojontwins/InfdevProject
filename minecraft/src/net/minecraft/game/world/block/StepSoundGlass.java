package net.minecraft.game.world.block;

final class StepSoundGlass extends StepSound {
	StepSoundGlass() {
		super("glass", 1.0F, 1.0F);
	}

	@Override
	public final String getBreakSound() {
		return "random.glass";
	}
}