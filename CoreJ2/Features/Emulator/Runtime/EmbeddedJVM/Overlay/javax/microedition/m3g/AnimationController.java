package javax.microedition.m3g;

/** Clean-room JSR-184 {@code AnimationController} stub. */
public class AnimationController extends Object3D {
    public void setActiveInterval(int start, int end) {}
    public int getActiveIntervalStart() { return 0; }
    public int getActiveIntervalEnd() { return 0; }
    public void setSpeed(float speed, int worldTime) {}
    public float getSpeed() { return 1f; }
    public void setPosition(float sequenceTime, int worldTime) {}
    public float getPosition(int worldTime) { return 0; }
    public void setWeight(float weight) {}
    public float getWeight() { return 1f; }
    public int getRefWorldTime() { return 0; }
}
