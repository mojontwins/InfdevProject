package net.minecraft.client.render;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.opengl.GL11;

/**
 * Thin wrapper around the LWJGL {@code glGenLists} / {@code glGenTextures}
 * calls that tracks every allocation in static lists so the matching
 * {@code glDeleteLists} / {@code glDeleteTextures} calls can be issued at
 * teardown (or the model display lists freed en masse when a model is
 * reloaded). Ported from r1.2.5 — the only LWJGL-side helper class that
 * sits between model rendering and raw OpenGL calls.
 */
public class GLAllocation {
	private static List<Integer> displayLists = new ArrayList<Integer>();
	private static List<Integer> textureNames = new ArrayList<Integer>();

	public static synchronized int generateDisplayLists(int count) {
		int id = GL11.glGenLists(count);
		displayLists.add(id);
		displayLists.add(count);
		return id;
	}

	public static synchronized void generateTextureNames(IntBuffer buffer) {
		GL11.glGenTextures(buffer);

		for(int i = buffer.position(); i < buffer.limit(); ++i) {
			textureNames.add(buffer.get(i));
		}
	}

	public static synchronized void deleteDisplayLists(int listId) {
		int index = displayLists.indexOf(listId);
		GL11.glDeleteLists(((Integer)displayLists.get(index)).intValue(), ((Integer)displayLists.get(index + 1)).intValue());
		displayLists.remove(index);
		displayLists.remove(index);
	}

	public static synchronized void deleteTexturesAndDisplayLists() {
		for(int i = 0; i < displayLists.size(); i += 2) {
			GL11.glDeleteLists(((Integer)displayLists.get(i)).intValue(), ((Integer)displayLists.get(i + 1)).intValue());
		}

		IntBuffer textureBuffer = createDirectIntBuffer(textureNames.size());
		textureBuffer.flip();
		GL11.glDeleteTextures(textureBuffer);

		for(int i = 0; i < textureNames.size(); ++i) {
			textureBuffer.put(((Integer)textureNames.get(i)).intValue());
		}

		textureBuffer.flip();
		GL11.glDeleteTextures(textureBuffer);
		displayLists.clear();
		textureNames.clear();
	}

	public static synchronized ByteBuffer createDirectByteBuffer(int size) {
		return ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
	}

	public static IntBuffer createDirectIntBuffer(int size) {
		return createDirectByteBuffer(size << 2).asIntBuffer();
	}

	public static FloatBuffer createDirectFloatBuffer(int size) {
		return createDirectByteBuffer(size << 2).asFloatBuffer();
	}
}
