package com.mojang.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class NBTTagList extends NBTBase {
	private List<NBTBase> tagList = new ArrayList<>();
	private byte tagType;

	final void writeTagContents(DataOutput output) throws IOException {
		if(this.tagList.size() > 0) {
			this.tagType = this.tagList.get(0).getType();
		} else {
			this.tagType = 1;
		}

		output.writeByte(this.tagType);
		output.writeInt(this.tagList.size());

		for(NBTBase tag : this.tagList) {
			tag.writeTagContents(output);
		}

	}

	final void readTagContents(DataInput input) throws IOException {
		this.tagType = input.readByte();
		int count = input.readInt();
		this.tagList = new ArrayList<>();

		for(int i = 0; i < count; i++) {
			NBTBase tag = NBTBase.createTagOfType(this.tagType);
			tag.readTagContents(input);
			this.tagList.add(tag);
		}

	}

	public final byte getType() {
		return (byte)9;
	}

	public final String toString() {
		return this.tagList.size() + " entries of type " + getTagName(this.tagType);
	}

	private static String getTagName(byte type) {
		switch(type) {
		case 0:
			return "TAG_End";
		case 1:
			return "TAG_Byte";
		case 2:
			return "TAG_Short";
		case 3:
			return "TAG_Int";
		case 4:
			return "TAG_Long";
		case 5:
			return "TAG_Float";
		case 6:
			return "TAG_Double";
		case 7:
			return "TAG_Byte_Array";
		case 8:
			return "TAG_String";
		case 9:
			return "TAG_List";
		case 10:
			return "TAG_Compound";
		default:
			return "UNKNOWN";
		}
	}

	public final void setTag(NBTBase tag) {
		this.tagType = tag.getType();
		this.tagList.add(tag);
	}

	public final NBTBase tagAt(int index) {
		if(index < 0 || index >= this.tagList.size()) {
			throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + this.tagList.size());
		}
		return this.tagList.get(index);
	}

	public final int tagCount() {
		return this.tagList.size();
	}

	@Override
	public final NBTBase copy() {
		NBTTagList duplicate = new NBTTagList();
		for(NBTBase child : this.tagList) {
			duplicate.setTag(child.copy());
		}
		return duplicate;
	}
}