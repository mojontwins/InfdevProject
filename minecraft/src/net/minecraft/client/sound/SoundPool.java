package net.minecraft.client.sound;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class SoundPool {
	// Groups loose sound files by a base name so each registered sound can be chosen at random.
	private Random rand = new Random();
	private Map<String, List<SoundPoolEntry>> nameToSoundPoolEntriesMapping = new HashMap<>();

	// Register a sound file; the base name (ignoring the trailing digit used for variants) is the grouping key.
	public final SoundPoolEntry addSound(String soundName, File file) {
		try {
			String fullName = soundName;

			// Strip the trailing digits that identify per-file variants (e.g. "step.grass1" -> "step.grass").
			for(soundName = soundName.substring(0, soundName.indexOf(".")); Character.isDigit(soundName.charAt(soundName.length() - 1)); soundName = soundName.substring(0, soundName.length() - 1)) {
			}

			soundName = soundName.replaceAll("/", ".");
			if(!this.nameToSoundPoolEntriesMapping.containsKey(soundName)) {
				this.nameToSoundPoolEntriesMapping.put(soundName, new ArrayList<>());
			}

			SoundPoolEntry entry = new SoundPoolEntry(fullName, file.toURI().toURL());
			this.nameToSoundPoolEntriesMapping.get(soundName).add(entry);
			return entry;
		} catch (MalformedURLException error) {
			error.printStackTrace();
			throw new RuntimeException(error);
		}
	}

	// Pick a random entry from the given sound's pool of variants.
	public final SoundPoolEntry getRandomSoundFromSoundPool(String soundName) {
		List<SoundPoolEntry> entries = this.nameToSoundPoolEntriesMapping.get(soundName);
		return entries == null ? null : entries.get(this.rand.nextInt(entries.size()));
	}
}
