package dev.abadx.corej2.overlay.m3g;

import java.io.File;
import java.io.FileOutputStream;

import org.recompile.mobile.Mobile;

/**
 * Writes ARGB8888 buffers as 24-bit BMP for device QA frame / texture dumps.
 * BMP needs no ImageIO (often absent on the embedded classpath).
 */
public final class ArgbBmpDump {

    private ArgbBmpDump() {
    }

    /** Directory under the app Documents root: {@code m3g-dumps/}. */
    public static File dumpDir() {
        String base = "";
        try {
            if (Mobile.getPlatform() != null && Mobile.getPlatform().dataPath != null) {
                base = Mobile.getPlatform().dataPath;
            }
        } catch (Throwable ignored) {
        }
        if (base == null || base.length() == 0) {
            base = System.getProperty("user.home", ".") + "/";
        }
        File dir = new File(base, "m3g-dumps");
        dir.mkdirs();
        return dir;
    }

    public static boolean dumpEnabled() {
        try {
            File flag = new File(dumpDir().getParentFile(), "qa-m3g-dump");
            return flag.isFile();
        } catch (Throwable t) {
            return false;
        }
    }

    public static void writeBmp(String name, int width, int height, int[] argb) {
        if (argb == null || width <= 0 || height <= 0) {
            return;
        }
        try {
            File out = new File(dumpDir(), name);
            int rowStride = ((width * 3 + 3) / 4) * 4;
            int pixelBytes = rowStride * height;
            int fileSize = 54 + pixelBytes;
            byte[] hdr = new byte[54];
            hdr[0] = 'B';
            hdr[1] = 'M';
            put32(hdr, 2, fileSize);
            put32(hdr, 10, 54);
            put32(hdr, 14, 40);
            put32(hdr, 18, width);
            put32(hdr, 22, height);
            put16(hdr, 26, 1);
            put16(hdr, 28, 24);
            put32(hdr, 34, pixelBytes);
            FileOutputStream fos = new FileOutputStream(out);
            fos.write(hdr);
            byte[] row = new byte[rowStride];
            // BMP stores bottom-up.
            for (int y = height - 1; y >= 0; y--) {
                int src = y * width;
                int o = 0;
                for (int x = 0; x < width; x++) {
                    int c = argb[src + x];
                    row[o++] = (byte) (c & 0xFF);
                    row[o++] = (byte) ((c >>> 8) & 0xFF);
                    row[o++] = (byte) ((c >>> 16) & 0xFF);
                }
                while (o < rowStride) {
                    row[o++] = 0;
                }
                fos.write(row);
            }
            fos.close();
            System.out.println("corej2-m3g: dump " + out.getAbsolutePath());
            System.out.flush();
        } catch (Throwable t) {
            System.out.println("corej2-m3g: dump failed " + name + " " + t.getMessage());
            System.out.flush();
        }
    }

    private static void put16(byte[] b, int o, int v) {
        b[o] = (byte) (v & 0xFF);
        b[o + 1] = (byte) ((v >>> 8) & 0xFF);
    }

    private static void put32(byte[] b, int o, int v) {
        b[o] = (byte) (v & 0xFF);
        b[o + 1] = (byte) ((v >>> 8) & 0xFF);
        b[o + 2] = (byte) ((v >>> 16) & 0xFF);
        b[o + 3] = (byte) ((v >>> 24) & 0xFF);
    }
}
