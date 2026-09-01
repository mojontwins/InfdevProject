package com.mojang.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/** NBT tag holding a single signed byte (type id = 1). */
public final class NBTTagByte extends NBTBase {
    public byte byteValue;

    public NBTTagByte() {
    }

    public NBTTagByte(byte value) {
        this.byteValue = value;
    }

    @Override
    final void writeTagContents(DataOutput output) throws IOException {
        output.writeByte(this.byteValue);
    }

    @Override
    final void readTagContents(DataInput input) throws IOException {
        this.byteValue = input.readByte();
    }

    @Override
    public final byte getType() {
        return (byte) 1;
    }

    @Override
    public final String toString() {
        return String.valueOf(this.byteValue);
    }

    @Override
    public final NBTBase copy() {
        return new NBTTagByte(this.byteValue);
    }
}
