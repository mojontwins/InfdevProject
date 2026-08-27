package com.mojang.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public final class NBTTagLong extends NBTBase {
	public long longValue;

	public NBTTagLong() {
	}

	public NBTTagLong(long value) {
		this.longValue = value;
	}

	final void writeTagContents(DataOutput output) throws IOException {
		output.writeLong(this.longValue);
	}

	final void readTagContents(DataInput input) throws IOException {
		this.longValue = input.readLong();
	}

	public final byte getType() {
		return (byte)4;
	}

	public final String toString() {
		return String.valueOf(this.longValue);
	}
}