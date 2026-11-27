package javax.microedition.lcdui;

/** Clean-room MIDP 2.0 {@code Gauge} (minimal). */
public class Gauge extends Item {
    public static final int INDEFINITE = -1;
    public static final int CONTINUOUS_IDLE = 0;
    public static final int INCREMENTAL_IDLE = 1;
    public static final int CONTINUOUS_RUNNING = 2;
    public static final int INCREMENTAL_UPDATING = 3;

    private int value;
    private int maxValue;
    private boolean interactive;

    public Gauge(String label, boolean interactive, int maxValue, int initialValue) {
        setLabel(label);
        this.interactive = interactive;
        this.maxValue = maxValue;
        setValue(initialValue);
    }

    public int getValue() { return value; }
    public void setValue(int value) {
        if (maxValue == INDEFINITE) { this.value = value; return; }
        if (value < 0) value = 0;
        if (value > maxValue) value = maxValue;
        this.value = value;
    }
    public int getMaxValue() { return maxValue; }
    public void setMaxValue(int maxValue) { this.maxValue = maxValue; setValue(value); }
    public boolean isInteractive() { return interactive; }
}
