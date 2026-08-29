package net.minecraft.client.render;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBVertexBufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

public final class Tessellator {
	/** Initial vertex buffer capacity (in ints) — matches the original 8 MB backing store. */
	private static final int INITIAL_BUFFER_CAPACITY = 1 << 21;
	/** Each vertex is 8 ints: 3 position + 2 texture + 1 color + 2 stride padding = 32 bytes. */
	private static final int INTS_PER_VERTEX = 8;
	private static final int BYTES_PER_INT = 4;
	private static final int BYTES_PER_VERTEX = INTS_PER_VERTEX * BYTES_PER_INT;
	/** Byte offsets of the texture and color slots within a 32-byte vertex. */
	private static final int UV_OFFSET = 12;
	private static final int COLOR_OFFSET = 20;

	private ByteBuffer byteBuffer = BufferUtils.createByteBuffer(INITIAL_BUFFER_CAPACITY * BYTES_PER_INT);
	private int[] rawBuffer = new int[INITIAL_BUFFER_CAPACITY];
	private int vertexCount = 0;
	private double textureU;
	private double textureV;
	private int color;
	private boolean hasColor = false;
	private boolean hasTexture = false;
	private int rawBufferIndex = 0;
	private boolean isColorDisabled = false;
	private int drawMode;
	private double xOffset;
	private double yOffset;
	private double zOffset;
	public static Tessellator instance = new Tessellator();
	private boolean isDrawing = false;
	private boolean useVBO = false;
	private IntBuffer vertexBuffers;
	private int vboIndex = 0;
	private int vboCount = 10;

	private Tessellator() {
	}

	public final boolean getUseVBO() {
		return this.useVBO;
	}

	public final void setUseVBO(boolean useVBO) {
		if(useVBO && this.vertexBuffers == null) {
			this.vertexBuffers = BufferUtils.createIntBuffer(this.vboCount);
			ARBVertexBufferObject.glGenBuffersARB(this.vertexBuffers);
		}

		this.useVBO = useVBO;
	}

	/** Grows the vertex buffers so an arbitrarily large mesh is never truncated mid-batch. */
	private void ensureCapacity(int required) {
		if(this.rawBufferIndex + required > this.rawBuffer.length) {
			int newCapacity = this.rawBuffer.length;
			while(newCapacity < this.rawBufferIndex + required) {
				newCapacity <<= 1;
			}

			this.rawBuffer = Arrays.copyOf(this.rawBuffer, newCapacity);
			this.byteBuffer = BufferUtils.createByteBuffer(newCapacity * BYTES_PER_INT);
		}

	}

	public final void draw() {
		if(!this.isDrawing) {
			throw new IllegalStateException("Not tesselating!");
		} else {
			this.isDrawing = false;
			if(this.vertexCount > 0) {
				IntBuffer intBuffer = this.byteBuffer.asIntBuffer();
				FloatBuffer floatBuffer = this.byteBuffer.asFloatBuffer();
				intBuffer.clear();
				intBuffer.put(this.rawBuffer, 0, this.rawBufferIndex);
				this.byteBuffer.position(0);
				this.byteBuffer.limit(this.rawBufferIndex * BYTES_PER_INT);
				if(this.useVBO) {
					this.vboIndex = (this.vboIndex + 1) % this.vboCount;
					ARBVertexBufferObject.glBindBufferARB(GL15.GL_ARRAY_BUFFER, this.vertexBuffers.get(this.vboIndex));
					ARBVertexBufferObject.glBufferDataARB(GL15.GL_ARRAY_BUFFER, this.byteBuffer, GL15.GL_STREAM_DRAW);
				}

				if(this.hasTexture) {
					if(this.useVBO) {
						GL11.glTexCoordPointer(2, GL11.GL_FLOAT, BYTES_PER_VERTEX, UV_OFFSET);
					} else {
						floatBuffer.position(UV_OFFSET / BYTES_PER_INT);
						GL11.glTexCoordPointer(2, BYTES_PER_VERTEX, (FloatBuffer)floatBuffer);
					}

					GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
				}

				if(this.hasColor) {
					if(this.useVBO) {
						GL11.glColorPointer(4, GL11.GL_UNSIGNED_BYTE, BYTES_PER_VERTEX, COLOR_OFFSET);
					} else {
						this.byteBuffer.position(COLOR_OFFSET);
						GL11.glColorPointer(4, true, BYTES_PER_VERTEX, this.byteBuffer);
					}

					GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
				}

				if(this.useVBO) {
					GL11.glVertexPointer(3, GL11.GL_FLOAT, BYTES_PER_VERTEX, 0L);
				} else {
					floatBuffer.position(0);
					GL11.glVertexPointer(3, BYTES_PER_VERTEX, (FloatBuffer)floatBuffer);
				}

				GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
				GL11.glDrawArrays(this.drawMode, GL11.GL_POINTS, this.vertexCount);
				GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
				if(this.hasTexture) {
					GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
				}

				if(this.hasColor) {
					GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
				}
			}

			this.reset();
		}
	}

