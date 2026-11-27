package dev.abadx.corej2.overlay;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Minimal pure-Java PNG decoder (non-interlaced RGB/RGBA/Gray/GrayA/Palette).
 * Used by the FreeJ2ME AWT overlay instead of {@code ImageIO}.
 */
public final class PngDecoder {

    private PngDecoder() {
    }

    public static IntArgbImage decode(InputStream in) throws IOException {
        byte[] all = readAll(in);
        return decode(all, 0, all.length);
    }

    public static IntArgbImage decode(byte[] data, int offset, int length) throws IOException {
        if (length < 8) {
            throw new IOException("PNG too short");
        }
        if ((data[offset] & 0xFF) != 0x89
                || data[offset + 1] != 'P'
                || data[offset + 2] != 'N'
                || data[offset + 3] != 'G') {
            throw new IOException("Not a PNG");
        }

        int pos = offset + 8;
        int end = offset + length;
        int width = 0;
        int height = 0;
        int bitDepth = 8;
        int colorType = 2;
        byte[] palette = null;
        byte[] trans = null;
        ByteArrayOutputStream idat = new ByteArrayOutputStream();

        while (pos + 8 <= end) {
            int chunkLen = readInt(data, pos);
            pos += 4;
            int type = readInt(data, pos);
            pos += 4;
            if (pos + chunkLen + 4 > end) {
                throw new EOFException("truncated PNG chunk");
            }
            if (type == 0x49484452) { // IHDR
                width = readInt(data, pos);
                height = readInt(data, pos + 4);
                bitDepth = data[pos + 8] & 0xFF;
                colorType = data[pos + 9] & 0xFF;
                int interlace = data[pos + 12] & 0xFF;
                if (bitDepth != 8 && !(colorType == 3 && (bitDepth == 1 || bitDepth == 2 || bitDepth == 4))) {
                    throw new IOException("Unsupported PNG bit depth " + bitDepth);
                }
                if (interlace != 0) {
                    throw new IOException("Interlaced PNG not supported");
                }
            } else if (type == 0x504C5445) { // PLTE
                palette = new byte[chunkLen];
                System.arraycopy(data, pos, palette, 0, chunkLen);
            } else if (type == 0x74524E53) { // tRNS
                trans = new byte[chunkLen];
                System.arraycopy(data, pos, trans, 0, chunkLen);
            } else if (type == 0x49444154) { // IDAT
                idat.write(data, pos, chunkLen);
            } else if (type == 0x49454E44) { // IEND
                break;
            }
            pos += chunkLen + 4; // data + CRC
        }

        if (width < 1 || height < 1) {
            throw new IOException("Invalid PNG size");
        }

        byte[] inflated = inflate(idat.toByteArray());
        int[] pixels = new int[width * height];
        decodeScanlines(inflated, pixels, width, height, colorType, bitDepth, palette, trans);
        return new IntArgbImage(width, height, pixels);
    }

    private static void decodeScanlines(
            byte[] inflated,
            int[] pixels,
            int width,
            int height,
            int colorType,
            int bitDepth,
            byte[] palette,
            byte[] trans
    ) throws IOException {
        int bpp;
        switch (colorType) {
            case 0: bpp = 1; break; // gray
            case 2: bpp = 3; break; // rgb
            case 3: bpp = 1; break; // palette (after unpack)
            case 4: bpp = 2; break; // gray+a
            case 6: bpp = 4; break; // rgba
            default: throw new IOException("Unsupported PNG color type " + colorType);
        }
        // Packed palette rows (1/2/4-bit) → expand to 8-bit indices first.
        final int packedBytesPerRow;
        if (colorType == 3 && bitDepth < 8) {
            packedBytesPerRow = (width * bitDepth + 7) / 8;
        } else {
            packedBytesPerRow = width * bpp;
        }
        int stride = width * bpp;
        byte[] prev = new byte[stride];
        byte[] cur = new byte[stride];
        byte[] packed = new byte[packedBytesPerRow];
        byte[] prevPacked = new byte[packedBytesPerRow];
        int ip = 0;
        for (int y = 0; y < height; y++) {
            if (ip >= inflated.length) {
                throw new IOException("PNG data truncated");
            }
            int filter = inflated[ip++] & 0xFF;
            if (ip + packedBytesPerRow > inflated.length) {
                throw new IOException("PNG scanline truncated");
            }
            if (colorType == 3 && bitDepth < 8) {
                System.arraycopy(inflated, ip, packed, 0, packedBytesPerRow);
                ip += packedBytesPerRow;
                applyFilter(filter, packed, prevPacked, 1);
                System.arraycopy(packed, 0, prevPacked, 0, packedBytesPerRow);
                unpackPaletteRow(packed, cur, width, bitDepth);
            } else {
                System.arraycopy(inflated, ip, cur, 0, stride);
                ip += stride;
                applyFilter(filter, cur, prev, bpp);
            }
            for (int x = 0; x < width; x++) {
                pixels[y * width + x] = sample(cur, x, bpp, colorType, palette, trans);
            }
            byte[] swap = prev;
            prev = cur;
            cur = swap;
        }
    }

