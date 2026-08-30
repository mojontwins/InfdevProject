package net.minecraft.client.model;

/**
 * Abstract base class for all box-model (entity) models. It defines the common
 * animation contract every model must honour, but leaves all parts to subclasses.
 */
public abstract class ModelBase {
	/**
	 * Renders the model at the given scale. Subclasses typically call
	 * {@link #setRotationAngles} first to pose the parts, then draw each one.
	 *
	 * @param limbSwing       accumulated walking/swimming animation time
	 * @param limbSwingAmount how far into a swing step the model is (0 to 1)
	 * @param ageInTicks      time in ticks the entity has been alive
	 * @param netHeadYaw      head yaw offset, in degrees
	 * @param headPitch       head pitch offset, in degrees
	 * @param scaleFactor     uniform scale applied to the whole model
	 */
	public void render(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor) {
	}

	/**
	 * Positions the model's parts based on the current animation state: head
	 * parts follow the entity's look direction, limbs swing to simulate walking.
	 *
	 * @param limbSwing       accumulated walking/swimming animation time
	 * @param limbSwingAmount how far into a swing step the model is (0 to 1)
	 * @param ageInTicks      time in ticks the entity has been alive
	 * @param netHeadYaw      head yaw offset, in degrees
	 * @param headPitch       head pitch offset, in degrees
	 * @param scaleFactor     uniform scale applied to the whole model
	 */
	public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor) {
	}
}
