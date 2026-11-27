package dev.abadx.corej2.bootstrap;

/**
 * CoreJ2 vibration bridge — forwards MIDP / OEM vibrate requests to Swift.
 *
 * <p>Uses the supported {@code (I)V} JNI trampoline. Duration is milliseconds;
 * {@code 0} means stop.
 */
public final class VibrationBridge {

    private VibrationBridge() {
    }

    /** Swift: vibrate for {@code durationMs} (0 = cancel). */
    public static native void nativeVibrate(int durationMs);

    public static void vibrate(int durationMs) {
        int ms = durationMs;
        if (ms < 0) {
            ms = 0;
        }
        // Cap runaway game loops (e.g. Integer.MAX_VALUE).
        if (ms > 10000) {
            ms = 10000;
        }
        System.out.println("corej2-vibrate: " + ms + "ms");
        System.out.flush();
        nativeVibrate(ms);
    }
}
