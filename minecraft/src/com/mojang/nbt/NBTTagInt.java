package com.mojang.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public final class NBTTagInt extends NBTBase {
	public int intValue;

	public NBTTagInt() {
	}

	public NBTTagInt(int value) {
		this.intValue = value;
	}

	final void writeTagContents(DataOutput output) throws IOException {
		output.writeInt(this.intValue);
	}

	final void readTagContents(DataInput input) throws IOException {
		this.intValue = input.readInt();
	}

	public final byte getType() {
		return (byte)3;
	}

	public final String toString() {
		return String.valueOf(this.intValue);
	}
}