	private void reset() {
		this.vertexCount = 0;
		this.byteBuffer.clear();
		this.rawBufferIndex = 0;
	}

	public final void startDrawingQuads() {
		this.startDrawing(7);
	}

	public final void startDrawing(int drawMode) {
		if(this.isDrawing) {
			throw new IllegalStateException("Already tesselating!");
		} else {
			this.isDrawing = true;
			this.reset();
			this.drawMode = drawMode;
			this.hasColor = false;
			this.hasTexture = false;
			this.isColorDisabled = false;
		}
	}

	public final void setColorOpaque_F(float red, float green, float blue) {
		this.setColorOpaque((int)(red * 255.0F), (int)(green * 255.0F), (int)(blue * 255.0F));
	}

	public final void setColorRGBA_F(float red, float green, float blue, float alpha) {
		this.setColorRGBA((int)(red * 255.0F), (int)(green * 255.0F), (int)(blue * 255.0F), (int)(alpha * 255.0F));
	}

	private void setColorOpaque(int red, int green, int blue) {
		this.setColorRGBA(red, green, blue, 255);
	}

	private void setColorRGBA(int red, int green, int blue, int alpha) {
		if(!this.isColorDisabled) {
			if(red > 255) {
				red = 255;
			}

			if(green > 255) {
				green = 255;
			}

			if(blue > 255) {
				blue = 255;
			}

			if(alpha > 255) {
				alpha = 255;
			}

			if(red < 0) {
				red = 0;
			}

			if(green < 0) {
				green = 0;
			}

			if(blue < 0) {
				blue = 0;
			}

			if(alpha < 0) {
				alpha = 0;
			}

			this.hasColor = true;
			this.color = alpha << 24 | blue << 16 | green << 8 | red;
		}
	}

	public final void addVertexWithUV(double x, double y, double z, double u, double v) {
		this.hasTexture = true;
		this.textureU = u;
		this.textureV = v;
		this.addVertex(x, y, z);
	}

	public final void addVertex(double x, double y, double z) {
		this.ensureCapacity(INTS_PER_VERTEX);
		if(this.hasTexture) {
			this.rawBuffer[this.rawBufferIndex + 3] = Float.floatToRawIntBits((float)this.textureU);
			this.rawBuffer[this.rawBufferIndex + 4] = Float.floatToRawIntBits((float)this.textureV);
		}

		if(this.hasColor) {
			this.rawBuffer[this.rawBufferIndex + 5] = this.color;
		}

		this.rawBuffer[this.rawBufferIndex] = Float.floatToRawIntBits((float)(x + this.xOffset));
		this.rawBuffer[this.rawBufferIndex + 1] = Float.floatToRawIntBits((float)(y + this.yOffset));
		this.rawBuffer[this.rawBufferIndex + 2] = Float.floatToRawIntBits((float)(z + this.zOffset));
		this.rawBufferIndex += INTS_PER_VERTEX;
		++this.vertexCount;
	}

	public final void setColorOpaque_I(int colorValue) {
		int red = colorValue >> 16 & 255;
		int green = colorValue >> 8 & 255;
		int blue = colorValue & 255;
		this.setColorOpaque(red, green, blue);
	}

	public final void disableColor() {
		this.isColorDisabled = true;
	}

	public static void setNormal(float x, float y, float z) {
		GL11.glNormal3f(x, y, z);
	}

	public final void setTranslationD(double x, double y, double z) {
		this.xOffset = x;
		this.yOffset = y;
		this.zOffset = z;
	}
}