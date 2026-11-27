package javax.microedition.m3g;

/** Clean-room JSR-184 {@code World}. */
public class World extends Group {
    private Camera activeCamera;
    private Background background;

    public Camera getActiveCamera() { return activeCamera; }
    public void setActiveCamera(Camera camera) { activeCamera = camera; }
    public Background getBackground() { return background; }
    public void setBackground(Background background) { this.background = background; }
}
