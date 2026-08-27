package com.mojang.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public final class NBTTagByteArray extends NBTBase {
	public byte[] byteArray;

	public NBTTagByteArray() {
	}

	public NBTTagByteArray(byte[] values) {
		this.byteArray = values;
	}

	final void writeTagContents(DataOutput output) throws IOException {
		output.writeInt(this.byteArray.length);
		output.write(this.byteArray);
	}

	final void readTagContents(DataInput input) throws IOException {
		int length = input.readInt();
		this.byteArray = new byte[length];
		input.readFully(this.byteArray);
	}

	public final byte getType() {
		return (byte)7;
	}

	public final String toString() {
		return "[" + this.byteArray.length + " bytes]";
	}
}