package javax.microedition.media.control;

import javax.microedition.media.Control;

/** Clean-room MMAPI {@code VolumeControl} (JSR-135). */
public interface VolumeControl extends Control {
    int getLevel();
    boolean isMuted();
    int setLevel(int level);
    void setMute(boolean mute);
}
