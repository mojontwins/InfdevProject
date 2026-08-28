package net.minecraft.client.render.texture;

import net.minecraft.game.world.block.Block;

public final class TextureWaterFlowFX extends TextureFX {
	private float[] red = new float[256];
	private float[] green = new float[256];
	private float[] blue = new float[256];
	private float[] alpha = new float[256];

	public TextureWaterFlowFX() {
		super(Block.waterMoving.blockIndexInTexture + 32);
	}

	public final void onTick() {
		for(int x = 0; x < 16; ++x) {
			for(int y = 0; y < 16; ++y) {
				float accumulator = 0.0F;
				for(int neighborY = y - 2; neighborY <= y; ++neighborY) {
					int sampleX = x & 15;
					int sampleY = neighborY & 15;
					accumulator += this.red[sampleX + (sampleY << 4)];
				}

				this.green[x + (y << 4)] = accumulator / 3.2F + this.blue[x + (y << 4)] * 0.8F;
			}
		}

		for(int x = 0; x < 16; ++x) {
			for(int y = 0; y < 16; ++y) {
				this.blue[x + (y << 4)] += this.alpha[x + (y << 4)] * 0.05F;
				if(this.blue[x + (y << 4)] < 0.0F) {
					this.blue[x + (y << 4)] = 0.0F;
				}

				this.alpha[x + (y << 4)] -= 0.3F;
				if(Math.random() < 0.2D) {
					this.alpha[x + (y << 4)] = 0.5F;
				}
			}
		}

		float[] swap = this.green;
		this.green = this.red;
		this.red = swap;

		for(int pixel = 0; pixel < 256; ++pixel) {
			float value = this.red[pixel];
			if(value > 1.0F) {
				value = 1.0F;
			}

			if(value < 0.0F) {
				value = 0.0F;
			}

			float valueSquared = value * value;
			int redChannel = (int)(32.0F + valueSquared * 32.0F);
			int greenChannel = (int)(50.0F + valueSquared * 64.0F);
			int blueChannel = 255;
			int alphaChannel = (int)(146.0F + valueSquared * 50.0F);
			if(this.anaglyphEnabled) {
				int anaglyphRed = (redChannel * 30 + greenChannel * 59 + 2805) / 100;
				int anaglyphGreen = (redChannel * 30 + greenChannel * 70) / 100;
				int anaglyphBlue = (redChannel * 30 + 17850) / 100;
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