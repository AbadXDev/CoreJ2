package com.nokia.mid.ui;

import javax.microedition.lcdui.Image;

/**
 * Clean-room Nokia UI {@code DirectGraphics} (observed MIDP extensions).
 * Implemented by CoreJ2 {@code PlatformGraphics}.
 */
public interface DirectGraphics {
    int FLIP_HORIZONTAL = 0x2000;
    int FLIP_VERTICAL = 0x4000;
    int ROTATE_90 = 90;
    int ROTATE_180 = 180;
    int ROTATE_270 = 270;

    int TYPE_BYTE_1_GRAY = 1;
    int TYPE_BYTE_1_GRAY_VERTICAL = 2;
    int TYPE_BYTE_2_GRAY = 4;
    int TYPE_BYTE_4_GRAY = 8;
    int TYPE_BYTE_8_GRAY = 16;
    int TYPE_BYTE_332_RGB = 256;
    int TYPE_USHORT_4444_ARGB = 4096;
    int TYPE_USHORT_444_RGB = 8192;
    int TYPE_USHORT_555_RGB = 16384;
    int TYPE_USHORT_1555_ARGB = 32768;
    int TYPE_USHORT_565_RGB = 65536;
    int TYPE_INT_888_RGB = 16777216;
    int TYPE_INT_8888_ARGB = 33554432;

    void drawImage(Image img, int x, int y, int anchor, int manipulation);
    void drawPixels(byte[] pixels, byte[] transparency, int offset, int scanlength,
                    int x, int y, int width, int height, int manipulation, int format);
    void drawPixels(int[] pixels, boolean transparency, int offset, int scanlength,
                    int x, int y, int width, int height, int manipulation, int format);
    void drawPixels(short[] pixels, boolean transparency, int offset, int scanlength,
                    int x, int y, int width, int height, int manipulation, int format);
    void drawPolygon(int[] xPoints, int xOffset, int[] yPoints, int yOffset, int nPoints, int argbColor);
    void drawTriangle(int x1, int y1, int x2, int y2, int x3, int y3, int argbColor);
    void fillPolygon(int[] xPoints, int xOffset, int[] yPoints, int yOffset, int nPoints, int argbColor);
    void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3, int argbColor);
    int getAlphaComponent();
    int getNativePixelFormat();
    void getPixels(byte[] pixels, byte[] transparency, int offset, int scanlength,
                   int x, int y, int width, int height, int format);
    void getPixels(int[] pixels, int offset, int scanlength, int x, int y, int width, int height, int format);
    void getPixels(short[] pixels, int offset, int scanlength, int x, int y, int width, int height, int format);
    void setARGBColor(int argbColor);
}
