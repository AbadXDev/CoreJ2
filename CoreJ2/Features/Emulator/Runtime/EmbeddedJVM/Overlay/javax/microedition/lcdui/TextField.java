package javax.microedition.lcdui;

/**
 * Clean-room MIDP 2.0 {@code TextField} (JSR-118).
 * Covers constructors/methods referenced by Diamond Rush.
 */
public class TextField extends Item {
    public static final int ANY = 0;
    public static final int EMAILADDR = 1;
    public static final int NUMERIC = 2;
    public static final int PHONENUMBER = 3;
    public static final int URL = 4;
    public static final int DECIMAL = 5;
    public static final int PASSWORD = 0x10000;
    public static final int UNEDITABLE = 0x20000;
    public static final int SENSITIVE = 0x40000;
    public static final int NON_PREDICTIVE = 0x80000;
    public static final int INITIAL_CAPS_WORD = 0x100000;
    public static final int INITIAL_CAPS_SENTENCE = 0x200000;
    public static final int CONSTRAINT_MASK = 0xFFFF;

    private String text = "";
    private int maxSize;
    private int constraints;

    public TextField(String label, String text, int maxSize, int constraints) {
        setLabel(label);
        if (maxSize <= 0) throw new IllegalArgumentException("maxSize");
        this.maxSize = maxSize;
        this.constraints = constraints;
        setString(text);
    }

    public String getString() { return text; }

    public void setString(String text) {
        String t = text == null ? "" : text;
        if (t.length() > maxSize) {
            t = t.substring(0, maxSize);
        }
        this.text = t;
    }

    public int getMaxSize() { return maxSize; }
    public int setMaxSize(int maxSize) {
        if (maxSize <= 0) throw new IllegalArgumentException();
        this.maxSize = maxSize;
        if (text.length() > maxSize) text = text.substring(0, maxSize);
        return this.maxSize;
    }
    public int getConstraints() { return constraints; }
    public void setConstraints(int constraints) { this.constraints = constraints; }
    public int size() { return text.length(); }
    public void delete(int offset, int length) {
        if (offset < 0 || length < 0 || offset + length > text.length()) throw new StringIndexOutOfBoundsException();
        text = text.substring(0, offset) + text.substring(offset + length);
    }
    public void insert(String src, int position) {
        String s = src == null ? "" : src;
        if (position < 0 || position > text.length()) throw new StringIndexOutOfBoundsException();
        String n = text.substring(0, position) + s + text.substring(position);
        setString(n);
    }
    public int getCaretPosition() { return text.length(); }
}
