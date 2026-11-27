package javax.microedition.lcdui;

/** Clean-room MIDP 2.0 {@code Item} (JSR-118) — base for Form elements. */
public abstract class Item {
    public static final int LAYOUT_DEFAULT = 0;
    public static final int LAYOUT_LEFT = 1;
    public static final int LAYOUT_RIGHT = 2;
    public static final int LAYOUT_CENTER = 3;
    public static final int LAYOUT_NEWLINE_BEFORE = 0x100;
    public static final int LAYOUT_NEWLINE_AFTER = 0x200;
    public static final int LAYOUT_EXPAND = 0x800;
    public static final int LAYOUT_VEXPAND = 0x1000;
    public static final int LAYOUT_2 = 0x4000;
    public static final int PLAIN = 0;
    public static final int HYPERLINK = 1;
    public static final int BUTTON = 2;

    String label = "";
    int layout = LAYOUT_DEFAULT;
    ItemCommandListener commandListener;
    final java.util.Vector commands = new java.util.Vector();

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label == null ? "" : label; }
    public int getLayout() { return layout; }
    public void setLayout(int layout) { this.layout = layout; }
    public void addCommand(Command cmd) { if (cmd != null) commands.addElement(cmd); }
    public void removeCommand(Command cmd) { commands.removeElement(cmd); }
    public void setItemCommandListener(ItemCommandListener l) { commandListener = l; }
    public void setPreferredSize(int width, int height) {}
    public int getPreferredWidth() { return -1; }
    public int getPreferredHeight() { return -1; }
    public int getMinimumWidth() { return 0; }
    public int getMinimumHeight() { return 0; }
}
