package com.nokia.mid.ui;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import org.recompile.mobile.PlatformGraphics;
import org.recompile.mobile.PlatformImage;

/**
 * Clean-room Nokia UI {@code DirectUtils}.
 */
public class DirectUtils {

    public DirectUtils() {
    }

    public static Image createImage(byte[] imageData, int imageOffset, int imageLength) {
        return Image.createImage(imageData, imageOffset, imageLength);
    }

    public static Image createImage(int width, int height, int argb) {
        PlatformImage img = new PlatformImage(width, height);
        PlatformGraphics g = img.getGraphics();
        g.setARGBColor(argb);
        g.fillRect(0, 0, width, height);
        return img;
    }

    public static DirectGraphics getDirectGraphics(Graphics g) {
        if (g instanceof DirectGraphics) {
            return (DirectGraphics) g;
        }
        throw new IllegalArgumentException("Graphics is not DirectGraphics");
    }
}
