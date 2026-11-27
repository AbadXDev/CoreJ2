package org.recompile.mobile;

import java.net.URL;

import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.GameCanvas;
import javax.microedition.m3g.Graphics3D;

/**
 * CoreJ2 overlay — same FQCN as FreeJ2ME {@code MobilePlatform}, without AWT.
 * {@link #getLCDImage()} exposes the software framebuffer for LcdSupport.
 */
public class MobilePlatform {

    private PlatformImage lcd;
    private PlatformGraphics gc;
    public int lcdWidth;
    public int lcdHeight;

    public MIDletLoader loader;

    public Runnable painter;

    public String dataPath = "";

    public int keyState = 0;

    public MobilePlatform(int width, int height) {
        lcdWidth = width;
        lcdHeight = height;

        lcd = new PlatformImage(width, height);
        gc = lcd.getGraphics();

        Mobile.setGraphics3D(new Graphics3D());

        // No javax.sound on iOS Zero — PlatformPlayer would touch MidiSystem and
        // poison jdk.internal.module.SystemModuleFinders via missing libjimage.
        Mobile.sound = false;

        // RMS writes under dataPath + "./rms/..."; sandbox forbids cwd. Prefer
        // java.home (Documents) which the embed already materializes as writable.
        String home = System.getProperty("java.home");
        if (home != null && !home.isEmpty()) {
            if (!home.endsWith("/")) {
                home = home + "/";
            }
            dataPath = home;
        }

        painter = new Runnable() {
            public void run() {
                // Placeholder
            }
        };
    }

    public void resizeLCD(int width, int height) {
        lcdWidth = width;
        lcdHeight = height;
        lcd = new PlatformImage(width, height);
        gc = lcd.getGraphics();
    }

    /** Embedded capture API — returns the software LCD image. */
    public PlatformImage getLCDImage() {
        return lcd;
    }

    /**
     * Compatibility shim for FreeJ2ME callers that expected a BufferedImage.
     * On the overlay this returns the {@link PlatformImage} itself.
     */
    public PlatformImage getLCD() {
        return lcd;
    }

    public void setPainter(Runnable r) {
        painter = r;
    }

    public void keyPressed(int keycode) {
        System.out.println("corej2-input: keyPressed " + keycode);
        System.out.flush();
        updateKeyState(keycode, 1);
        final javax.microedition.lcdui.Displayable current = Mobile.getDisplay().getCurrent();
        if (current != null) {
            current.keyPressed(keycode);
        }
    }

    public void keyReleased(int keycode) {
        System.out.println("corej2-input: keyReleased " + keycode);
        System.out.flush();
        updateKeyState(keycode, 0);
        final javax.microedition.lcdui.Displayable current = Mobile.getDisplay().getCurrent();
        if (current != null) {
            current.keyReleased(keycode);
        }
    }

    public void keyRepeated(int keycode) {
        final javax.microedition.lcdui.Displayable current = Mobile.getDisplay().getCurrent();
        if (current != null) {
            current.keyRepeated(keycode);
        }
    }

    public void pointerDragged(int x, int y) {
        final javax.microedition.lcdui.Displayable current = Mobile.getDisplay().getCurrent();
        if (current != null) {
            current.pointerDragged(x, y);
        }
    }

    public void pointerPressed(int x, int y) {
        System.out.println("corej2-input: pointerPressed " + x + "," + y);
        System.out.flush();
        final javax.microedition.lcdui.Displayable current = Mobile.getDisplay().getCurrent();
        if (current != null) {
            current.pointerPressed(x, y);
            // Diamond Rush (and similar menus) compute the hit index inside paint()
            // after storing pointer coords — force a synchronous paint now.
            if (current instanceof javax.microedition.lcdui.Canvas) {
                ((javax.microedition.lcdui.Canvas) current).repaint();
            }
        }
    }

    public void pointerReleased(int x, int y) {
        System.out.println("corej2-input: pointerReleased " + x + "," + y);
        System.out.flush();
        final javax.microedition.lcdui.Displayable current = Mobile.getDisplay().getCurrent();
        if (current != null) {
            // Ensure any paint-side hit test from pointerPressed has run before
            // the MIDlet reads the selected index on release.
            if (current instanceof javax.microedition.lcdui.Canvas) {
                ((javax.microedition.lcdui.Canvas) current).repaint();
            }
            current.pointerReleased(x, y);
        }
    }

