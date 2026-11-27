package org.recompile.mobile;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.Sprite;

import com.nokia.mid.ui.DirectGraphics;

import dev.abadx.corej2.overlay.BitmapFont;
import dev.abadx.corej2.overlay.IntArgbImage;

/**
 * CoreJ2 overlay — software rasterizer replacing FreeJ2ME AWT {@code PlatformGraphics}.
 */
public class PlatformGraphics extends javax.microedition.lcdui.Graphics implements DirectGraphics {

    protected IntArgbImage canvas;

    protected int argbColor = 0xFF000000;
    protected int strokeStyle = SOLID;
    protected Font font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);

    public PlatformGraphics platformGraphics;
    public PlatformImage platformImage;

    public PlatformGraphics(PlatformImage image) {
        canvas = image.getCanvas();
        platformImage = image;
        platformGraphics = this;
        clipX = 0;
        clipY = 0;
        clipWidth = canvas.width;
        clipHeight = canvas.height;
        setColor(0, 0, 0);
    }

    public void reset() {
        translate(-1 * translateX, -1 * translateY);
        setClip(0, 0, canvas.width, canvas.height);
        setColor(0, 0, 0);
        setFont(Font.getDefaultFont());
        setStrokeStyle(SOLID);
    }

    public IntArgbImage getCanvas() {
        return canvas;
    }

    public void clearRect(int x, int y, int width, int height) {
        int saved = argbColor;
        argbColor = 0x00000000;
        fillRect(x, y, width, height);
        argbColor = saved;
    }

    public void copyArea(int subx, int suby, int subw, int subh, int x, int y, int anchor) {
        x = AnchorX(x, subw, anchor);
        y = AnchorY(y, subh, anchor);
        IntArgbImage sub = canvas.getSubimage(tx(subx), ty(suby), subw, subh);
        blit(sub, x, y, false);
    }

    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        rasterArc(x, y, width, height, startAngle, arcAngle, false);
    }

    public void drawChar(char character, int x, int y, int anchor) {
        drawString(Character.toString(character), x, y, anchor);
    }

    public void drawChars(char[] data, int offset, int length, int x, int y, int anchor) {
        drawString(new String(data, offset, length), x, y, anchor);
    }

    public void drawImage(Image image, int x, int y, int anchor) {
        try {
            int imgWidth = image.getWidth();
            int imgHeight = image.getHeight();
            x = AnchorX(x, imgWidth, anchor);
            y = AnchorY(y, imgHeight, anchor);
            blit(image.platformImage.getCanvas(), x, y, true);
        } catch (Exception e) {
            System.out.println("drawImage A:" + e.getMessage());
        }
    }

    public void drawImage(Image image, int x, int y) {
        try {
            blit(image.platformImage.getCanvas(), x, y, true);
        } catch (Exception e) {
            System.out.println("drawImage B:" + e.getMessage());
        }
    }

    public void drawImage2(Image image, int x, int y) {
        blit(image.platformImage.getCanvas(), x, y, true);
    }

    public void drawImage2(IntArgbImage image, int x, int y) {
        blit(image, x, y, true);
    }

    public void flushGraphics(Image image, int x, int y, int width, int height) {
        try {
            IntArgbImage sub = image.platformImage.getCanvas().getSubimage(x, y, width, height);
            blit(sub, x, y, false);
        } catch (Exception e) {
            // ignore
        }
    }

    public void drawRegion(Image image, int subx, int suby, int subw, int subh, int transform, int x, int y, int anchor) {
        try {
            if (transform == 0) {
                IntArgbImage sub = image.platformImage.getCanvas().getSubimage(subx, suby, subw, subh);
                x = AnchorX(x, subw, anchor);
                y = AnchorY(y, subh, anchor);
                blit(sub, x, y, true);
            } else {
                PlatformImage sub = new PlatformImage(image, subx, suby, subw, subh, transform);
                x = AnchorX(x, sub.width, anchor);
                y = AnchorY(y, sub.height, anchor);
                blit(sub.getCanvas(), x, y, true);
            }
        } catch (Exception e) {
            // ignore
        }
    }

    public void drawRGB(int[] rgbData, int offset, int scanlength, int x, int y, int width, int height, boolean processAlpha) {
        if (width < 1 || height < 1) {
            return;
        }
        IntArgbImage temp = new IntArgbImage(width, height);
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int c = rgbData[offset + col + row * scanlength];
                if (!processAlpha) {
                    c = (c & 0x00FFFFFF) | 0xFF000000;
                }
                temp.data[row * width + col] = c;
            }
        }
        blit(temp, x, y, processAlpha);
    }

    public void drawLine(int x1, int y1, int x2, int y2) {
        x1 = tx(x1);
        y1 = ty(y1);
        x2 = tx(x2);
        y2 = ty(y2);
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;
        while (true) {
            putPixel(x1, y1, argbColor);
            if (x1 == x2 && y1 == y2) {
                break;
            }
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x1 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y1 += sy;
            }
        }
    }

    public void drawRect(int x, int y, int width, int height) {
        drawLine(x, y, x + width, y);
        drawLine(x + width, y, x + width, y + height);
        drawLine(x + width, y + height, x, y + height);
        drawLine(x, y + height, x, y);
    }

    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        drawRect(x, y, width, height);
    }

    public void drawString(String str, int x, int y, int anchor) {
        if (str == null) {
            return;
        }
        BitmapFont bf = font.platformFont.bitmapFont();
        x = AnchorX(x, bf.stringWidth(str), anchor);
        y = AnchorY(y, bf.getHeight(), anchor);
        // FreeJ2ME used ascent baseline; MIDP TOP anchor draws from top.
        int drawY = ty(y);
        int drawX = tx(x);
        bf.drawString(canvas, str, drawX, drawY, argbColor | 0xFF000000, clipX, clipY, clipWidth, clipHeight);
    }

    public void drawSubstring(String str, int offset, int len, int x, int y, int anchor) {
        if (str.length() >= offset + len) {
            drawString(str.substring(offset, offset + len), x, y, anchor);
        }
    }

    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        rasterArc(x, y, width, height, startAngle, arcAngle, true);
    }

    public void fillRect(int x, int y, int width, int height) {
        int x0 = tx(x);
        int y0 = ty(y);
        int x1 = x0 + width;
        int y1 = y0 + height;
        int left = Math.max(x0, clipX);
        int top = Math.max(y0, clipY);
        int right = Math.min(x1, clipX + clipWidth);
        int bottom = Math.min(y1, clipY + clipHeight);
        for (int yy = top; yy < bottom; yy++) {
            if (yy < 0 || yy >= canvas.height) {
                continue;
            }
            int row = yy * canvas.width;
            for (int xx = left; xx < right; xx++) {
                if (xx >= 0 && xx < canvas.width) {
                    canvas.data[row + xx] = argbColor;
                }
            }
        }
    }

    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        fillRect(x, y, width, height);
    }

    public void setColor(int rgb) {
        setColor((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }

    public void setColor(int r, int g, int b) {
        color = (r << 16) + (g << 8) + b;
        argbColor = 0xFF000000 | color;
    }

    public void setFont(Font font) {
        super.setFont(font);
        this.font = font;
    }

    public void setClip(int x, int y, int width, int height) {
        clipX = tx(x);
        clipY = ty(y);
        clipWidth = width;
        clipHeight = height;
    }

    public void clipRect(int x, int y, int width, int height) {
        int nx = Math.max(clipX, tx(x));
        int ny = Math.max(clipY, ty(y));
        int nr = Math.min(clipX + clipWidth, tx(x) + width);
        int nb = Math.min(clipY + clipHeight, ty(y) + height);
        clipX = nx;
        clipY = ny;
        clipWidth = Math.max(0, nr - nx);
        clipHeight = Math.max(0, nb - ny);
    }

    public void translate(int x, int y) {
        translateX += x;
        translateY += y;
        clipX -= x;
        clipY -= y;
    }

    private int AnchorX(int x, int width, int anchor) {
        int xout = x;
        if ((anchor & HCENTER) > 0) {
            xout = x - (width / 2);
        }
        if ((anchor & RIGHT) > 0) {
            xout = x - width;
        }
        if ((anchor & LEFT) > 0) {
            xout = x;
        }
        return xout;
    }

    private int AnchorY(int y, int height, int anchor) {
        int yout = y;
        if ((anchor & VCENTER) > 0) {
            yout = y - (height / 2);
        }
        if ((anchor & TOP) > 0) {
            yout = y;
        }
        if ((anchor & BOTTOM) > 0) {
            yout = y - height;
        }
        if ((anchor & BASELINE) > 0) {
            yout = y + height;
        }
        return yout;
    }

    public void setAlphaRGB(int ARGB) {
        argbColor = ARGB;
        color = ARGB & 0x00FFFFFF;
    }

    private int colorAlpha;

    public int getNativePixelFormat() {
        return DirectGraphics.TYPE_INT_8888_ARGB;
    }

    public int getAlphaComponent() {
        return colorAlpha;
    }

    public void setARGBColor(int argbColor) {
        colorAlpha = (argbColor >>> 24) & 0xFF;
        setAlphaRGB(argbColor);
    }

    public void drawImage(javax.microedition.lcdui.Image img, int x, int y, int anchor, int manipulation) {
        IntArgbImage image = manipulateImage(img.platformImage.getCanvas(), manipulation);
        x = AnchorX(x, image.width, anchor);
        y = AnchorY(y, image.height, anchor);
        blit(image, x, y, true);
    }

    public void drawPixels(byte[] pixels, byte[] transparencyMask, int offset, int scanlength, int x, int y, int width, int height, int manipulation, int format) {
        int[] Type1 = { 0xFFFFFFFF, 0xFF000000, 0x00FFFFFF, 0x00000000 };
        int[] data;
        IntArgbImage temp;
        switch (format) {
            case -1: {
                data = new int[width * height];
                int ods = offset / scanlength;
                int oms = offset % scanlength;
                int b = ods % 8;
                for (int yj = 0; yj < height; yj++) {
                    int tmp = (ods + yj) / 8 * scanlength + oms;
                    for (int xj = 0; xj < width; xj++) {
                        int c = ((pixels[tmp + xj] >> b) & 1);
                        if (transparencyMask != null) {
                            c |= (((transparencyMask[tmp + xj] >> b) & 1) ^ 1) << 1;
                        }
                        data[(yj * width) + xj] = Type1[c];
                    }
                    b++;
                    if (b > 7) {
                        b = 0;
                    }
                }
                temp = new IntArgbImage(width, height, data);
                blit(manipulateImage(temp, manipulation), x, y, true);
                break;
            }
            case 1: {
                data = new int[pixels.length * 8];
                for (int i = (offset / 8); i < pixels.length; i++) {
                    for (int j = 7; j >= 0; j--) {
                        int c = ((pixels[i] >> j) & 1);
                        if (transparencyMask != null) {
                            c |= (((transparencyMask[i] >> j) & 1) ^ 1) << 1;
                        }
                        data[(i * 8) + (7 - j)] = Type1[c];
                    }
                }
                temp = new IntArgbImage(width, height);
                temp.setRGB(0, 0, width, height, data, 0, scanlength);
                blit(manipulateImage(temp, manipulation), x, y, true);
                break;
            }
            default:
                System.out.println("drawPixels A : Format " + format + " Not Implemented");
        }
    }

    public void drawPixels(int[] pixels, boolean transparency, int offset, int scanlength, int x, int y, int width, int height, int manipulation, int format) {
        IntArgbImage temp = new IntArgbImage(width, height);
        temp.setRGB(0, 0, width, height, pixels, offset, scanlength);
        blit(manipulateImage(temp, manipulation), x, y, true);
    }

    public void drawPixels(short[] pixels, boolean transparency, int offset, int scanlength, int x, int y, int width, int height, int manipulation, int format) {
        int[] data = new int[pixels.length];
        for (int i = 0; i < pixels.length; i++) {
            data[i] = pixelToColor(pixels[i], format);
            if (!transparency) {
                data[i] &= 0x00FFFFFF;
                data[i] |= 0xFF000000;
            }
        }
        IntArgbImage temp = new IntArgbImage(width, height);
        temp.setRGB(0, 0, width, height, data, offset, scanlength);
        blit(manipulateImage(temp, manipulation), x, y, true);
    }

    public void drawPolygon(int[] xPoints, int xOffset, int[] yPoints, int yOffset, int nPoints, int argb) {
        int temp = color;
        setAlphaRGB(argb);
        for (int i = 0; i < nPoints; i++) {
            int j = (i + 1) % nPoints;
            drawLine(xPoints[xOffset + i], yPoints[yOffset + i], xPoints[xOffset + j], yPoints[yOffset + j]);
        }
        setColor(temp);
    }

    public void drawTriangle(int x1, int y1, int x2, int y2, int x3, int y3, int argbColor) {
        int temp = color;
        setAlphaRGB(argbColor);
        drawLine(x1, y1, x2, y2);
        drawLine(x2, y2, x3, y3);
        drawLine(x3, y3, x1, y1);
        setColor(temp);
    }

    public void fillPolygon(int[] xPoints, int xOffset, int[] yPoints, int yOffset, int nPoints, int argbColor) {
        int temp = color;
        setAlphaRGB(argbColor);
        int[] xs = new int[nPoints];
        int[] ys = new int[nPoints];
        for (int i = 0; i < nPoints; i++) {
            xs[i] = tx(xPoints[xOffset + i]);
            ys[i] = ty(yPoints[yOffset + i]);
        }
        fillPolygonScreen(xs, ys, nPoints);
        setColor(temp);
    }

    public void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3) {
        fillPolygon(new int[] { x1, x2, x3 }, 0, new int[] { y1, y2, y3 }, 0, 3, argbColor);
    }

    public void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3, int argbColor) {
        fillPolygon(new int[] { x1, x2, x3 }, 0, new int[] { y1, y2, y3 }, 0, 3, argbColor);
    }

    public void getPixels(byte[] pixels, byte[] transparencyMask, int offset, int scanlength, int x, int y, int width, int height, int format) {
        System.out.println("getPixels A");
    }

    public void getPixels(int[] pixels, int offset, int scanlength, int x, int y, int width, int height, int format) {
        canvas.getRGB(tx(x), ty(y), width, height, pixels, offset, scanlength);
    }

    public void getPixels(short[] pixels, int offset, int scanlength, int x, int y, int width, int height, int format) {
        int i = offset;
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                pixels[i++] = colorToShortPixel(canvas.getRGB(tx(col + x), ty(row + y)), format);
            }
        }
    }

    private void blit(IntArgbImage src, int x, int y, boolean alpha) {
        int dx = tx(x);
        int dy = ty(y);
        for (int row = 0; row < src.height; row++) {
            int dyy = dy + row;
            if (dyy < clipY || dyy >= clipY + clipHeight || dyy < 0 || dyy >= canvas.height) {
                continue;
            }
            for (int col = 0; col < src.width; col++) {
                int dxx = dx + col;
                if (dxx < clipX || dxx >= clipX + clipWidth || dxx < 0 || dxx >= canvas.width) {
                    continue;
                }
                int c = src.data[row * src.width + col];
                if (alpha) {
                    int a = (c >>> 24) & 0xFF;
                    if (a == 0) {
                        continue;
                    }
                    if (a == 255) {
                        canvas.data[dyy * canvas.width + dxx] = c;
                    } else {
                        canvas.data[dyy * canvas.width + dxx] = blend(canvas.data[dyy * canvas.width + dxx], c);
                    }
                } else {
                    canvas.data[dyy * canvas.width + dxx] = c | 0xFF000000;
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
        int outA = sa + (da * (255 - sa)) / 255;
        int outR = (sr * sa + dr * (255 - sa)) / 255;
        int outG = (sg * sa + dg * (255 - sa)) / 255;
        int outB = (sb * sa + db * (255 - sa)) / 255;
        return (outA << 24) | (outR << 16) | (outG << 8) | outB;
    }

    private void putPixel(int x, int y, int c) {
        if (x < clipX || y < clipY || x >= clipX + clipWidth || y >= clipY + clipHeight) {
            return;
        }
        if (x < 0 || y < 0 || x >= canvas.width || y >= canvas.height) {
            return;
        }
        canvas.data[y * canvas.width + x] = c;
    }

    private int tx(int x) {
        return x + translateX;
    }

    private int ty(int y) {
        return y + translateY;
    }

    private void rasterArc(int x, int y, int width, int height, int startAngle, int arcAngle, boolean fill) {
        // Coarse scan conversion of elliptical arc.
        double a0 = Math.toRadians(startAngle);
        double a1 = Math.toRadians(startAngle + arcAngle);
        if (a1 < a0) {
            double t = a0;
            a0 = a1;
            a1 = t;
        }
        int cx = tx(x) + width / 2;
        int cy = ty(y) + height / 2;
        double rx = Math.max(1, width / 2.0);
        double ry = Math.max(1, height / 2.0);
        for (int yy = ty(y); yy <= ty(y) + height; yy++) {
            for (int xx = tx(x); xx <= tx(x) + width; xx++) {
                double dx = (xx - cx) / rx;
                double dy = (yy - cy) / ry;
                if (dx * dx + dy * dy > 1.05) {
                    continue;
                }
                double ang = Math.atan2(-(yy - cy), xx - cx);
                if (ang < 0) {
                    ang += Math.PI * 2;
                }
                // Map FreeJ2ME/AWT angles (0=east, CCW) roughly.
                double awt = (Math.atan2(-(yy - cy), (xx - cx)) * 180.0 / Math.PI);
                if (awt < 0) {
                    awt += 360;
                }
                boolean inside = false;
                double s = startAngle % 360;
                if (s < 0) {
                    s += 360;
                }
                double e = s + arcAngle;
                if (arcAngle >= 360 || arcAngle <= -360) {
                    inside = true;
                } else if (arcAngle >= 0) {
                    double aa = awt;
                    if (aa < s) {
                        aa += 360;
                    }
                    inside = aa >= s && aa <= e;
                } else {
                    double aa = awt;
                    if (aa > s) {
                        aa -= 360;
                    }
                    inside = aa <= s && aa >= e;
                }
                if (!inside) {
                    continue;
                }
                if (fill) {
                    putPixel(xx, yy, argbColor);
                } else {
                    double r2 = dx * dx + dy * dy;
                    if (r2 > 0.85 && r2 < 1.05) {
                        putPixel(xx, yy, argbColor);
                    }
                }
            }
        }
    }

    private void fillPolygonScreen(int[] xs, int[] ys, int n) {
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (int i = 0; i < n; i++) {
            if (ys[i] < minY) {
                minY = ys[i];
            }
            if (ys[i] > maxY) {
                maxY = ys[i];
            }
        }
        for (int y = minY; y <= maxY; y++) {
            int[] nodeX = new int[n];
            int nodes = 0;
            int j = n - 1;
            for (int i = 0; i < n; i++) {
                if ((ys[i] < y && ys[j] >= y) || (ys[j] < y && ys[i] >= y)) {
                    nodeX[nodes++] = xs[i] + (y - ys[i]) * (xs[j] - xs[i]) / (ys[j] - ys[i]);
                }
                j = i;
            }
            // sort
            for (int i = 0; i < nodes - 1; i++) {
                for (int k = i + 1; k < nodes; k++) {
                    if (nodeX[i] > nodeX[k]) {
                        int t = nodeX[i];
                        nodeX[i] = nodeX[k];
                        nodeX[k] = t;
                    }
                }
            }
            for (int i = 0; i + 1 < nodes; i += 2) {
                int x0 = nodeX[i];
                int x1 = nodeX[i + 1];
                for (int x = x0; x <= x1; x++) {
                    putPixel(x, y, argbColor);
                }
            }
        }
    }

    private int pixelToColor(short c, int format) {
        int a = 0xFF;
        int r = 0;
        int g = 0;
        int b = 0;
        switch (format) {
            case DirectGraphics.TYPE_USHORT_1555_ARGB:
                a = ((c >> 15) & 0x01) * 0xFF;
                r = (c >> 10) & 0x1F;
                g = (c >> 5) & 0x1F;
                b = c & 0x1F;
                r = (r << 3) | (r >> 2);
                g = (g << 3) | (g >> 2);
                b = (b << 3) | (b >> 2);
                break;
            case DirectGraphics.TYPE_USHORT_444_RGB:
                r = (c >> 8) & 0xF;
                g = (c >> 4) & 0xF;
                b = c & 0xF;
                r = (r << 4) | r;
                g = (g << 4) | g;
                b = (b << 4) | b;
                break;
            case DirectGraphics.TYPE_USHORT_4444_ARGB:
                a = (c >> 12) & 0xF;
                r = (c >> 8) & 0xF;
                g = (c >> 4) & 0xF;
                b = c & 0xF;
                a = (a << 4) | a;
                r = (r << 4) | r;
                g = (g << 4) | g;
                b = (b << 4) | b;
                break;
            case DirectGraphics.TYPE_USHORT_555_RGB:
                r = (c >> 10) & 0x1F;
                g = (c >> 5) & 0x1F;
                b = c & 0x1F;
                r = (r << 3) | (r >> 2);
                g = (g << 3) | (g >> 2);
                b = (b << 3) | (b >> 2);
                break;
            case DirectGraphics.TYPE_USHORT_565_RGB:
                r = (c >> 11) & 0x1F;
                g = (c >> 5) & 0x3F;
                b = c & 0x1F;
                r = (r << 3) | (r >> 2);
                g = (g << 2) | (g >> 4);
                b = (b << 3) | (b >> 2);
                break;
            default:
                break;
        }
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private short colorToShortPixel(int c, int format) {
        int a;
        int r;
        int g;
        int b;
        int out = 0;
        switch (format) {
            case DirectGraphics.TYPE_USHORT_1555_ARGB:
                a = c >>> 31;
                r = ((c >> 19) & 0x1F);
                g = ((c >> 11) & 0x1F);
                b = ((c >> 3) & 0x1F);
                out = (a << 15) | (r << 10) | (g << 5) | b;
                break;
            case DirectGraphics.TYPE_USHORT_444_RGB:
                r = ((c >> 20) & 0xF);
                g = ((c >> 12) & 0xF);
                b = ((c >> 4) & 0xF);
                out = (r << 8) | (g << 4) | b;
                break;
            case DirectGraphics.TYPE_USHORT_4444_ARGB:
                a = ((c >>> 28) & 0xF);
                r = ((c >> 20) & 0xF);
                g = ((c >> 12) & 0xF);
                b = ((c >> 4) & 0xF);
                out = (a << 12) | (r << 8) | (g << 4) | b;
                break;
            case DirectGraphics.TYPE_USHORT_555_RGB:
                r = ((c >> 19) & 0x1F);
                g = ((c >> 11) & 0x1F);
                b = ((c >> 3) & 0x1F);
                out = (r << 10) | (g << 5) | b;
                break;
            case DirectGraphics.TYPE_USHORT_565_RGB:
                r = ((c >> 19) & 0x1F);
                g = ((c >> 10) & 0x3F);
                b = ((c >> 3) & 0x1F);
                out = (r << 11) | (g << 5) | b;
                break;
            default:
                break;
        }
        return (short) out;
    }

    private IntArgbImage manipulateImage(IntArgbImage image, int manipulation) {
        final int HV = DirectGraphics.FLIP_HORIZONTAL | DirectGraphics.FLIP_VERTICAL;
        final int H90 = DirectGraphics.FLIP_HORIZONTAL | DirectGraphics.ROTATE_90;
        switch (manipulation) {
            case DirectGraphics.FLIP_HORIZONTAL:
                return PlatformImage.transformImage(image, Sprite.TRANS_MIRROR);
            case DirectGraphics.FLIP_VERTICAL:
                return PlatformImage.transformImage(image, Sprite.TRANS_MIRROR_ROT180);
            case DirectGraphics.ROTATE_90:
                return PlatformImage.transformImage(image, Sprite.TRANS_ROT270);
            case DirectGraphics.ROTATE_180:
                return PlatformImage.transformImage(image, Sprite.TRANS_ROT180);
            case DirectGraphics.ROTATE_270:
                return PlatformImage.transformImage(image, Sprite.TRANS_ROT90);
            case HV:
                return PlatformImage.transformImage(image, Sprite.TRANS_ROT180);
            case H90:
                return PlatformImage.transformImage(
                        PlatformImage.transformImage(image, Sprite.TRANS_MIRROR),
                        Sprite.TRANS_ROT270);
            case 0:
                break;
            default:
                System.out.println("manipulateImage " + manipulation + " not defined");
        }
        return image;
    }
}
