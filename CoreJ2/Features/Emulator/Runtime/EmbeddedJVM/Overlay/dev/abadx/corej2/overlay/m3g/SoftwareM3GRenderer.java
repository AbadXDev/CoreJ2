package dev.abadx.corej2.overlay.m3g;

import javax.microedition.m3g.Appearance;
import javax.microedition.m3g.Camera;
import javax.microedition.m3g.Group;
import javax.microedition.m3g.IndexBuffer;
import javax.microedition.m3g.Mesh;
import javax.microedition.m3g.Node;
import javax.microedition.m3g.Texture2D;
import javax.microedition.m3g.Transform;
import javax.microedition.m3g.TriangleStripArray;
import javax.microedition.m3g.VertexArray;
import javax.microedition.m3g.VertexBuffer;
import javax.microedition.m3g.World;

import dev.abadx.corej2.overlay.IntArgbImage;

/**
 * Minimal software rasterizer for the CoreJ2 M3G overlay.
 *
 * <p>Supports triangle-strip meshes with optional textured sampling. Enough to
 * get Asphalt-class titles painting something onto the LCD; not a full M3G.
 */
public final class SoftwareM3GRenderer {

    private int trianglesDrawn;
    private int meshesDrawn;

    public int trianglesDrawn() {
        return trianglesDrawn;
    }

    public int meshesDrawn() {
        return meshesDrawn;
    }

    public void renderWorld(
            World world,
            Camera camera,
            Transform cameraTransform,
            IntArgbImage color,
            float[] depth,
            int vx,
            int vy,
            int vw,
            int vh
    ) {
        if (world == null || color == null) {
            return;
        }
        if (world.getBackground() != null) {
            // Background clear is handled by Graphics3D.clear; keep pixels.
        }
        Camera cam = camera != null ? camera : world.getActiveCamera();
        renderNode(world, null, cam, cameraTransform, color, depth, vx, vy, vw, vh);
        System.out.println(
            "corej2-m3g: world done meshes=" + meshesDrawn + " tris=" + trianglesDrawn
        );
        System.out.flush();
    }

    public void renderNode(
            Node node,
            Transform parent,
            Camera camera,
            Transform cameraTransform,
            IntArgbImage color,
            float[] depth,
            int vx,
            int vy,
            int vw,
            int vh
    ) {
        if (node == null || !node.isRenderingEnabled()) {
            return;
        }
        Transform local = new Transform();
        node.getCompositeTransform(local);
        if (parent != null) {
            Transform combined = new Transform(parent);
            combined.postMultiply(local);
            local = combined;
        }
        if (node instanceof Mesh) {
            renderMeshNode((Mesh) node, local, camera, cameraTransform, color, depth, vx, vy, vw, vh);
        }
        if (node instanceof Group) {
            Group g = (Group) node;
            int n = g.getChildCount();
            for (int i = 0; i < n; i++) {
                renderNode(g.getChild(i), local, camera, cameraTransform, color, depth, vx, vy, vw, vh);
            }
        }
    }

    public void renderMesh(
            VertexBuffer vertices,
            IndexBuffer triangles,
            Appearance appearance,
            Transform transform,
            Camera camera,
            Transform cameraTransform,
            IntArgbImage color,
            float[] depth,
            int vx,
            int vy,
            int vw,
            int vh
    ) {
        if (vertices == null || triangles == null || color == null) {
            return;
        }
        meshesDrawn++;
        Transform model = transform == null ? new Transform() : transform;
        Transform viewProj = new Transform();
        viewProj.setIdentity();
        if (camera != null) {
            camera.getProjection(viewProj);
        }
        if (cameraTransform != null) {
            Transform view = new Transform(cameraTransform);
            view.invert();
            viewProj.postMultiply(view);
        }
        viewProj.postMultiply(model);

        float[] scaleBias = new float[4];
        VertexArray positions = vertices.getPositions(scaleBias);
        if (positions == null) {
            return;
        }
        float scale = scaleBias[0];
        float bx = scaleBias[1], by = scaleBias[2], bz = scaleBias[3];
        int vcount = positions.getVertexCount();
        float[] eye = new float[vcount * 4];
        float[] tmp = new float[4];
        for (int i = 0; i < vcount; i++) {
            positions.getFloat(i, tmp);
            float x = tmp[0] * scale + bx;
            float y = (positions.getComponentCount() > 1 ? tmp[1] : 0) * scale + by;
            float z = (positions.getComponentCount() > 2 ? tmp[2] : 0) * scale + bz;
            float[] vec = new float[] { x, y, z, 1f };
            viewProj.transform(vec);
            eye[i * 4] = vec[0];
            eye[i * 4 + 1] = vec[1];
            eye[i * 4 + 2] = vec[2];
            eye[i * 4 + 3] = vec[3];
        }

        float[] uvScaleBias = new float[4];
        VertexArray uvs = vertices.getTexCoords(0, uvScaleBias);
        Texture2D texture = appearance != null ? appearance.getTexture(0) : null;
        int defaultColor = vertices.getDefaultColor();

        int[] idx = new int[triangles.getIndexCount()];
        triangles.getIndices(idx);
        int[] strips = null;
        if (triangles instanceof TriangleStripArray) {
            strips = ((TriangleStripArray) triangles).stripLengths();
        }
        if (strips == null || strips.length == 0) {
            strips = new int[] { idx.length };
        }
        int cursor = 0;
        for (int s = 0; s < strips.length; s++) {
            int len = strips[s];
            for (int i = 0; i + 2 < len; i++) {
                int i0 = idx[cursor + i];
                int i1 = idx[cursor + i + 1];
                int i2 = idx[cursor + i + 2];
                if ((i & 1) == 1) {
                    int t = i1;
                    i1 = i2;
                    i2 = t;
                }
                drawTriangle(
                    eye, i0, i1, i2, uvs, uvScaleBias, texture, defaultColor,
                    color, depth, vx, vy, vw, vh
                );
                trianglesDrawn++;
            }
            cursor += len;
        }
    }

