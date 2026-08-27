package com.mojang.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public abstract class NBTBase {
	private String key = null;

	abstract void writeTagContents(DataOutput output) throws IOException;

	abstract void readTagContents(DataInput input) throws IOException;

	public abstract byte getType();

	public final String getKey() {
		return this.key == null ? "" : this.key;
	}

	public final NBTBase setKey(String key) {
		this.key = key;
		return this;
	}

	private static final Map<Byte, Supplier<NBTBase>> TAG_FACTORIES = createTagFactories();

	private static Map<Byte, Supplier<NBTBase>> createTagFactories() {
		Map<Byte, Supplier<NBTBase>> factories = new HashMap<>();
		factories.put((byte)0, NBTTagEnd::new);
		factories.put((byte)1, NBTTagByte::new);
		factories.put((byte)2, NBTTagShort::new);
		factories.put((byte)3, NBTTagInt::new);
		factories.put((byte)4, NBTTagLong::new);
		factories.put((byte)5, NBTTagFloat::new);
		factories.put((byte)6, NBTTagDouble::new);
		factories.put((byte)7, NBTTagByteArray::new);
		factories.put((byte)8, NBTTagString::new);
		factories.put((byte)9, NBTTagList::new);
		factories.put((byte)10, NBTTagCompound::new);
		return factories;
	}

	public static NBTBase readNamedTag(DataInput input) throws IOException {
		byte type = input.readByte();
		if(type == 0) {
			return new NBTTagEnd();
		}
		NBTBase tag = createTagOfType(type);
		short keyLength = input.readShort();
		byte[] keyBytes = new byte[keyLength];
		input.readFully(keyBytes);
		tag.key = new String(keyBytes, StandardCharsets.UTF_8);
		tag.readTagContents(input);
		return tag;
	}

	public static void writeNamedTag(NBTBase tag, DataOutput output) throws IOException {
		byte type = tag.getType();
		output.writeByte(type);
		if(type != 0) {
			byte[] keyBytes = tag.getKey().getBytes(StandardCharsets.UTF_8);
			output.writeShort(keyBytes.length);
			output.write(keyBytes);
			tag.writeTagContents(output);
		}
	}

	public static NBTBase createTagOfType(byte type) {
		Supplier<NBTBase> factory = TAG_FACTORIES.get(type);
		return factory == null ? null : factory.get();
	}
}