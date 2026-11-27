package dev.abadx.corej2.bootstrap;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import org.recompile.mobile.MobilePlatform;

/**
 * Invokes {@link MobilePlatform#runJar()} and verifies MIDlet bring-up.
 *
 * <p>Probe fixtures print {@code START_APP_OK}; real games only print
 * {@code Create MIDlet} from the FreeJ2ME {@code MIDlet} constructor. Success
 * for production JARs is therefore {@code Create MIDlet} without construction /
 * start failure markers.
 */
public final class RunJarSupport {

    static final String CREATE_MIDLET_MARKER = "Create MIDlet";
    static final String START_APP_OK_MARKER = "START_APP_OK";

    private RunJarSupport() {
    }

    /**
     * JNI: {@code runAndVerifyStartApp(Lorg/recompile/mobile/MobilePlatform;)Z}
     */
    public static boolean runAndVerifyStartApp(MobilePlatform platform) {
        if (platform == null) {
            return false;
        }

        final PrintStream originalOut = System.out;
        final PrintStream originalErr = System.err;
        final ByteArrayOutputStream captured = new ByteArrayOutputStream();
        final PrintStream tee = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
                captured.write(b);
                originalOut.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) {
                captured.write(b, off, len);
                originalOut.write(b, off, len);
            }
        }, true);

        System.out.println("corej2-runjar: invoking MobilePlatform.runJar");
        System.out.flush();

        System.setOut(tee);
        System.setErr(tee);
        Throwable thrown = null;
        try {
            platform.runJar();
        } catch (Throwable t) {
            thrown = t;
            t.printStackTrace(tee);
        } finally {
            tee.flush();
            System.setOut(originalOut);
            System.setErr(originalErr);
        }

        final String output = captured.toString();
        System.err.println("corej2-runjar: captured " + output.length() + " chars");
        if (!output.isEmpty()) {
            System.err.println("corej2-runjar: ---- begin ----");
            System.err.print(output);
            if (!output.endsWith("\n")) {
                System.err.println();
            }
            System.err.println("corej2-runjar: ---- end ----");
        }
        if (thrown != null) {
            System.err.println("corej2-runjar: thrown " + thrown);
        }

        final boolean constructed = output.contains(CREATE_MIDLET_MARKER);
        final boolean probeOk = output.contains(START_APP_OK_MARKER);
        final boolean failed =
                output.contains("Error Running Jar")
                        || output.contains("Problem Constructing")
                        || output.contains("Can't Find startApp Method")
                        || thrown != null;

        // Fixtures: both markers. Real MIDlets: Create MIDlet without failure markers.
        final boolean ok;
        if (probeOk) {
            ok = constructed && !failed;
        } else {
            ok = constructed && !failed;
        }

        if (ok) {
            // Give the MIDlet paint thread a moment, then report LCD fill.
            try {
                Thread.sleep(800);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            try {
                final int nonzero = LcdSupport.nonZeroPixelCount(platform);
                System.out.println("corej2-runjar: lcdNonZeroPixels=" + nonzero);
                System.out.flush();
            } catch (Throwable t) {
                System.err.println("corej2-runjar: lcd probe failed " + t);
            }
        }
        return ok;
    }
}
