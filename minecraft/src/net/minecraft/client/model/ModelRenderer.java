package net.minecraft.client.model;

import net.minecraft.client.render.Tessellator;
import net.minecraft.game.physics.Vec3D;
import org.lwjgl.opengl.GL11;

/**
 * A single box-shaped part of an entity model (a head, a leg, a torso...). A
 * box is defined by its eight corner vertices with a texture offset, and is
 * rendered as six textured quads. It can be moved to a pivot
 * ({@code rotationPoint*}) and rotated about that pivot ({@code rotateAngle*}).
 * The whole box can be cached as an OpenGL display list and optionally mirrored.
 */
public final class ModelRenderer {
	private PositionTextureVertex[] corners;
	private TexturedQuad[] faces;
	private int textureOffsetX;
	private int textureOffsetY;
	private float rotationPointX;
	private float rotationPointY;
	private float rotationPointZ;
	public float rotateAngleX;
	public float rotateAngleY;
	public float rotateAngleZ;
	private boolean compiled = false;
	private int displayList = 0;
	public boolean mirror = false;
	public boolean showModel = true;

	/**
	 * @param textureOffsetX x coordinate of this box's region on the texture
	 * @param textureOffsetY y coordinate of this box's region on the texture
	 */
	public ModelRenderer(int textureOffsetX, int textureOffsetY) {
		this.textureOffsetX = textureOffsetX;
		this.textureOffsetY = textureOffsetY;
	}

	/**
	 * Builds the six faces of the box from the eight corners spanning from
	 * (x, y, z) to (x + width, y + height, z + depth), inflated by {@code expansion}
	 * on every side. When {@code mirror} is set the box is built mirrored and
	 * every face's winding is flipped so it still faces outward.
	 *
	 * @param x, y, z    origin of the box (smallest corner), in model units
	 * @param width      box size along X
	 * @param height     box size along Y
	 * @param depth      box size along Z
	 * @param expansion  extra size added on all six faces
	 */
	public final void addBox(float x, float y, float z, int width, int height, int depth, float expansion) {
		this.corners = new PositionTextureVertex[8];
		this.faces = new TexturedQuad[6];
		float endX = x + (float)width;
		float endY = y + (float)height;
		float endZ = z + (float)depth;
		x -= expansion;
		y -= expansion;
		z -= expansion;
		endX += expansion;
		endY += expansion;
		endZ += expansion;
		if(this.mirror) {
			expansion = endX;
			endX = x;
			x = expansion;
		}

		// The eight corners of the box; each name records the min/max extent
		// of the X, Y and Z axes the corner sits at.
		PositionTextureVertex minXMinYMinZ = new PositionTextureVertex(x, y, z, 0.0F, 0.0F);
		PositionTextureVertex maxXMinYMinZ = new PositionTextureVertex(endX, y, z, 0.0F, 8.0F);
		PositionTextureVertex maxXMaxYMinZ = new PositionTextureVertex(endX, endY, z, 8.0F, 8.0F);
		PositionTextureVertex minXMaxYMinZ = new PositionTextureVertex(x, endY, z, 8.0F, 0.0F);
		PositionTextureVertex minXMinYMaxZ = new PositionTextureVertex(x, y, endZ, 0.0F, 0.0F);
		PositionTextureVertex maxXMinYMaxZ = new PositionTextureVertex(endX, y, endZ, 0.0F, 8.0F);
		PositionTextureVertex maxXMaxYMaxZ = new PositionTextureVertex(endX, endY, endZ, 8.0F, 8.0F);
		PositionTextureVertex minXMaxYMaxZ = new PositionTextureVertex(x, endY, endZ, 8.0F, 0.0F);
		this.corners[0] = minXMinYMinZ;
		this.corners[1] = maxXMinYMinZ;
		this.corners[2] = maxXMaxYMinZ;
		this.corners[3] = minXMaxYMinZ;
		this.corners[4] = minXMinYMaxZ;
		this.corners[5] = maxXMinYMaxZ;
		this.corners[6] = maxXMaxYMaxZ;
		this.corners[7] = minXMaxYMaxZ;
		// The six quad faces, each mapping onto its rectangle of the texture:
		// +X, -X, +Y, -Y, +Z, -Z. Each face offsets its UVs by the box texture
		// position and by the relevant box dimensions.
		this.faces[0] = new TexturedQuad(new PositionTextureVertex[]{maxXMinYMaxZ, maxXMinYMinZ, maxXMaxYMinZ, maxXMaxYMaxZ}, this.textureOffsetX + depth + width, this.textureOffsetY + depth, this.textureOffsetX + depth + width + depth, this.textureOffsetY + depth + height);
		this.faces[1] = new TexturedQuad(new PositionTextureVertex[]{minXMinYMinZ, minXMinYMaxZ, minXMaxYMaxZ, minXMaxYMinZ}, this.textureOffsetX, this.textureOffsetY + depth, this.textureOffsetX + depth, this.textureOffsetY + depth + height);
		this.faces[2] = new TexturedQuad(new PositionTextureVertex[]{maxXMinYMaxZ, minXMinYMaxZ, minXMinYMinZ, maxXMinYMinZ}, this.textureOffsetX + depth, this.textureOffsetY, this.textureOffsetX + depth + width, this.textureOffsetY + depth);
		this.faces[3] = new TexturedQuad(new PositionTextureVertex[]{maxXMaxYMinZ, minXMaxYMinZ, minXMaxYMaxZ, maxXMaxYMaxZ}, this.textureOffsetX + depth + width, this.textureOffsetY, this.textureOffsetX + depth + width + width, this.textureOffsetY + depth);
		this.faces[4] = new TexturedQuad(new PositionTextureVertex[]{maxXMinYMinZ, minXMinYMinZ, minXMaxYMinZ, maxXMaxYMinZ}, this.textureOffsetX + depth, this.textureOffsetY + depth, this.textureOffsetX + depth + width, this.textureOffsetY + depth + height);
		this.faces[5] = new TexturedQuad(new PositionTextureVertex[]{minXMinYMaxZ, maxXMinYMaxZ, maxXMaxYMaxZ, minXMaxYMaxZ}, this.textureOffsetX + depth + width + depth, this.textureOffsetY + depth, this.textureOffsetX + depth + width + depth + width, this.textureOffsetY + depth + height);
		if(this.mirror) {
			// When mirrored, reverse each face's vertex order so it still faces
			// outward instead of becoming invisible from its normal side.
			for(int faceIndex = 0; faceIndex < this.faces.length; ++faceIndex) {
				TexturedQuad face = this.faces[faceIndex];
				PositionTextureVertex[] reversed = new PositionTextureVertex[face.vertexPositions.length];

				for(width = 0; width < face.vertexPositions.length; ++width) {
					reversed[width] = face.vertexPositions[face.vertexPositions.length - width - 1];
				}

				face.vertexPositions = reversed;
			}
		}

	}

