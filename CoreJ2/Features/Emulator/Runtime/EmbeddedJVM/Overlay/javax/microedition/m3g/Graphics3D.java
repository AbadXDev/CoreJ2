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
        int color = 0xFF000000;
        if (background != null) {
            try {
                color = 0xFF000000 | (background.getColor() & 0x00FFFFFF);
            } catch (Throwable ignored) {
            }
        }
        int[] data = colorBuffer.data;
        for (int i = 0; i < data.length; i++) {
            data[i] = color;
        }
        if (depth != null) {
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
            int nz = 0;
            int[] px = colorBuffer.data;
            for (int i = 0; i < px.length; i++) {
                if ((px[i] & 0x00FFFFFF) != 0) {
                    nz++;
                }
            }
            System.out.println(
                "corej2-m3g: render MeshVB#" + meshVBFrames
                    + " dTris=" + (renderer.trianglesDrawn() - trisBefore)
                    + " totalTris=" + renderer.trianglesDrawn()
                    + " nz=" + nz
                    + " cam=" + (camera == null ? "null" : "ok")
            );
            System.out.flush();
        }
        flushToPlatform();
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
