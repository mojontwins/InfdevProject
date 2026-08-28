package net.minecraft.client.render.texture;

import net.minecraft.game.world.block.Block;

public final class TextureFlamesFX extends TextureFX {
	private float[] currentFireFrame = new float[320];
	private float[] lastFireFrame = new float[320];

	public TextureFlamesFX(int fireIndex) {
		super(Block.fire.blockIndexInTexture + (fireIndex << 4));
	}

	public final void onTick() {
		for(int x = 0; x < 16; ++x) {
			for(int y = 0; y < 20; ++y) {
				int sampleCount = 18;
				float accumulator = this.currentFireFrame[x + ((y + 1) % 20 << 4)] * 18.0F;
				for(int neighborX = x - 1; neighborX <= x + 1; ++neighborX) {
					for(int neighborY = y; neighborY <= y + 1; ++neighborY) {
						if(neighborX >= 0 && neighborY >= 0 && neighborX < 16 && neighborY < 20) {
							accumulator += this.currentFireFrame[neighborX + (neighborY << 4)];
						}

						++sampleCount;
					}
				}

				this.lastFireFrame[x + (y << 4)] = accumulator / ((float)sampleCount * 1.06F);
				if(y >= 19) {
					this.lastFireFrame[x + (y << 4)] = (float)(Math.random() * Math.random() * Math.random() * 4.0D + Math.random() * (double)0.1F + (double)0.2F);
				}
			}
		}

		float[] swap = this.lastFireFrame;
		this.lastFireFrame = this.currentFireFrame;
		this.currentFireFrame = swap;

		for(int pixel = 0; pixel < 256; ++pixel) {
			float brightness = this.currentFireFrame[pixel] * 1.8F;
			if(brightness > 1.0F) {
				brightness = 1.0F;
			}

			if(brightness < 0.0F) {
				brightness = 0.0F;
			}

			int redChannel = (int)(brightness * 155.0F + 100.0F);
			int greenChannel = (int)(brightness * brightness * 255.0F);
			int blueChannel = (int)(brightness * brightness * brightness * brightness * brightness * brightness * brightness * brightness * brightness * brightness * 255.0F);
			short alphaChannel = 255;
			if(brightness < 0.5F) {
				alphaChannel = 0;
			}

			if(this.anaglyphEnabled) {
				int anaglyphRed = (redChannel * 30 + greenChannel * 59 + blueChannel * 11) / 100;
				int anaglyphGreen = (redChannel * 30 + greenChannel * 70) / 100;
				int anaglyphBlue = (redChannel * 30 + blueChannel * 70) / 100;
				redChannel = anaglyphRed;
				greenChannel = anaglyphGreen;
				blueChannel = anaglyphBlue;
			}

			this.imageData[pixel << 2] = (byte)redChannel;
			this.imageData[(pixel << 2) + 1] = (byte)greenChannel;
			this.imageData[(pixel << 2) + 2] = (byte)blueChannel;
			this.imageData[(pixel << 2) + 3] = (byte)alphaChannel;
		}
	}
}