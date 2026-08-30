package net.minecraft.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import org.lwjgl.input.Keyboard;

/**
 * Holds every user-tweakable option (sound, video, controls) plus the key
 * bindings, persisted to "options.txt" in the minecraft data directory.
 *
 * <p>The options themselves are described by the {@link GameOption} enum: each
 * constant knows its options.txt key, its value type and how to read/write its
 * backing field, so a new option is added by declaring one enum constant and
 * (if needed) one value field. The int-based methods ({@link #getKeyBinding(int)}
 * and {@link #setOptionFloatValue(int, int)}) address options by their row id
 * and are the public API used by the options screen.
 */
public final class GameSettings {
	private static final String[] RENDER_DISTANCES = new String[]{"FAR", "NORMAL", "SHORT", "TINY"};
	private static final String[] DIFFICULTIES = new String[]{"Peaceful", "Easy", "Normal", "Hard"};

	/** Step size used to nudge a 0-1 float option up or down by one delta. */
	private static final float FLOAT_OPTION_STEP = 0.05F;

	/** Clamps a value into the legal range of a float option (0-1). */
	private static float clamp01(float value) {
		return value < 0.0F ? 0.0F : (value > 1.0F ? 1.0F : value);
	}

	/**
	 * The kind of value an option holds. It drives how the option is cycled,
	 * rendered, loaded and saved.
	 */
	private enum OptionType {
		/** An ON/OFF toggle; delta is ignored, the option is inverted. */
		BOOLEAN,
		/** A discrete integer that wraps around through {@link GameOption#levelLabels}. */
		INTEGER,
		/** A continuous value between 0.0 and 1.0 (e.g. a percentage). */
		FLOAT;
	}

	/**
	 * A single row of the options screen. Each constant declares how its value
	 * is stored on disk, which backing field it reads/writes, how it should be
	 * displayed and what should happen when it changes.
	 */
	private enum GameOption {
		MUSIC(0, "music", OptionType.BOOLEAN, "Music", null, settings -> settings.music, (settings, value) -> settings.music = (Boolean) value, settings -> settings.mc.sndManager.onSoundOptionsChanged()),
		SOUND(1, "sound", OptionType.BOOLEAN, "Sound", null, settings -> settings.sound, (settings, value) -> settings.sound = (Boolean) value, settings -> settings.mc.sndManager.onSoundOptionsChanged()),
		INVERT_MOUSE(2, "invertYMouse", OptionType.BOOLEAN, "Invert mouse", null, settings -> settings.invertMouse, (settings, value) -> settings.invertMouse = (Boolean) value, null),
		SHOW_FPS(3, "showFrameRate", OptionType.BOOLEAN, "Show FPS", null, settings -> settings.showFPS, (settings, value) -> settings.showFPS = (Boolean) value, null),
		RENDER_DISTANCE(4, "viewDistance", OptionType.INTEGER, "Render distance", RENDER_DISTANCES, settings -> settings.renderDistance, (settings, value) -> settings.renderDistance = (Integer) value, null),
		VIEW_BOBBING(5, "bobView", OptionType.BOOLEAN, "View bobbing", null, settings -> settings.fancyGraphics, (settings, value) -> settings.fancyGraphics = (Boolean) value, null),
		ANAGLYPH(6, "anaglyph3d", OptionType.BOOLEAN, "3d anaglyph", null, settings -> settings.anaglyph, (settings, value) -> settings.anaglyph = (Boolean) value, settings -> settings.mc.renderEngine.refreshTextures()),
		LIMIT_FRAMERATE(7, "limitFramerate", OptionType.BOOLEAN, "Limit framerate", null, settings -> settings.limitFramerate, (settings, value) -> settings.limitFramerate = (Boolean) value, null),
		DIFFICULTY(8, "difficulty", OptionType.INTEGER, "Difficulty", DIFFICULTIES, settings -> settings.difficulty, (settings, value) -> settings.difficulty = (Integer) value, null);

		private final int id;
		private final String saveKey;
		private final OptionType type;
		private final String displayName;
		private final String[] levelLabels;
		private final Function<GameSettings, Object> getter;
		private final BiConsumer<GameSettings, Object> setter;
		private final Consumer<GameSettings> onChanged;

