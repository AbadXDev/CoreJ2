package javax.microedition.rms;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Vector;

import org.recompile.mobile.Mobile;

/**
 * Clean-room MIDP {@code RecordStore} (JSR-118).
 * Persists under {@code <dataPath>./rms/<suitename>/<store>} using the same
 * on-disk layout already used by the app's savedata UI
 * ({@code uint16} version, nextId, count, then length-prefixed records).
 */
public class RecordStore {

    public static final int AUTHMODE_PRIVATE = 0;
    public static final int AUTHMODE_ANY = 1;

    private static String rmsPath = "";

    private final Vector records = new Vector();
    private final Vector listeners = new Vector();
    private final String name;
    private final String appname;
    private final String rmsFile;
    private final File file;

    private int version;
    private int nextid;
    private int lastModified;
    private boolean open = true;

    private RecordStore(String storeName, boolean createIfNecessary)
            throws RecordStoreException, RecordStoreNotFoundException {
        records.addElement(new byte[0]); // index 0 unused; record IDs start at 1
        name = storeName;
        String suite = "suite";
        try {
            suite = Mobile.getPlatform().loader.suitename;
        } catch (Exception ignored) {
        }
        appname = suite == null ? "suite" : suite;

        String dataPath = "";
        try {
            dataPath = Mobile.getPlatform().dataPath;
        } catch (Exception ignored) {
        }
        if (dataPath == null) {
            dataPath = "";
        }
        rmsPath = dataPath + "./rms/" + appname;
        rmsFile = rmsPath + "/" + storeName;

        try {
            Files.createDirectories(Paths.get(rmsPath));
        } catch (Exception e) {
            throw new RecordStoreException("Problem Creating Record Store Path " + rmsPath);
        }

        file = new File(rmsFile);
        if (!file.exists()) {
            if (!createIfNecessary) {
                throw new RecordStoreNotFoundException("Record Store Doesn't Exist: " + rmsFile);
            }
            try {
                file.createNewFile();
                version = 1;
                nextid = 1;
                save();
                nextid = 1;
            } catch (Exception e) {
                throw new RecordStoreException(
                        "Problem Opening Record Store (createIfNecessary" + createIfNecessary + "): " + rmsFile);
            }
        }

        try {
            byte[] data = Files.readAllBytes(file.toPath());
            if (data.length >= 4) {
                int pos = 0;
                version = getUInt16(data, pos);
                pos += 2;
                nextid = getUInt16(data, pos);
                pos += 2;
                int count = getUInt16(data, pos);
                pos += 2;
                for (int i = 0; i < count; i++) {
                    int len = getUInt16(data, pos);
                    pos += 2;
                    loadRecord(data, pos, len);
                    pos += len;
                }
            }
        } catch (Exception e) {
            System.out.println("corej2-rms: read failed " + rmsFile + " " + e.getMessage());
            throw new RecordStoreException("Problem Reading Record Store: " + rmsFile);
        }
        System.out.println("corej2-rms: open " + rmsFile + " records=" + getNumRecords());
    }

    public static RecordStore openRecordStore(String recordStoreName, boolean createIfNecessary)
            throws RecordStoreException, RecordStoreNotFoundException {
        return new RecordStore(recordStoreName, createIfNecessary);
    }

    public static RecordStore openRecordStore(
            String recordStoreName, boolean createIfNecessary, int authmode, boolean writable)
            throws RecordStoreException, RecordStoreNotFoundException {
        return openRecordStore(recordStoreName, createIfNecessary);
    }

    public static RecordStore openRecordStore(String recordStoreName, String vendorName, String suiteName)
            throws RecordStoreException, RecordStoreNotFoundException {
        return openRecordStore(recordStoreName, false);
    }

    public static void deleteRecordStore(String recordStoreName) throws RecordStoreException {
        try {
            String suite = Mobile.getPlatform().loader.suitename;
            String dataPath = Mobile.getPlatform().dataPath;
            File f = new File(dataPath + "./rms/" + suite + "/" + recordStoreName);
            if (f.exists() && !f.delete()) {
                throw new RecordStoreException("delete failed");
            }
        } catch (RecordStoreException e) {
            throw e;
        } catch (Exception e) {
            throw new RecordStoreException(e.getMessage());
        }
    }