    private void updateKeyState(int key, int val) {
        int mask = 0;
        switch (key) {
            case Mobile.KEY_NUM2: mask = GameCanvas.UP_PRESSED; break;
            case Mobile.KEY_NUM4: mask = GameCanvas.LEFT_PRESSED; break;
            case Mobile.KEY_NUM6: mask = GameCanvas.RIGHT_PRESSED; break;
            case Mobile.KEY_NUM8: mask = GameCanvas.DOWN_PRESSED; break;
            case Mobile.KEY_NUM5: mask = GameCanvas.FIRE_PRESSED; break;
            case Mobile.KEY_NUM1: mask = GameCanvas.GAME_A_PRESSED; break;
            case Mobile.KEY_NUM3: mask = GameCanvas.GAME_B_PRESSED; break;
            case Mobile.KEY_NUM7: mask = GameCanvas.GAME_C_PRESSED; break;
            case Mobile.KEY_NUM9: mask = GameCanvas.GAME_D_PRESSED; break;
            case Mobile.NOKIA_UP: mask = GameCanvas.UP_PRESSED; break;
            case Mobile.NOKIA_LEFT: mask = GameCanvas.LEFT_PRESSED; break;
            case Mobile.NOKIA_RIGHT: mask = GameCanvas.RIGHT_PRESSED; break;
            case Mobile.NOKIA_DOWN: mask = GameCanvas.DOWN_PRESSED; break;
            case Mobile.NOKIA_SOFT3: mask = GameCanvas.FIRE_PRESSED; break;
            default: break;
        }
        keyState |= mask;
        keyState ^= mask;
        if (val == 1) {
            keyState |= mask;
        }
    }

    public boolean loadJar(String jarurl) {
        try {
            URL jar = new URL(jarurl);
            loader = new MIDletLoader(new URL[] { jar });
            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void runJar() {
        try {
            System.out.println("corej2-platform: runJar begin");
            System.out.flush();
            loader.start();
            System.out.println("corej2-platform: runJar done");
            System.out.flush();
        } catch (Throwable e) {
            System.out.println("Error Running Jar");
            e.printStackTrace();
            System.out.flush();
            // Re-throw so RunJarSupport can mark failure without System.exit.
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * Tears down the current MIDlet so {@link #loadJar}/{@link #runJar} can
     * start another game on the same HotSpot VM (one VM per process).
     */
    public void stopMidlet() {
        System.out.println("corej2-platform: stopMidlet begin");
        System.out.flush();
        // Silence MMAPI before / after destroy — many MIDlets never close Players
        // in destroyApp, and Swift AVMIDIPlayer would keep looping otherwise.
        try {
            dev.abadx.corej2.bootstrap.AudioBridge.stopAll();
        } catch (Throwable t) {
            System.out.println("corej2-platform: stopMidlet audio " + t);
        }
        try {
            if (loader != null) {
                loader.destroyCurrent();
            }
        } catch (Throwable t) {
            System.out.println("corej2-platform: stopMidlet loader " + t);
            t.printStackTrace();
        }
        loader = null;
        try {
            dev.abadx.corej2.bootstrap.AudioBridge.stopAll();
        } catch (Throwable ignored) {
        }
        try {
            // Blank LCD so the previous game's pixels don't linger.
            if (gc != null && lcd != null) {
                gc.setColor(0x000000);
                gc.fillRect(0, 0, lcdWidth, lcdHeight);
                if (painter != null) {
                    painter.run();
                }
            }
        } catch (Throwable ignored) {
        }
        System.out.println("corej2-platform: stopMidlet done");
        System.out.flush();
    }

    public void flushGraphics(Image img, int x, int y, int width, int height) {
        gc.flushGraphics(img, x, y, width, height);
        painter.run();
    }

    public void repaint(Image img, int x, int y, int width, int height) {
        gc.flushGraphics(img, x, y, width, height);
        painter.run();
    }
}
