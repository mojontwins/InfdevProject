package net.minecraft.client;

import java.awt.Canvas;
import java.awt.Component;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import javax.swing.JOptionPane;
import net.minecraft.client.controller.PlayerController;
import net.minecraft.client.controller.PlayerControllerCreative;
import net.minecraft.client.controller.PlayerControllerSP;
import net.minecraft.client.effect.EffectRenderer;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiErrorScreen;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.container.GuiInventory;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.player.EntityPlayerSP;
import net.minecraft.client.player.MovementInputFromOptions;
import net.minecraft.client.render.EntityRenderer;
import net.minecraft.client.render.RenderEngine;
import net.minecraft.client.render.RenderGlobal;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.texture.TextureFlamesFX;
import net.minecraft.client.render.texture.TextureGearsFX;
import net.minecraft.client.render.texture.TextureLavaFX;
import net.minecraft.client.render.texture.TextureWaterFX;
import net.minecraft.client.render.texture.TextureWaterFlowFX;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.game.entity.Entity;
import net.minecraft.game.entity.EntityLiving;
import net.minecraft.game.entity.player.InventoryPlayer;
import net.minecraft.game.item.Item;
import net.minecraft.game.item.ItemStack;
import net.minecraft.game.physics.MovingObjectPosition;
import net.minecraft.game.world.World;
import net.minecraft.game.world.block.Block;
import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.input.Controllers;
import org.lwjgl.input.Cursor;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;
import util.MathHelper;

/**
 * The main Minecraft class: owns the game loop, the display, input and every
 * major subsystem (world, player, renderer). Implements the classic 2010 loop
 * of up to 20 logic ticks per second interleaved with one render per frame.
 */
public final class Minecraft implements Runnable {
	public PlayerController playerController = new PlayerControllerSP(this);
	private boolean fullscreen = false;
	public int displayWidth;
	public int displayHeight;
	private Timer timer = new Timer(20.0F);
	public World theWorld;
	public RenderGlobal renderGlobal;
	public EntityPlayerSP thePlayer;
	public EffectRenderer effectRenderer;
	public Session session = null;
	public String minecraftUri;
	private Canvas mcCanvas;
	public boolean appletMode = true;
	public volatile boolean isGamePaused = false;
	public RenderEngine renderEngine;
	public FontRenderer fontRenderer;
	public GuiScreen currentScreen = null;
	private LoadingScreenRenderer loadingScreen = new LoadingScreenRenderer(this);
	public EntityRenderer entityRenderer = new EntityRenderer(this);
	private ThreadDownloadResources downloadResourcesThread;
	private int ticksRan = 0;
	private int leftClickCounter = 0;
	private int tempDisplayWidth;
	private int tempDisplayHeight;
	public String loadMapUser = null;
	public int loadMapID = 0;
	public GuiIngame ingameGUI;
	public boolean skipRenderWorld = false;
	public MovingObjectPosition objectMouseOver;
	public GameSettings gameSettings;
	public SoundManager sndManager;
	public MouseHelper mouseHelper;
	private File mcDataDir;
	private String server;
	private TextureWaterFX textureWaterFX;
	private TextureLavaFX textureLavaFX;
	private File minecraftDir;
	volatile boolean running;
	public String debug;
	public boolean inventoryScreen;
	private int prevFrameTime;
	public boolean inGameHasFocus;
	private long systemTime;

	/**
	 * Sets up the core subsystems. {@code canvas} is null when the client runs
	 * windowed/standalone; {@code applet} belongs to the 2010 applet start path
	 * and is retained for API compatibility (unused here).
	 */
	public Minecraft(Canvas canvas, MinecraftApplet applet, int width, int height, boolean fullscreen) {
		new ModelBiped(0.0F);
		this.objectMouseOver = null;
		this.sndManager = new SoundManager();
		this.server = null;
		this.textureWaterFX = new TextureWaterFX();
		this.textureLavaFX = new TextureLavaFX();
		this.minecraftDir = null;
		this.running = false;
		this.debug = "";
		this.inventoryScreen = false;
		this.prevFrameTime = 0;
		this.inGameHasFocus = false;
		this.systemTime = System.currentTimeMillis();
		this.tempDisplayWidth = width;
		this.tempDisplayHeight = height;
		this.fullscreen = fullscreen;
		// 2010 hack: keep this "timer hack thread" sleeping forever so the game thread drives the loop.
		new ThreadSleepForever(this, "Timer hack thread");
		this.mcCanvas = canvas;
		this.displayWidth = width;
		this.displayHeight = height;
		this.fullscreen = fullscreen;
	}

