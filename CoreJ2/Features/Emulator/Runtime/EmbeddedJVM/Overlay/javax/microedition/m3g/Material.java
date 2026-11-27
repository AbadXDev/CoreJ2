package javax.microedition.m3g;

/** Clean-room JSR-184 {@code Material}. */
public class Material extends Object3D {
    public static final int AMBIENT = 1024;
    public static final int DIFFUSE = 2048;
    public static final int EMISSIVE = 4096;
    public static final int SPECULAR = 8192;
    private boolean vertexColorTracking;
    private float shininess;
    private int ambient = 0x00333333;
    private int diffuse = 0xFFCCCCCC;
    private int emissive;
    private int specular;
    public void setColor(int target, int ARGB) {
        if ((target & AMBIENT) != 0) ambient = ARGB;
        if ((target & DIFFUSE) != 0) diffuse = ARGB;
        if ((target & EMISSIVE) != 0) emissive = ARGB;
        if ((target & SPECULAR) != 0) specular = ARGB;
    }
    public int getColor(int target) {
        if (target == AMBIENT) return ambient;
        if (target == DIFFUSE) return diffuse;
        if (target == EMISSIVE) return emissive;
        if (target == SPECULAR) return specular;
        return 0;
    }
    public float getShininess() { return shininess; }
    public void setShininess(float shininess) { this.shininess = shininess; }
    public boolean isVertexColorTrackingEnabled() { return vertexColorTracking; }
    public void setVertexColorTrackingEnable(boolean enable) { vertexColorTracking = enable; }
}
