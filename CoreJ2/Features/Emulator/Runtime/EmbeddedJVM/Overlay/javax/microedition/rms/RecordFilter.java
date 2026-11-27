package javax.microedition.rms;

/** Clean-room MIDP {@code RecordFilter} (JSR-118). */
public interface RecordFilter {
    boolean matches(byte[] candidate);
}
