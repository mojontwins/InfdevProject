package net.minecraft.client.model;

import util.MathHelper;

/**
 * Model for a cow. A standalone entity model — does not extend
 * {@link ModelQuadruped} because the latter has private leg pivot fields
 * and final render methods that prevent the cow's extra parts (horns,
 * udders) from being added cleanly.
 */
public final class ModelCow extends ModelBase {
	public ModelRenderer head;
	public ModelRenderer body;
	public ModelRenderer leg1;
	public ModelRenderer leg2;
	public ModelRenderer leg3;
	public ModelRenderer leg4;
	private final ModelRenderer udders;
	private final ModelRenderer horn1;
	private final ModelRenderer horn2;

	public ModelCow() {
		this.head = new ModelRenderer(0, 0);
		this.head.addBox(-4.0F, -4.0F, -6.0F, 8, 8, 6, 0.0F);
		this.head.setRotationPoint(0.0F, 4.0F, -8.0F);

		this.horn1 = new ModelRenderer(22, 0);
		this.horn1.addBox(-5.0F, -5.0F, -4.0F, 1, 3, 1, 0.0F);
		this.horn1.setRotationPoint(0.0F, 3.0F, -7.0F);

		this.horn2 = new ModelRenderer(22, 0);
		this.horn2.addBox(4.0F, -5.0F, -4.0F, 1, 3, 1, 0.0F);
		this.horn2.setRotationPoint(0.0F, 3.0F, -7.0F);

		this.udders = new ModelRenderer(52, 0);
		this.udders.addBox(-2.0F, -3.0F, 0.0F, 4, 6, 2, 0.0F);
		this.udders.setRotationPoint(0.0F, 14.0F, 6.0F);
		this.udders.rotateAngleX = (float) Math.PI * 0.5F;

		this.body = new ModelRenderer(18, 4);
		this.body.addBox(-6.0F, -10.0F, -7.0F, 12, 18, 10, 0.0F);
		this.body.setRotationPoint(0.0F, 5.0F, 2.0F);

		this.leg1 = new ModelRenderer(0, 16);
		this.leg1.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, 0.0F);
		this.leg1.setRotationPoint(-4.0F, 12.0F, 7.0F);

		this.leg2 = new ModelRenderer(0, 16);
		this.leg2.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, 0.0F);
		this.leg2.setRotationPoint(4.0F, 12.0F, 7.0F);

		this.leg3 = new ModelRenderer(0, 16);
		this.leg3.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, 0.0F);
		this.leg3.setRotationPoint(-4.0F, 12.0F, -6.0F);

		this.leg4 = new ModelRenderer(0, 16);
		this.leg4.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, 0.0F);
		this.leg4.setRotationPoint(4.0F, 12.0F, -6.0F);
	}

	@Override
	public final void render(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor) {
		this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor);
		this.head.render(scaleFactor);
		this.horn1.render(scaleFactor);
		this.horn2.render(scaleFactor);
		this.body.render(scaleFactor);
		this.leg1.render(scaleFactor);
		this.leg2.render(scaleFactor);
		this.leg3.render(scaleFactor);
		this.leg4.render(scaleFactor);
		this.udders.render(scaleFactor);
	}

	@Override
	public final void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor) {
		this.head.rotateAngleY = netHeadYaw / (180.0F / (float) Math.PI);
		this.head.rotateAngleX = headPitch / (180.0F / (float) Math.PI);
		this.horn1.rotateAngleY = this.head.rotateAngleY;
		this.horn1.rotateAngleX = this.head.rotateAngleX;
		this.horn2.rotateAngleY = this.head.rotateAngleY;
		this.horn2.rotateAngleX = this.head.rotateAngleX;

		this.leg1.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
		this.leg2.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
		this.leg3.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
		this.leg4.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
	}
}
