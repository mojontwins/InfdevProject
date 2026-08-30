package net.minecraft.client.model;

import util.MathHelper;

/**
 * Model for four-legged creatures (pigs, sheep). The body hangs between the
 * legs and the head sits at the front; the leg height is configurable so that
 * taller (sheep) and shorter (pig) variants can share one model definition.
 */
public class ModelQuadruped extends ModelBase {
	public ModelRenderer head = new ModelRenderer(0, 0);
	public ModelRenderer body;
	public ModelRenderer leg1;
	public ModelRenderer leg2;
	public ModelRenderer leg3;
	public ModelRenderer leg4;

	/**
	 * @param legHeight height of each leg box, which also lifts the head and
	 *        body vertically above the ground (pigs use 6, sheep use 12)
	 * @param unused     not referenced here; kept for signature compatibility
	 */
	public ModelQuadruped(int legHeight, float unused) {
		this.head.addBox(-4.0F, -4.0F, -8.0F, 8, 8, 8, 0.0F);
		this.head.setRotationPoint(0.0F, (float)(18 - legHeight), -6.0F);
		this.body = new ModelRenderer(28, 8);
		this.body.addBox(-5.0F, -10.0F, -7.0F, 10, 16, 8, 0.0F);
		this.body.setRotationPoint(0.0F, (float)(17 - legHeight), 2.0F);
		this.leg1 = new ModelRenderer(0, 16);
		this.leg1.addBox(-2.0F, 0.0F, -2.0F, 4, legHeight, 4, 0.0F);
		this.leg1.setRotationPoint(-3.0F, (float)(24 - legHeight), 7.0F);
		this.leg2 = new ModelRenderer(0, 16);
		this.leg2.addBox(-2.0F, 0.0F, -2.0F, 4, legHeight, 4, 0.0F);
		this.leg2.setRotationPoint(3.0F, (float)(24 - legHeight), 7.0F);
		this.leg3 = new ModelRenderer(0, 16);
		this.leg3.addBox(-2.0F, 0.0F, -2.0F, 4, legHeight, 4, 0.0F);
		this.leg3.setRotationPoint(-3.0F, (float)(24 - legHeight), -5.0F);
		this.leg4 = new ModelRenderer(0, 16);
		this.leg4.addBox(-2.0F, 0.0F, -2.0F, 4, legHeight, 4, 0.0F);
		this.leg4.setRotationPoint(3.0F, (float)(24 - legHeight), -5.0F);
	}

	public final void render(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor) {
		this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, 1.0F);
		this.head.render(1.0F);
		this.body.render(1.0F);
		this.leg1.render(1.0F);
		this.leg2.render(1.0F);
		this.leg3.render(1.0F);
		this.leg4.render(1.0F);
	}

	public final void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor) {
		// Head follows the entity's look direction (degrees to radians).
		this.head.rotateAngleY = netHeadYaw / (180.0F / (float)Math.PI);
		this.head.rotateAngleX = headPitch / (180.0F / (float)Math.PI);
		// The body lies horizontally (90 degrees about the X axis).
		this.body.rotateAngleX = (float)Math.PI * 0.5F;
		// Each opposite leg pair swings in antiphase to simulate a walking trot.
		this.leg1.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
		this.leg2.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float)Math.PI) * 1.4F * limbSwingAmount;
		this.leg3.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float)Math.PI) * 1.4F * limbSwingAmount;
		this.leg4.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
	}
}
