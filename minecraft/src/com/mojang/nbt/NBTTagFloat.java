package com.mojang.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/** NBT tag holding a single float value (type id = 5). */
public final class NBTTagFloat extends NBTBase {
    public float floatValue;

    public NBTTagFloat() {
    }

    public NBTTagFloat(float value) {
        this.floatValue = value;
    }

    @Override
    final void writeTagContents(DataOutput output) throws IOException {
        output.writeFloat(this.floatValue);
    }

    @Override
    final void readTagContents(DataInput input) throws IOException {
        this.floatValue = input.readFloat();
    }

    @Override
    public final byte getType() {
        return (byte) 5;
    }

    @Override
    public final String toString() {
        return String.valueOf(this.floatValue);
    }
}