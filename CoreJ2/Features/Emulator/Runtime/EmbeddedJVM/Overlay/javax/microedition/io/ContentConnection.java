package javax.microedition.io;

/** Clean-room GCF {@code ContentConnection}. */
public interface ContentConnection extends StreamConnection {
    String getType();
    String getEncoding();
    long getLength();
}
