package com.mojang.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class NBTTagCompound extends NBTBase {
	private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

	private Map<String, NBTBase> tagMap = new HashMap<>();

	final void writeTagContents(DataOutput output) throws IOException {
		for(NBTBase tag : this.tagMap.values()) {
			NBTBase.writeNamedTag(tag, output);
		}

		output.writeByte(0);
	}

	final void readTagContents(DataInput input) throws IOException {
		this.tagMap.clear();

		while(true) {
			NBTBase tag = NBTBase.readNamedTag(input);
			if(tag.getType() == 0) {
				return;
			}

			this.tagMap.put(tag.getKey(), tag);
		}
	}

	public final byte getType() {
		return (byte)10;
	}

	public final void setTag(String key, NBTBase tag) {
		this.tagMap.put(key, tag.setKey(key));
	}

	public final void setByte(String key, byte value) {
		this.tagMap.put(key, new NBTTagByte(value).setKey(key));
	}

	public final void setShort(String key, short value) {
		this.tagMap.put(key, new NBTTagShort(value).setKey(key));
	}

	public final void setInteger(String key, int value) {
		this.tagMap.put(key, new NBTTagInt(value).setKey(key));
	}

	public final void setLong(String key, long value) {
		this.tagMap.put(key, new NBTTagLong(value).setKey(key));
	}

	public final void setFloat(String key, float value) {
		this.tagMap.put(key, new NBTTagFloat(value).setKey(key));
	}

	public final void setString(String key, String value) {
		this.tagMap.put(key, new NBTTagString(value).setKey(key));
	}

	public final void setByteArray(String key, byte[] value) {
		this.tagMap.put(key, new NBTTagByteArray(value).setKey(key));
	}

	public final void setCompoundTag(String key, NBTTagCompound tag) {
		this.tagMap.put(key, tag.setKey(key));
	}

	public final void setBoolean(String key, boolean value) {
		this.setByte(key, (byte)(value ? 1 : 0));
	}

	public final boolean hasKey(String key) {
		return this.tagMap.containsKey(key);
	}

	public final byte getByte(String key) {
		NBTBase tag = this.tagMap.get(key);
		return tag instanceof NBTTagByte ? ((NBTTagByte)tag).byteValue : 0;
	}

	public final short getShort(String key) {
		NBTBase tag = this.tagMap.get(key);
		return tag instanceof NBTTagShort ? ((NBTTagShort)tag).shortValue : 0;
	}

	public final int getInteger(String key) {
		NBTBase tag = this.tagMap.get(key);
		return tag instanceof NBTTagInt ? ((NBTTagInt)tag).intValue : 0;
	}

	public final long getLong(String key) {
		NBTBase tag = this.tagMap.get(key);
		return tag instanceof NBTTagLong ? ((NBTTagLong)tag).longValue : 0L;
	}

	public final float getFloat(String key) {
		NBTBase tag = this.tagMap.get(key);
		return tag instanceof NBTTagFloat ? ((NBTTagFloat)tag).floatValue : 0.0F;
	}

	public final String getString(String key) {
		NBTBase tag = this.tagMap.get(key);
		return tag instanceof NBTTagString ? ((NBTTagString)tag).stringValue : "";
	}

	public final byte[] getByteArray(String key) {
		NBTBase tag = this.tagMap.get(key);
		return tag instanceof NBTTagByteArray ? ((NBTTagByteArray)tag).byteArray : EMPTY_BYTE_ARRAY;
	}

	public final NBTTagCompound getCompoundTag(String key) {
		NBTBase tag = this.tagMap.get(key);
		return tag instanceof NBTTagCompound ? (NBTTagCompound)tag : new NBTTagCompound();
	}

	public final NBTTagList getTagList(String key) {
		NBTBase tag = this.tagMap.get(key);
		return tag instanceof NBTTagList ? (NBTTagList)tag : new NBTTagList();
	}

	public final boolean getBoolean(String key) {
		return this.getByte(key) != 0;
	}

	public final String toString() {
		return this.tagMap.size() + " entries";
	}

	@Override
	public NBTBase copy() {
		NBTTagCompound duplicate = new NBTTagCompound();
		for(Map.Entry<String, NBTBase> entry : this.tagMap.entrySet()) {
			duplicate.setTag(entry.getKey(), entry.getValue().copy());
		}
		return duplicate;
	}
}