package javax.microedition.media;

import java.io.IOException;
import java.io.InputStream;

import org.recompile.mobile.PlatformPlayer;

/**
 * Clean-room MMAPI {@code Manager} (JSR-135).
 * Creates {@link PlatformPlayer} instances backed by {@code AudioBridge}.
 */
public final class Manager {

    public static final String TONE_DEVICE_LOCATOR = "device://tone";

    private Manager() {
    }

    public static Player createPlayer(InputStream stream, String type)
            throws IOException, MediaException {
        if (stream == null) {
            throw new IllegalArgumentException();
        }
        System.out.println("corej2-media: createPlayer stream type=" + type);
        return new PlatformPlayer(stream, type);
    }

    public static Player createPlayer(String locator) throws MediaException {
        System.out.println("corej2-media: createPlayer locator=" + locator);
        return new PlatformPlayer(locator);
    }

    public static String[] getSupportedContentTypes(String protocol) {
        return new String[] {
            "audio/midi", "audio/x-wav", "audio/amr", "audio/mpeg"
        };
    }

    public static String[] getSupportedProtocols(String contentType) {
        return new String[0];
    }

    public static void playTone(int note, int duration, int volume) {
        System.out.println("corej2-media: playTone note=" + note);
    }
}
