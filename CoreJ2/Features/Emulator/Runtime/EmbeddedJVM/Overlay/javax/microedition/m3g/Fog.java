package javax.microedition.m3g;

/** Clean-room JSR-184 {@code Fog}. */
public class Fog extends Object3D {
    public static final int EXPONENTIAL = 80;
    public static final int LINEAR = 81;
    private int mode = LINEAR;
    private int color;
    private float density = 1f;
    private float near = 0f;
    private float far = 1f;
    public int getMode() { return mode; }
    public void setMode(int mode) { this.mode = mode; }
    public int getColor() { return color; }
    public void setColor(int RGB) { color = RGB; }
    public float getDensity() { return density; }
    public void setDensity(float density) { this.density = density; }
    public void setLinear(float near, float far) { this.near = near; this.far = far; }
    public float getNearDistance() { return near; }
    public float getFarDistance() { return far; }
}
