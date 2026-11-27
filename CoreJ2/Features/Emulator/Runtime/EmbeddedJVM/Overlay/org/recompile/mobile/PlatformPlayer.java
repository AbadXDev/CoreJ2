package org.recompile.mobile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import javax.microedition.media.Control;
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;
import javax.microedition.media.control.VolumeControl;

import dev.abadx.corej2.bootstrap.AudioBridge;

/**
 * CoreJ2 overlay — MMAPI player that forwards MIDI/WAV to Swift via {@link AudioBridge}.
 * Same FQCN as FreeJ2ME; never touches {@code javax.sound} (no jsound / jimage poison).
 */
public class PlatformPlayer implements Player {

    private String contentType = "";
    private int state = Player.UNREALIZED;
    private final Vector listeners = new Vector();
    private final Control[] controls;
    private int playerId = -1;
    private int loopCount = 1;
    private boolean running = false;

    public PlatformPlayer(InputStream stream, String type) {
        listeners.clear();
        controls = new Control[3];
        contentType = type == null ? "" : type;
        controls[0] = new BridgeVolumeControl();
        controls[1] = new StubControl();
        controls[2] = new StubControl();

        try {
            byte[] data = readAll(stream);
            playerId = AudioBridge.createPlayer(data, contentType);
            if (playerId > 0) {
                state = Player.PREFETCHED;
            } else {
                System.out.println("corej2-audio: PlatformPlayer create failed type=" + contentType);
                state = Player.REALIZED;
            }
        } catch (Exception e) {
            System.out.println("corej2-audio: PlatformPlayer ctor " + e);
            e.printStackTrace();
            state = Player.REALIZED;
        }
    }

    public PlatformPlayer(String locator) {
        controls = new Control[3];
        controls[0] = new BridgeVolumeControl();
        controls[1] = new StubControl();
        controls[2] = new StubControl();
        contentType = locator == null ? "" : locator;
        System.out.println("corej2-audio: locator player unsupported: " + locator);
        state = Player.REALIZED;
    }

    public void close() {
        try {
            stop();
        } catch (Exception ignored) {
        }
        AudioBridge.close(playerId);
        playerId = -1;
        state = Player.CLOSED;
        notifyListeners(PlayerListener.CLOSED, null);
    }

    public int getState() {
        return state;
    }

    public void start() {
        if (state == Player.CLOSED) {
            return;
        }
        AudioBridge.setLoopCount(playerId, loopCount);
        AudioBridge.start(playerId);
        running = true;
        state = Player.STARTED;
        notifyListeners(PlayerListener.STARTED, Long.valueOf(0));
    }

    public void stop() {
        AudioBridge.stop(playerId);
        running = false;
        if (state == Player.STARTED) {
            state = Player.PREFETCHED;
            notifyListeners(PlayerListener.STOPPED, Long.valueOf(0));
        }
    }

    public void addPlayerListener(PlayerListener playerListener) {
        listeners.addElement(playerListener);
    }

    public void removePlayerListener(PlayerListener playerListener) {
        listeners.removeElement(playerListener);
    }

    private void notifyListeners(String event, Object eventData) {
        for (int i = 0; i < listeners.size(); i++) {
            ((PlayerListener) listeners.elementAt(i)).playerUpdate(this, event, eventData);
        }
    }

    public void deallocate() {
        stop();
        AudioBridge.close(playerId);
        playerId = -1;
        state = Player.CLOSED;
    }

    public String getContentType() {
        return contentType;
    }

    public long getDuration() {
        return Player.TIME_UNKNOWN;
    }

    public long getMediaTime() {
        return 0;
    }

    public void prefetch() {
        if (state != Player.CLOSED) {
            state = Player.PREFETCHED;
        }
    }

    public void realize() {
        if (state == Player.UNREALIZED) {
            state = Player.REALIZED;
        }
    }

    public void setLoopCount(int count) {
        loopCount = count;
        AudioBridge.setLoopCount(playerId, count);
    }

    public long setMediaTime(long now) {
        return now;
    }

    public Control getControl(String controlType) {
        if (controlType == null) {
            return null;
        }
        if (controlType.equals("VolumeControl")
                || controlType.equals("javax.microedition.media.control.VolumeControl")) {
            return controls[0];
        }
        if (controlType.equals("TempoControl")
                || controlType.equals("javax.microedition.media.control.TempoControl")) {
            return controls[1];
        }
        if (controlType.equals("MIDIControl")
                || controlType.equals("javax.microedition.media.control.MIDIControl")) {
            return controls[2];
        }
        return null;
    }

    public Control[] getControls() {
        return controls;
    }

    public static void readInputStreamData(InputStream input, byte[] output, int offset, int nBytes)
            throws IOException {
        int end = offset + nBytes;
        while (offset < end) {
            int read = input.read(output, offset, end - offset);
            if (read < 0) {
                throw new java.io.EOFException();
            }
            offset += read;
        }
    }

    private static byte[] readAll(InputStream stream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int n;
        while ((n = stream.read(chunk)) != -1) {
            buffer.write(chunk, 0, n);
        }
        return buffer.toByteArray();
    }

    private final class BridgeVolumeControl implements VolumeControl {
        private int level = 100;
        private boolean muted = false;

        public int getLevel() {
            return level;
        }

        public boolean isMuted() {
            return muted;
        }

        public int setLevel(int newLevel) {
            level = newLevel;
            if (level < 0) {
                level = 0;
            }
            if (level > 100) {
                level = 100;
            }
            if (!muted) {
                AudioBridge.setVolume(playerId, level);
            }
            return level;
        }

        public void setMute(boolean mute) {
            muted = mute;
            AudioBridge.setVolume(playerId, mute ? 0 : level);
        }
    }

    private static final class StubControl implements Control {
    }
}