    public static String[] listRecordStores() {
        try {
            String suite = Mobile.getPlatform().loader.suitename;
            String dataPath = Mobile.getPlatform().dataPath;
            File dir = new File(dataPath + "./rms/" + suite);
            String[] names = dir.list();
            return names == null ? null : names;
        } catch (Exception e) {
            return null;
        }
    }

    public int addRecord(byte[] data, int offset, int numBytes) throws RecordStoreException {
        ensureOpen();
        try {
            byte[] copy = Arrays.copyOfRange(data, offset, offset + numBytes);
            records.addElement(copy);
            lastModified = nextid;
            nextid++;
            version++;
            save();
            fireAdded(lastModified);
            return lastModified;
        } catch (Exception e) {
            throw new RecordStoreException("Can't Add RMS Record");
        }
    }

    public void setRecord(int recordId, byte[] newData, int offset, int numBytes)
            throws RecordStoreException, InvalidRecordIDException {
        ensureOpen();
        if (recordId <= 0) {
            throw new InvalidRecordIDException("Invalid Record ID: " + recordId);
        }
        // Observable: several matrix titles call setRecord(1, …) on a freshly
        // created empty store (no prior addRecord). Grow the sparse id space.
        while (records.size() <= recordId) {
            records.addElement(new byte[0]);
        }
        if (recordId >= nextid) {
            nextid = recordId + 1;
        }
        byte[] copy = Arrays.copyOfRange(newData, offset, offset + numBytes);
        records.setElementAt(copy, recordId);
        version++;
        lastModified = (int) (System.currentTimeMillis() & 0x7fffffff);
        save();
        fireChanged(recordId);
    }

    public void deleteRecord(int recordId) throws RecordStoreException, InvalidRecordIDException {
        ensureOpen();
        getRecordBytes(recordId); // validate
        records.setElementAt(new byte[0], recordId);
        version++;
        save();
        fireDeleted(recordId);
    }

    public byte[] getRecord(int recordId) throws InvalidRecordIDException, RecordStoreException {
        ensureOpen();
        byte[] data = getRecordBytes(recordId);
        if (data.length == 0) {
            return null;
        }
        return data;
    }

    public int getRecord(int recordId, byte[] buffer, int offset)
            throws InvalidRecordIDException, RecordStoreException {
        byte[] data = getRecord(recordId);
        if (data == null) {
            return 0;
        }
        System.arraycopy(data, 0, buffer, offset, data.length);
        return data.length;
    }

    public int getRecordSize(int recordId) throws InvalidRecordIDException, RecordStoreException {
        byte[] data = getRecordBytes(recordId);
        return data.length;
    }

    public int getNumRecords() {
        int n = 0;
        for (int i = 1; i < records.size(); i++) {
            if (((byte[]) records.elementAt(i)).length > 0) {
                n++;
            }
        }
        return n;
    }

    public int getNextRecordID() {
        return nextid;
    }

    public int getSize() {
        int total = 0;
        for (int i = 1; i < records.size(); i++) {
            total += ((byte[]) records.elementAt(i)).length;
        }
        return total;
    }

    public int getSizeAvailable() {
        return 1024 * 1024;
    }

    public int getVersion() {
        return version;
    }

    public long getLastModified() {
        return lastModified;
    }

    public String getName() {
        return name;
    }

    public void closeRecordStore() {
        open = false;
        System.out.println("corej2-rms: close " + name);
    }

    public void setMode(int authmode, boolean writable) {
    }

    public void addRecordListener(RecordListener listener) {
        if (listener != null) {
            listeners.addElement(listener);
        }
    }

    public void removeRecordListener(RecordListener listener) {
        listeners.removeElement(listener);
    }

    public RecordEnumeration enumerateRecords(RecordFilter filter, RecordComparator comparator, boolean keepUpdated) {
        return new EnumerationImpl(filter, comparator, keepUpdated);
    }

    private byte[] getRecordBytes(int recordId) throws InvalidRecordIDException {
        if (recordId <= 0 || recordId >= records.size()) {
            throw new InvalidRecordIDException("Invalid Record ID: " + recordId);
        }
        return (byte[]) records.elementAt(recordId);
    }

    private void ensureOpen() throws RecordStoreNotOpenException {
        if (!open) {
            throw new RecordStoreNotOpenException();
        }
    }

