package javax.microedition.lcdui;

import org.recompile.mobile.PlatformGraphics;
import org.recompile.mobile.PlatformImage;

/**
 * Clean-room MIDP 2.0 {@code Graphics} (JSR-118).
 * Drawing is performed by the CoreJ2 {@link PlatformGraphics} subclass;
 * this base holds MIDP state/constants and no-op defaults.
 */
public class Graphics {

    public static final int HCENTER = 1;
    public static final int VCENTER = 2;
    public static final int LEFT = 4;
    public static final int RIGHT = 8;
    public static final int TOP = 16;
    public static final int BOTTOM = 32;
    public static final int BASELINE = 64;

    public static final int SOLID = 0;
    public static final int DOTTED = 1;

    protected int translateX;
    protected int translateY;
    protected int clipX;
    protected int clipY;
    protected int clipWidth;
    protected int clipHeight;
    protected int color;
    protected Font font = Font.getDefaultFont();
    protected int strokeStyle = SOLID;

    /** Compatibility fields used by existing Platform* bridges. */
    public PlatformImage platformImage;
    public PlatformGraphics platformGraphics;

    public Graphics() {
    }

    public void translate(int x, int y) {
        translateX += x;
        translateY += y;
        clipX -= x;
        clipY -= y;
    }

    public int getTranslateX() {
        return translateX;
    }

    public int getTranslateY() {
        return translateY;
    }

    public void setClip(int x, int y, int width, int height) {
        clipX = x;
        clipY = y;
        clipWidth = width;
        clipHeight = height;
    }

    public void clipRect(int x, int y, int width, int height) {
        int nx = Math.max(clipX, x);
        int ny = Math.max(clipY, y);
        int nr = Math.min(clipX + clipWidth, x + width);
        int nb = Math.min(clipY + clipHeight, y + height);
        clipX = nx;
        clipY = ny;
        clipWidth = Math.max(0, nr - nx);
        clipHeight = Math.max(0, nb - ny);
    }

    public int getClipX() {
        return clipX;
    }

    public int getClipY() {
        return clipY;
    }

    public int getClipWidth() {
        return clipWidth;
    }

    public int getClipHeight() {
        return clipHeight;
    }

    public void setColor(int RGB) {
        color = RGB & 0x00FFFFFF;
    }

    public void setColor(int red, int green, int blue) {
        setColor(((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF));
    }

    public void setGrayScale(int value) {
        int v = value & 0xFF;
        setColor(v, v, v);
    }

    public int getColor() {
        return color;
    }

    public int getRedComponent() {
        return (color >> 16) & 0xFF;
    }

    public int getGreenComponent() {
        return (color >> 8) & 0xFF;
    }

    public int getBlueComponent() {
        return color & 0xFF;
    }

    public int getGrayScale() {
        return (getRedComponent() * 30 + getGreenComponent() * 59 + getBlueComponent() * 11) / 100;
    }

    public int getDisplayColor(int color) {
        return color & 0x00FFFFFF;
    }

    public void setFont(Font font) {
        this.font = font == null ? Font.getDefaultFont() : font;
    }

    public Font getFont() {
        return font;
    }

    public void setStrokeStyle(int style) {
        strokeStyle = style;
    }

    public int getStrokeStyle() {
        return strokeStyle;
    }

    public void drawLine(int x1, int y1, int x2, int y2) {
    }

    public void drawRect(int x, int y, int width, int height) {
    }

    public void fillRect(int x, int y, int width, int height) {
    }

    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
    }

    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
    }

    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
    }

    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
    }

    public void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3) {
    }

    public void drawChar(char character, int x, int y, int anchor) {
        drawString(String.valueOf(character), x, y, anchor);
    }

    public void drawChars(char[] data, int offset, int length, int x, int y, int anchor) {
        drawString(new String(data, offset, length), x, y, anchor);
    }

    public void drawString(String str, int x, int y, int anchor) {
    }

    public void drawSubstring(String str, int offset, int len, int x, int y, int anchor) {
        drawString(str.substring(offset, offset + len), x, y, anchor);
    }

    public void drawImage(Image img, int x, int y, int anchor) {
    }

    public void drawRegion(Image src, int x_src, int y_src, int width, int height,
                           int transform, int x_dest, int y_dest, int anchor) {
    }

    public void copyArea(int x_src, int y_src, int width, int height, int x_dest, int y_dest, int anchor) {
    }

    public void drawRGB(int[] rgbData, int offset, int scanlength, int x, int y,
                        int width, int height, boolean processAlpha) {
    }
}
