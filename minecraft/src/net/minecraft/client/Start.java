package net.minecraft.client;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Standalone bootstrap for the Minecraft Infdev client. Replaces the legacy
 * launchwrapper-based entry point with a single main() that does the same
 * argument parsing, builds a MinecraftApplet, and starts the game thread.
 *
 * <p>The launchwrapper used to:
 * <ul>
 *   <li>parse CLI args (--username, --session, --gameDir, --assetsDir, ...)</li>
 *   <li>populate a per-asset hash cache in {@code game/assets/objects/}</li>
 *   <li>register a custom URL stream handler for {@code asset://}</li>
 * </ul>
 *
 * <p>For this version, only sound files ever need to be downloaded. Textures
 * and other resources are served directly from the classpath (the original
 * {@code jars/minecraft.jar}). The game's own {@link ThreadDownloadResources}
 * now handles any missing sound downloads, optionally routing through a proxy
 * specified via {@code -Dhttp.proxyHost} / {@code -Dhttp.proxyPort}. That
 * proxy is what carries requests to betacraft.uk / meta.omniarchive.uk.
 *
 * <p>Supported CLI flags (mirroring the launchwrapper):
 * <ul>
 *   <li>{@code --username <name>}  - player handle</li>
 *   <li>{@code --session <id>}     - session id (use {@code -} for offline)</li>
 *   <li>{@code --uuid <uuid>}      - player uuid (use {@code -} for offline)</li>
 *   <li>{@code --version <v>}      - game version (e.g. {@code inf-20100420})</li>
 *   <li>{@code --gameDir <dir>}    - working directory (default {@code .})</li>
 *   <li>{@code --assetsDir <dir>}  - asset directory (default {@code .\\assets})</li>
 *   <li>{@code --assetIndex <id>}  - asset index id (default {@code 20100212})</li>
 *   <li>{@code --accessToken <t>}  - mojang-style access token (use {@code -} offline)</li>
 *   <li>{@code --userProperties <json>} - user properties JSON</li>
 *   <li>{@code --userType <t>}     - mojang-style user type</li>
 *   <li>{@code --versionType <t>}  - "release" or "snapshot"</li>
 *   <li>{@code --skinProxy <url>}  - skin proxy URL</li>
 *   <li>{@code --fullscreen true|false}</li>
 *   <li>{@code --server <host> --port <p>}</li>
 *   <li>{@code --loadmap_user <u> --loadmap_id <id>}</li>
 * </ul>
 */
public final class Start {
	private static final String[] KNOWN_FLAGS = {
		"username", "session", "uuid", "version", "gameDir", "assetsDir",
		"assetIndex", "accessToken", "userProperties", "userType",
		"versionType", "skinProxy", "fullscreen", "server", "port",
		"loadmap_user", "loadmap_id"
	};

	private Start() {
	}

	public static void main(String[] args) {
		Map<String, String> parsed = parseArgs(args);

		// Force the game to use the current directory as its working dir, so
		// saves, options.txt, and the resources/ folder are portable. This
		// overrides whatever the launchwrapper would normally do via
		// gameDir / assetsDir / assetIndex.
		String gameDir = parsed.get("gameDir");
		if(gameDir == null || gameDir.isEmpty()) {
			gameDir = ".";
		}
		try {
			System.setProperty("user.dir", new File(gameDir).getAbsolutePath());
		} catch (Exception ignored) {
		}

		// Promote every recognised flag into a system property so that
		// MinecraftApplet.init() can read them through getAppletParam().
		for(String flag : KNOWN_FLAGS) {
			String value = parsed.get(flag);
			if(value != null) {
				System.setProperty("net.minecraft." + flag, value);
			}
		}

		// Pre-empt Minecraft.getAppDir() with the requested gameDir so that
		// the per-user data folder stays relative to wherever the user launched
		// the game from. This is the equivalent of the original MCP Start.java
		// reflection trick.
		final MinecraftApplet applet = new MinecraftApplet();
		applet.init();
		applet.start();

		// Reflectively overwrite Minecraft.minecraftDir with the requested dir.
		// Done in this thread before the game thread starts, so Minecraft.run()
		// sees the value when it calls getAppDir() (or not, if it has already
		// cached minecraftDir).
		final Minecraft mc;
		try {
			Field mcField = MinecraftApplet.class.getDeclaredField("mc");
			mcField.setAccessible(true);
			mc = (Minecraft) mcField.get(applet);
			Field dirField = Minecraft.class.getDeclaredField("minecraftDir");
			dirField.setAccessible(true);
			dirField.set(mc, new File(gameDir));
		} catch (Exception ex) {
			System.err.println("Could not override minecraftDir: " + ex);
			throw new RuntimeException(ex);
		}

		// Wrap the applet in a visible Frame so the Canvas becomes displayable.
		// Without this, Display.setParent(canvas) inside Minecraft.run() fails with
		// "Parent.isDisplayable() must be true" because the Canvas was never
		// added to a displayable component hierarchy.
		// Read display dimensions from the Minecraft instance (already set to
		// 854x480 by MinecraftApplet.init() when running outside a browser).
		int gameWidth = mc.displayWidth > 0 ? mc.displayWidth : 854;
		int gameHeight = mc.displayHeight > 0 ? mc.displayHeight : 480;

		Frame frame = new Frame("Minecraft Minecraft Infdev");
		frame.setLayout(new BorderLayout());
		frame.add(applet, "Center");
		frame.setSize(gameWidth, gameHeight);
		frame.setResizable(true);

		// Centre the frame on the primary screen.
		Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment()
			.getDefaultScreenDevice().getDefaultConfiguration().getBounds();
		frame.setLocation(
			screenBounds.x + (screenBounds.width - gameWidth) / 2,
			screenBounds.y + (screenBounds.height - gameHeight) / 2
		);
		frame.setVisible(true);

		// Close the frame when the JVM exits (e.g. game window closed).
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			frame.dispose();
		}, "AWT-shutdown"));

		// Spawn the game thread. The applet's start() already does this in
		// browser mode, but start() is also called by the browser lifecycle
		// and we want to be sure the game thread is running before main()
		// returns.
		applet.startMainThread();
	}

	/**
	 * Parses the CLI arg vector into a flag -> value map. Supports both
	 * {@code --flag value} and {@code --flag=value} styles. Unknown flags
	 * are stored verbatim.
	 */
	private static Map<String, String> parseArgs(String[] args) {
		Map<String, String> result = new HashMap<>();
		for(int i = 0; i < args.length; i++) {
			String arg = args[i];
			if(arg == null || !arg.startsWith("--")) {
				continue;
			}
			String stripped = arg.substring(2);
			int eq = stripped.indexOf('=');
			String key;
			String value;
			if(eq >= 0) {
				key = stripped.substring(0, eq);
				value = stripped.substring(eq + 1);
			} else {
				key = stripped;
				if(i + 1 < args.length && (args[i + 1] == null || !args[i + 1].startsWith("--"))) {
					value = args[i + 1];
					i++;
				} else {
					value = "true";
				}
			}
			result.put(key, value);
		}
		return result;
	}
}
