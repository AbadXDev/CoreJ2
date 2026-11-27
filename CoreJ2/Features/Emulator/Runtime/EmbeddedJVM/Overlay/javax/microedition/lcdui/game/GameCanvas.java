package javax.microedition.lcdui.game;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.PlatformImage;

/**
 * Clean-room MIDP {@code GameCanvas} (JSR-118 Game API).
 */
public abstract class GameCanvas extends Canvas {

    public static final int UP_PRESSED = 1 << Canvas.UP;
    public static final int DOWN_PRESSED = 1 << Canvas.DOWN;
    public static final int LEFT_PRESSED = 1 << Canvas.LEFT;
    public static final int RIGHT_PRESSED = 1 << Canvas.RIGHT;
    public static final int FIRE_PRESSED = 1 << Canvas.FIRE;
    public static final int GAME_A_PRESSED = 1 << Canvas.GAME_A;
    public static final int GAME_B_PRESSED = 1 << Canvas.GAME_B;
    public static final int GAME_C_PRESSED = 1 << Canvas.GAME_C;
    public static final int GAME_D_PRESSED = 1 << Canvas.GAME_D;

    private boolean suppressKeyEvents;

    protected GameCanvas(boolean suppressKeyEvents) {
        this.suppressKeyEvents = suppressKeyEvents;
        width = Mobile.getPlatform().lcdWidth;
        height = Mobile.getPlatform().lcdHeight;
        platformImage = new PlatformImage(width, height);
        System.out.println("corej2-game: GameCanvas " + width + "x" + height);
    }

    protected Graphics getGraphics() {
        return platformImage.getGraphics();
    }

    public void paint(Graphics g) {
        // Subclasses typically draw via getGraphics()/flushGraphics.
    }

    public void flushGraphics(int x, int y, int w, int h) {
        Mobile.getPlatform().flushGraphics(platformImage, x, y, w, h);
    }

    public void flushGraphics() {
        flushGraphics(0, 0, width, height);
    }

    public int getKeyStates() {
        int state = Mobile.getPlatform().keyState;
        Mobile.getPlatform().keyState = 0;
        return state;
    }

    public void keyPressed(int keyCode) {
        if (!suppressKeyEvents) {
            super.keyPressed(keyCode);
        }
    }

    public void keyReleased(int keyCode) {
        if (!suppressKeyEvents) {
            super.keyReleased(keyCode);
        }
    }

    public void keyRepeated(int keyCode) {
        if (!suppressKeyEvents) {
            super.keyRepeated(keyCode);
        }
    }
}
