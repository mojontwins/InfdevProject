package com.mojang.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/** NBT tag holding a single short value (type id = 2). */
public final class NBTTagShort extends NBTBase {
    public short shortValue;

    public NBTTagShort() {
    }

    public NBTTagShort(short value) {
        this.shortValue = value;
    }

    @Override
    final void writeTagContents(DataOutput output) throws IOException {
        output.writeShort(this.shortValue);
    }

    @Override
    final void readTagContents(DataInput input) throws IOException {
        this.shortValue = input.readShort();
    }

    @Override
    public final byte getType() {
        return (byte) 2;
    }

    @Override
    public final String toString() {
        return String.valueOf(this.shortValue);
    }

    @Override
    public final NBTBase copy() {
        return new NBTTagShort(this.shortValue);
    }
}