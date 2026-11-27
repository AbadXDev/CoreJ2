package javax.microedition.lcdui;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.microedition.midlet.MIDlet;

import dev.abadx.corej2.bootstrap.VibrationBridge;
import org.recompile.mobile.Mobile;

/**
 * Clean-room MIDP 2.0 {@code Display} (JSR-118).
 * Vibration routed through CoreJ2 {@link VibrationBridge}.
 */
public class Display {

    public static final int LIST_ELEMENT = 1;
    public static final int CHOICE_GROUP_ELEMENT = 2;
    public static final int ALERT = 3;

    public static final int COLOR_BACKGROUND = 0;
    public static final int COLOR_FOREGROUND = 1;
    public static final int COLOR_HIGHLIGHTED_BACKGROUND = 2;
    public static final int COLOR_HIGHLIGHTED_FOREGROUND = 3;
    public static final int COLOR_BORDER = 4;
    public static final int COLOR_HIGHLIGHTED_BORDER = 5;

    private static Display display;

    private Displayable current;
    public Vector<Runnable> serialCalls;
    private final Timer timer;

    public Display() {
        display = this;
        Mobile.setDisplay(this);
        serialCalls = new Vector<Runnable>(16);
        timer = new Timer("corej2-display-serial", true);
        timer.schedule(new TimerTask() {
            public void run() {
                if (!serialCalls.isEmpty()) {
                    try {
                        Runnable r = serialCalls.get(0);
                        serialCalls.removeElementAt(0);
                        r.run();
                    } catch (Exception ignored) {
                    }
                }
            }
        }, 0, 17);
        System.out.println("corej2-lcdui: Display ready");
    }

    public void callSerially(Runnable r) {
        if (r != null) {
            serialCalls.addElement(r);
        }
    }

    public boolean flashBacklight(int duration) {
        return true;
    }

    public int getBestImageHeight(int imageType) {
        int h = Mobile.getPlatform().lcdHeight;
        if (imageType == LIST_ELEMENT || imageType == CHOICE_GROUP_ELEMENT) {
            return h / 8;
        }
        return h;
    }

    public int getBestImageWidth(int imageType) {
        return Mobile.getPlatform().lcdWidth;
    }

    public int getBorderStyle(boolean highlighted) {
        return Graphics.SOLID;
    }

    public int getColor(int colorSpecifier) {
        switch (colorSpecifier) {
            case COLOR_BACKGROUND:
                return 0x000000;
            case COLOR_FOREGROUND:
                return 0xFFFFFF;
            case COLOR_HIGHLIGHTED_BACKGROUND:
                return 0xFFFFFF;
            case COLOR_HIGHLIGHTED_FOREGROUND:
                return 0x000000;
            case COLOR_BORDER:
                return 0x808080;
            case COLOR_HIGHLIGHTED_BORDER:
                return 0xFFFFFF;
            default:
                return 0;
        }
    }

    public Displayable getCurrent() {
        return current;
    }

    public static Display getDisplay(MIDlet m) {
        return display;
    }

    public boolean isColor() {
        return true;
    }

    public int numAlphaLevels() {
        return 256;
    }

    public int numColors() {
        return 16777216;
    }

    public void setCurrent(Displayable next) {
        if (next == null) {
            return;
        }
        try {
            if (current != null && current != next) {
                current.hideNotify();
            }
            next.showNotify();
            current = next;
            current.notifySetCurrent();
            if (current.platformImage != null) {
                Mobile.getPlatform().flushGraphics(
                        current.platformImage, 0, 0, current.width, current.height);
            }
        } catch (Exception e) {
            System.out.println("corej2-lcdui: setCurrent " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setCurrent(Alert alert, Displayable next) {
        if (alert == null) {
            setCurrent(next);
            return;
        }
        alert.setNextScreen(next);
        setCurrent(alert);
    }

    public void setCurrentItem(Item item) {
        // High-level Forms/Items still evolve; keep a no-op for MIDlets that call it.
    }

    public boolean vibrate(int duration) {
        VibrationBridge.vibrate(duration);
        return true;
    }
}
