package com.mojang.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/** NBT tag holding a single byte array (type id = 7). */
public final class NBTTagByteArray extends NBTBase {
    public byte[] byteArray;

    public NBTTagByteArray() {
    }

    public NBTTagByteArray(byte[] values) {
        this.byteArray = values;
    }

    @Override
    final void writeTagContents(DataOutput output) throws IOException {
        output.writeInt(this.byteArray.length);
        output.write(this.byteArray);
    }

    @Override
    final void readTagContents(DataInput input) throws IOException {
        int length = input.readInt();
        this.byteArray = new byte[length];
        input.readFully(this.byteArray);
    }

    @Override
    public final byte getType() {
        return (byte) 7;
    }

    @Override
    public final String toString() {
        return "[" + this.byteArray.length + " bytes]";
    }

    @Override
    public final NBTBase copy() {
        byte[] copy = new byte[this.byteArray.length];
        System.arraycopy(this.byteArray, 0, copy, 0, this.byteArray.length);
        return new NBTTagByteArray(copy);
    }
}