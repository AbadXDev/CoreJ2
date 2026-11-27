package javax.microedition.lcdui;

/** Clean-room MIDP 2.0 {@code Ticker}. */
public class Ticker {
    private String text = "";
    public Ticker(String str) { setString(str); }
    public String getString() { return text; }
    public void setString(String str) { text = str == null ? "" : str; }
}
