package javax.microedition.m3g;

/** CoreJ2 overlay — rendering enabled by default. */
public abstract class Node extends Transformable {
    public static final int NONE = 144;
    public static final int ORIGIN = 145;
    public static final int X_AXIS = 146;
    public static final int Y_AXIS = 147;
    public static final int Z_AXIS = 148;

    private Node alignRef;
    private float alphaFactor = 1f;
    private boolean picking = true;
    private boolean rendering = true;
    private int scope = -1;
    private Node parent;

    public void align(Node reference) {}
    public Node getAlignmentReference(int axis) { return alignRef; }
    public int getAlignmentTarget(int axis) { return 0; }
    public float getAlphaFactor() { return alphaFactor; }
    public Node getParent() { return parent; }
    public int getScope() { return scope; }
    public boolean getTransformTo(Node target, Transform transform) { return false; }
    public boolean isPickingEnabled() { return picking; }
    public boolean isRenderingEnabled() { return rendering; }
    public void setAlignment(Node zRef, int zTarget, Node yRef, int yTarget) {}
    public void setAlphaFactor(float value) { alphaFactor = value; }
    public void setPickingEnable(boolean enable) { picking = enable; }
    public void setRenderingEnable(boolean enable) { rendering = enable; }
    public void setScope(int value) { scope = value; }
    void setParent(Node p) { parent = p; }
}
