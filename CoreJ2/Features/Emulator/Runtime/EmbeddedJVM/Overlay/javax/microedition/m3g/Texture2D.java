package javax.microedition.m3g;

/** CoreJ2 overlay — keeps Image2D reference from constructor. */
public class Texture2D extends Transformable {
    public static final int FILTER_BASE_LEVEL = 208;
    public static final int FILTER_LINEAR = 209;
    public static final int FILTER_NEAREST = 210;
    public static final int FUNC_ADD = 224;
    public static final int FUNC_BLEND = 225;
    public static final int FUNC_DECAL = 226;
    public static final int FUNC_MODULATE = 227;
    public static final int FUNC_REPLACE = 228;
    public static final int WRAP_CLAMP = 240;
    public static final int WRAP_REPEAT = 241;

    private int blending = FUNC_MODULATE;
    private int blendcolor = 0;
    private int filter = FILTER_NEAREST;
    private int filterlevel = FILTER_BASE_LEVEL;
    private int wraps = WRAP_REPEAT;
    private int wrapt = WRAP_REPEAT;
    private Image2D texImage;

    public Texture2D(Image2D image) { this.texImage = image; }

    public int getBlendColor() { return blendcolor; }
    public int getBlending() { return blending; }
    public Image2D getImage() { return texImage; }
    public int getImageFilter() { return filter; }
    public int getLevelFilter() { return filterlevel; }
    public int getWrappingS() { return wraps; }
    public int getWrappingT() { return wrapt; }
    public void setBlendColor(int RGB) { blendcolor = RGB; }
    public void setBlending(int func) { blending = func; }
    public void setFiltering(int levelFilter, int imageFilter) { filterlevel = levelFilter; filter = imageFilter; }
    public void setImage(Image2D image) { texImage = image; }
    public void setWrapping(int wrapS, int wrapT) { wraps = wrapS; wrapt = wrapT; }
}
