package javax.microedition.m3g;

import javax.microedition.lcdui.Image;
import org.recompile.mobile.PlatformImage;

/** CoreJ2 overlay — stores pixel bytes; converts from LCD Image when given. */
public class Image2D extends Object3D {
    public static final int ALPHA = 96;
    public static final int LUMINANCE = 97;
    public static final int LUMINANCE_ALPHA = 98;
    public static final int RGB = 99;
    public static final int RGBA = 100;

    private byte[] image;
    private int width;
    private int height;
    private int format;
    private boolean mutable = true;
    private int[] argb; // optional decoded cache

    public Image2D(int fmt, int w, int h) {
        format = fmt; width = w; height = h;
        image = new byte[w * h * bytesPerPixel(fmt)];
    }

    public Image2D(int fmt, int w, int h, byte[] img) {
        format = fmt; width = w; height = h; image = img;
    }

    public Image2D(int fmt, int w, int h, byte[] img, byte[] Palette) {
        this(fmt, w, h, img);
    }

    public Image2D(int fmt, Object img) {
        format = fmt;
        if (img instanceof Image) {
            Image src = (Image) img;
            width = src.getWidth();
            height = src.getHeight();
            argb = new int[width * height];
            src.getRGB(argb, 0, width, 0, 0, width, height);
            mutable = false;
        } else if (img instanceof byte[]) {
            image = (byte[]) img;
        }
    }

    public int getFormat() { return format; }
    public int getHeight() { return height; }
    public int getWidth() { return width; }
    public boolean isMutable() { return mutable; }

    public void set(int x, int y, int w, int h, byte[] img) {
        // Simplified: replace whole buffer when full-frame.
        if (x == 0 && y == 0 && w == width && h == height) {
            image = img;
            argb = null;
        }
    }

    public int sampleARGB(float u, float v) {
        if (width <= 0 || height <= 0) return 0xFFFFFFFF;
        int x = (int) Math.floor(u * width) % width;
        int y = (int) Math.floor(v * height) % height;
        if (x < 0) x += width;
        if (y < 0) y += height;
        ensureARGB();
        if (argb == null) return 0xFFFFFFFF;
        return argb[y * width + x];
    }

    private void ensureARGB() {
        if (argb != null) return;
        if (image == null) return;
        argb = new int[width * height];
        int bpp = bytesPerPixel(format);
        for (int i = 0; i < width * height; i++) {
            int o = i * bpp;
            if (format == RGB && o + 2 < image.length) {
                argb[i] = 0xFF000000 | ((image[o] & 0xFF) << 16) | ((image[o + 1] & 0xFF) << 8) | (image[o + 2] & 0xFF);
            } else if (format == RGBA && o + 3 < image.length) {
                argb[i] = ((image[o + 3] & 0xFF) << 24) | ((image[o] & 0xFF) << 16) | ((image[o + 1] & 0xFF) << 8) | (image[o + 2] & 0xFF);
            } else if (format == LUMINANCE && o < image.length) {
                int y = image[o] & 0xFF;
                argb[i] = 0xFF000000 | (y << 16) | (y << 8) | y;
            } else {
                argb[i] = 0xFFFFFFFF;
            }
        }
    }

    private static int bytesPerPixel(int fmt) {
        if (fmt == RGBA || fmt == LUMINANCE_ALPHA) return 4;
        if (fmt == RGB) return 3;
        if (fmt == LUMINANCE || fmt == ALPHA) return 1;
        return 4;
    }
}
