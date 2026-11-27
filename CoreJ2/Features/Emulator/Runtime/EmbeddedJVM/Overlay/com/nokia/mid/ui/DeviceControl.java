package com.nokia.mid.ui;

import dev.abadx.corej2.bootstrap.VibrationBridge;

/**
 * Clean-room Nokia UI {@code DeviceControl}.
 */
public class DeviceControl {

    public DeviceControl() {
    }

    public static void flashLights(long duration) {
    }

    public static void setLights(int num, int level) {
    }

    public static void startVibra(int freq, long duration) {
        int ms = duration > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) duration;
        VibrationBridge.vibrate(ms);
    }

    public static void stopVibra() {
        VibrationBridge.vibrate(0);
    }
}
