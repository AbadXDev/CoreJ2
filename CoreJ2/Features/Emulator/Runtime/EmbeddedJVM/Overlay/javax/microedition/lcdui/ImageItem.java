package javax.microedition.lcdui;

/** Clean-room MIDP 2.0 {@code ImageItem}. */
public class ImageItem extends Item {
    public static final int LAYOUT_DEFAULT = Item.LAYOUT_DEFAULT;
    public static final int LAYOUT_LEFT = Item.LAYOUT_LEFT;
    public static final int LAYOUT_RIGHT = Item.LAYOUT_RIGHT;
    public static final int LAYOUT_CENTER = Item.LAYOUT_CENTER;
    public static final int LAYOUT_NEWLINE_BEFORE = Item.LAYOUT_NEWLINE_BEFORE;
    public static final int LAYOUT_NEWLINE_AFTER = Item.LAYOUT_NEWLINE_AFTER;

    private Image image;
    private String altText = "";
    private int appearanceMode = PLAIN;

    public ImageItem(String label, Image img, int layout, String altText) {
        this(label, img, layout, altText, PLAIN);
    }

    public ImageItem(String label, Image img, int layout, String altText, int appearanceMode) {
        setLabel(label);
        this.image = img;
        setLayout(layout);
        this.altText = altText == null ? "" : altText;
        this.appearanceMode = appearanceMode;
    }

    public Image getImage() { return image; }
    public void setImage(Image img) { image = img; }
    public String getAltText() { return altText; }
    public void setAltText(String text) { altText = text == null ? "" : text; }
    public int getAppearanceMode() { return appearanceMode; }
}
