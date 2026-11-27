package javax.microedition.m3g;

import java.io.IOException;
import java.io.InputStream;

/** Clean-room JSR-184 {@code Loader} stub — returns empty until .m3g decode lands. */
public final class Loader {
    private Loader() {}
    public static Object3D[] load(byte[] data, int offset) throws IOException {
        System.out.println("corej2-m3g: Loader.load(bytes) stub");
        return new Object3D[0];
    }
    public static Object3D[] load(String name) throws IOException {
        System.out.println("corej2-m3g: Loader.load(" + name + ") stub");
        return new Object3D[0];
    }
    public static Object3D[] load(InputStream stream) throws IOException {
        System.out.println("corej2-m3g: Loader.load(stream) stub");
        return new Object3D[0];
    }
}
