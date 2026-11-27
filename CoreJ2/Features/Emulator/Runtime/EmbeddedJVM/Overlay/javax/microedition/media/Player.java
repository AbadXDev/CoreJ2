package javax.microedition.media;

/** Clean-room MMAPI {@code Player} (JSR-135). */
public interface Player extends Controllable {
    int UNREALIZED = 100;
    int REALIZED = 200;
    int PREFETCHED = 300;
    int STARTED = 400;
    int CLOSED = 0;
    long TIME_UNKNOWN = -1L;

    void addPlayerListener(PlayerListener playerListener);
    void removePlayerListener(PlayerListener playerListener);
    void realize() throws MediaException;
    void prefetch() throws MediaException;
    void start() throws MediaException;
    void stop() throws MediaException;
    void deallocate();
    void close();
    void setLoopCount(int count);
    long setMediaTime(long now) throws MediaException;
    long getMediaTime();
    long getDuration();
    String getContentType();
    int getState();
}
