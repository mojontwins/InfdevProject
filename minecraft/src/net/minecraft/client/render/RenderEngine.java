package net.minecraft.client.render;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import net.minecraft.client.GameSettings;
import net.minecraft.client.render.texture.TextureFX;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public class RenderEngine {
	private Map<String, Integer> textureMap = new HashMap<>();
	private Map<Integer, BufferedImage> textureNameToImageMap = new HashMap<>();
	private IntBuffer singleIntBuffer = BufferUtils.createIntBuffer(1);
	private ByteBuffer imageData = BufferUtils.createByteBuffer(262144);
	private List<TextureFX> textureList = new ArrayList<>();
	private Map<String, ThreadDownloadImageData> urlToImageDataMap = new HashMap<>();
	private GameSettings options;
	private boolean clampTexture = false;

	public RenderEngine(GameSettings options) {
		this.options = options;
	}

	public final int getTexture(String textureName) {
		Integer cachedId = this.textureMap.get(textureName);
		if(cachedId != null) {
			return cachedId.intValue();
		} else {
			try {
				this.singleIntBuffer.clear();
				GL11.glGenTextures(this.singleIntBuffer);
				int textureId = this.singleIntBuffer.get(0);
				if(textureName.startsWith("##")) {
					this.setupTexture(unwrapImageByColumns(ImageIO.read(RenderEngine.class.getResourceAsStream(textureName.substring(2)))), textureId);
				} else if(textureName.startsWith("%%")) {
					this.clampTexture = true;
					this.setupTexture(ImageIO.read(RenderEngine.class.getResourceAsStream(textureName.substring(2))), textureId);
					this.clampTexture = false;
				} else {
					this.setupTexture(ImageIO.read(RenderEngine.class.getResourceAsStream(textureName)), textureId);
				}

				this.textureMap.put(textureName, Integer.valueOf(textureId));
				return textureId;
			} catch (IOException exception) {
				throw new RuntimeException("!!");
			}
		}
	}

	private static BufferedImage unwrapImageByColumns(BufferedImage sourceImage) {
		int columnCount = sourceImage.getWidth() / 16;
		BufferedImage result = new BufferedImage(16, sourceImage.getHeight() * columnCount, 2);
		Graphics graphics = result.getGraphics();

		for(int column = 0; column < columnCount; ++column) {
			graphics.drawImage(sourceImage, -column << 4, column * sourceImage.getHeight(), (ImageObserver)null);
		}

		graphics.dispose();
		return result;
	}

	private void setupTexture(BufferedImage image, int textureId) {
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		if(this.clampTexture) {
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
		} else {
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
		}

		int width = image.getWidth();
		int height = image.getHeight();
		int[] pixelData = new int[width * height];
		byte[] pixelBytes = new byte[width * height << 2];
		image.getRGB(0, 0, width, height, pixelData, 0, width);

		for(int pixelIndex = 0; pixelIndex < pixelData.length; ++pixelIndex) {
			int alpha = pixelData[pixelIndex] >>> 24;
			int red = pixelData[pixelIndex] >> 16 & 255;
			int green = pixelData[pixelIndex] >> 8 & 255;
			int blue = pixelData[pixelIndex] & 255;
			if(this.options != null && this.options.anaglyph) {
				int anaglyphRed = (red * 30 + green * 59 + blue * 11) / 100;
				green = (red * 30 + green * 70) / 100;
				blue = (red * 30 + blue * 70) / 100;
				red = anaglyphRed;
			}

			pixelBytes[pixelIndex << 2] = (byte)red;
			pixelBytes[(pixelIndex << 2) + 1] = (byte)green;
			pixelBytes[(pixelIndex << 2) + 2] = (byte)blue;
			pixelBytes[(pixelIndex << 2) + 3] = (byte)alpha;
		}

		this.imageData.clear();
		this.imageData.put(pixelBytes);
		this.imageData.position(0).limit(pixelBytes.length);
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer)this.imageData);
	}

	public final int getTextureForDownloadableImage(String imageName, String fallbackName) {
		ThreadDownloadImageData imageData = this.urlToImageDataMap.get(imageName);
		if(imageData != null && imageData.image != null && !imageData.textureSetupComplete) {
			if(imageData.textureIntDownload < 0) {
				BufferedImage image = imageData.image;
				this.singleIntBuffer.clear();
				GL11.glGenTextures(this.singleIntBuffer);
				int textureId = this.singleIntBuffer.get(0);
				this.setupTexture(image, textureId);
				this.textureNameToImageMap.put(Integer.valueOf(textureId), image);
				imageData.textureIntDownload = textureId;
			} else {
				this.setupTexture(imageData.image, imageData.textureIntDownload);
			}

			imageData.textureSetupComplete = true;
		}

		return imageData != null && imageData.textureIntDownload >= 0 ? imageData.textureIntDownload : this.getTexture(fallbackName);
	}

	public final ThreadDownloadImageData obtainImageData(String imageName, ImageBufferDownload buffer) {
		ThreadDownloadImageData imageData = this.urlToImageDataMap.get(imageName);
		if(imageData == null) {
			this.urlToImageDataMap.put(imageName, new ThreadDownloadImageData(imageName, buffer));
		} else {
			++imageData.referenceCount;
		}

		return imageData;
	}

	public final void releaseImageData(String imageName) {
		ThreadDownloadImageData imageData = this.urlToImageDataMap.get(imageName);
		if(imageData != null) {
			--imageData.referenceCount;
			if(imageData.referenceCount == 0) {
				if(imageData.textureIntDownload >= 0) {
					int textureId = imageData.textureIntDownload;
					this.textureNameToImageMap.remove(Integer.valueOf(textureId));
					this.singleIntBuffer.clear();
					this.singleIntBuffer.put(textureId);
					this.singleIntBuffer.flip();
					GL11.glDeleteTextures(this.singleIntBuffer);
				}

				this.urlToImageDataMap.remove(imageName);
			}
		}

	}

	public final void registerTextureFX(TextureFX textureFX) {
		this.textureList.add(textureFX);
		textureFX.onTick();
	}

	public final void updateDynamicTextures() {
		for(int index = 0; index < this.textureList.size(); ++index) {
			TextureFX textureFX = this.textureList.get(index);
			textureFX.anaglyphEnabled = this.options.anaglyph;
			textureFX.onTick();
			this.imageData.clear();
			this.imageData.put(textureFX.imageData);
			this.imageData.position(0).limit(textureFX.imageData.length);
			GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, textureFX.iconIndex % 16 << 4, textureFX.iconIndex / 16 << 4, 16, 16, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, this.imageData);
		}

		for(int index = 0; index < this.textureList.size(); ++index) {
			this.textureList.get(index);
		}

	}

	public final void refreshTextures() {
		Iterator<Integer> textureIdIterator = this.textureNameToImageMap.keySet().iterator();

		while(textureIdIterator.hasNext()) {
			int textureId = textureIdIterator.next().intValue();
			BufferedImage image = this.textureNameToImageMap.get(Integer.valueOf(textureId));
			this.setupTexture(image, textureId);
		}

		Iterator<ThreadDownloadImageData> imageDataIterator = this.urlToImageDataMap.values().iterator();

		while(imageDataIterator.hasNext()) {
			ThreadDownloadImageData imageData = imageDataIterator.next();
			imageData.textureSetupComplete = false;
		}

		Iterator<String> textureNameIterator = this.textureMap.keySet().iterator();

		while(textureNameIterator.hasNext()) {
			String textureName = textureNameIterator.next();

			try {
				BufferedImage image;
				if(textureName.startsWith("##")) {
					image = unwrapImageByColumns(ImageIO.read(RenderEngine.class.getResourceAsStream(textureName.substring(2))));
				} else if(textureName.startsWith("%%")) {
					this.clampTexture = true;
					image = ImageIO.read(RenderEngine.class.getResourceAsStream(textureName.substring(2)));
					this.clampTexture = false;
				} else {
					image = ImageIO.read(RenderEngine.class.getResourceAsStream(textureName));
				}

				int textureId = this.textureMap.get(textureName).intValue();
				this.setupTexture(image, textureId);
			} catch (IOException exception) {
				exception.printStackTrace();
			}
		}

	}

	public static void bindTexture(int textureId) {
		if(textureId >= 0) {
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
		}
	}
}