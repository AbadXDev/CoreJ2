package javax.microedition.io;

import java.io.IOException;

/** Clean-room GCF {@code ConnectionNotFoundException}. */
public class ConnectionNotFoundException extends IOException {
    public ConnectionNotFoundException() { super(); }
    public ConnectionNotFoundException(String message) { super(message); }
}
