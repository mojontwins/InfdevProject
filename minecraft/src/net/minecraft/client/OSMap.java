package net.minecraft.client;

/**
 * Maps each {@link EnumOS} value to the numeric OS id used by the sound
 * system. The per-value try/catch tolerates running against older bytecode
 * where a given enum constant (and thus field) is absent.
 */
final class OSMap {
	static final int[] osValues = new int[EnumOS.values().length];

	static {
		try {
			osValues[EnumOS.linux.ordinal()] = 1;
		} catch (NoSuchFieldError e) {
		}

		try {
			osValues[EnumOS.solaris.ordinal()] = 2;
		} catch (NoSuchFieldError e) {
		}

		try {
			osValues[EnumOS.windows.ordinal()] = 3;
		} catch (NoSuchFieldError e) {
		}

		try {
			osValues[EnumOS.macos.ordinal()] = 4;
		} catch (NoSuchFieldError e) {
		}
	}
}
