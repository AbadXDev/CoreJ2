package javax.microedition.m3g;

/** Clean-room JSR-184 {@code Sprite3D}. */
public class Sprite3D extends Node {
    private Image2D image;
    private Appearance appearance;
    private boolean scaled;
    private int cropX, cropY, cropW, cropH;

    public Sprite3D(boolean scaled, Image2D image, Appearance appearance) {
        this.scaled = scaled;
        this.image = image;
        this.appearance = appearance;
        if (image != null) {
            cropW = image.getWidth();
            cropH = image.getHeight();
        }
        setRenderingEnable(true);
    }

    public Appearance getAppearance() { return appearance; }
    public void setAppearance(Appearance appearance) { this.appearance = appearance; }
    public Image2D getImage() { return image; }
    public void setImage(Image2D image) { this.image = image; }
    public boolean isScaled() { return scaled; }
    public void setCrop(int cropX, int cropY, int width, int height) {
        this.cropX = cropX; this.cropY = cropY; this.cropW = width; this.cropH = height;
    }
    public int getCropX() { return cropX; }
    public int getCropY() { return cropY; }
    public int getCropWidth() { return cropW; }
    public int getCropHeight() { return cropH; }
}
