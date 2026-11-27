package javax.microedition.lcdui;

import java.io.IOException;
import java.io.InputStream;

import org.recompile.mobile.PlatformImage;

/**
 * Clean-room MIDP 2.0 {@code Image} (JSR-118).
 * Factory methods create {@link org.recompile.mobile.PlatformImage} buffers.
 * {@link #getRGB} reads the software framebuffer (required by M3G Image2D).
 */
public class Image {

    public PlatformImage platformImage;

    public int width;
    public int height;

    public static Image createImage(byte[] imageData, int imageOffset, int imageLength) {
        if (imageData == null) {
            throw new NullPointerException();
        }
        if (imageOffset + imageLength > imageData.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        PlatformImage t = new PlatformImage(imageData, imageOffset, imageLength);
        if (t.isNull) {
            throw new IllegalArgumentException();
        }
        return t;
    }

    public static Image createImage(Image source) {
        if (source == null) {
            throw new NullPointerException();
        }
        return new PlatformImage(source);
    }

    public static Image createImage(Image img, int x, int y, int width, int height, int transform) {
        if (img == null) {
            throw new NullPointerException();
        }
        if (x + width > img.getWidth() || y + height > img.getHeight()) {
            throw new IllegalArgumentException();
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException();
        }
        return new PlatformImage(img, x, y, width, height, transform);
    }

    public static Image createImage(InputStream stream) throws IOException {
        if (stream == null) {
            throw new NullPointerException();
        }
        PlatformImage t = new PlatformImage(stream);
        if (t.isNull) {
            throw new IOException();
        }
        return t;
    }

    public static Image createImage(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException();
        }
        return new PlatformImage(width, height);
    }

    public static Image createImage(String name) throws IOException {
        if (name == null) {
            throw new NullPointerException();
        }
        long t0 = System.currentTimeMillis();
        PlatformImage t = new PlatformImage(name);
        long dt = System.currentTimeMillis() - t0;
        if (dt > 100 || t.isNull) {
            System.out.println("corej2-lcdui: createImage '" + name + "' " + dt + "ms null=" + t.isNull);
            System.out.flush();
        }
        if (t.isNull) {
            throw new IOException();
        }
        return t;
    }

    public static Image createRGBImage(int[] rgb, int width, int height, boolean processAlpha) {
        if (rgb == null) {
            throw new NullPointerException();
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException();
        }
        if (rgb.length < width * height) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return new PlatformImage(rgb, width, height, processAlpha);
    }

    public Graphics getGraphics() {
        return platformImage.getGraphics();
    }

    public int getHeight() {
        return height;
    }

    public void getRGB(int[] rgbData, int offset, int scanlength, int x, int y, int width, int height) {
        if (platformImage != null) {
            platformImage.getRGB(rgbData, offset, scanlength, x, y, width, height);
        }
    }

    public int getWidth() {
        return width;
    }

    public boolean isMutable() {
        return true;
    }
}
