package util;

/**
 * Fast approximate trigonometry backed by a pre-computed sine table.
 *
 * <p>At runtime {@code sin(x)} and {@code cos(x)} are reduced to a table
 * lookup: the argument is scaled to an integer index in {@link #SIN_TABLE}
 * and the nearest table entry is returned.  This is several orders of
 * magnitude faster than {@link Math#sin} on the JVM at the cost of one
 * 64 KiB static array.
 *
 * <p>The table covers exactly one full rotation (0 .. 2&pi;) quantised into
 * {@link #SIN_TABLE_SIZE} steps.  The scale factor {@link #SIN_INDEX_SCALE}
 * converts a float angle in radians to the integer table index; the mask
 * {@link #SIN_MASK} extracts the low 16 bits to emulate {@code x & 65535}
 * without sign-extension issues on negative values.
 */
public final class MathHelper {
    /** Number of entries in the sine lookup table. */
    private static final int SIN_TABLE_SIZE = 65536;
    /**
     * Mask applied after the float-to-int cast to obtain a table index in
     * [0, 65535] regardless of the sign of the argument.
     */
    private static final int SIN_MASK = 0xFFFF;
    /** Scale factor: {@code index = (int)(radians * SIN_INDEX_SCALE) & SIN_MASK}. */
    private static final float SIN_INDEX_SCALE = (float) (SIN_TABLE_SIZE / (2.0 * Math.PI));
    /** Pre-computed sine of every integer angle from 0 to 2&pi;. */
    private static final float[] SIN_TABLE = new float[SIN_TABLE_SIZE];

    static {
        double stepRadians = 2.0 * Math.PI / SIN_TABLE_SIZE;
        for (int i = 0; i < SIN_TABLE_SIZE; ++i) {
            SIN_TABLE[i] = (float) Math.sin(stepRadians * i);
        }
    }

    /**
     * Fast sine: reduces the argument to a table index and returns the nearest
     * pre-computed entry.
     *
     * @param value angle in radians
     * @return the sine of {@code value}
     */
    public static final float sin(float value) {
        return SIN_TABLE[(int) (value * SIN_INDEX_SCALE) & SIN_MASK];
    }

    /**
     * Fast cosine: equivalent to {@code sin(value + pi/2)} but expressed as an
     * offset table index.
     *
     * @param value angle in radians
     * @return the cosine of {@code value}
     */
    public static final float cos(float value) {
        return SIN_TABLE[(int) (value * SIN_INDEX_SCALE + 16384.0F) & SIN_MASK];
    }

    /**
     * Returns {@code sqrt(value)} cast to float.  Wraps {@link Math#sqrt}
     * purely for API consistency with {@link #sqrt_double}.
     *
     * @param value a non-negative float
     * @return {@code sqrt(value)}
     */
    public static final float sqrt_float(float value) {
        return (float) Math.sqrt(value);
    }

    /**
     * Returns {@code sqrt(value)} cast to float.  The intermediate double
     * precision is preserved for the duration of the sqrt call, giving a more
     * accurate result than {@link #sqrt_float} for large values.
     *
     * @param value a non-negative double
     * @return {@code sqrt(value)}
     */
    public static final float sqrt_double(double value) {
        return (float) Math.sqrt(value);
    }

    /**
     * Floor for a float without allocating a new object on each call.
     *
     * <p>Implementation note: {@code (int) f} truncates toward zero (not
     * toward negative infinity), so a correction is applied when {@code f} is
     * negative and not a whole number.
     *
     * @param value the float to floor
     * @return the greatest integer {@code <= value}
     */
    public static int floor_float(float value) {
        int truncated = (int) value;
        return value < truncated ? truncated - 1 : truncated;
    }

    /**
     * Floor for a double without allocating a new object on each call.
     * Same truncation-correcting logic as {@link #floor_float}.
     *
     * @param value the double to floor
     * @return the greatest integer {@code <= value}
     */
    public static int floor_double(double value) {
        int truncated = (int) value;
        return value < truncated ? truncated - 1 : truncated;
    }

    /**
     * Absolute value for a float.  Written by hand because it is branching on
     * the sign and the JVM's intrinsic can sometimes be inhibited by exception
     * safety concerns in the JIT.
     *
     * @param value the float
     * @return {@code abs(value)}
     */
    public static float abs(float value) {
        return value >= 0.0F ? value : -value;
    }
}
