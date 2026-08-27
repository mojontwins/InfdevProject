package com.mojang.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class NBTTagString extends NBTBase {
	public String stringValue;

	public NBTTagString() {
	}

	public NBTTagString(String value) {
		this.stringValue = value;
	}

	final void writeTagContents(DataOutput output) throws IOException {
		byte[] stringBytes = this.stringValue.getBytes(StandardCharsets.UTF_8);
		output.writeShort(stringBytes.length);
		output.write(stringBytes);
	}

	final void readTagContents(DataInput input) throws IOException {
		int length = input.readShort();
		byte[] stringBytes = new byte[length];
		input.readFully(stringBytes);
		this.stringValue = new String(stringBytes, StandardCharsets.UTF_8);
	}

	public final byte getType() {
		return (byte)8;
	}

	public final String toString() {
		return String.valueOf(this.stringValue);
	}
}