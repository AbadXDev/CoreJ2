package javax.microedition.io;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/** Clean-room GCF {@code InputConnection}. */
public interface InputConnection extends Connection {
    InputStream openInputStream() throws IOException;
    DataInputStream openDataInputStream() throws IOException;
}
