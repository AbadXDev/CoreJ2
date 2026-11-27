package javax.microedition.lcdui;

/** Clean-room MIDP 2.0 {@code StringItem}. */
public class StringItem extends Item {
    private String text = "";
    private int appearanceMode = PLAIN;
    private Font font;

    public StringItem(String label, String text) {
        this(label, text, PLAIN);
    }

    public StringItem(String label, String text, int appearanceMode) {
        setLabel(label);
        this.text = text == null ? "" : text;
        this.appearanceMode = appearanceMode;
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text == null ? "" : text; }
    public int getAppearanceMode() { return appearanceMode; }
    public Font getFont() { return font != null ? font : Font.getDefaultFont(); }
    public void setFont(Font font) { this.font = font; }
}
