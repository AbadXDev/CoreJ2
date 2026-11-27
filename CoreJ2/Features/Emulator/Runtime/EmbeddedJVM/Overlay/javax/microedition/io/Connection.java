package javax.microedition.io;

import java.io.IOException;

/** Clean-room GCF {@code Connection} (CLDC/MIDP). */
public interface Connection {
    void close() throws IOException;
}
