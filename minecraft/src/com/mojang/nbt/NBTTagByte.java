package com.mojang.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public final class NBTTagByte extends NBTBase {
	public byte byteValue;

	public NBTTagByte() {
	}

	public NBTTagByte(byte value) {
		this.byteValue = value;
	}

	final void writeTagContents(DataOutput output) throws IOException {
		output.writeByte(this.byteValue);
	}

	final void readTagContents(DataInput input) throws IOException {
		this.byteValue = input.readByte();
	}

	public final byte getType() {
		return (byte)1;
	}

	public final String toString() {
		return String.valueOf(this.byteValue);
	}
}