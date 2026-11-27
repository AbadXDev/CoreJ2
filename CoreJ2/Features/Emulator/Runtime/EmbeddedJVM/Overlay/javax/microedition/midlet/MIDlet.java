package javax.microedition.midlet;

import java.util.HashMap;

import javax.microedition.lcdui.Display;

/**
 * Clean-room MIDP 2.0 {@code MIDlet} (JSR-118).
 * Wired to the CoreJ2 Display singleton and embedded session model
 * (no {@code System.exit}).
 */
public abstract class MIDlet {

    public static HashMap<String, String> properties;

    private Display display;

    protected MIDlet() {
        System.out.println("Create MIDlet");
        Display existing = Display.getDisplay(this);
        if (existing != null) {
            display = existing;
        } else {
            display = new Display();
        }
    }

    public final int checkPermission(String permission) {
        System.out.println("corej2-midlet: checkPermission " + permission);
        return -1;
    }

    protected abstract void destroyApp(boolean unconditional) throws MIDletStateChangeException;

    public String getAppProperty(String key) {
        if (properties == null || key == null) {
            return null;
        }
        return properties.get(key);
    }

    public static void initAppProperties(HashMap<String, String> initProperties) {
        properties = initProperties;
    }

    public final void notifyDestroyed() {
        System.out.println("corej2-midlet: notifyDestroyed ignored (VM retained)");
    }

    public final void notifyPaused() {
        System.out.println("corej2-midlet: notifyPaused");
    }

    protected abstract void pauseApp();

    public final boolean platformRequest(String URL) {
        System.out.println("corej2-midlet: platformRequest " + URL);
        return false;
    }

    public final void resumeRequest() {
        System.out.println("corej2-midlet: resumeRequest");
    }

    protected abstract void startApp() throws MIDletStateChangeException;

    public Display getDisplay() {
        return display;
    }
}
