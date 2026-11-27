package javax.microedition.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.MobilePlatform;

/**
 * Clean-room GCF {@code Connector} — safe stubs; HTTP never blocks on network.
 */
public class Connector {

    public static final int READ = 1;
    public static final int READ_WRITE = 3;
    public static final int WRITE = 2;

    public static Connection open(String name) {
        return open(name, READ_WRITE, false);
    }

    public static Connection open(String name, int mode) {
        return open(name, mode, false);
    }

    public static Connection open(String name, int mode, boolean timeouts) {
        System.out.println("corej2-io: Connector.open " + name + " mode=" + mode);
        System.out.flush();
        // HTTP/socket not implemented — return stubs so MIDlets that probe
        // networking (ads, scores) during boot/menus do not ClassCast/hang.
        if (name != null) {
            String lower = name.toLowerCase();
            if (lower.startsWith("http:") || lower.startsWith("https:")) {
                return new StubHttpConnection(name);
            }
        }
        return new StubConnection();
    }

    public static InputStream openInputStream(String name) {
        if (name != null && name.startsWith("resource:")) {
            try {
                MobilePlatform platform = Mobile.getPlatform();
                if (platform != null && platform.loader != null) {
                    return platform.loader.getMIDletResourceAsSiemensStream(name.substring(9));
                }
            } catch (Throwable ignored) {
            }
            return new FakeInputStream();
        }
        System.out.println("corej2-io: Faked InputStream for " + name);
        return new FakeInputStream();
    }

    public static DataInputStream openDataInputStream(String name) {
        return new DataInputStream(openInputStream(name));
    }

    public static OutputStream openOutputStream(String name) {
        System.out.println("corej2-io: Faked OutputStream for " + name);
        return new FakeOutputStream();
    }

    public static DataOutputStream openDataOutputStream(String name) {
        return new DataOutputStream(openOutputStream(name));
    }

    private static final class StubConnection implements Connection {
        public void close() {
        }
    }

    /**
     * Minimal {@link HttpConnection} so casts from {@code Connector.open}
     * succeed. All reads EOF immediately; response code 200.
     */
    private static final class StubHttpConnection implements HttpConnection {
        private final String url;

        StubHttpConnection(String url) {
            this.url = url;
        }

        public void close() {
        }

        public String getURL() {
            return url;
        }

        public String getProtocol() {
            return url != null && url.toLowerCase().startsWith("https") ? "https" : "http";
        }

        public String getHost() {
            return "";
        }

        public String getFile() {
            return "";
        }

        public String getRef() {
            return null;
        }

        public String getQuery() {
            return null;
        }

        public int getPort() {
            return 80;
        }

        public String getRequestMethod() {
            return HttpConnection.GET;
        }

        public void setRequestMethod(String method) {
        }

        public String getRequestProperty(String key) {
            return null;
        }

        public void setRequestProperty(String key, String value) {
        }

        public int getResponseCode() {
            return HttpConnection.HTTP_OK;
        }

        public String getResponseMessage() {
            return "OK";
        }

        public long getExpiration() {
            return 0;
        }

        public long getDate() {
            return 0;
        }

        public long getLastModified() {
            return 0;
        }

        public String getHeaderField(String name) {
            return null;
        }

        public int getHeaderFieldInt(String name, int def) {
            return def;
        }

        public long getHeaderFieldDate(String name, long def) {
            return def;
        }

        public String getHeaderField(int n) {
            return null;
        }

        public String getHeaderFieldKey(int n) {
            return null;
        }

        public String getType() {
            return "text/plain";
        }

        public String getEncoding() {
            return null;
        }

        public long getLength() {
            return 0;
        }

        public DataInputStream openDataInputStream() {
            return new DataInputStream(openInputStream());
        }

        public InputStream openInputStream() {
            return new FakeInputStream();
        }

        public DataOutputStream openDataOutputStream() {
            return new DataOutputStream(openOutputStream());
        }

        public OutputStream openOutputStream() {
            return new FakeOutputStream();
        }
    }

    private static final class FakeInputStream extends InputStream {
        public int available() {
            return 0;
        }

        public void close() {
        }

        public int read() {
            return -1;
        }

        public int read(byte[] b, int off, int len) {
            return -1;
        }
    }

    private static final class FakeOutputStream extends OutputStream {
        public void write(int b) {
        }

        public void close() {
        }
    }
}
