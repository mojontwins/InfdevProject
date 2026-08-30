package net.minecraft.client;

/**
 * A single line of chat text together with the in-game counter at which it
 * was added (used to fade old messages out of the chat history).
 */
public final class ChatLine {
	public String message;
	public int updateCounter;

	public ChatLine(String message) {
		this.message = message;
		this.updateCounter = 0;
	}
}
