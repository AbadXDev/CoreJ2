package javax.microedition.rms;

/** Clean-room MIDP {@code RecordListener} (JSR-118). */
public interface RecordListener {
    void recordAdded(RecordStore recordStore, int recordId);
    void recordChanged(RecordStore recordStore, int recordId);
    void recordDeleted(RecordStore recordStore, int recordId);
}
