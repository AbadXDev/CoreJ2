package javax.microedition.m3g;

import javax.microedition.lcdui.Image;
import org.recompile.mobile.PlatformImage;

/**
 * Clean-room JSR-184 {@code Image2D}.
 *
 * <p>Supports the constructors/methods Asphalt uses: {@code (format,w,h)},
 * {@code (format, Image|byte[])}, and rectangular {@link #set}. Pixel bytes
 * follow the JSR-184 component order (RGB / RGBA / L / LA / A).
 */
public class Image2D extends Object3D {
    public static final int ALPHA = 96;
    public static final int LUMINANCE = 97;
    public static final int LUMINANCE_ALPHA = 98;
    public static final int RGB = 99;
    public static final int RGBA = 100;

    private byte[] image;
    private int width;
    private int height;
    private int format;
    private boolean mutable = true;
    private int[] argb;

    public Image2D(int fmt, int w, int h) {
        if (w <= 0 || h <= 0) {
            throw new IllegalArgumentException("size");
        }
        format = fmt;
        width = w;
        height = h;
        image = new byte[w * h * bytesPerPixel(fmt)];
        mutable = true;
    }

    public Image2D(int fmt, int w, int h, byte[] img) {
        this(fmt, w, h);
        if (img != null) {
            set(0, 0, w, h, img);
        }
    }

    public Image2D(int fmt, int w, int h, byte[] img, byte[] palette) {
        this(fmt, w, h, img);
    }

    public Image2D(int fmt, Object img) {
        format = fmt;
        if (img instanceof Image) {
            Image src = (Image) img;
            width = src.getWidth();
            height = src.getHeight();
            argb = new int[width * height];
            src.getRGB(argb, 0, width, 0, 0, width, height);
            // Keep a packed byte mirror so later set()/getters stay coherent.
            image = packFromARGB(argb, format);
            mutable = false;
        } else if (img instanceof byte[]) {
            byte[] bytes = (byte[]) img;
            // Width/height unknown — treat as opaque until set() or size inferred.
            image = bytes;
            int bpp = bytesPerPixel(format);
            if (bpp > 0 && bytes.length >= bpp) {
                // Best-effort square guess when only bytes are provided.
                int pixels = bytes.length / bpp;
                int side = (int) Math.sqrt(pixels);
                if (side * side * bpp == bytes.length) {
                    width = side;
                    height = side;
                }
            }
            mutable = true;
        } else if (img instanceof PlatformImage) {
            PlatformImage src = (PlatformImage) img;
            width = src.getWidth();
            height = src.getHeight();
            argb = src.getCanvas().data.clone();
            image = packFromARGB(argb, format);
            mutable = false;
        } else {
            width = 1;
            height = 1;
            image = new byte[bytesPerPixel(format)];
        }
    }

