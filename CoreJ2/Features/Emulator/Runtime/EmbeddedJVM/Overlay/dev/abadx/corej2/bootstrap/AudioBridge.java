package dev.abadx.corej2.bootstrap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CoreJ2 MMAPI audio bridge — no javax.sound.
 *
 * <p>Writes media bytes under {@code java.home/corej2-audio/} then notifies Swift
 * via {@code (I)V} natives (supported by the embedded JNI trampoline). Swift reads
 * path/mime/volume/loop through the static getters and plays with AVAudio*.
 */
public final class AudioBridge {

    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);
    private static final Map<Integer, String> PATHS = new ConcurrentHashMap<Integer, String>();
    private static final Map<Integer, String> MIMES = new ConcurrentHashMap<Integer, String>();
    private static final Map<Integer, Integer> VOLUMES = new ConcurrentHashMap<Integer, Integer>();
    private static final Map<Integer, Integer> LOOPS = new ConcurrentHashMap<Integer, Integer>();

    private AudioBridge() {
    }

    /** Swift: prepare/open player for id (read path/mime via getters). */
    public static native void nativePrepare(int playerId);

    /** Swift: start playback. */
    public static native void nativeStart(int playerId);

    /** Swift: stop playback. */
    public static native void nativeStop(int playerId);

    /** Swift: release player resources. */
    public static native void nativeClose(int playerId);

    /** Swift: apply volume 0–100. */
    public static native void nativeSetVolume(int playerId);

    public static String getPath(int playerId) {
        return PATHS.get(playerId);
    }

    public static String getMime(int playerId) {
        return MIMES.get(playerId);
    }

    public static int getVolume(int playerId) {
        Integer v = VOLUMES.get(playerId);
        return v == null ? 100 : v.intValue();
    }

    public static int getLoopCount(int playerId) {
        Integer v = LOOPS.get(playerId);
        return v == null ? 1 : v.intValue();
    }

    /**
     * Materializes {@code data} on disk and asks Swift to prepare a player.
     *
     * @return player id, or {@code -1} on failure
     */
    public static int createPlayer(byte[] data, String mimeType) {
        if (data == null || data.length == 0) {
            return -1;
        }
        final String mime = mimeType == null ? "" : mimeType;
        final int id = NEXT_ID.getAndIncrement();
        try {
            final File dir = audioDirectory();
            if (!dir.exists() && !dir.mkdirs()) {
                System.out.println("corej2-audio: mkdir failed " + dir);
                return -1;
            }
            final String ext = extensionForMime(mime);
            final File out = new File(dir, "p" + id + ext);
            FileOutputStream fos = new FileOutputStream(out);
            try {
                fos.write(data);
            } finally {
                fos.close();
            }
            PATHS.put(id, out.getAbsolutePath());
            MIMES.put(id, mime);
            VOLUMES.put(id, Integer.valueOf(100));
            LOOPS.put(id, Integer.valueOf(1));
            System.out.println("corej2-audio: create id=" + id + " mime=" + mime + " bytes=" + data.length);
            System.out.flush();
            nativePrepare(id);
            return id;
        } catch (Throwable t) {
            System.out.println("corej2-audio: create failed " + t);
            t.printStackTrace();
            return -1;
        }
    }

    public static void start(int playerId) {
        if (playerId > 0) {
            nativeStart(playerId);
        }
    }

    public static void stop(int playerId) {
        if (playerId > 0) {
            nativeStop(playerId);
        }
    }

    public static void close(int playerId) {
        if (playerId <= 0) {
            return;
        }
        nativeClose(playerId);
        String path = PATHS.remove(playerId);
        MIMES.remove(playerId);
        VOLUMES.remove(playerId);
        LOOPS.remove(playerId);
        if (path != null) {
            try {
                new File(path).delete();
            } catch (Throwable ignored) {
            }
        }
    }

    /**
     * Stops and releases every live player. Called from {@code MobilePlatform.stopMidlet}
     * so AVAudio/MIDI cannot keep playing after the user leaves a game.
     */
    public static void stopAll() {
        java.util.ArrayList<Integer> ids = new java.util.ArrayList<Integer>(PATHS.keySet());
        System.out.println("corej2-audio: stopAll count=" + ids.size());
        System.out.flush();
        for (int i = 0; i < ids.size(); i++) {
            int id = ids.get(i).intValue();
            try {
                nativeStop(id);
            } catch (Throwable ignored) {
            }
            try {
                close(id);
            } catch (Throwable ignored) {
            }
        }
    }

    public static void setVolume(int playerId, int level) {
        if (playerId <= 0) {
            return;
        }
        int clamped = level;
        if (clamped < 0) {
            clamped = 0;
        }
        if (clamped > 100) {
            clamped = 100;
        }
        VOLUMES.put(playerId, Integer.valueOf(clamped));
        nativeSetVolume(playerId);
    }

    public static void setLoopCount(int playerId, int count) {
        if (playerId <= 0) {
            return;
        }
        LOOPS.put(playerId, Integer.valueOf(count));
    }

    private static File audioDirectory() {
        String home = System.getProperty("java.home");
        if (home == null || home.length() == 0) {
            home = ".";
        }
        return new File(home, "corej2-audio");
    }

    private static String extensionForMime(String mime) {
        String m = mime.toLowerCase();
        if (m.indexOf("wav") >= 0) {
            return ".wav";
        }
        if (m.indexOf("mid") >= 0 || m.indexOf("sp-midi") >= 0) {
            return ".mid";
        }
        if (m.indexOf("mpeg") >= 0 || m.indexOf("mp3") >= 0) {
            return ".mp3";
        }
        return ".bin";
    }
}
