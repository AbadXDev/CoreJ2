package javax.microedition.lcdui;

import org.recompile.mobile.PlatformImage;

/**
 * Clean-room MIDP 2.0 {@code Form} (JSR-118) — minimal implementation
 * covering the methods referenced by Diamond Rush and similar titles.
 */
public class Form extends Screen {

    public ItemStateListener listener;

    public Form(String title) {
        setTitle(title);
        platformImage = new PlatformImage(width, height);
        render();
    }

    public Form(String title, Item[] elements) {
        setTitle(title);
        if (elements != null) {
            for (int i = 0; i < elements.length; i++) {
                if (elements[i] != null) {
                    items.add(elements[i]);
                }
            }
        }
        platformImage = new PlatformImage(width, height);
        render();
    }

    public int append(Image img) {
        return append(new ImageItem("", img, 0, ""));
    }

    public int append(Item item) {
        if (item == null) {
            throw new NullPointerException();
        }
        items.add(item);
        if (currentItem < 0) {
            currentItem = 0;
        }
        render();
        return items.size() - 1;
    }

    public int append(String str) {
        return append(new StringItem("", str == null ? "" : str));
    }

    public void delete(int itemNum) {
        items.remove(itemNum);
        if (currentItem >= items.size()) {
            currentItem = items.size() - 1;
        }
        render();
    }

    public void deleteAll() {
        items.clear();
        currentItem = -1;
        render();
    }

    public Item get(int itemNum) {
        return items.get(itemNum);
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public void insert(int itemNum, Item item) {
        items.add(itemNum, item);
        render();
    }

    public void set(int itemNum, Item item) {
        items.set(itemNum, item);
        render();
    }

    public void setItemStateListener(ItemStateListener iListener) {
        listener = iListener;
    }

    public int size() {
        return items.size();
    }

    public void keyPressed(int keyCode) {
        if (items.isEmpty()) {
            keyPressedCommands(keyCode);
            return;
        }
        int action = 0;
        // Map common keys without requiring a Canvas instance.
        if (keyCode == Canvas.KEY_NUM2 || keyCode == -1) {
            action = Canvas.UP;
        } else if (keyCode == Canvas.KEY_NUM8 || keyCode == -2) {
            action = Canvas.DOWN;
        } else if (keyCode == Canvas.KEY_NUM5 || keyCode == -5) {
            action = Canvas.FIRE;
        }
        if (action == Canvas.UP) {
            if (currentItem > 0) {
                currentItem--;
                render();
            }
            return;
        }
        if (action == Canvas.DOWN) {
            if (currentItem < items.size() - 1) {
                currentItem++;
                render();
            }
            return;
        }
        if (action == Canvas.FIRE && commandlistener != null && !commands.isEmpty()) {
            commandlistener.commandAction(commands.get(0), this);
            return;
        }
        keyPressedCommands(keyCode);
    }

    public void notifySetCurrent() {
        render();
    }
}
