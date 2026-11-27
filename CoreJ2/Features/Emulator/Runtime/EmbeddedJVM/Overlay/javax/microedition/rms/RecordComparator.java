package javax.microedition.rms;

/** Clean-room MIDP {@code RecordComparator} (JSR-118). */
public interface RecordComparator {
    int EQUIVALENT = 0;
    int FOLLOWS = 1;
    int PRECEDES = -1;
    int compare(byte[] rec1, byte[] rec2);
}
