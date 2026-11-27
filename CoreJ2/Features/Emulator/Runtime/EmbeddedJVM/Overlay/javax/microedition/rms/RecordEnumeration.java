package javax.microedition.rms;

/** Clean-room MIDP {@code RecordEnumeration} (JSR-118). */
public interface RecordEnumeration {
    void destroy();
    boolean hasNextElement();
    boolean hasPreviousElement();
    boolean isKeptUpdated();
    void keepUpdated(boolean keepUpdated);
    byte[] nextRecord() throws InvalidRecordIDException, RecordStoreException;
    int nextRecordId() throws InvalidRecordIDException;
    int numRecords();
    byte[] previousRecord() throws InvalidRecordIDException, RecordStoreException;
    int previousRecordId() throws InvalidRecordIDException;
    void rebuild();
    void reset();
}
