package com.mojang.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public final class NBTTagFloat extends NBTBase {
	public float floatValue;

	public NBTTagFloat() {
	}

	public NBTTagFloat(float value) {
		this.floatValue = value;
	}

	final void writeTagContents(DataOutput output) throws IOException {
		output.writeFloat(this.floatValue);
	}

	final void readTagContents(DataInput input) throws IOException {
		this.floatValue = input.readFloat();
	}

	public final byte getType() {
		return (byte)5;
	}

	public final String toString() {
		return String.valueOf(this.floatValue);
	}
}