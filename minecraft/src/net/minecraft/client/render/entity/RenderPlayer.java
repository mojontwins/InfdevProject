package net.minecraft.client.render.entity;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.game.entity.Entity;
import net.minecraft.game.entity.EntityLiving;
import net.minecraft.game.entity.player.EntityPlayer;
import net.minecraft.game.item.Item;
import net.minecraft.game.item.ItemArmor;
import net.minecraft.game.item.ItemStack;

public final class RenderPlayer extends RenderLiving {
	private ModelBiped modelBipedMain = (ModelBiped)this.mainModel;
	private ModelBiped modelArmorChestplate = new ModelBiped(1.0F);
	private ModelBiped modelArmor = new ModelBiped(0.5F);
	private static final String[] armorFilenamePrefix = new String[]{"cloth", "chain", "iron", "diamond", "gold"};

	public RenderPlayer() {
		super(new ModelBiped(0.0F), 0.5F);
	}

	private void renderPlayer(EntityPlayer player, double x, double y, double z, float yaw, float partialTick) {
		super.a(player, x, y - (double)player.yOffset, z, yaw, partialTick);
	}

	public final void drawFirstPersonHand() {
		this.modelBipedMain.bipedRightArm.render(1.0F);
	}

	protected final boolean shouldRenderPass(EntityLiving entity, int renderPass) {
		EntityPlayer player = (EntityPlayer)entity;
		int armorSlot = 3 - renderPass;
		ItemStack stack = player.inventory.armorInventory[armorSlot];
		if(stack != null) {
			Item item = stack.getItem();
			if(item instanceof ItemArmor) {
				ItemArmor armor = (ItemArmor)item;
				this.loadTexture("/armor/" + armorFilenamePrefix[armor.renderIndex] + "_" + (renderPass == 2 ? 2 : 1) + ".png");
				ModelBiped model = renderPass == 2 ? this.modelArmor : this.modelArmorChestplate;
				model.bipedHead.showModel = renderPass == 0;
				model.bipedHeadwear.showModel = renderPass == 0;
				model.bipedBody.showModel = renderPass == 1 || renderPass == 2;
				model.bipedRightArm.showModel = renderPass == 1;
				model.bipedLeftArm.showModel = renderPass == 1;
				model.bipedRightLeg.showModel = renderPass == 2 || renderPass == 3;
				model.bipedLeftLeg.showModel = renderPass == 2 || renderPass == 3;
				this.setRenderPassModel(model);
				return true;
			}
		}

		return false;
	}

	public final void a(EntityLiving entity, double x, double y, double z, float yaw, float partialTick) {
		this.renderPlayer((EntityPlayer)entity, x, y, z, yaw, partialTick);
	}

	public final void doRender(Entity entity, double x, double y, double z, float yaw, float partialTick) {
		this.renderPlayer((EntityPlayer)entity, x, y, z, yaw, partialTick);
	}
}