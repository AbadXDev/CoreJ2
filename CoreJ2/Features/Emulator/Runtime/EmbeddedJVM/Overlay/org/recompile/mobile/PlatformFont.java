package org.recompile.mobile;

import javax.microedition.lcdui.Font;

import dev.abadx.corej2.overlay.BitmapFont;

/**
 * CoreJ2 overlay — pure-Java replacement for FreeJ2ME {@code PlatformFont}.
 * Same FQCN; loaded ahead of Vendor on the embedded classpath.
 */
public class PlatformFont {

    /** Unused on iOS; kept so any reflective access to the field name still resolves. */
    public Object awtFont;

    private final BitmapFont bitmap;

    public PlatformFont(Font font) {
        boolean bold = (font.getStyle() & Font.STYLE_BOLD) != 0;
        bitmap = new BitmapFont(font.getPointSize(), bold);
        awtFont = bitmap;
    }

    public int stringWidth(String str) {
        return bitmap.stringWidth(str);
    }

    BitmapFont bitmapFont() {
        return bitmap;
    }
}
