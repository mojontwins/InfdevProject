package net.minecraft.client.render;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import net.minecraft.client.GameSettings;
import net.minecraft.client.render.texture.TextureFX;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public class RenderEngine {
	private HashMap<String, Integer> textureMap = new HashMap<>();
	private HashMap<Integer, BufferedImage> textureNameToImageMap = new HashMap<>();
	private IntBuffer singleIntBuffer = BufferUtils.createIntBuffer(1);
	private ByteBuffer imageData = BufferUtils.createByteBuffer(262144);
	private List<TextureFX> textureList = new ArrayList<>();
	private Map<String, ThreadDownloadImageData> urlToImageDataMap = new HashMap<>();
	private GameSettings options;
	private boolean clampTexture = false;

	public RenderEngine(GameSettings var1) {
		this.options = var1;
	}

	public final int getTexture(String var1) {
		Integer var2 = this.textureMap.get(var1);
		if(var2 != null) {
			return var2.intValue();
		} else {
			try {
				this.singleIntBuffer.clear();
				GL11.glGenTextures(this.singleIntBuffer);
				int var4 = this.singleIntBuffer.get(0);
				if(var1.startsWith("##")) {
					this.setupTexture(unwrapImageByColumns(ImageIO.read(RenderEngine.class.getResourceAsStream(var1.substring(2)))), var4);
				} else if(var1.startsWith("%%")) {
					this.clampTexture = true;
					this.setupTexture(ImageIO.read(RenderEngine.class.getResourceAsStream(var1.substring(2))), var4);
					this.clampTexture = false;
				} else {
					this.setupTexture(ImageIO.read(RenderEngine.class.getResourceAsStream(var1)), var4);
				}

				this.textureMap.put(var1, Integer.valueOf(var4));
				return var4;
			} catch (IOException var3) {
				throw new RuntimeException("!!");
			}
		}
	}

	private static BufferedImage unwrapImageByColumns(BufferedImage var0) {
		int var1 = var0.getWidth() / 16;
		BufferedImage var2 = new BufferedImage(16, var0.getHeight() * var1, 2);
		Graphics var3 = var2.getGraphics();

		for(int var4 = 0; var4 < var1; ++var4) {
			var3.drawImage(var0, -var4 << 4, var4 * var0.getHeight(), (ImageObserver)null);
		}

		var3.dispose();
		return var2;
	}

	private void setupTexture(BufferedImage var1, int var2) {
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, var2);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		if(this.clampTexture) {
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
		} else {
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
		}

		var2 = var1.getWidth();
		int var3 = var1.getHeight();
		int[] var4 = new int[var2 * var3];
		byte[] var5 = new byte[var2 * var3 << 2];
		var1.getRGB(0, 0, var2, var3, var4, 0, var2);

		for(int var11 = 0; var11 < var4.length; ++var11) {
			int var6 = var4[var11] >>> 24;
			int var7 = var4[var11] >> 16 & 255;
			int var8 = var4[var11] >> 8 & 255;
			int var9 = var4[var11] & 255;
			if(this.options != null && this.options.anaglyph) {
				int var10 = (var7 * 30 + var8 * 59 + var9 * 11) / 100;
				var8 = (var7 * 30 + var8 * 70) / 100;
				var9 = (var7 * 30 + var9 * 70) / 100;
				var7 = var10;
			}

			var5[var11 << 2] = (byte)var7;
			var5[(var11 << 2) + 1] = (byte)var8;
			var5[(var11 << 2) + 2] = (byte)var9;
			var5[(var11 << 2) + 3] = (byte)var6;
		}

		this.imageData.clear();
		this.imageData.put(var5);
		this.imageData.position(0).limit(var5.length);
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, var2, var3, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, this.imageData);
	}

	public final int getTextureForDownloadableImage(String var1, String var2) {
		ThreadDownloadImageData var6 = this.urlToImageDataMap.get(var1);
		if(var6 != null && var6.image != null && !var6.textureSetupComplete) {
			if(var6.textureIntDownload < 0) {
				BufferedImage var4 = var6.image;
				this.singleIntBuffer.clear();
				GL11.glGenTextures(this.singleIntBuffer);
				int var5 = this.singleIntBuffer.get(0);
				this.setupTexture(var4, var5);
				this.textureNameToImageMap.put(Integer.valueOf(var5), var4);
				var6.textureIntDownload = var5;
			} else {
				this.setupTexture(var6.image, var6.textureIntDownload);
			}

			var6.textureSetupComplete = true;
		}

		return var6 != null && var6.textureIntDownload >= 0 ? var6.textureIntDownload : this.getTexture(var2);
	}

	public final ThreadDownloadImageData obtainImageData(String var1, ImageBufferDownload var2) {
		ThreadDownloadImageData var3 = this.urlToImageDataMap.get(var1);
		if(var3 == null) {
			this.urlToImageDataMap.put(var1, new ThreadDownloadImageData(var1, var2));
		} else {
			++var3.referenceCount;
		}

		return var3;
	}

	public final void releaseImageData(String var1) {
		ThreadDownloadImageData var2 = this.urlToImageDataMap.get(var1);
		if(var2 != null) {
			--var2.referenceCount;
			if(var2.referenceCount == 0) {
				if(var2.textureIntDownload >= 0) {
					int var3 = var2.textureIntDownload;
					this.textureNameToImageMap.remove(Integer.valueOf(var3));
					this.singleIntBuffer.clear();
					this.singleIntBuffer.put(var3);
					this.singleIntBuffer.flip();
					GL11.glDeleteTextures(this.singleIntBuffer);
				}

				this.urlToImageDataMap.remove(var1);
			}
		}

	}

	public final void registerTextureFX(TextureFX var1) {
		this.textureList.add(var1);
		var1.onTick();
	}

	public final void updateDynamicTextures() {
		int var1;
		for(var1 = 0; var1 < this.textureList.size(); ++var1) {
			TextureFX var2 = this.textureList.get(var1);
			var2.anaglyphEnabled = this.options.anaglyph;
			var2.onTick();
			this.imageData.clear();
			this.imageData.put(var2.imageData);
			this.imageData.position(0).limit(var2.imageData.length);
			GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, var2.iconIndex % 16 << 4, var2.iconIndex / 16 << 4, 16, 16, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, this.imageData);
		}

		for(var1 = 0; var1 < this.textureList.size(); ++var1) {
			this.textureList.get(var1);
		}

	}

	public final void refreshTextures() {
		for(Integer var1 : this.textureNameToImageMap.keySet()) {
			this.setupTexture(this.textureNameToImageMap.get(var1), var1);
		}

		for(ThreadDownloadImageData var1 : this.urlToImageDataMap.values()) {
			var1.textureSetupComplete = false;
		}

		for(String var1 : this.textureMap.keySet()) {
			try {
				BufferedImage var2;
				if(var1.startsWith("##")) {
					var2 = unwrapImageByColumns(ImageIO.read(RenderEngine.class.getResourceAsStream(var1.substring(2))));
				} else if(var1.startsWith("%%")) {
					this.clampTexture = true;
					var2 = ImageIO.read(RenderEngine.class.getResourceAsStream(var1.substring(2)));
					this.clampTexture = false;
				} else {
					var2 = ImageIO.read(RenderEngine.class.getResourceAsStream(var1));
				}

				this.setupTexture(var2, this.textureMap.get(var1));
			} catch (IOException var3) {
				var3.printStackTrace();
			}
		}

	}

	public static void bindTexture(int var0) {
		if(var0 >= 0) {
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, var0);
		}
	}
}
