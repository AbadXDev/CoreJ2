package org.recompile.mobile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.Sprite;

import dev.abadx.corej2.overlay.IntArgbImage;
import dev.abadx.corej2.overlay.PngDecoder;

/**
 * CoreJ2 overlay — pure-Java {@code PlatformImage} (int[] framebuffer + PNG decode).
 * Same FQCN as FreeJ2ME; compiled over the Vendor class on the embedded classpath.
 */
public class PlatformImage extends javax.microedition.lcdui.Image {

    protected IntArgbImage canvas;
    protected PlatformGraphics gc;

    public boolean isNull = false;

    public IntArgbImage getCanvas() {
        return canvas;
    }

    public PlatformGraphics getGraphics() {
        return gc;
    }

    protected void createGraphics() {
        gc = new PlatformGraphics(this);
        gc.setColor(0x000000);
    }

    public PlatformImage(int Width, int Height) {
        width = Width;
        height = Height;
        canvas = new IntArgbImage(Width, Height);
        createGraphics();
        gc.setColor(0xFFFFFF);
        gc.fillRect(0, 0, width, height);
        gc.setColor(0x000000);
        platformImage = this;
    }

    public PlatformImage(String name) {
        InputStream stream = null;
        try {
            MobilePlatform platform = Mobile.getPlatform();
            if (platform != null && platform.loader != null) {
                stream = platform.loader.getMIDletResourceAsStream(name);
            }
        } catch (Throwable ignored) {
            stream = null;
        }
        if (stream == null) {
            System.out.println("Couldn't Load Image Stream (can't find " + name + ")");
            width = 1;
            height = 1;
            canvas = new IntArgbImage(1, 1);
            createGraphics();
            isNull = true;
        } else {
            try {
                IntArgbImage temp = decodeImage(stream);
                width = temp.width;
                height = temp.height;
                canvas = new IntArgbImage(width, height);
                createGraphics();
                gc.drawImage2(temp, 0, 0);
            } catch (Exception e) {
                System.out.println("Couldn't Load Image Stream " + name + ": " + e.getMessage());
                // Placeholder so callers that touch getHeight() do not NPE.
                width = 1;
                height = 1;
                canvas = new IntArgbImage(1, 1);
                createGraphics();
                isNull = true;
            }
        }
        platformImage = this;
    }

    public PlatformImage(InputStream stream) {
        try {
            IntArgbImage temp = decodeImage(stream);
            width = temp.width;
            height = temp.height;
            canvas = new IntArgbImage(width, height);
            createGraphics();
            gc.drawImage2(temp, 0, 0);
        } catch (Exception e) {
            System.out.println("Couldn't Load Image Stream: " + e.getMessage());
            width = 1;
            height = 1;
            canvas = new IntArgbImage(1, 1);
            createGraphics();
            isNull = true;
        }
        platformImage = this;
    }

    public PlatformImage(Image source) {
        width = source.platformImage.width;
        height = source.platformImage.height;
        canvas = new IntArgbImage(width, height);
        createGraphics();
        gc.drawImage2(source.platformImage.getCanvas(), 0, 0);
        platformImage = this;
    }

    public PlatformImage(byte[] imageData, int imageOffset, int imageLength) {
        try {
            IntArgbImage temp = decodeImage(new ByteArrayInputStream(imageData, imageOffset, imageLength));
            width = temp.width;
            height = temp.height;
            canvas = new IntArgbImage(width, height);
            createGraphics();
            gc.drawImage2(temp, 0, 0);
        } catch (Exception e) {
            System.out.println("Couldn't Load Image Data From Byte Array");
            canvas = new IntArgbImage(Mobile.getPlatform().lcdWidth, Mobile.getPlatform().lcdHeight);
            width = canvas.width;
            height = canvas.height;
            createGraphics();
            isNull = true;
        }
        platformImage = this;
    }

    public PlatformImage(int[] rgb, int Width, int Height, boolean processAlpha) {
        width = Width;
        height = Height;
        if (width < 1) {
            width = 1;
        }
        if (height < 1) {
            height = 1;
        }
        canvas = new IntArgbImage(width, height);
        createGraphics();
        gc.drawRGB(rgb, 0, width, 0, 0, width, height, true);
        platformImage = this;
    }

    public PlatformImage(Image image, int x, int y, int Width, int Height, int transform) {
        IntArgbImage sub = image.platformImage.canvas.getSubimage(x, y, Width, Height);
        canvas = transformImage(sub, transform);
        createGraphics();
        width = canvas.width;
        height = canvas.height;
        platformImage = this;
    }

    public void getRGB(int[] rgbData, int offset, int scanlength, int x, int y, int width, int height) {
        canvas.getRGB(x, y, width, height, rgbData, offset, scanlength);
    }

    public int getARGB(int x, int y) {
        return canvas.getRGB(x, y);
    }

    public int getPixel(int x, int y) {
        return canvas.getRGB(x, y);
    }

    public void setPixel(int x, int y, int color) {
        int[] rgbData = { color };
        gc.drawRGB(rgbData, 0, 1, x, y, 1, 1, false);
    }

    public static IntArgbImage transformImage(IntArgbImage image, int transform) {
        int w = image.width;
        int h = image.height;
        int outW = w;
        int outH = h;
        switch (transform) {
            case Sprite.TRANS_ROT90:
            case Sprite.TRANS_ROT270:
            case Sprite.TRANS_MIRROR_ROT90:
            case Sprite.TRANS_MIRROR_ROT270:
                outW = h;
                outH = w;
                break;
            default:
                break;
        }
        IntArgbImage out = new IntArgbImage(outW, outH);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c = image.getRGB(x, y);
                int dx = x;
                int dy = y;
                switch (transform) {
                    case Sprite.TRANS_NONE:
                        break;
                    case Sprite.TRANS_ROT90:
                        dx = h - 1 - y;
                        dy = x;
                        break;
                    case Sprite.TRANS_ROT180:
                        dx = w - 1 - x;
                        dy = h - 1 - y;
                        break;
                    case Sprite.TRANS_ROT270:
                        dx = y;
                        dy = w - 1 - x;
                        break;
                    case Sprite.TRANS_MIRROR:
                        dx = w - 1 - x;
                        dy = y;
                        break;
                    case Sprite.TRANS_MIRROR_ROT90:
                        dx = h - 1 - y;
                        dy = w - 1 - x;
                        break;
                    case Sprite.TRANS_MIRROR_ROT180:
                        dx = x;
                        dy = h - 1 - y;
                        break;
                    case Sprite.TRANS_MIRROR_ROT270:
                        dx = y;
                        dy = x;
                        break;
                    default:
                        break;
                }
                if (dx >= 0 && dy >= 0 && dx < outW && dy < outH) {
                    out.setRGB(dx, dy, c);
                }
            }
        }
        return out;
    }

    private static IntArgbImage decodeImage(InputStream stream) throws Exception {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = stream.read(buf)) >= 0) {
            bos.write(buf, 0, n);
        }
        byte[] all = bos.toByteArray();
        if (all.length >= 2 && (all[0] & 0xFF) == 0xFF && (all[1] & 0xFF) == 0xD8) {
            throw new Exception("JPEG not supported in overlay yet");
        }
        if (all.length >= 2 && (all[0] & 0xFF) == 'B' && (all[1] & 0xFF) == 'M') {
            return dev.abadx.corej2.overlay.BmpDecoder.decode(all, 0, all.length);
        }
        return PngDecoder.decode(all, 0, all.length);
    }
}
