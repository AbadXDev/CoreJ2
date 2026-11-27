package javax.microedition.m3g;

import java.util.Hashtable;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import dev.abadx.corej2.overlay.IntArgbImage;
import dev.abadx.corej2.overlay.m3g.SoftwareM3GRenderer;
import org.recompile.mobile.Mobile;
import org.recompile.mobile.PlatformGraphics;
import org.recompile.mobile.PlatformImage;

/**
 * CoreJ2 overlay — software M3G subset. FreeJ2ME Vendor stubs leave Asphalt
 * black; this class binds to the LCD {@link PlatformImage} and rasterizes
 * what the software renderer understands.
 */
public class Graphics3D {

    public static final int ANTIALIAS = 2;
    public static final int DITHER = 4;
    public static final int OVERWRITE = 16;
    public static final int TRUE_COLOR = 8;

    private static Hashtable properties = new Hashtable();

    private int viewx;
    private int viewy;
    private int vieww;
    private int viewh = 1;
    private float near = 0.1f;
    private float far = 100f;
    private Object target;
    private boolean depthBuffer;
    private int hints;
    private Camera camera;
    private Transform cameraTransform = new Transform();
    private final SoftwareM3GRenderer renderer = new SoftwareM3GRenderer();
    private IntArgbImage colorBuffer;
    private float[] depth;
    private Light[] lights = new Light[8];
    private Transform[] lightTransforms = new Transform[8];
    private int lightCount;

    public int addLight(Light light, Transform transform) {
        System.out.println("corej2-m3g: addLight");
        if (lightCount < lights.length) {
            lights[lightCount] = light;
            lightTransforms[lightCount] = transform == null ? new Transform() : transform;
            lightCount++;
            return lightCount - 1;
        }
        return 0;
    }

    public void bindTarget(Object target) {
        bindTarget(target, true, 0);
    }

    public void bindTarget(Object target, boolean depthBuffer, int hints) {
        this.target = target;
        this.depthBuffer = depthBuffer;
        this.hints = hints;
        colorBuffer = resolveColorBuffer(target);
        if (colorBuffer != null) {
            if (vieww <= 1) {
                vieww = colorBuffer.width;
            }
            if (viewh <= 1) {
                viewh = colorBuffer.height;
            }
            if (this.depthBuffer) {
                depth = new float[colorBuffer.width * colorBuffer.height];
                for (int i = 0; i < depth.length; i++) {
                    depth[i] = Float.MAX_VALUE;
                }
            }
        }
        System.out.println(
            "corej2-m3g: bindTarget " + (target == null ? "null" : target.getClass().getName())
                + " buf=" + (colorBuffer == null ? "null" : (colorBuffer.width + "x" + colorBuffer.height))
        );
        System.out.flush();
    }

    public void clear(Background background) {
        if (colorBuffer == null) {
            return;
        }
        boolean clearColor = true;
        boolean clearDepth = true;
        int color = 0xFF000000;
        Image2D bgImage = null;
        if (background != null) {
            try {
                clearColor = background.isColorClearEnabled();
                clearDepth = background.isDepthClearEnabled();
                color = 0xFF000000 | (background.getColor() & 0x00FFFFFF);
                bgImage = background.getImage();
            } catch (Throwable ignored) {
            }
        }
        int[] data = colorBuffer.data;
        if (clearColor) {
            for (int i = 0; i < data.length; i++) {
                data[i] = color;
            }
        }
        // Optional background image (stretch to viewport) — avoids a pure-black clear
        // when the title supplies a sky/backdrop Image2D.
        if (bgImage != null && vieww > 0 && viewh > 0) {
            for (int y = 0; y < viewh; y++) {
                float v = (y + 0.5f) / viewh;
                for (int x = 0; x < vieww; x++) {
                    float u = (x + 0.5f) / vieww;
                    int px = viewx + x;
                    int py = viewy + y;
                    if (px < 0 || py < 0 || px >= colorBuffer.width || py >= colorBuffer.height) {
                        continue;
                    }
                    data[py * colorBuffer.width + px] =
                        bgImage.sampleARGB(u, v) | 0xFF000000;
                }
            }
        }
        if (depth != null && clearDepth) {
            for (int i = 0; i < depth.length; i++) {
                depth[i] = Float.MAX_VALUE;
            }
        }
        flushToPlatform();
    }