	/**
	 * Moves the box's pivot point (the point it is rotated around) to (x, y, z).
	 */
	public final void setRotationPoint(float rotationPointX, float rotationPointY, float rotationPointZ) {
		this.rotationPointX = rotationPointX;
		this.rotationPointY = rotationPointY;
		this.rotationPointZ = rotationPointZ;
	}

	/**
	 * Draws the box, scaled by {@code scale}. On the first call the corners are
	 * compiled into a display list for speed; afterward the box is positioned at
	 * its pivot, rotated about it in XYZ order, and drawn from the cached list.
	 *
	 * @param scale uniform scale applied to the box and its pivot
	 */
	public final void render(float scale) {
		if(this.showModel) {
			if(!this.compiled) {
				this.displayList = GL11.glGenLists(1);
				GL11.glNewList(this.displayList, GL11.GL_COMPILE);
				Tessellator tessellator = Tessellator.instance;

				for(int faceIndex = 0; faceIndex < this.faces.length; ++faceIndex) {
					tessellator.startDrawingQuads();
					TexturedQuad face = this.faces[faceIndex];
					// Two edges of the quad; their cross product is the face
					// normal (a unit vector perpendicular to the surface).
					Vec3D edgeA = face.vertexPositions[1].vector3D.subtract(face.vertexPositions[0].vector3D).normalize();
					Vec3D edgeB = face.vertexPositions[1].vector3D.subtract(face.vertexPositions[2].vector3D).normalize();
					Vec3D normal = (new Vec3D(edgeA.yCoord * edgeB.zCoord - edgeA.zCoord * edgeB.yCoord, edgeA.zCoord * edgeB.xCoord - edgeA.xCoord * edgeB.zCoord, edgeA.xCoord * edgeB.yCoord - edgeA.yCoord * edgeB.xCoord)).normalize();
					Tessellator.setNormal((float)(-normal.xCoord), (float)(-normal.yCoord), (float)(-normal.zCoord));

					for(int vertexIndex = 0; vertexIndex < 4; ++vertexIndex) {
						PositionTextureVertex vertex = face.vertexPositions[vertexIndex];
						tessellator.addVertexWithUV((double)((float)vertex.vector3D.xCoord * scale), (double)((float)vertex.vector3D.yCoord * scale), (double)((float)vertex.vector3D.zCoord * scale), (double)vertex.texturePositionX, (double)vertex.texturePositionY);
					}

					tessellator.draw();
				}

				GL11.glEndList();
				this.compiled = true;
			}

			if(this.rotateAngleX == 0.0F && this.rotateAngleY == 0.0F && this.rotateAngleZ == 0.0F) {
				// Unrotated: draw at the pivot directly (or at the origin).
				if(this.rotationPointX == 0.0F && this.rotationPointY == 0.0F && this.rotationPointZ == 0.0F) {
					GL11.glCallList(this.displayList);
				} else {
					GL11.glTranslatef(this.rotationPointX * scale, this.rotationPointY * scale, this.rotationPointZ * scale);
					GL11.glCallList(this.displayList);
					GL11.glTranslatef(-this.rotationPointX * scale, -this.rotationPointY * scale, -this.rotationPointZ * scale);
				}
			} else {
				// Rotated: translate to the pivot, rotate (Z then Y then X), draw,
				// then restore the matrix.
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
				GL11.glPopMatrix();
			}
		}
	}
}
