package net.minecraft.client;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

/**
 * Background daemon thread that fetches the resource list from the Minecraft
 * server, downloads any missing files into the resources folder, and registers
 * the downloaded sounds/music with the sound manager. {@link #closeMinecraft()}
 * stops further work, which is used when the client shuts down.
 */
public final class ThreadDownloadResources extends Thread {
	private File resourcesFolder;
	private Minecraft mc;
	private boolean closing = false;

	public ThreadDownloadResources(File workingDirectory, Minecraft mc) {
		this.mc = mc;
		this.setName("Resource download thread");
		this.setDaemon(true);
		this.resourcesFolder = new File(workingDirectory, "resources/");
		if(!this.resourcesFolder.exists() && !this.resourcesFolder.mkdirs()) {
			throw new RuntimeException("The working directory could not be created: " + this.resourcesFolder);
		}
	}

	public final void run() {
				try {
			final ArrayList<String> list = new ArrayList<String>();
			final URL url = new URL("http://www.minecraft.net/resources/");
			final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(url.openStream()));
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				list.add(line);
			}
			bufferedReader.close();
			for (int i = 0; i < list.size(); ++i) {
				final String entry = list.get(i);
				final URL context = url;
				Label_0334: {
					try {
						// Each list entry is "relativePath,targetSize,...".
						final String[] parts = entry.split(",");
						final String fileName = parts[0];
						final int targetSize = Integer.parseInt(parts[1]);
						Long.parseLong(parts[2]);
						final File targetFile = new File(this.resourcesFolder, fileName);
						if (!targetFile.exists() || targetFile.length() != targetSize) {
							targetFile.getParentFile().mkdirs();
							this.downloadResource(new URL(context, fileName.replaceAll(" ", "%20")), targetFile);
							if (this.closing) {
								break Label_0334;
							}
						}
						// Split the path as "category/subasset" and register it.
						final String assetPath = fileName;
						final int slashIndex = assetPath.indexOf("/");
						final String category = assetPath.substring(0, slashIndex);
						final String assetName = assetPath.substring(slashIndex + 1);
						if (category.equalsIgnoreCase("sound")) {
							this.mc.sndManager.addSound(assetName, targetFile);
						}
						else if (category.equalsIgnoreCase("newsound")) {
							this.mc.sndManager.addSound(assetName, targetFile);
						}
						else if (category.equalsIgnoreCase("music")) {
							this.mc.sndManager.addMusic(assetName, targetFile);
						}
					}
					catch (Exception ex) {
						ex.printStackTrace();
					}
				}
				if (this.closing) {
					return;
				}
			}
		}
		catch (IOException ex2) {
			ex2.printStackTrace();
		}
	}

	private void downloadResource(URL resourceUrl, File targetFile) throws IOException {
		byte[] buffer = new byte[4096];
		try(DataInputStream input = new DataInputStream(resourceUrl.openStream()); DataOutputStream output = new DataOutputStream(new FileOutputStream(targetFile))) {
			do {
				int bytesRead = input.read(buffer);
				if(bytesRead < 0) {
					return;
				}

				output.write(buffer, 0, bytesRead);
			} while(!this.closing);
		}

	}

	public final void closeMinecraft() {
		this.closing = true;
	}
}