	/** Stores the multiplayer server address; the port is unused in this version. */
	public final void setServer(String server, int port) {
		this.server = server;
	}

	/**
	 * Returns (creating it if needed) the per-user Minecraft data directory,
	 * placed according to the detected OS convention.
	 */
	public final File getAppDir() {
		if(this.minecraftDir == null) {
			String folderName = "minecraft";
			String userHome = System.getProperty("user.home", ".");
			int[] osValues = OSMap.osValues;
			String osName = System.getProperty("os.name").toLowerCase();
			File appDir;
			switch(osValues[(osName.contains("win") ? EnumOS.windows : (osName.contains("mac") ? EnumOS.macos : (osName.contains("solaris") ? EnumOS.solaris : (osName.contains("sunos") ? EnumOS.solaris : (osName.contains("linux") ? EnumOS.linux : (osName.contains("unix") ? EnumOS.linux : EnumOS.unknown)))))).ordinal()]) {
			case 1:
			case 2:
				// Linux/Solaris: hidden ".minecraft" folder in the home directory.
				appDir = new File(userHome, '.' + folderName + '/');
				break;
			case 3:
				// Windows: use APPDATA when available, else fall back to home.
				osName = System.getenv("APPDATA");
				if(osName != null) {
					appDir = new File(osName, "." + folderName + '/');
				} else {
					appDir = new File(userHome, '.' + folderName + '/');
				}
				break;
			case 4:
				// macOS: standard "Library/Application Support/minecraft".
				appDir = new File(userHome, "Library/Application Support/" + folderName);
				break;
			default:
				appDir = new File(userHome, folderName + '/');
			}

			if(!appDir.exists() && !appDir.mkdirs()) {
				throw new RuntimeException("The working directory could not be created: " + appDir);
			}

			this.minecraftDir = appDir;
		}

		return this.minecraftDir;
	}

	/**
	 * Shows a GUI screen, closing the previous one. A null screen returns to the
	 * game (or opens the main menu / death screen, depending on world state).
	 * While a screen is open the mouse is released from the window.
	 */
	public final void displayGuiScreen(GuiScreen guiScreen) {
		if(!(this.currentScreen instanceof GuiErrorScreen)) {
			if(this.currentScreen != null) {
				this.currentScreen.onGuiClosed();
			}

			if(guiScreen == null && this.theWorld == null) {
				guiScreen = new GuiMainMenu();
			} else if(guiScreen == null && this.thePlayer.health <= 0) {
				guiScreen = new GuiGameOver();
			}

			this.currentScreen = guiScreen;
			if(guiScreen != null) {
				this.inputLock();
				ScaledResolution scaledRes = new ScaledResolution(this.displayWidth, this.displayHeight);
				int scaledWidth = scaledRes.getScaledWidth();
				int scaledHeight = scaledRes.getScaledHeight();
				guiScreen.setWorldAndResolution(this, scaledWidth, scaledHeight);
				this.skipRenderWorld = false;
			} else {
				this.setIngameFocus();
			}
		}
	}

	/** Tears the client down: stops resource downloads, saves the world, closes sound/input/display. */
	public final void shutdownMinecraftApplet() {
		try {
			if(this.downloadResourcesThread != null) {
				this.downloadResourcesThread.closeMinecraft();
			}
		} catch (Exception e) {
		}

		try {
			System.out.println("Stopping!");
			this.changeWorld2((World)null, "");
			this.sndManager.closeMinecraft();
			Mouse.destroy();
			Keyboard.destroy();
		} finally {
			Display.destroy();
		}

	}

