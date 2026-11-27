package dev.abadx.corej2.bootstrap;

import org.recompile.mobile.MobilePlatform;
import org.recompile.mobile.PlatformImage;

/**
 * LCD framebuffer extraction for the embedded PlatformBootstrap.
 *
 * <p>Reads the CoreJ2 AWT-overlay {@link PlatformImage} int[] framebuffer and
 * packs little-endian ARGB8888 bytes for the Swift capture path. Unchanged
 * frames return {@code null} from {@link #extractPixelsIfChanged} so Swift can
 * skip JNI copies and CGImage rebuilds.
 */
public final class LcdSupport {

    private static byte[] reuseBytes;
    private static int lastHash;
    private static boolean hasLastHash;
    private static long extractCount;
    private static long skippedCount;

    private LcdSupport() {
    }

    /** LCD width in pixels. */
    public static int lcdWidth(MobilePlatform platform) {
        return platform.lcdWidth;
    }

    /** LCD height in pixels. */
    public static int lcdHeight(MobilePlatform platform) {
        return platform.lcdHeight;
    }

    /** Independent little-endian ARGB8888 copy of the current LCD contents. */
    public static byte[] extractPixels(MobilePlatform platform) {
        byte[] changed = extractPixelsIfChanged(platform);
        if (changed != null) {
            return changed;
        }
        // Force a copy even when unchanged (diagnostics / first paint).
        return pack(platform.getLCDImage().getCanvas().data, true);
    }

    /**
     * Returns a little-endian ARGB copy when the LCD content changed since the
     * last extraction; otherwise {@code null}.
     */
    public static byte[] extractPixelsIfChanged(MobilePlatform platform) {
        final PlatformImage lcd = platform.getLCDImage();
        if (lcd == null || lcd.getCanvas() == null) {
            throw new IllegalStateException("getLCDImage() returned null");
        }
        final int[] pixels = lcd.getCanvas().data;
        final int hash = contentHash(pixels);
        if (hasLastHash && hash == lastHash) {
            skippedCount++;
            return null;
        }
        lastHash = hash;
        hasLastHash = true;
        extractCount++;
        return pack(pixels, false);
    }

    /** Resets change-detection state (call on MIDlet stop / new session). */
    public static void resetFrameCache() {
        hasLastHash = false;
        lastHash = 0;
        extractCount = 0;
        skippedCount = 0;
    }

    public static long extractCount() {
        return extractCount;
    }

    public static long skippedCount() {
        return skippedCount;
    }

    /** Count of non-zero ARGB pixels — used by device diagnostics. */
    public static int nonZeroPixelCount(MobilePlatform platform) {
        final int[] pixels = platform.getLCDImage().getCanvas().data;
        int n = 0;
        for (int p : pixels) {
            if ((p & 0x00FFFFFF) != 0) {
                n++;
            }
        }
        return n;
    }

    private static byte[] pack(int[] pixels, boolean forceNew) {
        final int bytes = pixels.length * 4;
        byte[] out;
        if (forceNew) {
            out = new byte[bytes];
        } else {
            if (reuseBytes == null || reuseBytes.length != bytes) {
                reuseBytes = new byte[bytes];
            }
            out = reuseBytes;
        }
        int o = 0;
        for (int i = 0; i < pixels.length; i++) {
            final int p = pixels[i];
            out[o++] = (byte) p;
            out[o++] = (byte) (p >>> 8);
            out[o++] = (byte) (p >>> 16);
            out[o++] = (byte) (p >>> 24);
        }
        if (!forceNew) {
            // Return a defensive copy sized for Swift ownership / release.
            byte[] copy = new byte[bytes];
            System.arraycopy(out, 0, copy, 0, bytes);
            return copy;
        }
        return out;
    }

    /** Fast content fingerprint — full XOR mix, cheap vs ByteBuffer allocate. */
    private static int contentHash(int[] pixels) {
        int h = pixels.length * 0x9E3779B9;
        // Stride sampling keeps cost low on large LCDs while catching most paints.
        final int step = pixels.length > 4096 ? 8 : 1;
        for (int i = 0; i < pixels.length; i += step) {
            h = (h * 31) ^ pixels[i];
        }
        // Always fold corners / center so tiny updates aren't missed by stride.
        if (pixels.length > 0) {
            h ^= pixels[0];
            h ^= pixels[pixels.length - 1];
            h ^= pixels[pixels.length / 2];
        }
        return h;
    }
}
