package javax.microedition.m3g;

/** CoreJ2 overlay — real 4×4 column-major transforms. */
public class Transform {
    private final float[] m = new float[16];

    public Transform() { setIdentity(); }

    public Transform(Transform transform) {
        set(transform);
    }

    public void get(float[] matrix) {
        System.arraycopy(m, 0, matrix, 0, 16);
    }

    public void invert() {
        // Adjugate-based invert for affine; fall back to identity on failure.
        float[] r = new float[16];
        if (!invert4x4(m, r)) {
            setIdentity();
            return;
        }
        System.arraycopy(r, 0, m, 0, 16);
    }

    public void postMultiply(Transform transform) {
        if (transform == null) return;
        float[] a = m.clone();
        float[] b = transform.m;
        mul(a, b, m);
    }

    public void postRotate(float angle, float ax, float ay, float az) {
        Transform r = new Transform();
        r.setIdentity();
        float rad = angle * (float) (Math.PI / 180.0);
        float c = (float) Math.cos(rad);
        float s = (float) Math.sin(rad);
        float len = (float) Math.sqrt(ax * ax + ay * ay + az * az);
        if (len < 1e-6f) return;
        ax /= len; ay /= len; az /= len;
        float t = 1f - c;
        r.m[0] = t * ax * ax + c;
        r.m[4] = t * ax * ay - s * az;
        r.m[8] = t * ax * az + s * ay;
        r.m[1] = t * ax * ay + s * az;
        r.m[5] = t * ay * ay + c;
        r.m[9] = t * ay * az - s * ax;
        r.m[2] = t * ax * az - s * ay;
        r.m[6] = t * ay * az + s * ax;
        r.m[10] = t * az * az + c;
        postMultiply(r);
    }

    public void postRotateQuat(float qx, float qy, float qz, float qw) {
        Transform r = new Transform();
        r.setIdentity();
        float xx = qx * qx, yy = qy * qy, zz = qz * qz;
        float xy = qx * qy, xz = qx * qz, yz = qy * qz;
        float wx = qw * qx, wy = qw * qy, wz = qw * qz;
        r.m[0] = 1 - 2 * (yy + zz);
        r.m[4] = 2 * (xy - wz);
        r.m[8] = 2 * (xz + wy);
        r.m[1] = 2 * (xy + wz);
        r.m[5] = 1 - 2 * (xx + zz);
        r.m[9] = 2 * (yz - wx);
        r.m[2] = 2 * (xz - wy);
        r.m[6] = 2 * (yz + wx);
        r.m[10] = 1 - 2 * (xx + yy);
        postMultiply(r);
    }

    public void postScale(float sx, float sy, float sz) {
        Transform t = new Transform();
        t.setIdentity();
        t.m[0] = sx; t.m[5] = sy; t.m[10] = sz;
        postMultiply(t);
    }

    public void postTranslate(float tx, float ty, float tz) {
        Transform t = new Transform();
        t.setIdentity();
        t.m[12] = tx; t.m[13] = ty; t.m[14] = tz;
        postMultiply(t);
    }

    public void set(float[] matrix) {
        System.arraycopy(matrix, 0, m, 0, 16);
    }

    public void set(Transform transform) {
        if (transform != null) {
            System.arraycopy(transform.m, 0, m, 0, 16);
        }
    }

    public void setIdentity() {
        for (int i = 0; i < 16; i++) m[i] = 0;
        m[0] = m[5] = m[10] = m[15] = 1;
    }

    public void transform(float[] vectors) {
        for (int i = 0; i + 3 < vectors.length; i += 4) {
            float x = vectors[i], y = vectors[i + 1], z = vectors[i + 2], w = vectors[i + 3];
            vectors[i]     = m[0] * x + m[4] * y + m[8] * z + m[12] * w;
            vectors[i + 1] = m[1] * x + m[5] * y + m[9] * z + m[13] * w;
            vectors[i + 2] = m[2] * x + m[6] * y + m[10] * z + m[14] * w;
            vectors[i + 3] = m[3] * x + m[7] * y + m[11] * z + m[15] * w;
        }
    }

    public void transform(VertexArray in, float[] out, boolean W) {
        if (in == null || out == null) return;
        int n = in.getVertexCount();
        int c = in.getComponentCount();
        float[] tmp = new float[Math.max(4, c)];
        for (int i = 0; i < n; i++) {
            in.getFloat(i, tmp);
            float x = c > 0 ? tmp[0] : 0;
            float y = c > 1 ? tmp[1] : 0;
            float z = c > 2 ? tmp[2] : 0;
            float w = W ? 1f : (c > 3 ? tmp[3] : 1f);
            out[i * 4]     = m[0] * x + m[4] * y + m[8] * z + m[12] * w;
            out[i * 4 + 1] = m[1] * x + m[5] * y + m[9] * z + m[13] * w;
            out[i * 4 + 2] = m[2] * x + m[6] * y + m[10] * z + m[14] * w;
            out[i * 4 + 3] = m[3] * x + m[7] * y + m[11] * z + m[15] * w;
        }
    }

