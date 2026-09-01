package com.mojang.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/** NBT tag holding a single long value (type id = 4). */
public final class NBTTagLong extends NBTBase {
    public long longValue;

    public NBTTagLong() {
    }

    public NBTTagLong(long value) {
        this.longValue = value;
    }

    @Override
    final void writeTagContents(DataOutput output) throws IOException {
        output.writeLong(this.longValue);
    }

    @Override
    final void readTagContents(DataInput input) throws IOException {
        this.longValue = input.readLong();
    }

    @Override
    public final byte getType() {
        return (byte) 4;
    }

    @Override
    public final String toString() {
        return String.valueOf(this.longValue);
    }
}