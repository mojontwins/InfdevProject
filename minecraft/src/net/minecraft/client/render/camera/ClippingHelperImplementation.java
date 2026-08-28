package net.minecraft.client.render.camera;

import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import util.MathHelper;

/**
 * Builds the six view frustum planes from the current GL projection and model
 * view matrices (the classic Gribb-Hartmann extraction). Planes are stored in
 * a [[a,b,c,d]x6] structure where a point p is inside the frustum when
 * a*px + b*py + c*pz + d <= 0 for every plane.
 */
public final class ClippingHelperImplementation extends ClippingHelper {
	private static final ClippingHelperImplementation instance = new ClippingHelperImplementation();
	private final FloatBuffer projectionMatrixBuffer = BufferUtils.createFloatBuffer(16);
	private final FloatBuffer modelViewMatrixBuffer = BufferUtils.createFloatBuffer(16);
	private final FloatBuffer floatBuffer16 = BufferUtils.createFloatBuffer(16);

	/**
	 * Reads the current matrices and recomputes the combined clipping matrix
	 * (projection x modelview), then extracts and normalizes the six planes.
	 */
	public static ClippingHelper init() {
		ClippingHelperImplementation helper = instance;
		helper.projectionMatrixBuffer.clear();
		helper.modelViewMatrixBuffer.clear();
		helper.floatBuffer16.clear();
		GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, helper.projectionMatrixBuffer);
		GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, helper.modelViewMatrixBuffer);
		helper.projectionMatrixBuffer.flip().limit(16);
		helper.projectionMatrixBuffer.get(helper.projectionMatrix);
		helper.modelViewMatrixBuffer.flip().limit(16);
		helper.modelViewMatrixBuffer.get(helper.modelViewMatrix); 

		// clippingMatrix = projection x modelview in the same flat layout the
		// original produced with its explicit unrolled multiplications. Element j
		// combines modelview row j>>2 with projection column j&3: for column k,
		// M[(j>>2)*4+k] * P[(j&3)+4*k]. (Row-major P column j&3 == flat index
		// (j&3) + 4k, NOT 4*(j&3) + k.)
		for(int j = 0; j < 16; ++j) {
			int projectColumn = j & 3;
			int modelViewRow = j >> 2;
			float product = 0.0F;
			for(int k = 0; k < 4; ++k) {
				product += helper.modelViewMatrix[modelViewRow * 4 + k] * helper.projectionMatrix[projectColumn + 4 * k];
			}
			helper.clippingMatrix[j] = product;
		}

		// Each frustum plane combines row 3 (the "w" row of the clipped space)
		// with row 0/1/2, once with a + and once with a - sign. The sign pattern
		// (-, +, +, -, -, +) pairs the two boundaries of each axis.
		extractPlane(helper, 0, -1);
		extractPlane(helper, 1, 1);
		extractPlane(helper, 2, 1);
		extractPlane(helper, 3, -1);
		extractPlane(helper, 4, -1);
		extractPlane(helper, 5, 1);
		return instance;
	}

	/**
	 * Extracts and normalizes one plane: plane[n] = clip[n] + sign * clip[srcR+n]
	 * for n in {0,4,8,12}, where srcR = planeId >> 1.
	 */
	private static void extractPlane(ClippingHelperImplementation helper, int planeId, int sourceRowSign) {
		float[] plane = helper.frustrum[planeId];
		float[] clipping = helper.clippingMatrix;
		int sourceRow = planeId >> 1;
		plane[0] = clipping[3] + sourceRowSign * clipping[sourceRow];
		plane[1] = clipping[7] + sourceRowSign * clipping[4 + sourceRow];
		plane[2] = clipping[11] + sourceRowSign * clipping[8 + sourceRow];
		plane[3] = clipping[15] + sourceRowSign * clipping[12 + sourceRow];

		// Normalize so the plane distance (d) is in world units.
		float length = MathHelper.sqrt_float(plane[0] * plane[0] + plane[1] * plane[1] + plane[2] * plane[2]);
		plane[0] /= length;
		plane[1] /= length;
		plane[2] /= length;
		plane[3] /= length;
	}
}