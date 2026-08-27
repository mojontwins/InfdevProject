package com.mojang.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public final class NBTTagShort extends NBTBase {
	public short shortValue;

	public NBTTagShort() {
	}

	public NBTTagShort(short value) {
		this.shortValue = value;
	}

	final void writeTagContents(DataOutput output) throws IOException {
		output.writeShort(this.shortValue);
	}

	final void readTagContents(DataInput input) throws IOException {
		this.shortValue = input.readShort();
	}

	public final byte getType() {
		return (byte)2;
	}

	public final String toString() {
		return String.valueOf(this.shortValue);
	}
}