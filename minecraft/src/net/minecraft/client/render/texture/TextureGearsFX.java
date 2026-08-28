package net.minecraft.client.render.texture;

import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import net.minecraft.game.world.block.Block;
import util.MathHelper;

public class TextureGearsFX extends TextureFX {
	private int gearRotation = 0;
	private int[] gearColor = new int[1024];
	private int[] gearMiddleColor = new int[1024];
	private int gearRotationDir;

	public TextureGearsFX(int gearIndex) {
		super(Block.cog.blockIndexInTexture + gearIndex);
		this.gearRotationDir = (gearIndex << 1) - 1;
		this.gearRotation = 2;

		try {
			BufferedImage image = ImageIO.read(TextureGearsFX.class.getResource("/misc/gear.png"));
			image.getRGB(0, 0, 32, 32, this.gearColor, 0, 32);
			image = ImageIO.read(TextureGearsFX.class.getResource("/misc/gearmiddle.png"));
			image.getRGB(0, 0, 16, 16, this.gearMiddleColor, 0, 16);
		} catch (IOException exception) {
			exception.printStackTrace();
		}
	}

	public final void onTick() {
		this.gearRotation = this.gearRotation + this.gearRotationDir & 63;
		float rotationSin = MathHelper.sin((float)this.gearRotation / 64.0F * (float)Math.PI * 2.0F);
		float rotationCos = MathHelper.cos((float)this.gearRotation / 64.0F * (float)Math.PI * 2.0F);

		for(int xTile = 0; xTile < 16; ++xTile) {
			for(int yTile = 0; yTile < 16; ++yTile) {
				float xOffset = ((float)xTile / 15.0F - 0.5F) * 31.0F;
				float yOffset = ((float)yTile / 15.0F - 0.5F) * 31.0F;
				float rotatedX = rotationCos * xOffset - rotationSin * yOffset;
				float rotatedY = rotationCos * yOffset + rotationSin * xOffset;
				int texX = (int)(rotatedX + 16.0F);
				int texY = (int)(rotatedY + 16.0F);
				int color = 0;
				if(texX >= 0 && texY >= 0 && texX < 32 && texY < 32) {
					color = this.gearColor[texX + (texY << 5)];
					int middleColor = this.gearMiddleColor[xTile + (yTile << 4)];
					if(middleColor >>> 24 > 128) {
						color = middleColor;
					}
				}

				int red = color >> 16 & 255;
				int green = color >> 8 & 255;
				int blue = color & 255;
				int alpha = color >>> 24 > 128 ? 255 : 0;
				int index = xTile + (yTile << 4);
				this.imageData[index << 2] = (byte)red;
				this.imageData[(index << 2) + 1] = (byte)green;
				this.imageData[(index << 2) + 2] = (byte)blue;
				this.imageData[(index << 2) + 3] = (byte)alpha;
			}
		}
	}
}