    private static void unpackPaletteRow(byte[] packed, byte[] outIndices, int width, int bitDepth) {
        int bit = 0;
        for (int x = 0; x < width; x++) {
            int value = 0;
            for (int b = 0; b < bitDepth; b++) {
                int byteIndex = bit / 8;
                int bitInByte = 7 - (bit % 8);
                value = (value << 1) | ((packed[byteIndex] >> bitInByte) & 1);
                bit++;
            }
            outIndices[x] = (byte) value;
        }
    }

    private static int sample(byte[] row, int x, int bpp, int colorType, byte[] palette, byte[] trans) {
        int i = x * bpp;
        switch (colorType) {
            case 0: {
                int g = row[i] & 0xFF;
                int a = 255;
                if (trans != null && trans.length >= 2) {
                    int tg = ((trans[0] & 0xFF) << 8) | (trans[1] & 0xFF);
                    if (g == tg) {
                        a = 0;
                    }
                }
                return (a << 24) | (g << 16) | (g << 8) | g;
            }
            case 2: {
                int r = row[i] & 0xFF;
                int g = row[i + 1] & 0xFF;
                int b = row[i + 2] & 0xFF;
                int a = 255;
                if (trans != null && trans.length >= 6) {
                    int tr = ((trans[0] & 0xFF) << 8) | (trans[1] & 0xFF);
                    int tg = ((trans[2] & 0xFF) << 8) | (trans[3] & 0xFF);
                    int tb = ((trans[4] & 0xFF) << 8) | (trans[5] & 0xFF);
                    if (r == tr && g == tg && b == tb) {
                        a = 0;
                    }
                }
                return (a << 24) | (r << 16) | (g << 8) | b;
            }
            case 3: {
                int idx = row[i] & 0xFF;
                int r = palette[idx * 3] & 0xFF;
                int g = palette[idx * 3 + 1] & 0xFF;
                int b = palette[idx * 3 + 2] & 0xFF;
                int a = 255;
                if (trans != null && idx < trans.length) {
                    a = trans[idx] & 0xFF;
                }
                return (a << 24) | (r << 16) | (g << 8) | b;
            }
            case 4: {
                int g = row[i] & 0xFF;
                int a = row[i + 1] & 0xFF;
                return (a << 24) | (g << 16) | (g << 8) | g;
            }
            case 6: {
                int r = row[i] & 0xFF;
                int g = row[i + 1] & 0xFF;
                int b = row[i + 2] & 0xFF;
                int a = row[i + 3] & 0xFF;
                return (a << 24) | (r << 16) | (g << 8) | b;
            }
            default:
                return 0;
        }
    }

    private static void applyFilter(int filter, byte[] cur, byte[] prev, int bpp) throws IOException {
        switch (filter) {
            case 0: // None
                break;
            case 1: // Sub
                for (int i = bpp; i < cur.length; i++) {
                    cur[i] = (byte) ((cur[i] & 0xFF) + (cur[i - bpp] & 0xFF));
                }
                break;
            case 2: // Up
                for (int i = 0; i < cur.length; i++) {
                    cur[i] = (byte) ((cur[i] & 0xFF) + (prev[i] & 0xFF));
                }
                break;
            case 3: // Average
                for (int i = 0; i < cur.length; i++) {
                    int left = i < bpp ? 0 : (cur[i - bpp] & 0xFF);
                    int up = prev[i] & 0xFF;
                    cur[i] = (byte) ((cur[i] & 0xFF) + ((left + up) / 2));
                }
                break;
            case 4: // Paeth
                for (int i = 0; i < cur.length; i++) {
                    int a = i < bpp ? 0 : (cur[i - bpp] & 0xFF);
                    int b = prev[i] & 0xFF;
                    int c = i < bpp ? 0 : (prev[i - bpp] & 0xFF);
                    cur[i] = (byte) ((cur[i] & 0xFF) + paeth(a, b, c));
                }
                break;
            default:
                throw new IOException("Unknown PNG filter " + filter);
        }
    }

    private static int paeth(int a, int b, int c) {
        int p = a + b - c;
        int pa = Math.abs(p - a);
        int pb = Math.abs(p - b);
        int pc = Math.abs(p - c);
        if (pa <= pb && pa <= pc) {
            return a;
        }
        if (pb <= pc) {
            return b;
        }
        return c;
    }

    private static byte[] inflate(byte[] compressed) throws IOException {
        Inflater inflater = new Inflater();
        inflater.setInput(compressed);
        ByteArrayOutputStream out = new ByteArrayOutputStream(compressed.length * 2);
        byte[] buf = new byte[4096];
        try {
            while (!inflater.finished()) {
                int n = inflater.inflate(buf);
                if (n == 0) {
                    if (inflater.needsInput()) {
                        break;
                    }
                } else {
                    out.write(buf, 0, n);
                }
            }
        } catch (DataFormatException e) {
            throw new IOException("PNG inflate failed", e);
        } finally {
            inflater.end();
        }
        return out.toByteArray();
    }

    private static int readInt(byte[] data, int pos) {
        return ((data[pos] & 0xFF) << 24)
                | ((data[pos + 1] & 0xFF) << 16)
                | ((data[pos + 2] & 0xFF) << 8)
                | (data[pos + 3] & 0xFF);
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) >= 0) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }
}