		GameOption(int id, String saveKey, OptionType type, String displayName, String[] levelLabels, Function<GameSettings, Object> getter, BiConsumer<GameSettings, Object> setter, Consumer<GameSettings> onChanged) {
			this.id = id;
			this.saveKey = saveKey;
			this.type = type;
			this.displayName = displayName;
			this.levelLabels = levelLabels;
			this.getter = getter;
			this.setter = setter;
			this.onChanged = onChanged;
		}

		/** Looks up an option by its options-screen row id, or returns null when unknown. */
		static GameOption byId(int optionId) {
			for(GameOption option : values()) {
				if(option.id == optionId) {
					return option;
				}
			}

			return null;
		}

		/** Looks up an option by its options.txt key, or returns null when unknown (e.g. a key binding line). */
		static GameOption bySaveKey(String saveKey) {
			for(GameOption option : values()) {
				if(option.saveKey.equals(saveKey)) {
					return option;
				}
			}

			return null;
		}

		/** The number of discrete levels of an integer option (labels.length), 0 for other types. */
		int levelCount() {
			return this.levelLabels == null ? 0 : this.levelLabels.length;
		}
	}

	public boolean music = true;
	public boolean sound = true;
	public boolean invertMouse = false;
	public boolean showFPS = false;
	public int renderDistance = 0;
	public boolean fancyGraphics = true;
	public boolean anaglyph = false;
	public boolean limitFramerate = false;
	public KeyBinding keyBindForward = new KeyBinding("Forward", 17);
	public KeyBinding keyBindLeft = new KeyBinding("Left", 30);
	public KeyBinding keyBindBack = new KeyBinding("Back", 31);
	public KeyBinding keyBindRight = new KeyBinding("Right", 32);
	public KeyBinding keyBindJump = new KeyBinding("Jump", 57);
	public KeyBinding keyBindSneak = new KeyBinding("Sneak", 42);
	public KeyBinding keyBindInventory = new KeyBinding("Inventory", 23);
	public KeyBinding keyBindDrop = new KeyBinding("Drop", 16);
	private KeyBinding keyBindChat = new KeyBinding("Chat", 20);
	public KeyBinding keyBindToggleFog = new KeyBinding("Toggle fog", 33);
	public KeyBinding keyBindSave = new KeyBinding("Save location", 28);
	public KeyBinding keyBindLoad = new KeyBinding("Load location", 19);
	public KeyBinding[] keyBindings = new KeyBinding[]{this.keyBindForward, this.keyBindLeft, this.keyBindBack, this.keyBindRight, this.keyBindJump, this.keyBindSneak, this.keyBindInventory, this.keyBindDrop, this.keyBindChat, this.keyBindToggleFog, this.keyBindSave, this.keyBindLoad};
	private Minecraft mc;
	private File optionsFile;

	/** Number of option rows in the options screen; always equals the number of {@link GameOption} constants. */
	public int numberOfOptions = GameOption.values().length;
	public int difficulty = 2;
	public boolean thirdPersonView = false;

	/** Creates the settings holder and immediately loads any saved options. */
	public GameSettings(Minecraft minecraft, File dataDir) {
		this.mc = minecraft;
		this.optionsFile = new File(dataDir, "options.txt");
		this.loadOptions();
	}

	/** Returns the label shown next to a key binding in the options screen. */
	public final String getOptionDisplayString(int bindingIndex) {
		return this.keyBindings[bindingIndex].keyDescription + ": " + Keyboard.getKeyName(this.keyBindings[bindingIndex].keyCode);
	}

	/** Stores a new key code for a binding and persists the change. */
	public final void setKeyBinding(int bindingIndex, int keyCode) {
		this.keyBindings[bindingIndex].keyCode = keyCode;
		this.saveOptions();
	}

	/** Reads the current boolean value of the given option. */
	private boolean getBoolean(GameOption option) {
		return (Boolean)option.getter.apply(this);
	}

	/** Reads the current integer value of the given option. */
	private int getInteger(GameOption option) {
		return (Integer)option.getter.apply(this);
	}

	/** Reads the current 0-1 float value of the given option. */
	private float getFloat(GameOption option) {
		return (Float)option.getter.apply(this);
	}

