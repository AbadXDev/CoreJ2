package javax.microedition.rms;

/** Clean-room MIDP {@code InvalidRecordIDException} (JSR-118). */
public class InvalidRecordIDException extends RecordStoreException {
    public InvalidRecordIDException() { super(); }
    public InvalidRecordIDException(String message) { super(message); }
}
