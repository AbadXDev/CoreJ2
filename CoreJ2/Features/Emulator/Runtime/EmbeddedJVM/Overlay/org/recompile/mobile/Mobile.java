package org.recompile.mobile;

import java.io.InputStream;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Display;
import javax.microedition.m3g.Graphics3D;

/**
 * CoreJ2 platform registry (clean-room).
 *
 * <p>Holds the active {@link MobilePlatform}, {@link Display}, and
 * {@link Graphics3D} for the embedded runtime. Keycode constants mirror
 * public MIDP / OEM values observed by matrix titles.
 */
public final class Mobile {

    private static MobilePlatform platform;
    private static Display display;
    private static Graphics3D graphics3d;

    public static boolean quiet = false;
    public static boolean nokia = false;
    public static boolean siemens = false;
    public static boolean motorola = false;
    public static boolean sound = true;

    public static final int KEY_NUM0 = Canvas.KEY_NUM0;
    public static final int KEY_NUM1 = Canvas.KEY_NUM1;
    public static final int KEY_NUM2 = Canvas.KEY_NUM2;
    public static final int KEY_NUM3 = Canvas.KEY_NUM3;
    public static final int KEY_NUM4 = Canvas.KEY_NUM4;
    public static final int KEY_NUM5 = Canvas.KEY_NUM5;
    public static final int KEY_NUM6 = Canvas.KEY_NUM6;
    public static final int KEY_NUM7 = Canvas.KEY_NUM7;
    public static final int KEY_NUM8 = Canvas.KEY_NUM8;
    public static final int KEY_NUM9 = Canvas.KEY_NUM9;
    public static final int KEY_STAR = Canvas.KEY_STAR;
    public static final int KEY_POUND = Canvas.KEY_POUND;
    public static final int GAME_UP = Canvas.UP;
    public static final int GAME_DOWN = Canvas.DOWN;
    public static final int GAME_LEFT = Canvas.LEFT;
    public static final int GAME_RIGHT = Canvas.RIGHT;
    public static final int GAME_FIRE = Canvas.FIRE;
    public static final int GAME_A = Canvas.GAME_A;
    public static final int GAME_B = Canvas.GAME_B;
    public static final int GAME_C = Canvas.GAME_C;
    public static final int GAME_D = Canvas.GAME_D;

    public static final int NOKIA_UP = -1;
    public static final int NOKIA_DOWN = -2;
    public static final int NOKIA_LEFT = -3;
    public static final int NOKIA_RIGHT = -4;
    public static final int NOKIA_SOFT1 = -6;
    public static final int NOKIA_SOFT2 = -7;
    public static final int NOKIA_SOFT3 = -5;
    public static final int NOKIA_END = -11;
    public static final int NOKIA_SEND = -10;

    public static final int SIEMENS_UP = -59;
    public static final int SIEMENS_DOWN = -60;
    public static final int SIEMENS_LEFT = -61;
    public static final int SIEMENS_RIGHT = -62;
    public static final int SIEMENS_SOFT1 = -1;
    public static final int SIEMENS_SOFT2 = -4;
    public static final int SIEMENS_FIRE = -26;

    public static final int MOTOROLA_UP = -1;
    public static final int MOTOROLA_DOWN = -6;
    public static final int MOTOROLA_LEFT = -2;
    public static final int MOTOROLA_RIGHT = -5;
    public static final int MOTOROLA_SOFT1 = -21;
    public static final int MOTOROLA_SOFT2 = -22;
    public static final int MOTOROLA_FIRE = -20;

    private Mobile() {}

    public static MobilePlatform getPlatform() { return platform; }
    public static void setPlatform(MobilePlatform p) { platform = p; }

    public static Display getDisplay() { return display; }
    public static void setDisplay(Display d) { display = d; }

    public static Graphics3D getGraphics3D() { return graphics3d; }
    public static void setGraphics3D(Graphics3D g) { graphics3d = g; }

    /** Used by MIDletLoader bytecode rewrite of {@code Class.getResourceAsStream}. */
    public static InputStream getResourceAsStream(Class c, String resource) {
        if (platform == null || platform.loader == null) {
            return c != null ? c.getResourceAsStream(resource) : null;
        }
        return platform.loader.getMIDletResourceAsStream(resource);
    }

    public static InputStream getMIDletResourceAsStream(String resource) {
        if (platform == null || platform.loader == null) {
            return null;
        }
        return platform.loader.getMIDletResourceAsStream(resource);
    }

    public static void log(String text) {
        if (!quiet) {
            System.out.println(text);
        }
    }
}
