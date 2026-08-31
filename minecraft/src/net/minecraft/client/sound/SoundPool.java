package net.minecraft.client.sound;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public final class SoundPool {
	private Random rand = new Random();
	private final Map<String, List<SoundPoolEntry>> nameToSoundPoolEntriesMapping = new ConcurrentHashMap<>();

	public final SoundPoolEntry addSound(String soundName, File file) {
		String fullName = soundName;

		int dot = soundName.indexOf('.');
		if(dot >= 0) {
			soundName = soundName.substring(0, dot);
		}
		for(; !soundName.isEmpty() && Character.isDigit(soundName.charAt(soundName.length() - 1)); soundName = soundName.substring(0, soundName.length() - 1)) {
		}

		soundName = soundName.replaceAll("/", ".");
		String fileExt = "";
		String fileName = file.getName();
		int extDot = fileName.lastIndexOf('.');
		if(extDot >= 0 && extDot < fileName.length() - 1) {
			fileExt = fileName.substring(extDot);
		}
		String soundNameForEntry = soundName + fileExt;
		List<SoundPoolEntry> entries = this.nameToSoundPoolEntriesMapping.get(soundName);
		if(entries == null) {
			entries = new ArrayList<>();
			List<SoundPoolEntry> existing = this.nameToSoundPoolEntriesMapping.putIfAbsent(soundName, entries);
			if(existing != null) {
				entries = existing;
			}
		}

		try {
			SoundPoolEntry entry = new SoundPoolEntry(soundNameForEntry, file.toURI().toURL());
			synchronized(entries) {
				entries.add(entry);
			}
			return entry;
		} catch(MalformedURLException error) {
			error.printStackTrace();
			return null;
		}
	}

	public final SoundPoolEntry getRandomSoundFromSoundPool(String soundName) {
		List<SoundPoolEntry> entries = this.nameToSoundPoolEntriesMapping.get(soundName);
		if(entries == null) {
			return null;
		}
		synchronized(entries) {
			if(entries.isEmpty()) {
				return null;
			}
			return entries.get(this.rand.nextInt(entries.size()));
		}
	}

	/**
	 * Returns whether a sound name is registered (regardless of how many
	 * entries it has). Used by the resource download thread to detect
	 * which sounds are missing locally so it can attempt to download them.
	 */
	public final boolean hasSound(String soundName) {
		return this.nameToSoundPoolEntriesMapping.containsKey(soundName);
	}
}