	/**
	 * The game-thread entry point: opens the LWJGL display and GL context, boots
	 * all subsystems, then runs the classic 2010 loop -- up to 20 world ticks per
	 * second with one render per frame, plus a once-per-second FPS readout.
	 */
	public final void run() {
		this.running = true;

		try {
			Minecraft mc = this;
			if(this.mcCanvas != null) {
				Display.setParent(this.mcCanvas);
			} else if(this.fullscreen) {
				Display.setFullscreen(true);
				this.displayWidth = Display.getDisplayMode().getWidth();
				this.displayHeight = Display.getDisplayMode().getHeight();
			} else {
				Display.setDisplayMode(new DisplayMode(this.displayWidth, this.displayHeight));
			}

			Display.setTitle("Minecraft Minecraft Infdev");

			ContextCapabilities capabilities;
			IntBuffer intBuffer;
			try {
				Display.create();
				System.out.println("LWJGL version: " + Sys.getVersion());
				System.out.println("GL RENDERER: " + GL11.glGetString(GL11.GL_RENDERER));
				System.out.println("GL VENDOR: " + GL11.glGetString(GL11.GL_VENDOR));
				System.out.println("GL VERSION: " + GL11.glGetString(GL11.GL_VERSION));
				capabilities = GLContext.getCapabilities();
				System.out.println("OpenGL 3.0: " + capabilities.OpenGL30);
				System.out.println("OpenGL 3.1: " + capabilities.OpenGL31);
				System.out.println("OpenGL 3.2: " + capabilities.OpenGL32);
				System.out.println("ARB_compatibility: " + capabilities.GL_ARB_compatibility);
				if(capabilities.OpenGL32) {
					// Query the GL context profile mask (0x9126) through a scratch int buffer.
					intBuffer = ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder()).asIntBuffer();
					GL11.glGetInteger('\u9126', intBuffer);
					int profileMask = intBuffer.get(0);
					System.out.println("PROFILE MASK: " + Integer.toBinaryString(profileMask));
					System.out.println("CORE PROFILE: " + ((profileMask & 1) != 0));
					System.out.println("COMPATIBILITY PROFILE: " + ((profileMask & 2) != 0));
				}
			} catch (LWJGLException e) {
				e.printStackTrace();

				// Retry once after a pause; some drivers need a moment after a first failure.
				try {
					Thread.sleep(1000L);
				} catch (InterruptedException ie) {
				}

				Display.create();
			}

			Keyboard.create();
			Mouse.create();
			this.mouseHelper = new MouseHelper(this.mcCanvas);

			try {
				Controllers.create();
			} catch (Exception e) {
				e.printStackTrace();
			}

			// Baseline GL state for the 2010 fixed-function pipeline.
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			GL11.glShadeModel(GL11.GL_SMOOTH);
			GL11.glClearDepth(1.0D);
			GL11.glEnable(GL11.GL_DEPTH_TEST);
			GL11.glDepthFunc(GL11.GL_LEQUAL);
			GL11.glEnable(GL11.GL_ALPHA_TEST);
			GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
			GL11.glCullFace(GL11.GL_BACK);
			GL11.glMatrixMode(GL11.GL_PROJECTION);
			GL11.glLoadIdentity();
			GL11.glMatrixMode(GL11.GL_MODELVIEW);
			this.mcDataDir = this.getAppDir();
			this.gameSettings = new GameSettings(this, this.mcDataDir);
			this.sndManager.loadSoundSettings(this.gameSettings);
			this.renderEngine = new RenderEngine(this.gameSettings);
			this.renderEngine.registerTextureFX(this.textureLavaFX);
			this.renderEngine.registerTextureFX(this.textureWaterFX);
			this.renderEngine.registerTextureFX(new TextureWaterFlowFX());
			this.renderEngine.registerTextureFX(new TextureFlamesFX(0));
			this.renderEngine.registerTextureFX(new TextureFlamesFX(1));
			this.renderEngine.registerTextureFX(new TextureGearsFX(0));
			this.renderEngine.registerTextureFX(new TextureGearsFX(1));
			this.fontRenderer = new FontRenderer(this.gameSettings, "/default.png", this.renderEngine);
			// Scratch buffer (256 ints) for texture-index round-trips with GL.
			intBuffer = BufferUtils.createIntBuffer(256);
			intBuffer.clear().limit(256);
			this.renderGlobal = new RenderGlobal(this, this.renderEngine);
			GL11.glViewport(0, 0, this.displayWidth, this.displayHeight);
			if(this.server != null && this.session != null) {
				// Multiplayer startup: go straight into an empty world.
				capabilities = null;
				this.changeWorld2((World)null, "");
			} else if(this.theWorld == null) {
				this.displayGuiScreen(new GuiMainMenu());
			}

			this.effectRenderer = new EffectRenderer(this.theWorld, this.renderEngine);

			try {
				mc.downloadResourcesThread = new ThreadDownloadResources(mc.mcDataDir, mc);
				mc.downloadResourcesThread.start();
			} catch (Exception e) {
			}

			this.ingameGUI = new GuiIngame(this);
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog((Component)null, e.toString(), "Failed to start Minecraft", 0);
			return;
		}

		long lastDebugUpdate = System.currentTimeMillis();
		int frameCount = 0;

