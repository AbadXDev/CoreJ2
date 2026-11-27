package javax.microedition.media;

/** Clean-room MMAPI {@code Controllable} (JSR-135). */
public interface Controllable {
    Control getControl(String controlType);
    Control[] getControls();
}
