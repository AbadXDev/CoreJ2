package javax.microedition.m3g;

/** Clean-room JSR-184 {@code Light}. */
public class Light extends Node {
    public static final int AMBIENT = 128;
    public static final int DIRECTIONAL = 129;
    public static final int OMNI = 130;
    public static final int SPOT = 131;

    private int mode = DIRECTIONAL;
    private int color = 0xFFFFFF;
    private float intensity = 1f;

    public int getMode() { return mode; }
    public void setMode(int mode) { this.mode = mode; }
    public int getColor() { return color; }
    public void setColor(int RGB) { color = RGB; }
    public float getIntensity() { return intensity; }
    public void setIntensity(float intensity) { this.intensity = intensity; }
    public void setSpotAngle(float angle) {}
    public float getSpotAngle() { return 45f; }
    public void setSpotExponent(float exponent) {}
    public float getSpotExponent() { return 0; }
    public void setAttenuation(float constant, float linear, float quadratic) {}
}
