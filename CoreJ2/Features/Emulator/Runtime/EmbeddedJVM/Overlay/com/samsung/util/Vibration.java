package com.samsung.util;

import dev.abadx.corej2.bootstrap.VibrationBridge;

public final class Vibration
{
	public static boolean isSupported() { return true; }

	public static void start(int duration, int strength) {
		VibrationBridge.vibrate(duration);
	}

	public static void stop() {
		VibrationBridge.vibrate(0);
	}
}
