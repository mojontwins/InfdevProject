package net.minecraft.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;

/**
 * Scans the local resources folder and registers available sounds with the sound
 * manager. After the initial scan, attempts to download any sounds that the game
 * references but are not present locally.
 *
 * <p>When the game was launched via the RetroMCP launchwrapper, it pre-populated
 * {@code game/assets/objects/} from the asset index. The game's own resource thread
 * now mirrors that behaviour by walking {@code game/resources/} and registering every
 * real {@code .ogg} file found there. Any sounds still missing (e.g. {@code bow.ogg}
 * whose origin server {@code minecraft.net} has been dead for years) are fetched
 * directly via HTTP.
 *
 * <p>To use the betacraft proxy for downloads, set JVM arguments:
 * {@code -Dhttp.proxyHost=betacraft.uk -Dhttp.proxyPort=11702}
 * The downloader honours those system properties automatically; if they are absent
 * it falls back to a direct connection.
 */
public final class ThreadDownloadResources extends Thread {
	private static final int MIN_FILE_SIZE = 512;
	private static final int CONNECT_TIMEOUT_MS = 5000;
	private static final int READ_TIMEOUT_MS = 10000;
	private static final String SOUND_URL_BASE = "https://meta.omniarchive.uk/c/inf-20100420/20100212/";

	private final File resourcesFolder;
	private final Minecraft mc;
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
		registerFromFolder(this.resourcesFolder);
		fillMissingSounds();
	}

	private void registerFromFolder(File folder) {
		File[] entries = folder.listFiles();
		if(entries == null) {
			return;
		}
		for(File entry : entries) {
			if(this.closing) {
				return;
			}
			if(entry.isDirectory()) {
				registerFromFolder(entry);
			} else if(entry.isFile() && entry.getName().toLowerCase().endsWith(".ogg")) {
				if(entry.length() < MIN_FILE_SIZE) {
					continue;
				}
				String relativePath = this.resourcesFolder.toPath().relativize(entry.toPath()).toString().replace('\\', '/');
				int slashIdx = relativePath.indexOf('/');
				if(slashIdx <= 0 || slashIdx >= relativePath.length() - 1) {
					continue;
				}
				String category = relativePath.substring(0, slashIdx);
				String assetName = relativePath.substring(slashIdx + 1);
				int dotIdx = assetName.lastIndexOf('.');
				if(dotIdx > 0) {
					assetName = assetName.substring(0, dotIdx);
				}
				if(category.equalsIgnoreCase("sound") || category.equalsIgnoreCase("newsound")) {
					this.mc.sndManager.addSound(assetName, entry);
				} else if(category.equalsIgnoreCase("music")) {
					this.mc.sndManager.addMusic(assetName, entry);
				}
			}
		}
	}

	/**
	 * Looks up the sounds the game actively uses (random.bow is the only one
	 * that has been missing historically) and attempts to download any that are
	 * not yet registered locally. Downloads go to the appropriate subdirectory
	 * of {@code resources/} so the folder-scan above picks them up on the next
	 * launch; they are also registered immediately via {@code addSound} so the
	 * current session can use them without a restart.
	 */
	private void fillMissingSounds() {
		for(MissingSound missing : MISSING_SOUNDS) {
			if(this.closing) {
				return;
			}
			if(this.mc.sndManager.hasSound(missing.poolKey)) {
				continue;
			}
			File dest = new File(this.resourcesFolder, missing.relativePath);
			if(dest.exists() && dest.length() >= MIN_FILE_SIZE) {
				this.mc.sndManager.addSound(missing.poolKey, dest);
				continue;
			}
			byte[] data = downloadWithProxyFallback(missing.remotePath);
			if(data != null && data.length >= MIN_FILE_SIZE) {
				try {
					File parent = dest.getParentFile();
					if(!parent.exists()) {
						parent.mkdirs();
					}
					try (FileOutputStream fos = new FileOutputStream(dest)) {
						fos.write(data);
					}
					this.mc.sndManager.addSound(missing.poolKey, dest);
					System.out.println("[Resources] Downloaded " + missing.relativePath + " (" + data.length + " bytes)");
				} catch (Exception e) {
					System.err.println("[Resources] Failed to save " + missing.relativePath + ": " + e);
				}
			} else {
				System.err.println("[Resources] Could not download " + missing.relativePath + " (server returned empty or too-small response)");
			}
		}
	}

	/**
	 * Downloads a file from the asset mirror, trying the JVM proxy first (from
	 * {@code -Dhttp.proxyHost} / {@code -Dhttp.proxyPort}) and falling back
	 * to a direct connection.
	 */
	private byte[] downloadWithProxyFallback(String remotePath) {
		String proxyHost = System.getProperty("http.proxyHost");
		String proxyPort = System.getProperty("http.proxyPort");
		Proxy proxy = Proxy.NO_PROXY;
		if(proxyHost != null && !proxyHost.isEmpty()) {
			int port = 80;
			try {
				if(proxyPort != null) {
					port = Integer.parseInt(proxyPort);
				}
			} catch (NumberFormatException ignored) {
			}
			proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, port));
		}

		String urlStr = SOUND_URL_BASE + remotePath;
		HttpURLConnection conn = null;
		try {
			URL url = new URL(urlStr);
			conn = (HttpURLConnection) url.openConnection(proxy);
			conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
			conn.setReadTimeout(READ_TIMEOUT_MS);
			conn.setRequestProperty("User-Agent", "Minecraft/Infdev 20100420");
			int response = conn.getResponseCode();
			if(response == HttpURLConnection.HTTP_OK) {
				byte[] buffer = new byte[8192];
				int total = 0;
				try (InputStream in = conn.getInputStream()) {
					int read;
					while((read = in.read(buffer)) != -1) {
						total += read;
					}
				}
				if(total > 0) {
					byte[] result = new byte[total];
					int offset = 0;
					try (InputStream in = conn.getInputStream()) {
						int read;
						while(offset < total && (read = in.read(result, offset, total - offset)) != -1) {
							offset += read;
						}
					}
					return result;
				}
			}
			System.err.println("[Resources] HTTP " + response + " for " + urlStr);
		} catch (Exception e) {
			System.err.println("[Resources] Download error for " + urlStr + ": " + e.getMessage());
		} finally {
			if(conn != null) {
				conn.disconnect();
			}
		}
		return null;
	}

	public final void closeMinecraft() {
		this.closing = true;
	}

	private static final class MissingSound {
		final String poolKey;
		final String relativePath;
		final String remotePath;

		MissingSound(String poolKey, String relativePath, String remotePath) {
			this.poolKey = poolKey;
			this.relativePath = relativePath;
			this.remotePath = remotePath;
		}
	}

	private static final MissingSound[] MISSING_SOUNDS = {
		new MissingSound("random.bow", "newsound/random/bow.ogg", "newsound/random/bow.ogg"),
	};
}
