package javax.microedition.m3g;

/** CoreJ2 overlay — translation/rotation/scale composite. */
public abstract class Transformable extends Object3D {
    private final Transform local = new Transform();
    private float tx, ty, tz;
    private float sx = 1, sy = 1, sz = 1;
    private float angle, ax = 0, ay = 1, az = 0;

    public void getCompositeTransform(Transform transform) {
        Transform c = new Transform();
        c.setIdentity();
        c.postTranslate(tx, ty, tz);
        if (angle != 0) c.postRotate(angle, ax, ay, az);
        c.postScale(sx, sy, sz);
        c.postMultiply(local);
        if (transform != null) transform.set(c);
    }

    public void getOrientation(float[] angleAxis) {
        if (angleAxis != null && angleAxis.length >= 4) {
            angleAxis[0] = angle; angleAxis[1] = ax; angleAxis[2] = ay; angleAxis[3] = az;
        }
    }

    public void getScale(float[] xyz) {
        if (xyz != null && xyz.length >= 3) { xyz[0] = sx; xyz[1] = sy; xyz[2] = sz; }
    }

    public void getTransform(Transform transform) {
        if (transform != null) transform.set(local);
    }

    public void getTranslation(float[] xyz) {
        if (xyz != null && xyz.length >= 3) { xyz[0] = tx; xyz[1] = ty; xyz[2] = tz; }
    }

    public void postRotate(float angle, float ax, float ay, float az) {
        local.postRotate(angle, ax, ay, az);
    }

    public void preRotate(float angle, float ax, float ay, float az) {
        Transform r = new Transform();
        r.setIdentity();
        r.postRotate(angle, ax, ay, az);
        r.postMultiply(local);
        local.set(r);
    }

    public void scale(float sx, float sy, float sz) {
        this.sx *= sx; this.sy *= sy; this.sz *= sz;
    }

    public void setOrientation(float angle, float ax, float ay, float az) {
        this.angle = angle; this.ax = ax; this.ay = ay; this.az = az;
    }

    public void setScale(float sx, float sy, float sz) {
        this.sx = sx; this.sy = sy; this.sz = sz;
    }

    public void setTransform(Transform transform) {
        if (transform != null) local.set(transform);
        else local.setIdentity();
    }

    public void setTranslation(float tx, float ty, float tz) {
        this.tx = tx; this.ty = ty; this.tz = tz;
    }

    public void translate(float tx, float ty, float tz) {
        this.tx += tx; this.ty += ty; this.tz += tz;
    }
}