	/** Writes a new value (Boolean, Integer or Float matching the option type) to the option's field. */
	private void setValue(GameOption option, Object value) {
		option.setter.accept(this, value);
	}

	/**
	 * Advances one option row by one step: {@code delta} is the cycle step for
	 * integer/float options and is ignored for boolean toggles. Persists the
	 * change afterwards.
	 */
	public final void setOptionFloatValue(int optionId, int delta) {
		GameOption option = GameOption.byId(optionId);
		if(option != null) {
			switch(option.type) {
				case BOOLEAN:
					setValue(option, !getBoolean(option));
					break;
				case INTEGER:
					// Wrap around through the option's levels, e.g. (0 + -1) % 4 -> 3.
					int levelCount = option.levelCount();
					int nextLevel = (getInteger(option) + delta) % levelCount;
					if(nextLevel < 0) {
						nextLevel += levelCount;
					}

					setValue(option, nextLevel);
					break;
				case FLOAT:
					// Nudge by the fixed step and clamp into the legal 0-1 range.
					setValue(option, clamp01(getFloat(option) + (float)delta * FLOAT_OPTION_STEP));
					break;
			}

			if(option.onChanged != null) {
				option.onChanged.accept(this);
			}
		}

		this.saveOptions();
	}

	/**
	 * Builds the textual description of one option row for the options screen,
	 * e.g. "Music: ON", "Render distance: FAR" or a percentage for float options.
	 */
	public final String getKeyBinding(int optionId) {
		GameOption option = GameOption.byId(optionId);
		if(option == null) {
			return "";
		}

		String prefix = option.displayName + ": ";
		switch(option.type) {
			case BOOLEAN:
				return prefix + (getBoolean(option) ? "ON" : "OFF");
			case INTEGER:
				return prefix + option.levelLabels[getInteger(option)];
			case FLOAT:
				return prefix + (int)(getFloat(option) * 100.0F) + "%";
		}

		return "";
	}

	/** Parses "options.txt": each line is a "key:value" pair applied to the matching setting or key binding. */
	private void loadOptions() {
		try {
			if(this.optionsFile.exists()) {
				BufferedReader reader = new BufferedReader(new FileReader(this.optionsFile));

				while(true) {
					String line = reader.readLine();
					if(line == null) {
						reader.close();
						return;
					}

					String[] parts = line.split(":");
					GameOption option = GameOption.bySaveKey(parts[0]);
					if(option != null) {
						applyLoadedValue(option, parts[1]);
					} else {
						for(int i = 0; i < this.keyBindings.length; ++i) {
							if(parts[0].equals("key_" + this.keyBindings[i].keyDescription)) {
								this.keyBindings[i].keyCode = Integer.parseInt(parts[1]);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			System.out.println("Failed to load options");
			e.printStackTrace();
		}
	}

	/** Parses the string value read from options.txt into the option's field. */
	private void applyLoadedValue(GameOption option, String value) {
		switch(option.type) {
			case BOOLEAN:
				setValue(option, value.equals("true"));
				break;
			case INTEGER:
				setValue(option, Integer.parseInt(value));
				break;
			case FLOAT:
				setValue(option, clamp01(Float.parseFloat(value)));
				break;
		}
	}

	/** Writes every setting plus all key bindings back to "options.txt". */
	public final void saveOptions() {
		try {
			PrintWriter writer = new PrintWriter(new FileWriter(this.optionsFile));
			for(GameOption option : GameOption.values()) {
				writer.println(option.saveKey + ":" + optionValueToString(option));
			}

			for(int i = 0; i < this.keyBindings.length; ++i) {
				writer.println("key_" + this.keyBindings[i].keyDescription + ":" + this.keyBindings[i].keyCode);
			}

			writer.close();
		} catch (Exception e) {
			System.out.println("Failed to save options");
			e.printStackTrace();
		}
	}

	/** Serialises the option's current value to its text form for options.txt. */
	private String optionValueToString(GameOption option) {
		switch(option.type) {
			case BOOLEAN:
				return Boolean.toString(getBoolean(option));
			case INTEGER:
				return Integer.toString(getInteger(option));
			case FLOAT:
				return Float.toString(getFloat(option));
		}

		return "";
	}
}