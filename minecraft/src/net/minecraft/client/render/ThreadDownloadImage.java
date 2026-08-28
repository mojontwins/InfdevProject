package net.minecraft.client.render;

import java.net.HttpURLConnection;
import java.net.URL;
import javax.imageio.ImageIO;

final class ThreadDownloadImage extends Thread {
	private String location;
	private ImageBufferDownload buffer;
	private ThreadDownloadImageData imageData;

	ThreadDownloadImage(ThreadDownloadImageData imageData, String location, ImageBufferDownload buffer) {
		this.imageData = imageData;
		this.location = location;
		this.buffer = buffer;
	}

	public final void run() {
		HttpURLConnection connection = null;

		try {
			URL url = new URL(this.location);
			connection = (HttpURLConnection)url.openConnection();
			connection.setDoInput(true);
			connection.setDoOutput(false);
			connection.connect();
			if(connection.getResponseCode() != 404) {
				if(this.buffer == null) {
					this.imageData.image = ImageIO.read(connection.getInputStream());
				} else {
					this.imageData.image = this.buffer.parseUserSkin(ImageIO.read(connection.getInputStream()));
				}

				return;
			}
		} catch (Exception exception) {
			exception.printStackTrace();
			return;
		} finally {
			connection.disconnect();
		}
	}
}