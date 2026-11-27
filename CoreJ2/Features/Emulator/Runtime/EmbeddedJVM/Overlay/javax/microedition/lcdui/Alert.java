package javax.microedition.lcdui;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.PlatformImage;

/**
 * Clean-room MIDP 2.0 {@code Alert} (JSR-118) — minimal implementation.
 */
public class Alert extends Screen {

    public static final int FOREVER = -2;
    public static final Command DISMISS_COMMAND = new Command("OK", Command.OK, 0);

    public CommandListener defaultListener;
    private int timeout = FOREVER;
    private String text = "";
    private Image image;
    private AlertType type;
    private Gauge indicator;
    private Displayable nextScreen;

    public Alert(String title) {
        this(title, null, null, null);
    }

    public Alert(String title, String alertText, Image alertImage, AlertType alertType) {
        setTitle(title);
        text = alertText == null ? "" : alertText;
        image = alertImage;
        type = alertType;
        platformImage = new PlatformImage(width, height);
        defaultListener = new CommandListener() {
            public void commandAction(Command c, Displayable d) {
                Displayable next = nextScreen;
                if (next != null) {
                    Mobile.getDisplay().setCurrent(next);
                }
            }
        };
        addCommand(DISMISS_COMMAND);
        setCommandListener(defaultListener);
        paintAlert();
    }

    private void paintAlert() {
        if (platformImage == null) {
            return;
        }
        Graphics g = platformImage.getGraphics();
        g.setColor(0x000000);
        g.fillRect(0, 0, width, height);
        g.setColor(0xFFFFFF);
        String t = getTitle();
        if (t != null) {
            g.drawString(t, width / 2, 8, Graphics.HCENTER | Graphics.TOP);
        }
        g.drawString(text, 8, 32, Graphics.LEFT | Graphics.TOP);
        if (image != null) {
            g.drawImage(image, width / 2, height / 2, Graphics.HCENTER | Graphics.VCENTER);
        }
    }

    public int getDefaultTimeout() {
        return 5000;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int time) {
        timeout = time;
    }

    public AlertType getType() {
        return type;
    }

    public void setType(AlertType type) {
        this.type = type;
    }

    public String getString() {
        return text;
    }

    public void setString(String str) {
        text = str == null ? "" : str;
        paintAlert();
    }

    public Image getImage() {
        return image;
    }

    public void setImage(Image img) {
        image = img;
        paintAlert();
    }

    public void setIndicator(Gauge indicator) {
        this.indicator = indicator;
    }

    public Gauge getIndicator() {
        return indicator;
    }

    public void addCommand(Command cmd) {
        super.addCommand(cmd);
    }

    public void removeCommand(Command cmd) {
        if (cmd != DISMISS_COMMAND) {
            super.removeCommand(cmd);
        }
    }

    public void setCommandListener(CommandListener l) {
        super.setCommandListener(l == null ? defaultListener : l);
    }

    public void setNextScreen(Displayable next) {
        nextScreen = next;
    }

    public void notifySetCurrent() {
        paintAlert();
        if (isShown() && platformImage != null) {
            Mobile.getPlatform().repaint(platformImage, 0, 0, width, height);
        }
    }
}