    public int getFormat() {
        return format;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public boolean isMutable() {
        return mutable;
    }

    public void set(int x, int y, int w, int h, byte[] img) {
        if (!mutable) {
            throw new IllegalStateException("immutable");
        }
        if (img == null || w <= 0 || h <= 0) {
            return;
        }
        int bpp = bytesPerPixel(format);
        if (image == null || image.length < width * height * bpp) {
            image = new byte[width * height * bpp];
        }
        // Full-frame replace (common for Asphalt uploads).
        if (x == 0 && y == 0 && w == width && h == height && img.length >= width * height * bpp) {
            System.arraycopy(img, 0, image, 0, width * height * bpp);
            argb = null;
            return;
        }
        int srcStride = w * bpp;
        for (int row = 0; row < h; row++) {
            int dy = y + row;
            if (dy < 0 || dy >= height) {
                continue;
            }
            int src = row * srcStride;
            int dst = (dy * width + x) * bpp;
            int copyW = Math.min(w, width - Math.max(x, 0));
            if (x < 0) {
                src += (-x) * bpp;
                dst = (dy * width) * bpp;
                copyW = Math.min(w + x, width);
            }
            if (copyW <= 0) {
                continue;
            }
            System.arraycopy(img, src, image, dst, copyW * bpp);
        }
        argb = null;
    }

    /**
     * Sample texture with normalized or unnormalized UV, applying wrap mode.
     *
     * @param wrapS {@link Texture2D#WRAP_CLAMP} or {@link Texture2D#WRAP_REPEAT}
     * @param wrapT same for T
     * @param filter {@link Texture2D#FILTER_NEAREST} or {@link Texture2D#FILTER_LINEAR}
     */
    public int sampleARGB(float u, float v, int wrapS, int wrapT, int filter) {
        if (width <= 0 || height <= 0) {
            return 0xFFFFFFFF;
        }
        ensureARGB();
        if (argb == null) {
            return 0xFFFFFFFF;
        }
        // JSR-184: after texture transform + projection, (s'', t'') are
        // normalized with (0,0)=upper-left and (1,1)=lower-right. Always map
        // that way; wrap modes handle values outside [0,1].
        float tu = u * width;
        float tv = v * height;
        if (filter == Texture2D.FILTER_LINEAR) {
            return sampleBilinear(tu - 0.5f, tv - 0.5f, wrapS, wrapT);
        }
        int x = wrapCoord((int) Math.floor(tu), width, wrapS);
        int y = wrapCoord((int) Math.floor(tv), height, wrapT);
        return argb[y * width + x];
    }

    /** Expose decoded ARGB for QA texture dumps. */
    public int[] copyARGB() {
        ensureARGB();
        if (argb == null) {
            return null;
        }
        return argb.clone();
    }

    /** Default sample: repeat + nearest (legacy call sites). */
    public int sampleARGB(float u, float v) {
        return sampleARGB(u, v, Texture2D.WRAP_REPEAT, Texture2D.WRAP_REPEAT, Texture2D.FILTER_NEAREST);
    }

    private int sampleBilinear(float x, float y, int wrapS, int wrapT) {
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        float fx = x - x0;
        float fy = y - y0;
        int x1 = x0 + 1;
        int y1 = y0 + 1;
        int c00 = argb[wrapCoord(y0, height, wrapT) * width + wrapCoord(x0, width, wrapS)];
        int c10 = argb[wrapCoord(y0, height, wrapT) * width + wrapCoord(x1, width, wrapS)];
        int c01 = argb[wrapCoord(y1, height, wrapT) * width + wrapCoord(x0, width, wrapS)];
        int c11 = argb[wrapCoord(y1, height, wrapT) * width + wrapCoord(x1, width, wrapS)];
        return lerpARGB(lerpARGB(c00, c10, fx), lerpARGB(c01, c11, fx), fy);
    }

    private static int wrapCoord(int c, int size, int mode) {
        if (size <= 0) {
            return 0;
        }
        if (mode == Texture2D.WRAP_CLAMP) {
            if (c < 0) {
                return 0;
            }
            if (c >= size) {
                return size - 1;
            }
            return c;
        }
        int m = c % size;
        if (m < 0) {
            m += size;
        }
        return m;
    }

    private static int lerpARGB(int a, int b, float t) {
        if (t <= 0f) {
            return a;
        }
        if (t >= 1f) {
            return b;
        }
        int aa = (a >>> 24) & 0xFF, ar = (a >>> 16) & 0xFF, ag = (a >>> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >>> 16) & 0xFF, bg = (b >>> 8) & 0xFF, bb = b & 0xFF;
        int oa = aa + (int) ((ba - aa) * t);
        int or = ar + (int) ((br - ar) * t);
        int og = ag + (int) ((bg - ag) * t);
        int ob = ab + (int) ((bb - ab) * t);
        return (oa << 24) | (or << 16) | (og << 8) | ob;
    }

    private void ensureARGB() {
        if (argb != null) {
            return;
        }
        if (image == null || width <= 0 || height <= 0) {
            return;
        }
        argb = new int[width * height];
        int bpp = bytesPerPixel(format);
        for (int i = 0; i < width * height; i++) {
            int o = i * bpp;
            argb[i] = unpackPixel(image, o, format);
        }
    }

    private static int unpackPixel(byte[] src, int o, int format) {
        if (src == null || o < 0) {
            return 0xFFFFFFFF;
        }
        switch (format) {
            case RGB:
                if (o + 2 >= src.length) {
                    return 0xFF000000;
                }
                return 0xFF000000
                    | ((src[o] & 0xFF) << 16)
                    | ((src[o + 1] & 0xFF) << 8)
                    | (src[o + 2] & 0xFF);
            case RGBA:
                if (o + 3 >= src.length) {
                    return 0xFF000000;
                }
                // JSR-184: R,G,B,A
                return ((src[o + 3] & 0xFF) << 24)
                    | ((src[o] & 0xFF) << 16)
                    | ((src[o + 1] & 0xFF) << 8)
                    | (src[o + 2] & 0xFF);
            case LUMINANCE:
                if (o >= src.length) {
                    return 0xFF000000;
                }
                int y = src[o] & 0xFF;
                return 0xFF000000 | (y << 16) | (y << 8) | y;
            case LUMINANCE_ALPHA:
                if (o + 1 >= src.length) {
                    return 0xFF000000;
                }
                int l = src[o] & 0xFF;
                int a = src[o + 1] & 0xFF;
                return (a << 24) | (l << 16) | (l << 8) | l;
            case ALPHA:
                if (o >= src.length) {
                    return 0x00FFFFFF;
                }
                return ((src[o] & 0xFF) << 24) | 0x00FFFFFF;
            default:
                return 0xFFFFFFFF;
        }
    }

    private static byte[] packFromARGB(int[] argb, int format) {
        int bpp = bytesPerPixel(format);
        byte[] out = new byte[argb.length * bpp];
        for (int i = 0; i < argb.length; i++) {
            int c = argb[i];
            int a = (c >>> 24) & 0xFF;
            int r = (c >>> 16) & 0xFF;
            int g = (c >>> 8) & 0xFF;
            int b = c & 0xFF;
            int o = i * bpp;
            switch (format) {
                case RGB:
                    out[o] = (byte) r;
                    out[o + 1] = (byte) g;
                    out[o + 2] = (byte) b;
                    break;
                case RGBA:
                    out[o] = (byte) r;
                    out[o + 1] = (byte) g;
                    out[o + 2] = (byte) b;
                    out[o + 3] = (byte) a;
                    break;
                case LUMINANCE:
                    out[o] = (byte) ((r + g + b) / 3);
                    break;
                case LUMINANCE_ALPHA:
                    out[o] = (byte) ((r + g + b) / 3);
                    out[o + 1] = (byte) a;
                    break;
                case ALPHA:
                    out[o] = (byte) a;
                    break;
                default:
                    break;
            }
        }
        return out;
    }

    static int bytesPerPixel(int fmt) {
        if (fmt == RGBA) {
            return 4;
        }
        if (fmt == RGB) {
            return 3;
        }
        if (fmt == LUMINANCE_ALPHA) {
            return 2;
        }
        if (fmt == LUMINANCE || fmt == ALPHA) {
            return 1;
        }
        return 4;
    }
}
