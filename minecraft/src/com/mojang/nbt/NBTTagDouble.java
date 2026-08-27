package com.mojang.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public final class NBTTagDouble extends NBTBase {
	public double doubleValue;

	public NBTTagDouble() {
	}

	public NBTTagDouble(double value) {
		this.doubleValue = value;
	}

	final void writeTagContents(DataOutput output) throws IOException {
		output.writeDouble(this.doubleValue);
	}

	final void readTagContents(DataInput input) throws IOException {
		this.doubleValue = input.readDouble();
	}

	public final byte getType() {
		return (byte)6;
	}

	public final String toString() {
		return String.valueOf(this.doubleValue);
	}
}