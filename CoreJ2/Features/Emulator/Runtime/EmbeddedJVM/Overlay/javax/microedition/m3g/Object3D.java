package javax.microedition.m3g;

/** Clean-room JSR-184 {@code Object3D} base. */
public abstract class Object3D {
    private int userID;
    private Object userObject;

    public Object3D duplicate() {
        return this;
    }

    public Object3D find(int userID) { return this.userID == userID ? this : null; }
    public int getReferences(Object3D[] references) { return 0; }
    public int getUserID() { return userID; }
    public void setUserID(int userID) { this.userID = userID; }
    public Object getUserObject() { return userObject; }
    public void setUserObject(Object userObject) { this.userObject = userObject; }
    public void addAnimationTrack(AnimationTrack animationTrack) {}
    public AnimationTrack getAnimationTrack(int index) { return null; }
    public void removeAnimationTrack(AnimationTrack animationTrack) {}
    public int getAnimationTrackCount() { return 0; }
    public int animate(int time) { return 0; }
}
