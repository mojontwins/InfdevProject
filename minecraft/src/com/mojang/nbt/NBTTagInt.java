package com.mojang.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/** NBT tag holding a single int value (type id = 3). */
public final class NBTTagInt extends NBTBase {
    public int intValue;

    public NBTTagInt() {
    }

    public NBTTagInt(int value) {
        this.intValue = value;
    }

    @Override
    final void writeTagContents(DataOutput output) throws IOException {
        output.writeInt(this.intValue);
    }

    @Override
    final void readTagContents(DataInput input) throws IOException {
        this.intValue = input.readInt();
    }

    @Override
    public final byte getType() {
        return (byte) 3;
    }

    @Override
    public final String toString() {
        return String.valueOf(this.intValue);
    }

    @Override
    public final NBTBase copy() {
        return new NBTTagInt(this.intValue);
    }
}