    private void renderMeshNode(
            Mesh mesh,
            Transform local,
            Camera camera,
            Transform cameraTransform,
            IntArgbImage color,
            float[] depth,
            int vx,
            int vy,
            int vw,
            int vh
    ) {
        VertexBuffer vb = mesh.getVertexBuffer();
        int count = mesh.getSubmeshCount();
        for (int i = 0; i < count; i++) {
            renderMesh(
                vb,
                mesh.getIndexBuffer(i),
                mesh.getAppearance(i),
                local,
                camera,
                cameraTransform,
                color,
                depth,
                vx,
                vy,
                vw,
                vh
            );
        }
    }

    private void drawTriangle(
            float[] eye,
            int i0,
            int i1,
            int i2,
            VertexArray uvs,
            float[] uvScaleBias,
            Texture2D texture,
            int defaultColor,
            IntArgbImage color,
            float[] depth,
            int vx,
            int vy,
            int vw,
            int vh
    ) {
        float clipW0 = eye[i0 * 4 + 3];
        float clipW1 = eye[i1 * 4 + 3];
        float clipW2 = eye[i2 * 4 + 3];
        // Clip against near (w<=0) — reject tris fully behind the camera.
        if (clipW0 <= 0 && clipW1 <= 0 && clipW2 <= 0) {
            return;
        }
        if (clipW0 == 0 || clipW1 == 0 || clipW2 == 0) {
            return;
        }
        float x0 = eye[i0 * 4] / clipW0, y0 = eye[i0 * 4 + 1] / clipW0, z0 = eye[i0 * 4 + 2] / clipW0;
        float x1 = eye[i1 * 4] / clipW1, y1 = eye[i1 * 4 + 1] / clipW1, z1 = eye[i1 * 4 + 2] / clipW1;
        float x2 = eye[i2 * 4] / clipW2, y2 = eye[i2 * 4 + 1] / clipW2, z2 = eye[i2 * 4 + 2] / clipW2;

        // NDC → viewport
        float sx0 = vx + (x0 * 0.5f + 0.5f) * vw;
        float sy0 = vy + (1f - (y0 * 0.5f + 0.5f)) * vh;
        float sx1 = vx + (x1 * 0.5f + 0.5f) * vw;
        float sy1 = vy + (1f - (y1 * 0.5f + 0.5f)) * vh;
        float sx2 = vx + (x2 * 0.5f + 0.5f) * vw;
        float sy2 = vy + (1f - (y2 * 0.5f + 0.5f)) * vh;

        int minX = Math.max(vx, (int) Math.floor(Math.min(sx0, Math.min(sx1, sx2))));
        int maxX = Math.min(vx + vw - 1, (int) Math.ceil(Math.max(sx0, Math.max(sx1, sx2))));
        int minY = Math.max(vy, (int) Math.floor(Math.min(sy0, Math.min(sy1, sy2))));
        int maxY = Math.min(vy + vh - 1, (int) Math.ceil(Math.max(sy0, Math.max(sy1, sy2))));
        if (minX > maxX || minY > maxY) {
            return;
        }

        float area = edge(sx0, sy0, sx1, sy1, sx2, sy2);
        if (Math.abs(area) < 1e-4f) {
            return;
        }

        float[] uv0 = new float[2];
        float[] uv1 = new float[2];
        float[] uv2 = new float[2];
        sampleUV(uvs, uvScaleBias, i0, uv0);
        sampleUV(uvs, uvScaleBias, i1, uv1);
        sampleUV(uvs, uvScaleBias, i2, uv2);

        // Perspective-correct: interpolate attrib/w, then divide by 1/w.
        float iw0 = 1f / clipW0, iw1 = 1f / clipW1, iw2 = 1f / clipW2;
        float u0w = uv0[0] * iw0, v0w = uv0[1] * iw0;
        float u1w = uv1[0] * iw1, v1w = uv1[1] * iw1;
        float u2w = uv2[0] * iw2, v2w = uv2[1] * iw2;
        float z0w = z0 * iw0, z1w = z1 * iw1, z2w = z2 * iw2;

        int wrapS = texture != null ? texture.getWrappingS() : Texture2D.WRAP_REPEAT;
        int wrapT = texture != null ? texture.getWrappingT() : Texture2D.WRAP_REPEAT;
        int filter = texture != null ? texture.getImageFilter() : Texture2D.FILTER_NEAREST;
        int blend = texture != null ? texture.getBlending() : Texture2D.FUNC_REPLACE;

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                float a0 = edge(sx1, sy1, sx2, sy2, x + 0.5f, y + 0.5f) / area;
                float a1 = edge(sx2, sy2, sx0, sy0, x + 0.5f, y + 0.5f) / area;
                float a2 = edge(sx0, sy0, sx1, sy1, x + 0.5f, y + 0.5f) / area;
                if (a0 < 0 || a1 < 0 || a2 < 0) {
                    continue;
                }
                float invW = a0 * iw0 + a1 * iw1 + a2 * iw2;
                if (invW == 0f) {
                    continue;
                }
                float z = (a0 * z0w + a1 * z1w + a2 * z2w) / invW;
                int di = y * color.width + x;
                if (depth != null) {
                    if (z >= depth[di]) {
                        continue;
                    }
                    depth[di] = z;
                }
                int frag = defaultColor | 0xFF000000;
                if (texture != null && texture.getImage() != null) {
                    float u = (a0 * u0w + a1 * u1w + a2 * u2w) / invW;
                    float v = (a0 * v0w + a1 * v1w + a2 * v2w) / invW;
                    int tex = texture.getImage().sampleARGB(u, v, wrapS, wrapT, filter);
                    frag = applyTextureBlend(blend, tex, defaultColor);
                }
                color.data[di] = frag;
            }
        }
    }

    /** JSR-184 texture functions (REPLACE / MODULATE / DECAL / BLEND / ADD). */
    private static int applyTextureBlend(int func, int tex, int frag) {
        int ta = (tex >>> 24) & 0xFF, tr = (tex >>> 16) & 0xFF, tg = (tex >>> 8) & 0xFF, tb = tex & 0xFF;
        int fa = (frag >>> 24) & 0xFF, fr = (frag >>> 16) & 0xFF, fg = (frag >>> 8) & 0xFF, fb = frag & 0xFF;
        int oa = fa, or, og, ob;
        switch (func) {
            case Texture2D.FUNC_MODULATE:
                or = (tr * fr) / 255;
                og = (tg * fg) / 255;
                ob = (tb * fb) / 255;
                oa = (ta * fa) / 255;
                break;
            case Texture2D.FUNC_DECAL:
                or = (tr * ta + fr * (255 - ta)) / 255;
                og = (tg * ta + fg * (255 - ta)) / 255;
                ob = (tb * ta + fb * (255 - ta)) / 255;
                break;
            case Texture2D.FUNC_BLEND:
                or = (tr * fr + (255 - ta) * fr) / 255; // approx with blend color=frag
                og = (tg * fg + (255 - ta) * fg) / 255;
                ob = (tb * fb + (255 - ta) * fb) / 255;
                break;
            case Texture2D.FUNC_ADD:
                or = Math.min(255, tr + fr);
                og = Math.min(255, tg + fg);
                ob = Math.min(255, tb + fb);
                oa = Math.min(255, ta + fa);
                break;
            case Texture2D.FUNC_REPLACE:
            default:
                or = tr;
                og = tg;
                ob = tb;
                oa = ta == 0 ? 255 : ta;
                break;
        }
        // LCD path is opaque ARGB; keep coverage visible.
        if (oa < 16) {
            oa = 255;
        }
        return (oa << 24) | (or << 16) | (og << 8) | ob;
    }

    private static float edge(float ax, float ay, float bx, float by, float cx, float cy) {
        return (cx - ax) * (by - ay) - (cy - ay) * (bx - ax);
    }

    private static void sampleUV(VertexArray uvs, float[] scaleBias, int index, float[] out) {
        out[0] = 0;
        out[1] = 0;
        if (uvs == null) {
            return;
        }
        float[] tmp = new float[4];
        uvs.getFloat(index, tmp);
        float scale = scaleBias != null ? scaleBias[0] : 1f;
        float bx = scaleBias != null && scaleBias.length > 1 ? scaleBias[1] : 0;
        float by = scaleBias != null && scaleBias.length > 2 ? scaleBias[2] : 0;
        out[0] = tmp[0] * scale + bx;
        out[1] = (uvs.getComponentCount() > 1 ? tmp[1] : 0) * scale + by;
    }
}
