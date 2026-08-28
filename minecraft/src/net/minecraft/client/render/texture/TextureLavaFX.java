package net.minecraft.client.render.texture;

import net.minecraft.game.world.block.Block;
import util.MathHelper;

public final class TextureLavaFX extends TextureFX {
	private float[] red = new float[256];
	private float[] green = new float[256];
	private float[] blue = new float[256];
	private float[] alpha = new float[256];

	public TextureLavaFX() {
		super(Block.lavaMoving.blockIndexInTexture);
	}

	public final void onTick() {
		for(int x = 0; x < 16; ++x) {
			for(int y = 0; y < 16; ++y) {
				float accumulator = 0.0F;
				int xOffset = (int)(MathHelper.sin((float)y * (float)Math.PI * 2.0F / 16.0F) * 1.2F);
				int yOffset = (int)(MathHelper.sin((float)x * (float)Math.PI * 2.0F / 16.0F) * 1.2F);

				for(int neighborX = x - 1; neighborX <= x + 1; ++neighborX) {
					for(int neighborY = y - 1; neighborY <= y + 1; ++neighborY) {
						int sampleX = neighborX + xOffset & 15;
						int sampleY = neighborY + yOffset & 15;
						accumulator += this.red[sampleX + (sampleY << 4)];
					}
				}

				this.green[x + (y << 4)] = accumulator / 10.0F + (this.blue[(x & 15) + ((y & 15) << 4)] + this.blue[(x + 1 & 15) + ((y & 15) << 4)] + this.blue[(x + 1 & 15) + ((y + 1 & 15) << 4)] + this.blue[(x & 15) + ((y + 1 & 15) << 4)]) / 4.0F * 0.8F;
				this.blue[x + (y << 4)] += this.alpha[x + (y << 4)] * 0.01F;
				if(this.blue[x + (y << 4)] < 0.0F) {
					this.blue[x + (y << 4)] = 0.0F;
				}

				this.alpha[x + (y << 4)] -= 0.06F;
				if(Math.random() < 0.005D) {
					this.alpha[x + (y << 4)] = 1.5F;
				}
			}
		}

		float[] swap = this.green;
		this.green = this.red;
		this.red = swap;

		for(int pixel = 0; pixel < 256; ++pixel) {
			float value = this.red[pixel] * 2.0F;
			if(value > 1.0F) {
				value = 1.0F;
			}

			if(value < 0.0F) {
				value = 0.0F;
			}

			int redChannel = (int)(value * 100.0F + 155.0F);
			int greenChannel = (int)(value * value * 255.0F);
			int blueChannel = (int)(value * value * value * value * 128.0F);
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
			this.imageData[(pixel << 2) + 3] = -1;
		}
	}
}