    private void loadRecord(byte[] data, int offset, int length) {
        byte[] copy = Arrays.copyOfRange(data, offset, offset + length);
        if (copy == null) {
            copy = new byte[0];
        }
        records.addElement(copy);
    }

    private void save() {
        try {
            byte[] hdr = new byte[2];
            FileOutputStream out = new FileOutputStream(rmsFile);
            setUInt16(hdr, 0, version);
            out.write(hdr);
            setUInt16(hdr, 0, nextid);
            out.write(hdr);
            setUInt16(hdr, 0, records.size() - 1);
            out.write(hdr);
            for (int i = 1; i < records.size(); i++) {
                byte[] rec = (byte[]) records.elementAt(i);
                setUInt16(hdr, 0, rec.length);
                out.write(hdr);
                out.write(rec);
            }
            out.close();
        } catch (Exception e) {
            System.out.println("corej2-rms: save failed " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static int getUInt16(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
    }

    private static void setUInt16(byte[] data, int offset, int value) {
        data[offset] = (byte) ((value >> 8) & 0xFF);
        data[offset + 1] = (byte) (value & 0xFF);
    }

    private void fireAdded(int id) {
        for (int i = 0; i < listeners.size(); i++) {
            ((RecordListener) listeners.elementAt(i)).recordAdded(this, id);
        }
    }

    private void fireChanged(int id) {
        for (int i = 0; i < listeners.size(); i++) {
            ((RecordListener) listeners.elementAt(i)).recordChanged(this, id);
        }
    }

    private void fireDeleted(int id) {
        for (int i = 0; i < listeners.size(); i++) {
            ((RecordListener) listeners.elementAt(i)).recordDeleted(this, id);
        }
    }

    private final class EnumerationImpl implements RecordEnumeration {
        private int index;
        private int[] elements = new int[0];
        private int count;
        private boolean keepupdated;
        private final RecordFilter filter;
        private final RecordComparator comparator;

        EnumerationImpl(RecordFilter filter, RecordComparator comparator, boolean keepUpdated) {
            this.filter = filter;
            this.comparator = comparator;
            this.keepupdated = keepUpdated;
            build();
        }

        private void build() {
            int[] tmp = new int[records.size() + 1];
            count = 0;
            for (int id = 1; id < records.size(); id++) {
                byte[] rec = (byte[]) records.elementAt(id);
                if (rec.length == 0) {
                    continue;
                }
                if (filter != null && !filter.matches(rec)) {
                    continue;
                }
                tmp[count++] = id;
            }
            elements = new int[count];
            System.arraycopy(tmp, 0, elements, 0, count);
            if (comparator != null) {
                // simple bubble using comparator on record bytes
                for (int i = 0; i < count; i++) {
                    for (int j = i + 1; j < count; j++) {
                        byte[] a = (byte[]) records.elementAt(elements[i]);
                        byte[] b = (byte[]) records.elementAt(elements[j]);
                        if (comparator.compare(a, b) == RecordComparator.FOLLOWS) {
                            int s = elements[i];
                            elements[i] = elements[j];
                            elements[j] = s;
                        }
                    }
                }
            }
            index = 0;
        }

        public void destroy() {
            elements = new int[0];
            count = 0;
        }

        public boolean hasNextElement() {
            if (keepupdated) {
                rebuild();
            }
            return index < count;
        }

        public boolean hasPreviousElement() {
            return index > 0;
        }

        public boolean isKeptUpdated() {
            return keepupdated;
        }

        public void keepUpdated(boolean keepUpdated) {
            this.keepupdated = keepUpdated;
        }

        public byte[] nextRecord() throws InvalidRecordIDException, RecordStoreException {
            return RecordStore.this.getRecord(nextRecordId());
        }

        public int nextRecordId() throws InvalidRecordIDException {
            if (!hasNextElement()) {
                throw new InvalidRecordIDException();
            }
            return elements[index++];
        }

        public int numRecords() {
            if (keepupdated) {
                rebuild();
            }
            return count;
        }

        public byte[] previousRecord() throws InvalidRecordIDException, RecordStoreException {
            return RecordStore.this.getRecord(previousRecordId());
        }

        public int previousRecordId() throws InvalidRecordIDException {
            if (!hasPreviousElement()) {
                throw new InvalidRecordIDException();
            }
            return elements[--index];
        }

        public void rebuild() {
            build();
        }

        public void reset() {
            index = 0;
        }
    }
}
