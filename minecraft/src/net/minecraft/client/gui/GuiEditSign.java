package net.minecraft.client.gui;

import net.minecraft.client.render.TileEntityRenderer;
import net.minecraft.game.world.block.Block;
import net.minecraft.game.world.block.tileentity.TileEntitySign;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

/**
 * Full-screen editor for a sign's four lines of text. Key repeat is enabled
 * while the screen is open; the sign's rendered preview sits behind the
 * type-in controls and the edited line blinks with the cursor.
 */
public class GuiEditSign extends GuiScreen {
	/** Every character the font atlas can draw, and therefore every character the player may type. */
	private static final String ALLOWED_CHARS = " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_'abcdefghijklmnopqrstuvwxyz{|}~\u2302\u00c7\u00fc\u00e9\u00e2\u00e4\u00e0\u00e5\u00e7\u00ea\u00eb\u00e8\u00ef\u00ee\u00ec\u00c4\u00c5\u00c9\u00e6\u00c6\u00f4\u00f6\u00f2\u00fb\u00f9\u00ff\u00d6\u00dc\u00f8\u00a3\u00d8\u00d7\u0192\u00e1\u00ed\u00f3\u00fa\u00f1\u00d1\u00aa\u00ba\u00bf\u00ae\u00ac\u00bd\u00bc\u00a1\u00ab\u00bb";
	protected String screenTitle = "Edit sign message:";
	private TileEntitySign entitySign;
	private int updateCounter;
	private int editLine = 0;

	public GuiEditSign(TileEntitySign entitySign) {
		this.entitySign = entitySign;
	}

	public void initGui() {
		this.controlList.clear();
		Keyboard.enableRepeatEvents(true);
		this.controlList.add(new GuiButton(0, this.width / 2 - 100, this.height / 4 + 120, "Done"));
	}

	public void onGuiClosed() {
		Keyboard.enableRepeatEvents(false);
	}

	public void updateScreen() {
		++this.updateCounter;
	}

	protected void actionPerformed(GuiButton button) {
		if(button.enabled) {
			if(button.id == 0) {
				this.entitySign.onInventoryChanged();
				this.mc.displayGuiScreen((GuiScreen)null);
			}

		}

	}

	protected void keyTyped(char typedChar, int keyCode) {
		// Up arrow: move the cursor to the previous line (wrapping around the 4 lines).
		if(keyCode == 200) {
			this.editLine = this.editLine - 1 & 3;
		}

		// Down arrow / Enter: move the cursor to the next line (wrapping around the 4 lines).
		if(keyCode == 208 || keyCode == 28) {
			this.editLine = this.editLine + 1 & 3;
		}

		// Backspace: delete the last character of the edited line.
		if(keyCode == 14 && this.entitySign.signText[this.editLine].length() > 0) {
			this.entitySign.signText[this.editLine] = this.entitySign.signText[this.editLine].substring(0, this.entitySign.signText[this.editLine].length() - 1);
		}

		// Printable characters: append when the line has room for one more.
		if(ALLOWED_CHARS.indexOf(typedChar) >= 0 && this.entitySign.signText[this.editLine].length() < 15) {
			this.entitySign.signText[this.editLine] = this.entitySign.signText[this.editLine] + typedChar;
		}

	}

	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		drawCenteredString(this.fontRenderer, this.screenTitle, this.width / 2, 40, 16777215);
		GL11.glPushMatrix();
		GL11.glTranslatef((float)(this.width / 2), (float)(this.height / 2), 50.0F);
		float viewScale = 93.75F;
		GL11.glScalef(-viewScale, -viewScale, -viewScale);
		GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
		Block block = Block.blocksList[this.entitySign.worldObj.getBlockId(this.entitySign.xCoord, this.entitySign.yCoord, this.entitySign.zCoord)];
		// Orient the preview like the real sign in the world, using its block metadata.
		if(block == Block.signStanding) {
			float rotation = (float)(this.entitySign.worldObj.getBlockMetadata(this.entitySign.xCoord, this.entitySign.yCoord, this.entitySign.zCoord) * 360) / 16.0F;
			GL11.glRotatef(rotation, 0.0F, 1.0F, 0.0F);
		} else {
			int metadata = this.entitySign.worldObj.getBlockMetadata(this.entitySign.xCoord, this.entitySign.yCoord, this.entitySign.zCoord);
			float rotation = 0.0F;
			if(metadata == 2) {
				rotation = 180.0F;
			}

			if(metadata == 4) {
				rotation = 90.0F;
			}

			if(metadata == 5) {
				rotation = -90.0F;
			}

			GL11.glRotatef(rotation, 0.0F, 1.0F, 0.0F);
			GL11.glTranslatef(0.0F, 5.0F / 16.0F, 0.0F);
		}

		// Blink the cursor on alternate 6-tick halves of the update cycle.
		if(this.updateCounter / 6 % 2 == 0) {
			this.entitySign.lineBeingEdited = this.editLine;
		}

		TileEntityRenderer.instance.renderTileEntityAt(this.entitySign, -0.5D, -0.75D, -0.5D, 0.0F);
		this.entitySign.lineBeingEdited = -1;
		GL11.glPopMatrix();
		super.drawScreen(mouseX, mouseY, partialTicks);
	}
}