    public void transpose() {
        float[] t = m.clone();
        m[1] = t[4]; m[2] = t[8]; m[3] = t[12];
        m[4] = t[1]; m[6] = t[9]; m[7] = t[13];
        m[8] = t[2]; m[9] = t[6]; m[11] = t[14];
        m[12] = t[3]; m[13] = t[7]; m[14] = t[11];
    }

    float[] matrix() { return m; }

    private static void mul(float[] a, float[] b, float[] out) {
        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 4; row++) {
                out[col * 4 + row] =
                    a[0 * 4 + row] * b[col * 4 + 0]
                  + a[1 * 4 + row] * b[col * 4 + 1]
                  + a[2 * 4 + row] * b[col * 4 + 2]
                  + a[3 * 4 + row] * b[col * 4 + 3];
            }
        }
    }

    private static boolean invert4x4(float[] m, float[] invOut) {
        float[] inv = new float[16];
        inv[0] = m[5] * m[10] * m[15] - m[5] * m[11] * m[14] - m[9] * m[6] * m[15]
               + m[9] * m[7] * m[14] + m[13] * m[6] * m[11] - m[13] * m[7] * m[10];
        inv[4] = -m[4] * m[10] * m[15] + m[4] * m[11] * m[14] + m[8] * m[6] * m[15]
               - m[8] * m[7] * m[14] - m[12] * m[6] * m[11] + m[12] * m[7] * m[10];
        inv[8] = m[4] * m[9] * m[15] - m[4] * m[11] * m[13] - m[8] * m[5] * m[15]
               + m[8] * m[7] * m[13] + m[12] * m[5] * m[11] - m[12] * m[7] * m[9];
        inv[12] = -m[4] * m[9] * m[14] + m[4] * m[10] * m[13] + m[8] * m[5] * m[14]
                - m[8] * m[6] * m[13] - m[12] * m[5] * m[10] + m[12] * m[6] * m[9];
        inv[1] = -m[1] * m[10] * m[15] + m[1] * m[11] * m[14] + m[9] * m[2] * m[15]
               - m[9] * m[3] * m[14] - m[13] * m[2] * m[11] + m[13] * m[3] * m[10];
        inv[5] = m[0] * m[10] * m[15] - m[0] * m[11] * m[14] - m[8] * m[2] * m[15]
               + m[8] * m[3] * m[14] + m[12] * m[2] * m[11] - m[12] * m[3] * m[10];
        inv[9] = -m[0] * m[9] * m[15] + m[0] * m[11] * m[13] + m[8] * m[1] * m[15]
               - m[8] * m[3] * m[13] - m[12] * m[1] * m[11] + m[12] * m[3] * m[9];
        inv[13] = m[0] * m[9] * m[14] - m[0] * m[10] * m[13] - m[8] * m[1] * m[14]
                + m[8] * m[2] * m[13] + m[12] * m[1] * m[10] - m[12] * m[2] * m[9];
        inv[2] = m[1] * m[6] * m[15] - m[1] * m[7] * m[14] - m[5] * m[2] * m[15]
               + m[5] * m[3] * m[14] + m[13] * m[2] * m[7] - m[13] * m[3] * m[6];
        inv[6] = -m[0] * m[6] * m[15] + m[0] * m[7] * m[14] + m[4] * m[2] * m[15]
               - m[4] * m[3] * m[14] - m[12] * m[2] * m[7] + m[12] * m[3] * m[6];
        inv[10] = m[0] * m[5] * m[15] - m[0] * m[7] * m[13] - m[4] * m[1] * m[15]
                + m[4] * m[3] * m[13] + m[12] * m[1] * m[7] - m[12] * m[3] * m[5];
        inv[14] = -m[0] * m[5] * m[14] + m[0] * m[6] * m[13] + m[4] * m[1] * m[14]
                - m[4] * m[2] * m[13] - m[12] * m[1] * m[6] + m[12] * m[2] * m[5];
        inv[3] = -m[1] * m[6] * m[11] + m[1] * m[7] * m[10] + m[5] * m[2] * m[11]
               - m[5] * m[3] * m[10] - m[9] * m[2] * m[7] + m[9] * m[3] * m[6];
        inv[7] = m[0] * m[6] * m[11] - m[0] * m[7] * m[10] - m[4] * m[2] * m[11]
               + m[4] * m[3] * m[10] + m[8] * m[2] * m[7] - m[8] * m[3] * m[6];
        inv[11] = -m[0] * m[5] * m[11] + m[0] * m[7] * m[9] + m[4] * m[1] * m[11]
                - m[4] * m[3] * m[9] - m[8] * m[1] * m[7] + m[8] * m[3] * m[5];
        inv[15] = m[0] * m[5] * m[10] - m[0] * m[6] * m[9] - m[4] * m[1] * m[10]
                + m[4] * m[2] * m[9] + m[8] * m[1] * m[6] - m[8] * m[2] * m[5];
        float det = m[0] * inv[0] + m[1] * inv[4] + m[2] * inv[8] + m[3] * inv[12];
        if (Math.abs(det) < 1e-8f) return false;
        det = 1f / det;
        for (int i = 0; i < 16; i++) invOut[i] = inv[i] * det;
        return true;
    }
}
