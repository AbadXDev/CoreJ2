package javax.microedition.media;

/** Clean-room MMAPI {@code MediaException} (JSR-135). */
public class MediaException extends Exception {
    public MediaException() { super(); }
    public MediaException(String reason) { super(reason); }
}
