package javax.microedition.lcdui;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.PlatformGraphics;
import org.recompile.mobile.PlatformImage;

/**
 * Clean-room MIDP 2.0 {@code Canvas} (JSR-118).
 * Paints into a {@link PlatformImage} framebuffer and flushes via MobilePlatform.
 */
public abstract class Canvas extends Displayable {

    public static final int UP = 1;
    public static final int DOWN = 6;
    public static final int LEFT = 2;
    public static final int RIGHT = 5;
    public static final int FIRE = 8;
    public static final int GAME_A = 9;
    public static final int GAME_B = 10;
    public static final int GAME_C = 11;
    public static final int GAME_D = 12;

    public static final int KEY_NUM0 = 48;
    public static final int KEY_NUM1 = 49;
    public static final int KEY_NUM2 = 50;
    public static final int KEY_NUM3 = 51;
    public static final int KEY_NUM4 = 52;
    public static final int KEY_NUM5 = 53;
    public static final int KEY_NUM6 = 54;
    public static final int KEY_NUM7 = 55;
    public static final int KEY_NUM8 = 56;
    public static final int KEY_NUM9 = 57;
    public static final int KEY_STAR = 42;
    public static final int KEY_POUND = 35;

    protected Canvas() {
        width = Mobile.getPlatform().lcdWidth;
        height = Mobile.getPlatform().lcdHeight;
        platformImage = new PlatformImage(width, height);
        System.out.println("corej2-lcdui: Canvas " + width + "x" + height);
    }

    protected abstract void paint(Graphics g);

    public void repaint() {
        repaint(0, 0, width, height);
    }

    public void repaint(int x, int y, int w, int h) {
        try {
            PlatformGraphics g = platformImage.getGraphics();
            g.reset();
            paint(g);
            Display display = Mobile.getDisplay();
            if (display != null && display.getCurrent() == this) {
                Mobile.getPlatform().repaint(platformImage, x, y, w, h);
            }
        } catch (Exception e) {
            System.out.println("corej2-lcdui: Canvas.repaint " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void serviceRepaints() {
        Mobile.getPlatform().repaint(platformImage, 0, 0, width, height);
    }

    public void setFullScreenMode(boolean mode) {
        fullScreen = mode;
        if (fullScreen) {
            width = Mobile.getPlatform().lcdWidth;
            height = Mobile.getPlatform().lcdHeight;
        }
    }

    public boolean isDoubleBuffered() {
        return true;
    }

    public boolean hasPointerEvents() {
        return true;
    }

    public boolean hasPointerMotionEvents() {
        return true;
    }

    public boolean hasRepeatEvents() {
        return true;
    }

    public void showNotify() {
    }

    public void hideNotify() {
    }

    public void keyPressed(int keyCode) {
    }

    public void keyReleased(int keyCode) {
    }

    public void keyRepeated(int keyCode) {
    }

    public void pointerPressed(int x, int y) {
    }

    public void pointerReleased(int x, int y) {
    }

    public void pointerDragged(int x, int y) {
    }

    protected void sizeChanged(int w, int h) {
        width = w;
        height = h;
    }

    public void notifySetCurrent() {
        repaint();
    }

    public int getGameAction(int keyCode) {
        switch (keyCode) {
            case -1:
            case KEY_NUM2:
                return UP;
            case -2:
            case KEY_NUM8:
                return DOWN;
            case -3:
            case KEY_NUM4:
                return LEFT;
            case -4:
            case KEY_NUM6:
                return RIGHT;
            case -5:
            case KEY_NUM5:
                return FIRE;
            case KEY_NUM1:
                return GAME_A;
            case KEY_NUM3:
                return GAME_B;
            case KEY_NUM7:
                return GAME_C;
            case KEY_NUM9:
                return GAME_D;
            default:
                return 0;
        }
    }

    public int getKeyCode(int gameAction) {
        switch (gameAction) {
            case UP:
                return -1;
            case LEFT:
                return -3;
            case RIGHT:
                return -4;
            case DOWN:
                return -2;
            case FIRE:
                return -5;
            case GAME_A:
                return KEY_NUM1;
            case GAME_B:
                return KEY_NUM3;
            case GAME_C:
                return KEY_NUM7;
            case GAME_D:
                return KEY_NUM9;
            default:
                return -5;
        }
    }

    public String getKeyName(int keyCode) {
        return String.valueOf(keyCode);
    }
}
