package com.siemens.mp.game;

import dev.abadx.corej2.bootstrap.VibrationBridge;

public class Vibrator
{
	public static void startVibrator() {
		VibrationBridge.vibrate(500);
	}
	public static void stopVibrator() {
		VibrationBridge.vibrate(0);
	}
}
