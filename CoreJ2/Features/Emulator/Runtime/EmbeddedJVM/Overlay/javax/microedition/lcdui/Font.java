package javax.microedition.lcdui;

import org.recompile.mobile.PlatformFont;

/**
 * Clean-room MIDP 2.0 {@code Font} (JSR-118).
 * Metrics backed by CoreJ2 {@link PlatformFont}/{@code BitmapFont}.
 */
public final class Font {

    public static final int FACE_SYSTEM = 0;
    public static final int FACE_MONOSPACE = 32;
    public static final int FACE_PROPORTIONAL = 64;

    public static final int STYLE_PLAIN = 0;
    public static final int STYLE_BOLD = 1;
    public static final int STYLE_ITALIC = 2;
    public static final int STYLE_UNDERLINED = 4;

    public static final int SIZE_SMALL = 8;
    public static final int SIZE_MEDIUM = 0;
    public static final int SIZE_LARGE = 16;

    public static final int FONT_STATIC_TEXT = 0;
    public static final int FONT_INPUT_TEXT = 1;

    private static final Font DEFAULT = new Font(FACE_SYSTEM, STYLE_PLAIN, SIZE_MEDIUM);

    private final int face;
    private final int style;
    private final int size;

    /** Bridge used by {@code PlatformGraphics} / {@code PlatformFont}. */
    public PlatformFont platformFont;

    private Font(int face, int style, int size) {
        this.face = face;
        this.style = style;
        this.size = size;
        this.platformFont = new PlatformFont(this);
    }

    public static Font getDefaultFont() {
        return DEFAULT;
    }

    public static Font getFont(int fontSpecifier) {
        return DEFAULT;
    }

    public static Font getFont(int face, int style, int size) {
        return new Font(face, style, size);
    }

    public int getFace() {
        return face;
    }

    public int getStyle() {
        return style;
    }

    public int getSize() {
        return size;
    }

    /** Pixel height used by the bitmap font bridge. */
    public int getPointSize() {
        return convertSize(size);
    }

    public int getHeight() {
        return convertSize(size);
    }

    public int getBaselinePosition() {
        return convertSize(size) - 2;
    }

    public boolean isPlain() {
        return style == STYLE_PLAIN;
    }

    public boolean isBold() {
        return (style & STYLE_BOLD) != 0;
    }

    public boolean isItalic() {
        return (style & STYLE_ITALIC) != 0;
    }

    public boolean isUnderlined() {
        return (style & STYLE_UNDERLINED) != 0;
    }

    public int charWidth(char ch) {
        return stringWidth(String.valueOf(ch));
    }

    public int charsWidth(char[] ch, int offset, int length) {
        if (ch == null) {
            throw new NullPointerException();
        }
        return stringWidth(new String(ch, offset, length));
    }

    public int stringWidth(String str) {
        if (str == null) {
            throw new NullPointerException();
        }
        return platformFont.stringWidth(str);
    }

    public int substringWidth(String str, int offset, int len) {
        if (str == null) {
            throw new NullPointerException();
        }
        return stringWidth(str.substring(offset, offset + len));
    }

    private static int convertSize(int size) {
        if (size == SIZE_LARGE) {
            return 16;
        }
        if (size == SIZE_SMALL) {
            return 9;
        }
        return 13;
    }
}
