package net.minecraft.client.model;

import util.MathHelper;

/**
 * Model for the Spider: a small head, a neck, a large low-slung body and eight
 * legs arranged symmetrically on each side. The legs get their permanently
 * splayed pose here, with a walking gait added on top in
 * {@link #setRotationAngles}.
 */
public final class ModelSpider extends ModelBase {
	private ModelRenderer spiderHead = new ModelRenderer(32, 4);
	private ModelRenderer spiderNeck;
	private ModelRenderer spiderBody;
	private ModelRenderer spiderLeg1;
	private ModelRenderer spiderLeg2;
	private ModelRenderer spiderLeg3;
	private ModelRenderer spiderLeg4;
	private ModelRenderer spiderLeg5;
	private ModelRenderer spiderLeg6;
	private ModelRenderer spiderLeg7;
	private ModelRenderer spiderLeg8;

	public ModelSpider() {
		this.spiderHead.addBox(-4.0F, -4.0F, -8.0F, 8, 8, 8, 0.0F);
		this.spiderHead.setRotationPoint(0.0F, 15.0F, -3.0F);
		this.spiderNeck = new ModelRenderer(0, 0);
		this.spiderNeck.addBox(-3.0F, -3.0F, -3.0F, 6, 6, 6, 0.0F);
		this.spiderNeck.setRotationPoint(0.0F, 15.0F, 0.0F);
		this.spiderBody = new ModelRenderer(0, 12);
		this.spiderBody.addBox(-5.0F, -4.0F, -6.0F, 10, 8, 12, 0.0F);
		this.spiderBody.setRotationPoint(0.0F, 15.0F, 9.0F);
		// The eight legs: odd ones extend from the left side, even ones from the
		// right; they repeat at four depths (z = 2, 1, 0, -1).
		this.spiderLeg1 = new ModelRenderer(18, 0);
		this.spiderLeg1.addBox(-15.0F, -1.0F, -1.0F, 16, 2, 2, 0.0F);
		this.spiderLeg1.setRotationPoint(-4.0F, 15.0F, 2.0F);
		this.spiderLeg2 = new ModelRenderer(18, 0);
		this.spiderLeg2.addBox(-1.0F, -1.0F, -1.0F, 16, 2, 2, 0.0F);
		this.spiderLeg2.setRotationPoint(4.0F, 15.0F, 2.0F);
		this.spiderLeg3 = new ModelRenderer(18, 0);
		this.spiderLeg3.addBox(-15.0F, -1.0F, -1.0F, 16, 2, 2, 0.0F);
		this.spiderLeg3.setRotationPoint(-4.0F, 15.0F, 1.0F);
		this.spiderLeg4 = new ModelRenderer(18, 0);
		this.spiderLeg4.addBox(-1.0F, -1.0F, -1.0F, 16, 2, 2, 0.0F);
		this.spiderLeg4.setRotationPoint(4.0F, 15.0F, 1.0F);
		this.spiderLeg5 = new ModelRenderer(18, 0);
		this.spiderLeg5.addBox(-15.0F, -1.0F, -1.0F, 16, 2, 2, 0.0F);
		this.spiderLeg5.setRotationPoint(-4.0F, 15.0F, 0.0F);
		this.spiderLeg6 = new ModelRenderer(18, 0);
		this.spiderLeg6.addBox(-1.0F, -1.0F, -1.0F, 16, 2, 2, 0.0F);
		this.spiderLeg6.setRotationPoint(4.0F, 15.0F, 0.0F);
		this.spiderLeg7 = new ModelRenderer(18, 0);
		this.spiderLeg7.addBox(-15.0F, -1.0F, -1.0F, 16, 2, 2, 0.0F);
		this.spiderLeg7.setRotationPoint(-4.0F, 15.0F, -1.0F);
		this.spiderLeg8 = new ModelRenderer(18, 0);
		this.spiderLeg8.addBox(-1.0F, -1.0F, -1.0F, 16, 2, 2, 0.0F);
		this.spiderLeg8.setRotationPoint(4.0F, 15.0F, -1.0F);
	}

