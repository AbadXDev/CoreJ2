package dev.abadx.corej2.overlay;

/**
 * Pure-Java ARGB8888 framebuffer used by the FreeJ2ME AWT overlay.
 * Replaces {@code java.awt.image.BufferedImage} for embedded iOS.
 */
public final class IntArgbImage {

    public final int width;
    public final int height;
    /** Row-major TYPE_INT_ARGB pixels. */
    public final int[] data;

    public IntArgbImage(int width, int height) {
        if (width < 1) {
            width = 1;
        }
        if (height < 1) {
            height = 1;
        }
        this.width = width;
        this.height = height;
        this.data = new int[width * height];
    }

    public IntArgbImage(int width, int height, int[] data) {
        this.width = width;
        this.height = height;
        this.data = data;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getRGB(int x, int y) {
        return data[y * width + x];
    }

    public void setRGB(int x, int y, int rgb) {
        data[y * width + x] = rgb;
    }

    public void getRGB(int startX, int startY, int w, int h, int[] rgbArray, int offset, int scansize) {
        for (int row = 0; row < h; row++) {
            int src = (startY + row) * width + startX;
            int dst = offset + row * scansize;
            System.arraycopy(data, src, rgbArray, dst, w);
        }
    }

    public void setRGB(int startX, int startY, int w, int h, int[] rgbArray, int offset, int scansize) {
        for (int row = 0; row < h; row++) {
            int dst = (startY + row) * width + startX;
            int src = offset + row * scansize;
            System.arraycopy(rgbArray, src, data, dst, w);
        }
    }

    public IntArgbImage getSubimage(int x, int y, int w, int h) {
        IntArgbImage out = new IntArgbImage(w, h);
        getRGB(x, y, w, h, out.data, 0, w);
        return out;
    }

    public void clear(int argb) {
        for (int i = 0; i < data.length; i++) {
            data[i] = argb;
        }
    }

    public void blit(IntArgbImage src, int dx, int dy) {
        blit(src, 0, 0, src.width, src.height, dx, dy, true);
    }

    public void blit(IntArgbImage src, int sx, int sy, int sw, int sh, int dx, int dy, boolean alpha) {
        for (int row = 0; row < sh; row++) {
            int syy = sy + row;
            int dyy = dy + row;
            if (syy < 0 || syy >= src.height || dyy < 0 || dyy >= height) {
                continue;
            }
            for (int col = 0; col < sw; col++) {
                int sxx = sx + col;
                int dxx = dx + col;
                if (sxx < 0 || sxx >= src.width || dxx < 0 || dxx >= width) {
                    continue;
                }
                int c = src.data[syy * src.width + sxx];
                if (alpha) {
                    int a = (c >>> 24) & 0xFF;
                    if (a == 0) {
                        continue;
                    }
                    if (a == 255) {
                        data[dyy * width + dxx] = c;
                    } else {
                        data[dyy * width + dxx] = blend(data[dyy * width + dxx], c);
                    }
                } else {
                    data[dyy * width + dxx] = c | 0xFF000000;
                }
            }
        }
    }

    private static int blend(int dst, int src) {
        int sa = (src >>> 24) & 0xFF;
        int da = (dst >>> 24) & 0xFF;
        int sr = (src >>> 16) & 0xFF;
        int sg = (src >>> 8) & 0xFF;
        int sb = src & 0xFF;
        int dr = (dst >>> 16) & 0xFF;
        int dg = (dst >>> 8) & 0xFF;
        int db = dst & 0xFF;
        int outA = sa + ((da * (255 - sa)) / 255);
        int outR = (sr * sa + dr * (255 - sa)) / 255;
        int outG = (sg * sa + dg * (255 - sa)) / 255;
        int outB = (sb * sa + db * (255 - sa)) / 255;
        return (outA << 24) | (outR << 16) | (outG << 8) | outB;
    }
}