    public Camera getCamera(Transform transform) {
        if (transform != null && cameraTransform != null) {
            transform.set(cameraTransform);
        }
        return camera == null ? new Camera() : camera;
    }

    public float getDepthRangeFar() {
        return far;
    }

    public float getDepthRangeNear() {
        return near;
    }

    public int getHints() {
        return hints;
    }

    public static Graphics3D getInstance() {
        return Mobile.getGraphics3D();
    }

    public Light getLight(int index, Transform transform) {
        if (index < 0 || index >= lightCount) {
            return new Light();
        }
        if (transform != null && lightTransforms[index] != null) {
            transform.set(lightTransforms[index]);
        }
        return lights[index] == null ? new Light() : lights[index];
    }

    public int getLightCount() {
        return lightCount;
    }

    public static Hashtable getProperties() {
        return properties;
    }

    public Object getTarget() {
        return target;
    }

    public int getViewportHeight() {
        return viewh;
    }

    public int getViewportWidth() {
        return vieww;
    }

    public int getViewportX() {
        return viewx;
    }

    public int getViewportY() {
        return viewy;
    }

    public boolean isDepthBufferEnabled() {
        return depthBuffer;
    }

    public void releaseTarget() {
        System.out.println("corej2-m3g: releaseTarget");
        flushToPlatform();
        target = null;
        colorBuffer = null;
        depth = null;
    }

    public void render(Node node, Transform transform) {
        System.out.println("corej2-m3g: render Node " + (node == null ? "null" : node.getClass().getName()));
        if (colorBuffer == null || node == null) {
            return;
        }
        renderer.renderNode(node, transform, camera, cameraTransform, colorBuffer, depth, viewx, viewy, vieww, viewh);
        flushToPlatform();
    }

    public void render(VertexBuffer vertices, IndexBuffer triangles, Appearance appearance, Transform transform) {
        render(vertices, triangles, appearance, transform, -1);
    }

    private int meshVBFrames;
    private int frameDumps;

    public void render(VertexBuffer vertices, IndexBuffer triangles, Appearance appearance, Transform transform, int scope) {
        if (colorBuffer == null || vertices == null || triangles == null) {
            System.out.println("corej2-m3g: render MeshVB skipped (null buf/vb)");
            return;
        }
        int trisBefore = renderer.trianglesDrawn();
        renderer.renderMesh(vertices, triangles, appearance, transform, camera, cameraTransform,
            colorBuffer, depth, viewx, viewy, vieww, viewh);
        meshVBFrames++;
        if (meshVBFrames <= 3 || (meshVBFrames % 120) == 0) {
            System.out.println(
                "corej2-m3g: render MeshVB#" + meshVBFrames
                    + " dTris=" + (renderer.trianglesDrawn() - trisBefore)
                    + " totalTris=" + renderer.trianglesDrawn()
                    + " " + frameStats(colorBuffer)
                    + " cam=" + (camera == null ? "null" : "ok")
                    + " tex0=" + (appearance != null && appearance.getTexture(0) != null
                        ? appearance.getTexture(0).getImage() != null
                        : false)
            );
            System.out.flush();
            maybeDumpFrame();
        }
        flushToPlatform();
    }

    private void maybeDumpFrame() {
        if (!dev.abadx.corej2.overlay.m3g.ArgbBmpDump.dumpEnabled() || colorBuffer == null) {
            return;
        }
        if (frameDumps >= 6) {
            return;
        }
        // Dump every logged MeshVB tick after the scene has some tris.
        if (renderer.trianglesDrawn() < 100 && frameDumps == 0) {
            return;
        }
        frameDumps++;
        dev.abadx.corej2.overlay.m3g.ArgbBmpDump.writeBmp(
            "frame-" + frameDumps + "-" + colorBuffer.width + "x" + colorBuffer.height + ".bmp",
            colorBuffer.width,
            colorBuffer.height,
            colorBuffer.data
        );
    }

