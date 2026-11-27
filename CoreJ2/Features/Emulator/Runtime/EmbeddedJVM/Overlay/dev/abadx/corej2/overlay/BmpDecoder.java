package dev.abadx.corej2.overlay;

import java.io.IOException;

/**
 * Minimal Windows BMP decoder for Gameloft and other J2ME assets that ship
 * raw {@code .bmp} resources instead of PNG (e.g. Real Football {@code /l1}).
 *
 * <p>Supports uncompressed BI_RGB 8-bit (palette) and 24-bit bitmaps.
 */
public final class BmpDecoder {

    private BmpDecoder() {
    }

    public static IntArgbImage decode(byte[] data, int offset, int length) throws IOException {
        if (length < 54) {
            throw new IOException("BMP too small");
        }
        if ((data[offset] & 0xFF) != 'B' || (data[offset + 1] & 0xFF) != 'M') {
            throw new IOException("Not a BMP");
        }

        int pixelOffset = u32(data, offset + 10);
        int dibSize = u32(data, offset + 14);
        if (dibSize < 40) {
            throw new IOException("Unsupported BMP DIB size " + dibSize);
        }

        int width = s32(data, offset + 18);
        int heightRaw = s32(data, offset + 22);
        boolean topDown = heightRaw < 0;
        int height = Math.abs(heightRaw);
        int planes = u16(data, offset + 26);
        int bitCount = u16(data, offset + 28);
        int compression = u32(data, offset + 30);

        if (planes != 1) {
            throw new IOException("Unsupported BMP planes " + planes);
        }
        if (compression != 0) {
            throw new IOException("Unsupported BMP compression " + compression);
        }
        if (width <= 0 || height <= 0) {
            throw new IOException("Invalid BMP size " + width + "x" + height);
        }
        if (bitCount != 8 && bitCount != 24) {
            throw new IOException("Unsupported BMP bit count " + bitCount);
        }

        int[] palette = null;
        if (bitCount == 8) {
            int colorsUsed = u32(data, offset + 46);
            if (colorsUsed == 0) {
                colorsUsed = 256;
            }
            int paletteOffset = offset + 14 + dibSize;
            palette = new int[colorsUsed];
            for (int i = 0; i < colorsUsed; i++) {
                int b = data[paletteOffset + i * 4] & 0xFF;
                int g = data[paletteOffset + i * 4 + 1] & 0xFF;
                int r = data[paletteOffset + i * 4 + 2] & 0xFF;
                palette[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
            }
        }

        int rowBytes = ((bitCount * width + 31) / 32) * 4;
        int pixelsStart = offset + pixelOffset;
        if (pixelsStart + rowBytes * height > offset + length) {
            throw new IOException("BMP pixel data truncated");
        }

        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int srcY = topDown ? y : (height - 1 - y);
            int row = pixelsStart + srcY * rowBytes;
            if (bitCount == 8) {
                for (int x = 0; x < width; x++) {
                    int idx = data[row + x] & 0xFF;
                    pixels[y * width + x] = idx < palette.length ? palette[idx] : 0xFF000000;
                }
            } else {
                for (int x = 0; x < width; x++) {
                    int p = row + x * 3;
                    int b = data[p] & 0xFF;
                    int g = data[p + 1] & 0xFF;
                    int r = data[p + 2] & 0xFF;
                    pixels[y * width + x] = 0xFF000000 | (r << 16) | (g << 8) | b;
                }
            }
        }

        return new IntArgbImage(width, height, pixels);
    }

    private static int u16(byte[] d, int o) {
        return (d[o] & 0xFF) | ((d[o + 1] & 0xFF) << 8);
    }

    private static int u32(byte[] d, int o) {
        return (d[o] & 0xFF)
                | ((d[o + 1] & 0xFF) << 8)
                | ((d[o + 2] & 0xFF) << 16)
                | ((d[o + 3] & 0xFF) << 24);
    }

    private static int s32(byte[] d, int o) {
        return u32(d, o);
    }
}
