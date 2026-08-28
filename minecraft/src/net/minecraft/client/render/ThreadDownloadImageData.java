package net.minecraft.client.render;

import java.awt.image.BufferedImage;

public final class ThreadDownloadImageData {
	public BufferedImage image;
	public int referenceCount = 1;
	public int textureIntDownload = -1;
	public boolean textureSetupComplete = false;

	public ThreadDownloadImageData(String location, ImageBufferDownload buffer) {
		(new ThreadDownloadImage(this, location, buffer)).start();
	}
}