    /**
     * Frame evidence: non-zero RGB count + channel means + variance (not just nz).
     * High variance with non-trivial means ⇒ real textured colour, not flat black/white.
     */
    public static String frameStats(IntArgbImage buf) {
        if (buf == null || buf.data == null || buf.data.length == 0) {
            return "nz=0 var=0";
        }
        int[] px = buf.data;
        long n = px.length;
        long nz = 0;
        long sumR = 0, sumG = 0, sumB = 0;
        for (int i = 0; i < px.length; i++) {
            int c = px[i];
            int r = (c >>> 16) & 0xFF;
            int g = (c >>> 8) & 0xFF;
            int b = c & 0xFF;
            if ((c & 0x00FFFFFF) != 0) {
                nz++;
            }
            sumR += r;
            sumG += g;
            sumB += b;
        }
        double meanR = sumR / (double) n;
        double meanG = sumG / (double) n;
        double meanB = sumB / (double) n;
        double var = 0;
        for (int i = 0; i < px.length; i++) {
            int c = px[i];
            double dr = ((c >>> 16) & 0xFF) - meanR;
            double dg = ((c >>> 8) & 0xFF) - meanG;
            double db = (c & 0xFF) - meanB;
            var += dr * dr + dg * dg + db * db;
        }
        var /= (n * 3.0);
        return "nz=" + nz
            + " meanRGB=" + (int) meanR + "/" + (int) meanG + "/" + (int) meanB
            + " var=" + (int) var;
    }

    public void render(World world) {
        System.out.println("corej2-m3g: render World");
        System.out.flush();
        if (colorBuffer == null || world == null) {
            return;
        }
        Camera active = world.getActiveCamera();
        if (active != null) {
            camera = active;
        }
        renderer.renderWorld(world, camera, cameraTransform, colorBuffer, depth, viewx, viewy, vieww, viewh);
        flushToPlatform();
    }

    public void resetLights() {
        lightCount = 0;
    }

    public void setCamera(Camera camera, Transform transform) {
        this.camera = camera;
        this.cameraTransform = transform == null ? new Transform() : transform;
        System.out.println("corej2-m3g: setCamera");
    }

    public void setDepthRange(float Near, float Far) {
        near = Near;
        far = Far;
    }

    public void setLight(int index, Light light, Transform transform) {
        if (index < 0 || index >= lights.length) {
            return;
        }
        lights[index] = light;
        lightTransforms[index] = transform == null ? new Transform() : transform;
        if (index >= lightCount) {
            lightCount = index + 1;
        }
    }

    public void setViewport(int x, int y, int width, int height) {
        viewx = x;
        viewy = y;
        vieww = width;
        viewh = height;
        System.out.println("corej2-m3g: setViewport " + x + "," + y + " " + width + "x" + height);
    }

    private IntArgbImage resolveColorBuffer(Object target) {
        if (target instanceof PlatformImage) {
            return ((PlatformImage) target).getCanvas();
        }
        if (target instanceof Image) {
            Image img = (Image) target;
            if (img.platformImage != null) {
                return img.platformImage.getCanvas();
            }
        }
        if (target instanceof PlatformGraphics) {
            PlatformGraphics pg = (PlatformGraphics) target;
            if (pg.getCanvas() != null) {
                return pg.getCanvas();
            }
            if (pg.platformImage != null) {
                return pg.platformImage.getCanvas();
            }
        }
        if (target instanceof Graphics && target instanceof PlatformGraphics) {
            return ((PlatformGraphics) target).getCanvas();
        }
        // Fall back to the live LCD so games that bind an opaque target still paint.
        try {
            PlatformImage lcd = Mobile.getPlatform().getLCDImage();
            if (lcd != null) {
                return lcd.getCanvas();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private void flushToPlatform() {
        try {
            if (Mobile.getPlatform() != null && Mobile.getPlatform().painter != null) {
                Mobile.getPlatform().painter.run();
            }
        } catch (Throwable ignored) {
        }
    }
}
