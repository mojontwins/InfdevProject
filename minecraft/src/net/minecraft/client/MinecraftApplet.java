package net.minecraft.client;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Canvas;

/**
 * Browser/desktop applet wrapper that hosts the Minecraft {@link Canvas} in an
 * AWT layout. Reads launch parameters (fullscreen, username/session, map or
 * server) from the applet and drives the game's main thread lifecycle.
 */
public class MinecraftApplet extends Applet {
	private static final long serialVersionUID = 1L;
	private Canvas mcCanvas;
	private Minecraft mc;
	private Thread mcThread = null;

	public void init() {
		this.mcCanvas = new CanvasMinecraftApplet(this);
		boolean fullscreen = false;
		if(this.getParameter("fullscreen") != null) {
			fullscreen = this.getParameter("fullscreen").equalsIgnoreCase("true");
		}

		this.mc = new Minecraft(this.mcCanvas, this, this.getWidth(), this.getHeight(), fullscreen);
		this.mc.minecraftUri = this.getDocumentBase().getHost();
		if(this.getDocumentBase().getPort() > 0) {
			this.mc.minecraftUri = this.mc.minecraftUri + ":" + this.getDocumentBase().getPort();
		}

		if(this.getParameter("username") != null && this.getParameter("sessionid") != null) {
			this.mc.session = new Session(this.getParameter("username"), this.getParameter("sessionid"));
			if(this.getParameter("mppass") != null) {
				this.getParameter("mppass");
			}
		}

		if(this.getParameter("loadmap_user") != null && this.getParameter("loadmap_id") != null) {
			this.mc.loadMapUser = this.getParameter("loadmap_user");
			this.mc.loadMapID = Integer.parseInt(this.getParameter("loadmap_id"));
		} else if(this.getParameter("server") != null && this.getParameter("port") != null) {
			this.mc.setServer(this.getParameter("server"), Integer.parseInt(this.getParameter("port")));
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
}
