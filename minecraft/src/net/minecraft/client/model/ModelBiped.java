package net.minecraft.client.model;

import util.MathHelper;

/**
 * The classic human-sized, two-legged model used for the player and most
 * bipedal mobs (zombies, skeletons, giants). It is built from a head, an
 * optional headwear layer, a torso and four limbs, each a separate box.
 *
 * <p>The headwear box is a child of the head and is automatically rendered
 * with it. The ears box is also a child of the head and is rendered
 * separately via {@link #renderEars}. The cloak is a child of the body and
 * is rendered separately via {@link #renderCloak}.
 */
public class ModelBiped extends ModelBase {
	public ModelRenderer bipedHead;
	public ModelRenderer bipedHeadwear;
	public ModelRenderer bipedBody;
	public ModelRenderer bipedRightArm;
	public ModelRenderer bipedLeftArm;
	public ModelRenderer bipedRightLeg;
	public ModelRenderer bipedLeftLeg;
	public ModelRenderer bipedEars;
	public ModelRenderer bipedCloak;

	public ModelBiped() {
		this(0.0F);
	}

	public ModelBiped(float scale) {
		this(scale, 0.0F);
	}

	/**
	 * Full constructor. {@code scale} inflates every box; {@code yOffset} shifts
	 * the whole model's pivot down by that many model units (used for players).
	 */
	protected ModelBiped(float scale, float yOffset) {
		this.bipedHead = new ModelRenderer(0, 0);
		this.bipedHead.addBox("head", -4.0F, -8.0F, -4.0F, 8, 8, 8, scale);
		this.bipedHead.setRotationPoint(0.0F, 0.0F + yOffset, 0.0F);
		this.bipedHeadwear = new ModelRenderer(32, 0);
		this.bipedHeadwear.addBox("headwear", -4.0F, -8.0F, -4.0F, 8, 8, 8, scale + 0.5F);
		this.bipedHeadwear.setRotationPoint(0.0F, 0.0F + yOffset, 0.0F);
		this.bipedHead.addChild(this.bipedHeadwear);
		this.bipedEars = new ModelRenderer(24, 0);
		this.bipedEars.addBox("ears", -3.0F, -6.0F, -1.0F, 6, 6, 1, scale);
		this.bipedEars.setRotationPoint(0.0F, 0.0F + yOffset, 0.0F);
		this.bipedHead.addChild(this.bipedEars);

		this.bipedBody = new ModelRenderer(16, 16);
		this.bipedBody.addBox("body", -4.0F, 0.0F, -2.0F, 8, 12, 4, scale);
		this.bipedBody.setRotationPoint(0.0F, 0.0F + yOffset, 0.0F);
		this.bipedCloak = new ModelRenderer(0, 0);
		this.bipedCloak.addBox("cloak", -5.0F, 0.0F, -1.0F, 10, 16, 1, scale);
		this.bipedCloak.setRotationPoint(0.0F, 0.0F + yOffset, 0.0F);
		this.bipedBody.addChild(this.bipedCloak);

		this.bipedRightArm = new ModelRenderer(40, 16);
		this.bipedRightArm.addBox("rightArm", -3.0F, -2.0F, -2.0F, 4, 12, 4, scale);
		this.bipedRightArm.setRotationPoint(-5.0F, 2.0F + yOffset, 0.0F);
		this.bipedLeftArm = new ModelRenderer(40, 16);
		this.bipedLeftArm.mirror = true;
		this.bipedLeftArm.addBox("leftArm", -1.0F, -2.0F, -2.0F, 4, 12, 4, scale);
		this.bipedLeftArm.setRotationPoint(5.0F, 2.0F + yOffset, 0.0F);
		this.bipedRightLeg = new ModelRenderer(0, 16);
		this.bipedRightLeg.addBox("rightLeg", -2.0F, 0.0F, -2.0F, 4, 12, 4, scale);
		this.bipedRightLeg.setRotationPoint(-2.0F, 12.0F + yOffset, 0.0F);
		this.bipedLeftLeg = new ModelRenderer(0, 16);
		this.bipedLeftLeg.mirror = true;
		this.bipedLeftLeg.addBox("leftLeg", -2.0F, 0.0F, -2.0F, 4, 12, 4, scale);
		this.bipedLeftLeg.setRotationPoint(2.0F, 12.0F + yOffset, 0.0F);
	}

	public final void render(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor) {
		this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, 1.0F);
		this.bipedHead.render(1.0F);
		this.bipedBody.render(1.0F);
		this.bipedRightArm.render(1.0F);
		this.bipedLeftArm.render(1.0F);
		this.bipedRightLeg.render(1.0F);
		this.bipedLeftLeg.render(1.0F);
	}

	public void renderEars(float scaleFactor) {
		this.bipedEars.render(scaleFactor);
	}

	public void renderCloak(float scaleFactor) {
		this.bipedCloak.render(scaleFactor);
	}

	public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor) {
		this.bipedHead.rotateAngleY = netHeadYaw / (180.0F / (float)Math.PI);
		this.bipedHead.rotateAngleX = headPitch / (180.0F / (float)Math.PI);
		this.bipedEars.rotateAngleY = this.bipedHead.rotateAngleY;
		this.bipedEars.rotateAngleX = this.bipedHead.rotateAngleX;
		this.bipedRightArm.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float)Math.PI) * 2.0F * limbSwingAmount;
		this.bipedRightArm.rotateAngleZ = (MathHelper.cos(limbSwing * 0.2312F) + 1.0F) * limbSwingAmount;
		this.bipedLeftArm.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * 2.0F * limbSwingAmount;
		this.bipedLeftArm.rotateAngleZ = (MathHelper.cos(limbSwing * 0.2812F) - 1.0F) * limbSwingAmount;
		this.bipedRightLeg.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
		this.bipedLeftLeg.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float)Math.PI) * 1.4F * limbSwingAmount;
		this.bipedRightArm.rotateAngleZ += MathHelper.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
		this.bipedLeftArm.rotateAngleZ -= MathHelper.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
		this.bipedRightArm.rotateAngleX += MathHelper.sin(ageInTicks * 0.067F) * 0.05F;
		this.bipedLeftArm.rotateAngleX -= MathHelper.sin(ageInTicks * 0.067F) * 0.05F;
	}
}
