package net.minecraft.client.render;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.ImageObserver;

public class ImageBufferDownload {
	private int[] imageData;
	private int imageWidth;
	private int imageHeight;

	public BufferedImage parseUserSkin(BufferedImage sourceImage) {
		this.imageWidth = 64;
		this.imageHeight = 32;
		BufferedImage image = new BufferedImage(this.imageWidth, this.imageHeight, 2);
		Graphics graphics = image.getGraphics();
		graphics.drawImage(sourceImage, 0, 0, (ImageObserver)null);
		graphics.dispose();
		this.imageData = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
		this.setAreaOpaque(0, 0, 32, 16);
		this.setAreaTransparent(32, 0, 64, 32);
		this.setAreaOpaque(0, 16, 64, 32);
		return image;
	}

	private void setAreaTransparent(int x1, int y1, int x2, int y2) {
		boolean foundTransparent = false;
		outer:
		for(int x = x1; x < x2; ++x) {
			for(int y = y1; y < y2; ++y) {
				int pixel = this.imageData[x + y * this.imageWidth];
				if(pixel >>> 24 < 128) {
					foundTransparent = true;
					break outer;
				}
			}
		}

		if(!foundTransparent) {
			for(int x = x1; x < x2; ++x) {
				for(int y = y1; y < y2; ++y) {
					this.imageData[x + y * this.imageWidth] &= 16777215;
				}
			}
		}
	}

	private void setAreaOpaque(int x1, int y1, int x2, int y2) {
		for(int x = 0; x < x2; ++x) {
			for(int y = y1; y < y2; ++y) {
				this.imageData[x + y * this.imageWidth] |= -16777216;
			}
		}
	}
}