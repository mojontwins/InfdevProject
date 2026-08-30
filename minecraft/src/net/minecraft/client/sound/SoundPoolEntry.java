package net.minecraft.client.sound;

import java.net.URL;

public final class SoundPoolEntry {
	// One registered sound: its display name and the URL it is loaded from.
	public String soundName;
	public URL soundUrl;

	public SoundPoolEntry(String soundName, URL soundUrl) {
		this.soundName = soundName;
		this.soundUrl = soundUrl;
	}
}
