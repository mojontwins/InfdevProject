package net.minecraft.client.model;

/**
 * The wooden sign's model: a flat board plus the supporting post below it.
 * Drawn at the standard 1/16 model scale (one model unit per texture pixel);
 * the post is hidden for wall-mounted signs.
 */
public class SignModel {
	public ModelRenderer signBoard = new ModelRenderer(0, 0);
	public ModelRenderer signStick;

	public SignModel() {
		this.signBoard.addBox(-12.0F, -14.0F, -1.0F, 24, 12, 2, 0.0F);
		this.signStick = new ModelRenderer(0, 14);
		this.signStick.addBox(-1.0F, -2.0F, -1.0F, 2, 14, 2, 0.0F);
	}

	/** Renders the board and the post at the standard model-to-block scale. */
	public void renderSign() {
		this.signBoard.render(1.0F / 16.0F);
		this.signStick.render(1.0F / 16.0F);
	}
}