		try {
			while(this.running) {
				if(this.mcCanvas == null && Display.isCloseRequested()) {
					this.running = false;
				}

				try {
					if(this.isGamePaused) {
						// While a pause-capable GUI is up, freeze the render step so the
						// scene (and its partial-tick interpolation) does not jump forward.
						float partialTicks = this.timer.renderPartialTicks;
						this.timer.updateTimer();
						this.timer.renderPartialTicks = partialTicks;
					} else {
						this.timer.updateTimer();
					}

					int ticksExecuted = 0;

					while(true) {
						if(ticksExecuted >= this.timer.elapsedTicks) {
							// All queued world ticks are done: render exactly one frame.
							if(this.isGamePaused) {
								this.timer.renderPartialTicks = 1.0F;
							}

							this.sndManager.setListener(this.thePlayer, this.timer.renderPartialTicks);
							GL11.glEnable(GL11.GL_TEXTURE_2D);
							if(this.theWorld != null) {
								while(this.theWorld.updatingLighting()) {
								}
							}

							this.playerController.setPartialTime(this.timer.renderPartialTicks);
							this.entityRenderer.updateCameraAndRender(this.timer.renderPartialTicks);
							if(!Display.isActive()) {
								if(this.fullscreen) {
									this.toggleFullscreen();
								}

								// Window lost focus: yield so other apps get CPU time.
								Thread.sleep(10L);
							}

							if(this.mcCanvas != null && !this.fullscreen && (this.mcCanvas.getWidth() != this.displayWidth || this.mcCanvas.getHeight() != this.displayHeight)) {
								this.displayWidth = this.mcCanvas.getWidth();
								this.displayHeight = this.mcCanvas.getHeight();
								this.resize(this.displayWidth, this.displayHeight);
							}

							if(this.gameSettings.limitFramerate) {
								Thread.sleep(5L);
							}

							++frameCount;
							this.isGamePaused = this.currentScreen != null && this.currentScreen.doesGuiPauseGame();
							break;
						}

						++this.ticksRan;
						this.runTick();
						++ticksExecuted;
					}
				} catch (Exception e) {
					this.displayGuiScreen(new GuiErrorScreen("Client error", "The game broke! [" + e + "]"));
					e.printStackTrace();
					return;
				}

				// Refresh the on-screen FPS / chunk-update line once per second.
				while(System.currentTimeMillis() >= lastDebugUpdate + 1000L) {
					this.debug = frameCount + " fps, " + WorldRenderer.chunksUpdated + " chunk updates";
					WorldRenderer.chunksUpdated = 0;
					lastDebugUpdate += 1000L;
					frameCount = 0;
				}
			}

			return;
		} catch (MinecraftError e) {
			return;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			this.shutdownMinecraftApplet();
		}

	}

	/** Regains in-game focus: grabs the mouse cursor and closes any open GUI. */
	public final void setIngameFocus() {
		if(Display.isActive()) {
			if(!this.inventoryScreen) {
				this.inventoryScreen = true;
				this.mouseHelper.grabMouseCursor();
				this.displayGuiScreen((GuiScreen)null);
				this.prevFrameTime = this.ticksRan + 10000;
			}
		}
	}

	/**
	 * Called when a GUI opens: ungrab the native mouse cursor so menus can be
	 * used, reset any stuck movement keys, and mark that we left the game view.
	 */
	private void inputLock() {
		if(this.inventoryScreen) {
			if(this.thePlayer != null) {
				EntityPlayerSP player = this.thePlayer;
				player.movementInput.resetKeyState();
			}

			this.inventoryScreen = false;

			try {
				Mouse.setNativeCursor((Cursor)null);
			} catch (LWJGLException e) {
				e.printStackTrace();
			}
		}
	}

	/** Opens the in-game pause menu (Esc), if no other screen is already up. */
	public final void displayInGameMenu() {
		if(this.currentScreen == null) {
			this.displayGuiScreen(new GuiIngameMenu());
		}
	}

	/**
	 * Processes a mouse button click (0 = left, 1 = right) against whatever the
	 * crosshair points at: entities are attacked, blocks are mined, and the held
	 * item is used or placed.
	 */
	private void clickMouse(int mouseButton) {
		if(mouseButton != 0 || this.leftClickCounter <= 0) {
			if(mouseButton == 0) {
				this.entityRenderer.itemRenderer.equippedItemRender();
			}

			ItemStack itemStack;
			World world;
			if(mouseButton == 1) {
				itemStack = this.thePlayer.inventory.getCurrentItem();
				if(itemStack != null) {
					int stackSize = itemStack.stackSize;
					EntityPlayerSP player = this.thePlayer;
					world = this.theWorld;
					ItemStack resultStack = itemStack.getItem().onItemRightClick(itemStack, world, player);
					if(resultStack != itemStack || resultStack != null && resultStack.stackSize != stackSize) {
						this.thePlayer.inventory.mainInventory[this.thePlayer.inventory.currentItem] = resultStack;
						this.entityRenderer.itemRenderer.resetEquippedProgress();
						if(resultStack.stackSize == 0) {
							this.thePlayer.inventory.mainInventory[this.thePlayer.inventory.currentItem] = null;
						}
					}
				}
			}

			if(this.objectMouseOver == null) {
				if(mouseButton == 0 && !(this.playerController instanceof PlayerControllerCreative)) {
					// Delay the next auto-repeat left click.
					this.leftClickCounter = 10;
				}

			} else {
				ItemStack heldItem;
				if(this.objectMouseOver.typeOfHit == 1) {
					if(mouseButton == 0) {
						Entity entityHit = this.objectMouseOver.entityHit;
						EntityPlayerSP player = this.thePlayer;
						InventoryPlayer inventory = player.inventory;
						heldItem = inventory.getStackInSlot(inventory.currentItem);
						int attackDamage = heldItem != null ? Item.itemsList[heldItem.itemID].getDamageVsEntity() : 1;
						if(attackDamage > 0) {
							entityHit.attackEntityFrom(player, attackDamage);
							itemStack = player.inventory.getCurrentItem();
							if(itemStack != null && entityHit instanceof EntityLiving) {
								Item.itemsList[itemStack.itemID].hitEntity(itemStack);
								if(itemStack.stackSize <= 0) {
									player.displayInventoryGUI();
								}
							}
						}

						return;
					}
				} else if(this.objectMouseOver.typeOfHit == 0) {
					int blockX = this.objectMouseOver.blockX;
					int blockY = this.objectMouseOver.blockY;
					int blockZ = this.objectMouseOver.blockZ;
					int sideHit = this.objectMouseOver.sideHit;
					Block block = Block.blocksList[this.theWorld.getBlockId(blockX, blockY, blockZ)];
					if(mouseButton == 0) {
						this.theWorld.extinguishFire(blockX, blockY, blockZ, this.objectMouseOver.sideHit);
						if(block != Block.bedrock) {
							this.playerController.clickBlock(blockX, blockY, blockZ);
							return;
						}

					} else {
						heldItem = this.thePlayer.inventory.getCurrentItem();
						int blockId = this.theWorld.getBlockId(blockX, blockY, blockZ);
						if(blockId > 0 && Block.blocksList[blockId].blockActivated(this.theWorld, blockX, blockY, blockZ, this.thePlayer)) {
							return;
						}

						if(heldItem == null) {
							return;
						}

						int stackSize = heldItem.stackSize;
						int side = sideHit;
						world = this.theWorld;
						float xWithinFace = (float)(this.objectMouseOver.hitVec.xCoord - (double)blockX);
						float yWithinFace = (float)(this.objectMouseOver.hitVec.yCoord - (double)blockY);
						float zWithinFace = (float)(this.objectMouseOver.hitVec.zCoord - (double)blockZ);
						if(heldItem.getItem().onItemUse(heldItem, world, blockX, blockY, blockZ, side, xWithinFace, yWithinFace, zWithinFace)) {
							this.entityRenderer.itemRenderer.equippedItemRender();
						}

						if(heldItem.stackSize == 0) {
							this.thePlayer.inventory.mainInventory[this.thePlayer.inventory.currentItem] = null;
							return;
						}

						if(heldItem.stackSize != stackSize) {
							this.entityRenderer.itemRenderer.resetEquippedProgress2();
						}
					}
				}

			}
		}
	}

	/** Switches between windowed and fullscreen, rebuilding the display and re-focusing. */
	public final void toggleFullscreen() {
		try {
			this.fullscreen = !this.fullscreen;
			System.out.println("Toggle fullscreen!");
			if(this.fullscreen) {
				Display.setDisplayMode(Display.getDesktopDisplayMode());
				this.displayWidth = Display.getDisplayMode().getWidth();
				this.displayHeight = Display.getDisplayMode().getHeight();
			} else {
				if(this.mcCanvas != null) {
					this.displayWidth = this.mcCanvas.getWidth();
					this.displayHeight = this.mcCanvas.getHeight();
				} else {
					this.displayWidth = this.tempDisplayWidth;
					this.displayHeight = this.tempDisplayHeight;
				}

				Display.setDisplayMode(new DisplayMode(this.tempDisplayWidth, this.tempDisplayHeight));
			}

			this.inputLock();
			Display.setFullscreen(this.fullscreen);
			Display.update();
			// Let the mode switch settle before re-grabbing input.
			Thread.sleep(1000L);
			if(this.fullscreen) {
				this.setIngameFocus();
			}

			if(this.currentScreen != null) {
				this.inputLock();
				this.resize(this.displayWidth, this.displayHeight);
			}

			System.out.println("Size: " + this.displayWidth + ", " + this.displayHeight);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/** Re-scales any open GUI after a canvas or display resize. */
	private void resize(int width, int height) {
		this.displayWidth = width;
		this.displayHeight = height;
		if(this.currentScreen != null) {
			ScaledResolution scaledRes = new ScaledResolution(width, height);
			int scaledWidth = scaledRes.getScaledWidth();
			int scaledHeight = scaledRes.getScaledHeight();
			this.currentScreen.setWorldAndResolution(this, scaledWidth, scaledHeight);
		}

	}

	/**
	 * Advances the game one logic tick (1/20 second): GUI updates, input polling,
	 * then world/entity ticking and cloud/effect updates. Reads pending mouse and
	 * keyboard events straight from the LWJGL event queue.
	 */
	private void runTick() {
		this.ingameGUI.updateTick();
		if(!this.isGamePaused && this.theWorld != null) {
			this.playerController.onUpdate();
		}

		GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.renderEngine.getTexture("/terrain.png"));
		if(!this.isGamePaused) {
			this.renderEngine.updateDynamicTextures();
		}

		if(this.currentScreen == null && this.thePlayer != null && this.thePlayer.health <= 0) {
			this.displayGuiScreen((GuiScreen)null);
		}

		if(this.currentScreen == null || this.currentScreen.allowUserInput) {
			inputDone:
			while(true) {
				while(true) {
					while(true) {
						long elapsedSinceTick;
						do {
							if(!Mouse.next()) {
								if(this.leftClickCounter > 0) {
									--this.leftClickCounter;
								}

								while(true) {
									while(true) {
										do {
											if(!Keyboard.next()) {
												if(this.currentScreen == null) {
													// Auto-repeat held mouse buttons (at ~1/4 second intervals).
													if(Mouse.isButtonDown(0) && (float)(this.ticksRan - this.prevFrameTime) >= this.timer.ticksPerSecond / 4.0F && this.inventoryScreen) {
														this.clickMouse(0);
														this.prevFrameTime = this.ticksRan;
													}

													if(Mouse.isButtonDown(1) && (float)(this.ticksRan - this.prevFrameTime) >= this.timer.ticksPerSecond / 4.0F && this.inventoryScreen) {
														this.clickMouse(1);
														this.prevFrameTime = this.ticksRan;
													}
												}

												// Left button held: keep digging the targeted block.
												boolean breakingBlock = this.currentScreen == null && Mouse.isButtonDown(0) && this.inventoryScreen;
												if(!this.playerController.isInTestMode && this.leftClickCounter <= 0) {
													if(breakingBlock && this.objectMouseOver != null && this.objectMouseOver.typeOfHit == 0) {
														int blockX = this.objectMouseOver.blockX;
														int blockY = this.objectMouseOver.blockY;
														int blockZ = this.objectMouseOver.blockZ;
														this.playerController.sendBlockRemoving(blockX, blockY, blockZ, this.objectMouseOver.sideHit);
														this.effectRenderer.addBlockHitEffects(blockX, blockY, blockZ, this.objectMouseOver.sideHit);
													} else {
														this.playerController.resetBlockRemoving();
													}
												}
												break inputDone;
											}

											EntityPlayerSP player = this.thePlayer;
											int keyCode = Keyboard.getEventKey();
											boolean keyDown = Keyboard.getEventKeyState();
											player.movementInput.checkKeyForMovementInput(keyCode, keyDown);
										} while(!Keyboard.getEventKeyState());

										if(Keyboard.getEventKey() == Keyboard.KEY_F11) {
											this.toggleFullscreen();
										} else {
											if(this.currentScreen != null) {
												this.currentScreen.handleKeyboardInput();
											} else {
												if(Keyboard.getEventKey() == Keyboard.KEY_ESCAPE) {
													this.displayInGameMenu();
												}

												if(this.playerController instanceof PlayerControllerCreative) {
													Keyboard.getEventKey();
													Keyboard.getEventKey();
												}

												if(Keyboard.getEventKey() == Keyboard.KEY_F5) {
													// Toggle third-person view and mirror the focus flag.
													this.gameSettings.thirdPersonView = !this.gameSettings.thirdPersonView;
													this.inGameHasFocus = !this.inGameHasFocus;
												}

												if(Keyboard.getEventKey() == this.gameSettings.keyBindInventory.keyCode) {
													this.displayGuiScreen(new GuiInventory(this.thePlayer.inventory));
												}

												if(Keyboard.getEventKey() == this.gameSettings.keyBindDrop.keyCode) {
													this.thePlayer.dropPlayerItemWithRandomChoice(this.thePlayer.inventory.decrStackSize(this.thePlayer.inventory.currentItem, 1), false);
												}
											}

											// Hotbar slots 1-9 map to key codes 2-10.
											for(int slotIndex = 0; slotIndex < 9; ++slotIndex) {
												if(Keyboard.getEventKey() == slotIndex + 2) {
													this.thePlayer.inventory.currentItem = slotIndex;
												}
											}

											if(Keyboard.getEventKey() == this.gameSettings.keyBindToggleFog.keyCode) {
												// Shift reverses the render-distance cycling direction.
												this.gameSettings.setOptionFloatValue(4, !Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) && !Keyboard.isKeyDown(Keyboard.KEY_RSHIFT) ? 1 : -1);
											}
										}
									}
								}
							}

							// Drain mouse events only while they arrive within 200 ms of this tick.
							elapsedSinceTick = System.currentTimeMillis() - this.systemTime;
						} while(elapsedSinceTick > 200L);

						// Mouse wheel: move the hotbar selection, wrapping 0-8.
						int wheelDelta = Mouse.getEventDWheel();
						if(wheelDelta != 0) {
							int wheelDirection = wheelDelta;
							InventoryPlayer inventory = this.thePlayer.inventory;
							if(wheelDelta > 0) {
								wheelDirection = 1;
							}

							if(wheelDirection < 0) {
								wheelDirection = -1;
							}

							for(inventory.currentItem -= wheelDirection; inventory.currentItem < 0; inventory.currentItem += 9) {
							}

							while(inventory.currentItem >= 9) {
								inventory.currentItem -= 9;
							}
						}

						if(this.currentScreen == null) {
							if(!this.inventoryScreen && Mouse.getEventButtonState()) {
								// Clicked a previously-unfocused window: grab the cursor.
								this.setIngameFocus();
							} else {
								if(Mouse.getEventButton() == 0 && Mouse.getEventButtonState()) {
									this.clickMouse(0);
									this.prevFrameTime = this.ticksRan;
								}

								if(Mouse.getEventButton() == 1 && Mouse.getEventButtonState()) {
									this.clickMouse(1);
									this.prevFrameTime = this.ticksRan;
								}

								// Middle click: hand out a simplified variant of the targeted block.
								if(Mouse.getEventButton() == 2 && Mouse.getEventButtonState() && this.objectMouseOver != null) {
									int blockId = this.theWorld.getBlockId(this.objectMouseOver.blockX, this.objectMouseOver.blockY, this.objectMouseOver.blockZ);
									if(blockId == Block.grass.blockID) {
										blockId = Block.dirt.blockID;
									}

									if(blockId == Block.stairDouble.blockID) {
										blockId = Block.stairSingle.blockID;
									}

									if(blockId == Block.bedrock.blockID) {
										blockId = Block.stone.blockID;
									}

									this.thePlayer.inventory.getFirstEmptyStack(blockId);
								}
							}
						} else if(this.currentScreen != null) {
							this.currentScreen.handleMouseInput();
						}
					}
				}
			}
		}

		if(this.currentScreen != null) {
			this.prevFrameTime = this.ticksRan + 10000;
		}

		if(this.currentScreen != null) {
			// Let the open GUI consume the remaining queued input, then step it.
			GuiScreen screen = this.currentScreen;

			while(Mouse.next()) {
				screen.handleMouseInput();
			}

			while(Keyboard.next()) {
				screen.handleKeyboardInput();
			}

			if(this.currentScreen != null) {
				this.currentScreen.updateScreen();
			}
		}

		if(this.theWorld != null) {
			this.theWorld.difficultySetting = this.gameSettings.difficulty;
			if(!this.isGamePaused) {
				this.entityRenderer.updateRenderer();
			}

			if(!this.isGamePaused) {
				this.renderGlobal.updateClouds();
			}

			if(!this.isGamePaused) {
				this.theWorld.levelEntities();
			}

			if(!this.isGamePaused) {
				this.theWorld.tick();
			}

			if(!this.isGamePaused) {
				this.theWorld.randomDisplayUpdates(MathHelper.floor_double(this.thePlayer.posX), MathHelper.floor_double(this.thePlayer.posY), MathHelper.floor_double(this.thePlayer.posZ));
			}

			if(!this.isGamePaused) {
				this.effectRenderer.updateEffects();
			}
		}

		this.systemTime = System.currentTimeMillis();
	}

	/** Loads (or generates) the named world, walking through the loading screens. */
	public final void startWorld(String worldName) {
		this.changeWorld2((World)null, "");
		System.gc();
		World world = new World(new File(this.getAppDir(), "saves"), worldName);
		if(world.isNewWorld) {
			this.changeWorld2(world, "Generating level");
		} else {
			this.changeWorld2(world, "Loading level");
		}

		this.loadingScreen.setText("Preparing lights");
		int progress = 0;

		// Busy-wait the initial chunk lighting, repainting the progress bar.
		while(world.lightUpdatesNeeded() > 0) {
			this.loadingScreen.setProgress(progress++ % 100);
			world.updatingLighting();
		}

	}

	/** Unloads the current world (the parameter is unused in this version). */
	public final void closeWorld(World world) {
		this.changeWorld2((World)null, "");
	}

	/**
	 * Replaces the active world: saves the previous one, (re)builds the local
	 * player if needed, wires up renderer/effects/controller and spawns the player.
	 */
	private void changeWorld2(World world, String loadingMessage) {
		if(this.theWorld != null) {
			this.theWorld.saveWorldIndirectly();
		}

		this.theWorld = world;
		if(world != null) {
			this.thePlayer = null;
			world.playerEntity = this.thePlayer;
			this.changeWorld1(loadingMessage);
			if(this.thePlayer == null) {
				this.thePlayer = new EntityPlayerSP(this, world, this.session);
				this.thePlayer.preparePlayerToSpawn();
			}

			this.thePlayer.movementInput = new MovementInputFromOptions(this.gameSettings);
			if(this.renderGlobal != null) {
				this.renderGlobal.changeWorld(world);
			}

			if(this.effectRenderer != null) {
				this.effectRenderer.clearEffects(world);
			}

			this.playerController.onRespawn(this.thePlayer);
			world.playerEntity = this.thePlayer;
			world.spawnPlayer();
		}

		System.gc();
		this.systemTime = 0L;
	}

	/** Warms up the chunks around the player's position, showing a loading screen. */
	private void changeWorld1(String loadingMessage) {
		this.loadingScreen.setTitle(loadingMessage);
		this.loadingScreen.setText("Preparing chunks");

		// Touching blocks forces chunk generation across the ~12 chunk radius.
		for(int chunkX = -196; chunkX <= 196; chunkX += 16) {
			this.loadingScreen.setProgress((chunkX + 196) * 100 / 392);
			int centerX = this.theWorld.spawnX;
			int centerZ = this.theWorld.spawnZ;
			if(this.theWorld.playerEntity != null) {
				centerX = (int)this.theWorld.playerEntity.posX;
				centerZ = (int)this.theWorld.playerEntity.posZ;
			}

			for(int chunkZ = -196; chunkZ <= 196; chunkZ += 16) {
				this.theWorld.getBlockId(centerX + chunkX, 64, centerZ + chunkZ);
			}
		}

		this.theWorld.dropOldChunks();
	}

	/** Respawns after death: kills the old player entity, recreates it and reloads chunks. */
	public final void respawn() {
		if(this.thePlayer != null && this.theWorld != null) {
			World.setEntityDead(this.thePlayer);
		}

		this.thePlayer = new EntityPlayerSP(this, this.theWorld, this.session);
		this.thePlayer.preparePlayerToSpawn();
		if(this.theWorld != null) {
			this.theWorld.playerEntity = this.thePlayer;
			this.theWorld.spawnPlayer();
		}

		this.thePlayer.movementInput = new MovementInputFromOptions(this.gameSettings);
		this.playerController.onRespawn(this.thePlayer);
		this.changeWorld1("Respawning");
	}
}
