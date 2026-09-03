package net.minecraft.client.model;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.render.GLAllocation;
import net.minecraft.client.render.Tessellator;
import org.lwjgl.opengl.GL11;

/**
 * A single part of an entity model (a head, a leg, a torso, a torso overlay
 * &hellip;). Each renderer holds a list of {@link ModelBox}es that share a
 * common pivot, plus an optional list of child renderers that move with it.
 *
 * <p>A renderer can be:
 * <ul>
 *   <li>moved to a pivot ({@link #setRotationPoint}),</li>
 *   <li>rotated about that pivot ({@code rotateAngle*}),</li>
 *   <li>built mirrored ({@link #mirror}),</li>
 *   <li>hidden altogether ({@link #showModel}).</li>
 * </ul>
 *
 * <p>On the first {@link #render(float, float)} call the renderer's boxes
 * and children are baked into an OpenGL display list. Subsequent calls just
 * play the list back, so the per-tick render cost is independent of the
 * number of boxes a renderer holds. The list is freed by {@link GLAllocation}
 * when the model is reloaded.
 */
public class ModelRenderer {
	/** Name of this part, used to look it up by string in {@link ModelBase}. */
	public String name;
	private int textureOffsetX;
	private int textureOffsetY;
	/** Texture dimensions the parent's boxes are sampled from. */
	public float textureWidth;
	public float textureHeight;
	/** Pivot this part rotates about, in model space. */
	public float rotationPointX;
	public float rotationPointY;
	public float rotationPointZ;
	/** Euler rotation applied each frame. */
	public float rotateAngleX;
	public float rotateAngleY;
	public float rotateAngleZ;
	/** Child renderers that move with this part (e.g. a head's headwear). */
	private List<ModelRenderer> children = new ArrayList<ModelRenderer>();
	/** Boxes drawn by this renderer. */
	private List<ModelBox> cubeList = new ArrayList<ModelBox>();
	private boolean compiled;
	/** GL display list id; valid once {@link #compiled} is true. */
	private int displayList;
	public boolean mirror;
	public boolean showModel = true;

	/**
	 * @param name           part name (must be unique inside the owning model)
	 * @param textureOffsetX x coordinate of this part's region on the texture
	 * @param textureOffsetY y coordinate of this part's region on the texture
	 */
	public ModelRenderer(ModelBase model, String name, int textureOffsetX, int textureOffsetY) {
		this.name = name;
		this.textureOffsetX = textureOffsetX;
		this.textureOffsetY = textureOffsetY;
		this.textureWidth = 64.0F;
		this.textureHeight = 32.0F;
	}

	/**
	 * Compatibility constructor used by infdev-era model classes that do not
	 * know the model they belong to yet. The {@code textureOffset*} is
	 * stored and {@code textureWidth/Height} default to the legacy 64x32
	 * model grid.
	 */
	public ModelRenderer(int textureOffsetX, int textureOffsetY) {
		this.textureOffsetX = textureOffsetX;
		this.textureOffsetY = textureOffsetY;
		this.textureWidth = 64.0F;
		this.textureHeight = 32.0F;
	}

	/**
	 * Builds a {@link ModelBox} from the eight corners of the cuboid spanning
	 * (x, y, z) to (x + width, y + height, z + depth), inflated by
	 * {@code expansion} on every side, and registers it with this renderer.
	 * When {@link #mirror} is set, the box is built mirrored and every face's
	 * winding is flipped so it still faces outward.
	 */
	public void addBox(String partName, float x, float y, float z, int width, int height, int depth, float expansion) {
		partName = partName;
		this.cubeList.add(new ModelBox(this, this.textureOffsetX, this.textureOffsetY, x, y, z, width, height, depth, expansion));
	}

	/**
	 * Overload that omits the per-box name. Behaviour is identical to the
	 * named form when the name is not relevant.
	 */
	public void addBox(float x, float y, float z, int width, int height, int depth, float expansion) {
		this.cubeList.add(new ModelBox(this, this.textureOffsetX, this.textureOffsetY, x, y, z, width, height, depth, expansion));
	}

	/**
	 * Adds a child renderer that will be drawn as part of this renderer's
	 * transform &mdash; its own pivot and rotation are applied on top of
	 * this renderer's.
	 */
	public void addChild(ModelRenderer child) {
		this.children.add(child);
	}

	/**
	 * Moves the renderer's pivot (the point it rotates around) to
	 * (x, y, z) in model space.
	 */
	public void setRotationPoint(float x, float y, float z) {
		this.rotationPointX = x;
		this.rotationPointY = y;
		this.rotationPointZ = z;
	}

	/**
	 * Bakes every box and child of this renderer into a display list. Called
	 * automatically the first time the renderer is drawn, but can be
	 * invoked eagerly to hide the compilation cost from gameplay.
	 */
	public void compileDisplayList(float scaleFactor) {
		this.displayList = GLAllocation.generateDisplayLists(1);
		GL11.glNewList(this.displayList, GL11.GL_COMPILE);
		Tessellator tessellator = Tessellator.instance;

		for(int i = 0; i < this.cubeList.size(); ++i) {
			this.cubeList.get(i).render(tessellator, scaleFactor);
		}

		GL11.glEndList();
		this.compiled = true;
	}

