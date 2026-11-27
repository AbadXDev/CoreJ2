package javax.microedition.m3g;

/** Clean-room JSR-184 {@code PolygonMode}. */
public class PolygonMode extends Object3D {
    public static final int CULL_BACK = 160;
    public static final int CULL_FRONT = 161;
    public static final int CULL_NONE = 162;
    public static final int SHADE_FLAT = 164;
    public static final int SHADE_SMOOTH = 165;
    public static final int WINDING_CCW = 168;
    public static final int WINDING_CW = 169;

    private int culling = CULL_BACK;
    private int shading = SHADE_SMOOTH;
    private int winding = WINDING_CCW;
    private boolean perspectiveCorrection = true;
    private boolean localCameraLighting;
    private boolean twoSidedLighting;

    public int getCulling() { return culling; }
    public void setCulling(int culling) { this.culling = culling; }
    public int getShading() { return shading; }
    public void setShading(int shading) { this.shading = shading; }
    public int getWinding() { return winding; }
    public void setWinding(int winding) { this.winding = winding; }
    public boolean isPerspectiveCorrectionEnabled() { return perspectiveCorrection; }
    public void setPerspectiveCorrectionEnable(boolean enable) { perspectiveCorrection = enable; }
    public boolean isLocalCameraLightingEnabled() { return localCameraLighting; }
    public void setLocalCameraLightingEnable(boolean enable) { localCameraLighting = enable; }
    public boolean isTwoSidedLightingEnabled() { return twoSidedLighting; }
    public void setTwoSidedLightingEnable(boolean enable) { twoSidedLighting = enable; }
}
