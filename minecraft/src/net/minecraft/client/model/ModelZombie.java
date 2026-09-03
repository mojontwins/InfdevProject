package net.minecraft.client.model;

import util.MathHelper;

/**
 * Model for the Zombie. It reuses the biped model but overrides the arm pose
 * so the undead hold both arms outstretched straight in front of them, the
 * classic zombie lunge. The headwear box (a child of the head) is hidden on
 * a zombie — they have no second skin layer.
 */
public class ModelZombie extends ModelBiped {
	public ModelZombie() {
		super(0.0F, 0.0F);
		this.bipedHeadwear.showModel = false;
	}

	public final void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor) {
		super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor);
		// Stable arm pose for a zombie: unlike a living biped its arms do not
		// swing while walking, so the sway terms below evaluate to zero.
		float armSwing = MathHelper.sin(0.0F);
		float armLift = MathHelper.sin(0.0F);
		this.bipedRightArm.rotateAngleZ = 0.0F;
		this.bipedLeftArm.rotateAngleZ = 0.0F;
		this.bipedRightArm.rotateAngleY = -(0.1F - armSwing * 0.6F);
		this.bipedLeftArm.rotateAngleY = 0.1F - armSwing * 0.6F;
		this.bipedRightArm.rotateAngleX = (float)Math.PI * -0.5F;
		this.bipedLeftArm.rotateAngleX = (float)Math.PI * -0.5F;
		this.bipedRightArm.rotateAngleX -= armSwing * 1.2F - armLift * 0.4F;
		this.bipedLeftArm.rotateAngleX -= armSwing * 1.2F - armLift * 0.4F;
		this.bipedRightArm.rotateAngleZ += MathHelper.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
		this.bipedLeftArm.rotateAngleZ -= MathHelper.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
		this.bipedRightArm.rotateAngleX += MathHelper.sin(ageInTicks * 0.067F) * 0.05F;
		this.bipedLeftArm.rotateAngleX -= MathHelper.sin(ageInTicks * 0.067F) * 0.05F;
	}
}
