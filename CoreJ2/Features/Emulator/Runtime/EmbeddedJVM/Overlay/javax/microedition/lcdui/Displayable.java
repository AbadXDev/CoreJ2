package javax.microedition.lcdui;

import java.util.ArrayList;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.PlatformGraphics;
import org.recompile.mobile.PlatformImage;

/**
 * Clean-room MIDP 2.0 {@code Displayable} (JSR-118).
 * Public {@code platformImage}/{@code width}/{@code height} keep the existing
 * framebuffer and input bridges working.
 */
public abstract class Displayable {

    public PlatformImage platformImage;
    public int width;
    public int height;
    public boolean fullScreen;
    public Ticker ticker;

    protected String title = "";
    protected ArrayList<Command> commands = new ArrayList<Command>();
    protected ArrayList<Item> items = new ArrayList<Item>();
    protected CommandListener commandlistener;
    protected boolean listCommands;
    protected int currentCommand;
    protected int currentItem = -1;

    public Displayable() {
        width = Mobile.getPlatform().lcdWidth;
        height = Mobile.getPlatform().lcdHeight;
    }

    public void addCommand(Command cmd) {
        if (cmd == null) {
            throw new NullPointerException();
        }
        if (!commands.contains(cmd)) {
            commands.add(cmd);
        }
    }

    public void removeCommand(Command cmd) {
        if (cmd != null) {
            commands.remove(cmd);
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String s) {
        title = s == null ? "" : s;
    }

    public boolean isShown() {
        Display d = Mobile.getDisplay();
        return d != null && d.getCurrent() == this;
    }

    public Ticker getTicker() {
        return ticker;
    }

    public void setTicker(Ticker ticker) {
        this.ticker = ticker;
    }

    public void setCommandListener(CommandListener l) {
        commandlistener = l;
    }

    public Display getDisplay() {
        return Mobile.getDisplay();
    }

    public ArrayList<Command> getCommands() {
        return commands;
    }

    public void keyPressed(int keyCode) {
    }

    public void keyReleased(int keyCode) {
    }

    public void keyRepeated(int keyCode) {
    }

    public void pointerDragged(int x, int y) {
    }

    public void pointerPressed(int x, int y) {
    }

    public void pointerReleased(int x, int y) {
    }

    public void showNotify() {
    }

    public void hideNotify() {
    }

    public void notifySetCurrent() {
    }

    protected void sizeChanged(int w, int h) {
    }

    protected void render() {
        if (listCommands) {
            renderCommands();
        } else {
            renderItems();
        }
    }

    protected void renderCommands() {
        renderItems();
    }

    public void renderItems() {
        if (platformImage == null) {
            return;
        }
        PlatformGraphics g = platformImage.getGraphics();
        g.reset();
        g.setColor(0xFFFFFF);
        g.fillRect(0, 0, width, height);
        g.setColor(0x000000);
        int y = 4;
        if (title != null && title.length() > 0) {
            g.drawString(title, width / 2, y, Graphics.HCENTER | Graphics.TOP);
            y += Font.getDefaultFont().getHeight() + 4;
        }
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            String label = item.getLabel();
            String text = label == null ? "" : label;
            if (item instanceof StringItem) {
                String t = ((StringItem) item).getText();
                if (t != null) {
                    text = text.length() == 0 ? t : text + ": " + t;
                }
            } else if (item instanceof TextField) {
                String t = ((TextField) item).getString();
                if (t != null) {
                    text = text.length() == 0 ? t : text + ": " + t;
                }
            }
            if (i == currentItem) {
                g.setColor(0x0000AA);
                g.fillRect(0, y - 1, width, Font.getDefaultFont().getHeight() + 2);
                g.setColor(0xFFFFFF);
            } else {
                g.setColor(0x000000);
            }
            g.drawString(text, 4, y, Graphics.LEFT | Graphics.TOP);
            y += Font.getDefaultFont().getHeight() + 4;
        }
        if (isShown()) {
            Mobile.getPlatform().repaint(platformImage, 0, 0, width, height);
        }
    }

    protected void keyPressedCommands(int keyCode) {
        if (commandlistener == null || commands.isEmpty()) {
            return;
        }
        // Soft-left / soft-right style: fire first command on FIRE-like keys.
        if (keyCode == -6 || keyCode == -7 || keyCode == -5 || keyCode == Canvas.KEY_NUM5) {
            commandlistener.commandAction(commands.get(0), this);
        }
    }
}
