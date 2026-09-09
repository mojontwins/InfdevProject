package net.minecraft.client.render.tileentity;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.model.SignModel;
import net.minecraft.client.render.TileEntitySpecialRenderer;
import net.minecraft.game.world.block.Block;
import net.minecraft.game.world.block.tileentity.TileEntity;
import net.minecraft.game.world.block.tileentity.TileEntitySign;
import org.lwjgl.opengl.GL11;

/**
 * Special renderer for {@link TileEntitySign}: draws the wooden sign model
 * (standing or wall-mounted, oriented from the block metadata) and stamps the
 * four text lines onto the board in front of it.
 */
public class TileEntitySignRenderer extends TileEntitySpecialRenderer {
	private SignModel modelSign = new SignModel();

	/**
	 * Renders a sign at the world-space offset (x, y, z). The block type and
	 * metadata are read straight from the tile entity's own cell in its world,
	 * since this version's {@link TileEntity} has no accessor for them.
	 */
	public void renderTileEntitySignAt(TileEntitySign sign, double x, double y, double z, float partialTick) {
		Block block = Block.blocksList[sign.worldObj.getBlockId(sign.xCoord, sign.yCoord, sign.zCoord)];
		GL11.glPushMatrix();
		float scale = 2.0F / 3.0F;
		if(block == Block.signStanding) {
			GL11.glTranslatef((float)x + 0.5F, (float)y + 12.0F / 16.0F * scale, (float)z + 0.5F);
			float rotation = (float)(sign.worldObj.getBlockMetadata(sign.xCoord, sign.yCoord, sign.zCoord) * 360) / 16.0F;
			GL11.glRotatef(-rotation, 0.0F, 1.0F, 0.0F);
			this.modelSign.signStick.showModel = true;
		} else {
			int metadata = sign.worldObj.getBlockMetadata(sign.xCoord, sign.yCoord, sign.zCoord);
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

			GL11.glTranslatef((float)x + 0.5F, (float)y + 12.0F / 16.0F * scale, (float)z + 0.5F);
			GL11.glRotatef(-rotation, 0.0F, 1.0F, 0.0F);
			GL11.glTranslatef(0.0F, -(5.0F / 16.0F), -(7.0F / 16.0F));
			this.modelSign.signStick.showModel = false;
		}

		this.bindTextureByName("/item/sign.png");
		GL11.glPushMatrix();
		GL11.glScalef(scale, -scale, -scale);
		this.modelSign.renderSign();
		GL11.glPopMatrix();
		FontRenderer fontRenderer = this.getFontRenderer();
		float textScale = (float)(1.0D / 60.0D) * scale;
		GL11.glTranslatef(0.0F, 0.5F * scale, 0.07F * scale);
		GL11.glScalef(textScale, -textScale, textScale);
		GL11.glNormal3f(0.0F, 0.0F, -1.0F * textScale);
		GL11.glDepthMask(false);
		int lineColour = 0;

		for(int line = 0; line < sign.signText.length; ++line) {
			String lineText = sign.signText[line];
			if(line == sign.lineBeingEdited) {
				lineText = "> " + lineText + " <";
			}

			fontRenderer.drawString(lineText, -fontRenderer.getStringWidth(lineText) / 2, line * 10 - sign.signText.length * 5, lineColour);
		}

		GL11.glDepthMask(true);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		GL11.glPopMatrix();
	}

	public void renderTileEntityAt(TileEntity tileEntity, double x, double y, double z, float partialTick) {
		this.renderTileEntitySignAt((TileEntitySign)tileEntity, x, y, z, partialTick);
	}
}