	/**
	 * Posed render of this renderer. On the first call the boxes and
	 * children are baked into a display list; afterward the list is just
	 * replayed at the current transform. The list is freed by
	 * {@link GLAllocation} when the model is reloaded.
	 */
	public void render(float scale) {
		if(!this.showModel) {
			return;
		}
		if(!this.compiled) {
			this.compileDisplayList(scale);
		}

		if(this.rotateAngleX == 0.0F && this.rotateAngleY == 0.0F && this.rotateAngleZ == 0.0F) {
			if(this.rotationPointX == 0.0F && this.rotationPointY == 0.0F && this.rotationPointZ == 0.0F) {
				GL11.glCallList(this.displayList);
				for(int i = 0; i < this.children.size(); ++i) {
					this.children.get(i).render(scale);
				}
			} else {
				GL11.glTranslatef(this.rotationPointX * scale, this.rotationPointY * scale, this.rotationPointZ * scale);
				GL11.glCallList(this.displayList);
				for(int i = 0; i < this.children.size(); ++i) {
					this.children.get(i).render(scale);
				}
				GL11.glTranslatef(-this.rotationPointX * scale, -this.rotationPointY * scale, -this.rotationPointZ * scale);
			}
		} else {
			GL11.glPushMatrix();
			GL11.glTranslatef(this.rotationPointX * scale, this.rotationPointY * scale, this.rotationPointZ * scale);
			if(this.rotateAngleZ != 0.0F) {
				GL11.glRotatef(this.rotateAngleZ * (180.0F / (float)Math.PI), 0.0F, 0.0F, 1.0F);
			}

			if(this.rotateAngleY != 0.0F) {
				GL11.glRotatef(this.rotateAngleY * (180.0F / (float)Math.PI), 0.0F, 1.0F, 0.0F);
			}

			if(this.rotateAngleX != 0.0F) {
				GL11.glRotatef(this.rotateAngleX * (180.0F / (float)Math.PI), 1.0F, 0.0F, 0.0F);
			}

			GL11.glCallList(this.displayList);
			for(int i = 0; i < this.children.size(); ++i) {
				this.children.get(i).render(scale);
			}

			GL11.glPopMatrix();
		}
	}

	/**
	 * Recursively poses and draws this renderer. Equivalent to
	 * {@link #render(float)} but the recursion lives inside a single method
	 * so subclasses can override it without having to also override the
	 * display-list logic.
	 */
	public void renderWithRotation(float scale) {
		if(!this.showModel) {
			return;
		}
		if(!this.compiled) {
			this.compileDisplayList(scale);
		}

		GL11.glPushMatrix();
		GL11.glTranslatef(this.rotationPointX * scale, this.rotationPointY * scale, this.rotationPointZ * scale);
		if(this.rotateAngleY != 0.0F) {
			GL11.glRotatef(this.rotateAngleY * (180.0F / (float)Math.PI), 0.0F, 1.0F, 0.0F);
		}

		if(this.rotateAngleX != 0.0F) {
			GL11.glRotatef(this.rotateAngleX * (180.0F / (float)Math.PI), 1.0F, 0.0F, 0.0F);
		}

		if(this.rotateAngleZ != 0.0F) {
			GL11.glRotatef(this.rotateAngleZ * (180.0F / (float)Math.PI), 0.0F, 0.0F, 1.0F);
		}

		GL11.glCallList(this.displayList);
		for(int i = 0; i < this.children.size(); ++i) {
			this.children.get(i).renderWithRotation(scale);
		}

		GL11.glPopMatrix();
	}

	/**
	 * Recursively draws this renderer at its current rotation, but always
	 * relative to the origin (no pivot translate). Used for body parts
	 * that should rotate in place (e.g. a wing flapping about its root).
	 */
	public void postRender(float scale) {
		if(!this.showModel) {
			return;
		}
		if(!this.compiled) {
			this.compileDisplayList(scale);
		}

		if(this.rotateAngleX == 0.0F && this.rotateAngleY == 0.0F && this.rotateAngleZ == 0.0F) {
			GL11.glCallList(this.displayList);
			for(int i = 0; i < this.children.size(); ++i) {
				this.children.get(i).render(scale);
			}
		} else {
			GL11.glPushMatrix();
			GL11.glTranslatef(this.rotationPointX * scale, this.rotationPointY * scale, this.rotationPointZ * scale);
			if(this.rotateAngleZ != 0.0F) {
				GL11.glRotatef(this.rotateAngleZ * (180.0F / (float)Math.PI), 0.0F, 0.0F, 1.0F);
			}

			if(this.rotateAngleY != 0.0F) {
				GL11.glRotatef(this.rotateAngleY * (180.0F / (float)Math.PI), 0.0F, 1.0F, 0.0F);
			}

			if(this.rotateAngleX != 0.0F) {
				GL11.glRotatef(this.rotateAngleX * (180.0F / (float)Math.PI), 1.0F, 0.0F, 0.0F);
			}

			GL11.glCallList(this.displayList);
			for(int i = 0; i < this.children.size(); ++i) {
				this.children.get(i).render(scale);
			}

			GL11.glPopMatrix();
		}
	}
}