	public final void render(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor) {
		this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, 1.0F);
		this.spiderHead.render(1.0F);
		this.spiderNeck.render(1.0F);
		this.spiderBody.render(1.0F);
		this.spiderLeg1.render(1.0F);
		this.spiderLeg2.render(1.0F);
		this.spiderLeg3.render(1.0F);
		this.spiderLeg4.render(1.0F);
		this.spiderLeg5.render(1.0F);
		this.spiderLeg6.render(1.0F);
		this.spiderLeg7.render(1.0F);
		this.spiderLeg8.render(1.0F);
	}

	public final void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor) {
		// Head follows the entity's look direction (degrees to radians).
		this.spiderHead.rotateAngleY = netHeadYaw / (180.0F / (float)Math.PI);
		this.spiderHead.rotateAngleX = headPitch / (180.0F / (float)Math.PI);
		// Permanent spread of the eight legs: rear and front pairs stick out
		// farthest, middle pairs are pulled in slightly.
		this.spiderLeg1.rotateAngleZ = (float)Math.PI * -0.25F;
		this.spiderLeg2.rotateAngleZ = (float)Math.PI * 0.25F;
		this.spiderLeg3.rotateAngleZ = -((float)Math.PI * 0.185F);
		this.spiderLeg4.rotateAngleZ = (float)Math.PI * 0.185F;
		this.spiderLeg5.rotateAngleZ = -((float)Math.PI * 0.185F);
		this.spiderLeg6.rotateAngleZ = (float)Math.PI * 0.185F;
		this.spiderLeg7.rotateAngleZ = (float)Math.PI * -0.25F;
		this.spiderLeg8.rotateAngleZ = (float)Math.PI * 0.25F;
		// Each leg is angled backward on the right side and forward on the left,
		// giving the spider its characteristic crouched stance.
		this.spiderLeg1.rotateAngleY = (float)Math.PI * 0.25F;
		this.spiderLeg2.rotateAngleY = (float)Math.PI * -0.25F;
		this.spiderLeg3.rotateAngleY = (float)Math.PI * 0.125F;
		this.spiderLeg4.rotateAngleY = (float)Math.PI * -0.125F;
		this.spiderLeg5.rotateAngleY = (float)Math.PI * -0.125F;
		this.spiderLeg6.rotateAngleY = (float)Math.PI * 0.125F;
		this.spiderLeg7.rotateAngleY = (float)Math.PI * -0.25F;
		this.spiderLeg8.rotateAngleY = (float)Math.PI * 0.25F;
		// Walking gait: the four leg pairs alternate between sweeping outward in
		// yaw and lifting upward in pitch, using a doubled limb-swing wave.
		float yawSweepLeft = -(MathHelper.cos(limbSwing * 0.6662F * 2.0F) * 0.4F) * limbSwingAmount;
		float yawSweepRight = -(MathHelper.cos(limbSwing * 0.6662F * 2.0F + (float)Math.PI) * 0.4F) * limbSwingAmount;
		float yawSweepLeftMid = -(MathHelper.cos(limbSwing * 0.6662F * 2.0F + (float)Math.PI * 0.5F) * 0.4F) * limbSwingAmount;
		float yawSweepRightMid = -(MathHelper.cos(limbSwing * 0.6662F * 2.0F + (float)Math.PI * 3.0F / 2.0F) * 0.4F) * limbSwingAmount;
		float liftLeft = Math.abs(MathHelper.sin(limbSwing * 0.6662F) * 0.4F) * limbSwingAmount;
		float liftRight = Math.abs(MathHelper.sin(limbSwing * 0.6662F + (float)Math.PI) * 0.4F) * limbSwingAmount;
		float liftLeftMid = Math.abs(MathHelper.sin(limbSwing * 0.6662F + (float)Math.PI * 0.5F) * 0.4F) * limbSwingAmount;
		float liftRightMid = Math.abs(MathHelper.sin(limbSwing * 0.6662F + (float)Math.PI * 3.0F / 2.0F) * 0.4F) * limbSwingAmount;
		// Opposite legs on the two sides sweep in opposite directions.
		this.spiderLeg1.rotateAngleY += yawSweepLeft;
		this.spiderLeg2.rotateAngleY -= yawSweepLeft;
		this.spiderLeg3.rotateAngleY += yawSweepRight;
		this.spiderLeg4.rotateAngleY -= yawSweepRight;
		this.spiderLeg5.rotateAngleY += yawSweepLeftMid;
		this.spiderLeg6.rotateAngleY -= yawSweepLeftMid;
		this.spiderLeg7.rotateAngleY += yawSweepRightMid;
		this.spiderLeg8.rotateAngleY -= yawSweepRightMid;
		this.spiderLeg1.rotateAngleZ += liftLeft;
		this.spiderLeg2.rotateAngleZ -= liftLeft;
		this.spiderLeg3.rotateAngleZ += liftRight;
		this.spiderLeg4.rotateAngleZ -= liftRight;
		this.spiderLeg5.rotateAngleZ += liftLeftMid;
		this.spiderLeg6.rotateAngleZ -= liftLeftMid;
		this.spiderLeg7.rotateAngleZ += liftRightMid;
		this.spiderLeg8.rotateAngleZ -= liftRightMid;
	}
}
