package javax.microedition.m3g;

/** Clean-room JSR-184 {@code Background}. */
public class Background extends Object3D {
    public static final int BORDER = 32;
    public static final int REPEAT = 33;

    private int color = 0;
    private boolean colorClear = true;
    private boolean depthClear = true;
    private Image2D image;
    private int modeX = BORDER;
    private int modeY = BORDER;
    private int cropX, cropY, cropW = -1, cropH = -1;

    public int getColor() { return color; }
    public void setColor(int ARGB) { color = ARGB; }
    public boolean isColorClearEnabled() { return colorClear; }
    public void setColorClearEnable(boolean enable) { colorClear = enable; }
    public boolean isDepthClearEnabled() { return depthClear; }
    public void setDepthClearEnable(boolean enable) { depthClear = enable; }
    public Image2D getImage() { return image; }
    public void setImage(Image2D image) { this.image = image; }
    public void setImageMode(int modeX, int modeY) { this.modeX = modeX; this.modeY = modeY; }
    public int getImageModeX() { return modeX; }
    public int getImageModeY() { return modeY; }
    public void setCrop(int cropX, int cropY, int width, int height) {
        this.cropX = cropX; this.cropY = cropY; this.cropW = width; this.cropH = height;
    }
}
