package javax.microedition.m3g;

/** Clean-room JSR-184 {@code CompositingMode}. */
public class CompositingMode extends Object3D {
    public static final int ALPHA = 64;
    public static final int ALPHA_ADD = 65;
    public static final int MODULATE = 66;
    public static final int MODULATE_X2 = 67;
    public static final int REPLACE = 68;

    private int blending = REPLACE;
    private float alphaThreshold;
    private boolean depthTest = true;
    private boolean depthWrite = true;
    private boolean colorWrite = true;
    private boolean alphaWrite = true;
    private float depthOffsetFactor;
    private float depthOffsetUnits;

    public int getBlending() { return blending; }
    public void setBlending(int blending) { this.blending = blending; }
    public float getAlphaThreshold() { return alphaThreshold; }
    public void setAlphaThreshold(float threshold) { alphaThreshold = threshold; }
    public boolean isDepthTestEnabled() { return depthTest; }
    public void setDepthTestEnable(boolean enable) { depthTest = enable; }
    public boolean isDepthWriteEnabled() { return depthWrite; }
    public void setDepthWriteEnable(boolean enable) { depthWrite = enable; }
    public boolean isColorWriteEnabled() { return colorWrite; }
    public void setColorWriteEnable(boolean enable) { colorWrite = enable; }
    public boolean isAlphaWriteEnabled() { return alphaWrite; }
    public void setAlphaWriteEnable(boolean enable) { alphaWrite = enable; }
    public void setDepthOffset(float factor, float units) { depthOffsetFactor = factor; depthOffsetUnits = units; }
    public float getDepthOffsetFactor() { return depthOffsetFactor; }
    public float getDepthOffsetUnits() { return depthOffsetUnits; }
}
