package javax.microedition.m3g;

/** CoreJ2 overlay — perspective/parallel projection state. */
public class Camera extends Node {
    public static final int GENERIC = 48;
    public static final int PARALLEL = 49;
    public static final int PERSPECTIVE = 50;

    private int mode = PERSPECTIVE;
    private float fovy = 60f, aspect = 1f, near = 0.1f, far = 100f;
    private final Transform generic = new Transform();

    public Camera() { setRenderingEnable(true); }

    public int getProjection(float[] params) {
        if (params != null && params.length >= 4) {
            params[0] = fovy; params[1] = aspect; params[2] = near; params[3] = far;
        }
        return mode;
    }

    public int getProjection(Transform transform) {
        if (transform == null) return mode;
        if (mode == GENERIC) {
            transform.set(generic);
            return GENERIC;
        }
        transform.setIdentity();
        if (mode == PERSPECTIVE) {
            float f = 1f / (float) Math.tan((fovy * Math.PI / 180.0) / 2.0);
            float[] m = new float[16];
            for (int i = 0; i < 16; i++) m[i] = 0;
            m[0] = f / aspect;
            m[5] = f;
            m[10] = (far + near) / (near - far);
            m[11] = -1f;
            m[14] = (2f * far * near) / (near - far);
            transform.set(m);
        } else {
            // parallel: simple orthographic
            float[] m = new float[16];
            for (int i = 0; i < 16; i++) m[i] = 0;
            m[0] = 1f / aspect; m[5] = 1f; m[10] = -2f / (far - near); m[15] = 1f;
            m[14] = -(far + near) / (far - near);
            transform.set(m);
        }
        return mode;
    }

    public void setGeneric(Transform transform) {
        mode = GENERIC;
        if (transform != null) generic.set(transform);
    }

    public void setParallel(float fovy, float aspectRatio, float near, float far) {
        mode = PARALLEL; this.fovy = fovy; this.aspect = aspectRatio; this.near = near; this.far = far;
    }

    public void setPerspective(float fovy, float aspectRatio, float near, float far) {
        mode = PERSPECTIVE; this.fovy = fovy; this.aspect = aspectRatio; this.near = near; this.far = far;
    }
}
