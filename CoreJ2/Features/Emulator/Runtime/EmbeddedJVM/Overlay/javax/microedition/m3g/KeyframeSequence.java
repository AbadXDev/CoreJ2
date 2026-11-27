package javax.microedition.m3g;

/** Clean-room JSR-184 {@code KeyframeSequence} stub. */
public class KeyframeSequence extends Object3D {
    public static final int LINEAR = 176;
    public static final int SLERP = 177;
    public static final int SPLINE = 178;
    public static final int SQUAD = 179;
    public static final int STEP = 180;
    public static final int CONSTANT = 192;
    public static final int LOOP = 193;

    public KeyframeSequence(int numKeyframes, int numComponents, int interpolation) {}
    public void setKeyframe(int index, int time, float[] value) {}
    public void setValidRange(int first, int last) {}
    public void setDuration(int duration) {}
    public int getDuration() { return 0; }
    public void setRepeatMode(int mode) {}
    public int getRepeatMode() { return CONSTANT; }
}
