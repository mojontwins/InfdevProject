package net.minecraft.client;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Canvas;

/**
 * Browser/desktop applet wrapper that hosts the Minecraft {@link Canvas} in an
 * AWT layout. Reads launch parameters (fullscreen, username/session, map or
 * server) from the applet and drives the game's main thread lifecycle.
 *
 * <p>When started standalone (via {@link Start#main(String[])} instead of a
 * real browser), the applet parameters from {@code <param>} tags are not
 * available. {@link #getAppletParam(String)} falls back to system properties
 * of the form {@code net.minecraft.<name>}, which {@link Start} populates
 * from the CLI args.
 */
public class MinecraftApplet extends Applet {
	private static final long serialVersionUID = 1L;
	private Canvas mcCanvas;
	private Minecraft mc;
	private Thread mcThread = null;

	public void init() {
		this.mcCanvas = new CanvasMinecraftApplet(this);
		boolean fullscreen = false;
		String fullscreenParam = this.getAppletParam("fullscreen");
		if(fullscreenParam != null) {
			fullscreen = fullscreenParam.equalsIgnoreCase("true");
		}

		// When the applet is constructed outside a browser (via Start.main),
		// getWidth() / getHeight() both return 0 because no layout has been
		// performed. Fall back to a sensible default so the game thread can
		// open a display without divide-by-zero in the renderer.
		int width = this.getWidth();
		int height = this.getHeight();
		if(width <= 0) width = 1280;
		if(height <= 0) height = 720;

		this.mc = new Minecraft(this.mcCanvas, this, width, height, fullscreen);
		String host = null;
		try {
			if(this.getDocumentBase() != null) {
				host = this.getDocumentBase().getHost();
				if(this.getDocumentBase().getPort() > 0) {
					host = host + ":" + this.getDocumentBase().getPort();
				}
			}
		} catch (NullPointerException ignored) {
			// No applet context
		}
		this.mc.minecraftUri = host;

		String username = this.getAppletParam("username");
		String sessionid = this.getAppletParam("sessionid");
		if(username != null && sessionid != null) {
			this.mc.session = new Session(username, sessionid);
			// mppass was a legacy field read for its side effect of validating
			// the parameter; preserved for parity with the 2010 client.
			this.getAppletParam("mppass");
		}

		String loadmapUser = this.getAppletParam("loadmap_user");
		String loadmapId = this.getAppletParam("loadmap_id");
		if(loadmapUser != null && loadmapId != null) {
			this.mc.loadMapUser = loadmapUser;
			this.mc.loadMapID = Integer.parseInt(loadmapId);
		} else {
			String server = this.getAppletParam("server");
			String port = this.getAppletParam("port");
			if(server != null && port != null) {
				this.mc.setServer(server, Integer.parseInt(port));
			}
		}

		this.mc.appletMode = true;
		this.setLayout(new BorderLayout());
		this.add(this.mcCanvas, "Center");
		this.mcCanvas.setFocusable(true);
		this.validate();
	}

	public final void startMainThread() {
		if(this.mcThread == null) {
			this.mcThread = new Thread(this.mc, "Minecraft main thread");
			this.mcThread.start();
		}
	}

	public void start() {
		this.mc.isGamePaused = false;
	}

	public void stop() {
		this.mc.isGamePaused = true;
	}

	public void destroy() {
		this.shutdown();
	}

	public final void shutdown() {
		if(this.mcThread != null) {
			Minecraft mc = this.mc;
			mc.running = false;

			try {
				this.mcThread.join(1000L);
			} catch (InterruptedException e) {
				try {
					this.mc.shutdownMinecraftApplet();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}

			this.mcThread = null;
		}
	}

	/**
	 * Reads a launch parameter, falling back to a system property of the
	 * same name (prefixed with {@code net.minecraft.}) when no applet
	 * parameter is set. This is what lets {@link Start#main(String[])}
	 * pass CLI arguments through to a freshly-constructed applet.
	 *
	 * <p>Note: when the applet is constructed outside a browser (via
	 * {@code new MinecraftApplet()}), {@code super.getParameter(name)} throws
	 * a {@link NullPointerException} because there is no AppletContext. The
	 * try/catch below swallows that case and proceeds to the system-property
	 * fallback, which is what we want for standalone launches.
	 */
	private String getAppletParam(String name) {
		String fromHtml = null;
		try {
			fromHtml = super.getParameter(name);
		} catch (NullPointerException ignored) {
			// No applet context (i.e. running from main(), not a browser)
		}
		if(fromHtml != null) {
			return fromHtml;
		}
		return System.getProperty("net.minecraft." + name);
	}
}
