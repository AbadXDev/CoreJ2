package dev.abadx.corej2.overlay.m3g;

import javax.microedition.m3g.Appearance;
import javax.microedition.m3g.Camera;
import javax.microedition.m3g.CompositingMode;
import javax.microedition.m3g.Group;
import javax.microedition.m3g.Image2D;
import javax.microedition.m3g.IndexBuffer;
import javax.microedition.m3g.Mesh;
import javax.microedition.m3g.Node;
import javax.microedition.m3g.PolygonMode;
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
 * <p>Supports triangle-strip meshes with textured sampling, Texture2D
 * composite transform, and basic compositing. Enough for Asphalt-class titles.
 */
public final class SoftwareM3GRenderer {

    private int trianglesDrawn;
    private int meshesDrawn;
    private int texturesDumped;
    private boolean loggedUV;

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
        VertexArray colors = vertices.getColors();
        CompositingMode compositing = appearance != null ? appearance.getCompositingMode() : null;
        PolygonMode polygonMode = appearance != null ? appearance.getPolygonMode() : null;

        // Texture2D composite transform (T R S M) applied per-vertex before raster.
        Transform texXform = null;
        if (texture != null) {
            texXform = new Transform();
            texture.getCompositeTransform(texXform);
            dumpTextureOnce(texture);
        }
        if (!loggedUV && uvs != null && texture != null) {
            loggedUV = true;
            float umin = Float.MAX_VALUE, umax = -Float.MAX_VALUE;
            float vmin = Float.MAX_VALUE, vmax = -Float.MAX_VALUE;
            float[] uvTmp = new float[4];
            int n = uvs.getVertexCount();
            for (int vi = 0; vi < n; vi++) {
                uvs.getFloat(vi, uvTmp);
                float su = uvTmp[0] * uvScaleBias[0] + uvScaleBias[1];
                float sv = (uvs.getComponentCount() > 1 ? uvTmp[1] : 0) * uvScaleBias[0] + uvScaleBias[2];
                if (su < umin) umin = su;
                if (su > umax) umax = su;
                if (sv < vmin) vmin = sv;
                if (sv > vmax) vmax = sv;
            }
            float[] mat = new float[16];
            texXform.get(mat);
            boolean id =
                mat[0] == 1 && mat[5] == 1 && mat[10] == 1 && mat[15] == 1
                    && mat[12] == 0 && mat[13] == 0 && mat[14] == 0;
            System.out.println(
                "corej2-m3g: uvRange u=[" + umin + "," + umax + "] v=[" + vmin + "," + vmax + "]"
                    + " n=" + n
                    + " scale=" + uvScaleBias[0]
                    + " bias=" + uvScaleBias[1] + "," + uvScaleBias[2]
                    + " comps=" + uvs.getComponentCount()
                    + " ctype=" + uvs.getComponentType()
                    + " blend=" + texture.getBlending()
                    + " wrap=" + texture.getWrappingS() + "/" + texture.getWrappingT()
                    + " texXformIdentity=" + id
                    + " img=" + texture.getImage().getWidth() + "x" + texture.getImage().getHeight()
                    + " fmt=" + texture.getImage().getFormat()
            );
            System.out.flush();
        }

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
                float[] uv0 = new float[2];
                float[] uv1 = new float[2];
                float[] uv2 = new float[2];
                sampleUV(uvs, uvScaleBias, texXform, i0, uv0);
                sampleUV(uvs, uvScaleBias, texXform, i1, uv1);
                sampleUV(uvs, uvScaleBias, texXform, i2, uv2);
                int c0 = vertexColor(colors, i0, defaultColor);
                int c1 = vertexColor(colors, i1, defaultColor);
                int c2 = vertexColor(colors, i2, defaultColor);
                drawTriangleTessellated(
                    eye[i0 * 4], eye[i0 * 4 + 1], eye[i0 * 4 + 2], eye[i0 * 4 + 3], uv0[0], uv0[1], c0,
                    eye[i1 * 4], eye[i1 * 4 + 1], eye[i1 * 4 + 2], eye[i1 * 4 + 3], uv1[0], uv1[1], c1,
                    eye[i2 * 4], eye[i2 * 4 + 1], eye[i2 * 4 + 2], eye[i2 * 4 + 3], uv2[0], uv2[1], c2,
                    texture, compositing, polygonMode, color, depth, vx, vy, vw, vh, 0
                );
            }
            cursor += len;
        }
    }

    /**
     * Subdivide tris whose UV span is large (Asphalt road tiling) so perspective
     * correction stays stable, then rasterize the leaves.
     */
    private void drawTriangleTessellated(
            float x0, float y0, float z0, float w0, float u0, float v0, int c0,
            float x1, float y1, float z1, float w1, float u1, float v1, int c1,
            float x2, float y2, float z2, float w2, float u2, float v2, int c2,
            Texture2D texture,
            CompositingMode compositing,
            PolygonMode polygonMode,
            IntArgbImage color,
            float[] depthBuf,
            int vx, int vy, int vw, int vh,
            int depth
    ) {
        float du01 = Math.abs(u1 - u0) + Math.abs(v1 - v0);
        float du12 = Math.abs(u2 - u1) + Math.abs(v2 - v1);
        float du20 = Math.abs(u0 - u2) + Math.abs(v0 - v2);
        float longest = Math.max(du01, Math.max(du12, du20));
        // Approximate on-screen size from NDC; tiny projected tris don't need
        // deep UV splits (costly and invisible).
        float ndcSize = 0f;
        if (w0 != 0 && w1 != 0 && w2 != 0) {
            float ax = x0 / w0, ay = y0 / w0;
            float bx = x1 / w1, by = y1 / w1;
            float cx = x2 / w2, cy = y2 / w2;
            ndcSize = Math.abs(ax - bx) + Math.abs(ay - by)
                + Math.abs(bx - cx) + Math.abs(by - cy)
                + Math.abs(cx - ax) + Math.abs(cy - ay);
        }
        // Selective binary UV split: only large, high-tiling screen tris.
        // Cap depth to keep Asphalt playable (heat / frame time).
        if (longest > 2.0f && depth < 4 && ndcSize > 0.22f) {
            int d = depth + 1;
            if (du01 >= du12 && du01 >= du20) {
                float xm = 0.5f * (x0 + x1), ym = 0.5f * (y0 + y1), zm = 0.5f * (z0 + z1), wm = 0.5f * (w0 + w1);
                float um = 0.5f * (u0 + u1), vm = 0.5f * (v0 + v1);
                int cm = lerpColor2(c0, c1);
                drawTriangleTessellated(x0, y0, z0, w0, u0, v0, c0, xm, ym, zm, wm, um, vm, cm, x2, y2, z2, w2, u2, v2, c2,
                    texture, compositing, polygonMode, color, depthBuf, vx, vy, vw, vh, d);
                drawTriangleTessellated(xm, ym, zm, wm, um, vm, cm, x1, y1, z1, w1, u1, v1, c1, x2, y2, z2, w2, u2, v2, c2,
                    texture, compositing, polygonMode, color, depthBuf, vx, vy, vw, vh, d);
            } else if (du12 >= du20) {
                float xm = 0.5f * (x1 + x2), ym = 0.5f * (y1 + y2), zm = 0.5f * (z1 + z2), wm = 0.5f * (w1 + w2);
                float um = 0.5f * (u1 + u2), vm = 0.5f * (v1 + v2);
                int cm = lerpColor2(c1, c2);
                drawTriangleTessellated(x0, y0, z0, w0, u0, v0, c0, x1, y1, z1, w1, u1, v1, c1, xm, ym, zm, wm, um, vm, cm,
                    texture, compositing, polygonMode, color, depthBuf, vx, vy, vw, vh, d);
                drawTriangleTessellated(x0, y0, z0, w0, u0, v0, c0, xm, ym, zm, wm, um, vm, cm, x2, y2, z2, w2, u2, v2, c2,
                    texture, compositing, polygonMode, color, depthBuf, vx, vy, vw, vh, d);
            } else {
                float xm = 0.5f * (x2 + x0), ym = 0.5f * (y2 + y0), zm = 0.5f * (z2 + z0), wm = 0.5f * (w2 + w0);
                float um = 0.5f * (u2 + u0), vm = 0.5f * (v2 + v0);
                int cm = lerpColor2(c2, c0);
                drawTriangleTessellated(x0, y0, z0, w0, u0, v0, c0, x1, y1, z1, w1, u1, v1, c1, xm, ym, zm, wm, um, vm, cm,
                    texture, compositing, polygonMode, color, depthBuf, vx, vy, vw, vh, d);
                drawTriangleTessellated(xm, ym, zm, wm, um, vm, cm, x1, y1, z1, w1, u1, v1, c1, x2, y2, z2, w2, u2, v2, c2,
                    texture, compositing, polygonMode, color, depthBuf, vx, vy, vw, vh, d);
            }
            return;
        }
        emitNearClipped(
            x0, y0, z0, w0, u0, v0, c0,
            x1, y1, z1, w1, u1, v1, c1,
            x2, y2, z2, w2, u2, v2, c2,
            texture, compositing, polygonMode, color, depthBuf, vx, vy, vw, vh
        );
    }

    /**
     * Clip a triangle against the near plane {@code W = ε}. Produces 0–2
     * triangles so foreground road tris that straddle W≈0 keep correct UVs
     * instead of being dropped or streaking.
     */
    private void emitNearClipped(
            float x0, float y0, float z0, float w0, float u0, float v0, int c0,
            float x1, float y1, float z1, float w1, float u1, float v1, int c1,
            float x2, float y2, float z2, float w2, float u2, float v2, int c2,
            Texture2D texture,
            CompositingMode compositing,
            PolygonMode polygonMode,
            IntArgbImage color,
            float[] depthBuf,
            int vx, int vy, int vw, int vh
    ) {
        final float eps = 1e-3f;
        boolean in0 = w0 > eps;
        boolean in1 = w1 > eps;
        boolean in2 = w2 > eps;
        int inside = (in0 ? 1 : 0) + (in1 ? 1 : 0) + (in2 ? 1 : 0);
        if (inside == 0) {
            return;
        }
        if (inside == 3) {
            drawTriangleClip(
                x0, y0, z0, w0, u0, v0, c0,
                x1, y1, z1, w1, u1, v1, c1,
                x2, y2, z2, w2, u2, v2, c2,
                texture, compositing, polygonMode, color, depthBuf, vx, vy, vw, vh
            );
            trianglesDrawn++;
            return;
        }

        float[] xs = new float[4];
        float[] ys = new float[4];
        float[] zs = new float[4];
        float[] ws = new float[4];
        float[] us = new float[4];
        float[] vs = new float[4];
        int[] cs = new int[4];
        int n = 0;

        float[] px = { x0, x1, x2 };
        float[] py = { y0, y1, y2 };
        float[] pz = { z0, z1, z2 };
        float[] pw = { w0, w1, w2 };
        float[] pu = { u0, u1, u2 };
        float[] pv = { v0, v1, v2 };
        int[] pc = { c0, c1, c2 };
        boolean[] pin = { in0, in1, in2 };

        for (int i = 0; i < 3; i++) {
            int j = (i + 1) % 3;
            if (pin[i]) {
                xs[n] = px[i]; ys[n] = py[i]; zs[n] = pz[i]; ws[n] = pw[i];
                us[n] = pu[i]; vs[n] = pv[i]; cs[n] = pc[i];
                n++;
            }
            if (pin[i] != pin[j]) {
                float t = (eps - pw[i]) / (pw[j] - pw[i]);
                if (t < 0f) {
                    t = 0f;
                } else if (t > 1f) {
                    t = 1f;
                }
                xs[n] = px[i] + t * (px[j] - px[i]);
                ys[n] = py[i] + t * (py[j] - py[i]);
                zs[n] = pz[i] + t * (pz[j] - pz[i]);
                ws[n] = eps;
                us[n] = pu[i] + t * (pu[j] - pu[i]);
                vs[n] = pv[i] + t * (pv[j] - pv[i]);
                cs[n] = lerpColor2(pc[i], pc[j]);
                // Bias lerp toward the destination when t is closer to ends.
                if (t > 0.5f) {
                    cs[n] = lerpColor2(cs[n], pc[j]);
                } else {
                    cs[n] = lerpColor2(pc[i], cs[n]);
                }
                n++;
            }
        }
        if (n < 3) {
            return;
        }
        // Fan triangulation from vertex 0.
        for (int i = 1; i + 1 < n; i++) {
            drawTriangleClip(
                xs[0], ys[0], zs[0], ws[0], us[0], vs[0], cs[0],
                xs[i], ys[i], zs[i], ws[i], us[i], vs[i], cs[i],
                xs[i + 1], ys[i + 1], zs[i + 1], ws[i + 1], us[i + 1], vs[i + 1], cs[i + 1],
                texture, compositing, polygonMode, color, depthBuf, vx, vy, vw, vh
            );
            trianglesDrawn++;
        }
    }

    private static int lerpColor2(int a, int b) {
        int ar = (a >>> 16) & 0xFF, ag = (a >>> 8) & 0xFF, ab = a & 0xFF, aa = (a >>> 24) & 0xFF;
        int br = (b >>> 16) & 0xFF, bg = (b >>> 8) & 0xFF, bb = b & 0xFF, ba = (b >>> 24) & 0xFF;
        return (((aa + ba) / 2) << 24) | (((ar + br) / 2) << 16) | (((ag + bg) / 2) << 8) | ((ab + bb) / 2);
    }

    private void dumpTextureOnce(Texture2D texture) {
        if (!ArgbBmpDump.dumpEnabled() || texturesDumped >= 8) {
            return;
        }
        Image2D img = texture.getImage();
        if (img == null) {
            return;
        }
        int[] px = img.copyARGB();
        if (px == null) {
            return;
        }
        texturesDumped++;
        ArgbBmpDump.writeBmp(
            "tex-" + texturesDumped + "-" + img.getWidth() + "x" + img.getHeight()
                + "-fmt" + img.getFormat() + ".bmp",
            img.getWidth(),
            img.getHeight(),
            px
        );
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

    /**
     * Rasterize one triangle from clip-space verts (already UV-sampled).
     * Tessellation above keeps UV span small so perspective /w stays stable.
     */
    private void drawTriangleClip(
            float cx0, float cy0, float cz0, float clipW0, float uu0, float vv0, int c0,
            float cx1, float cy1, float cz1, float clipW1, float uu1, float vv1, int c1,
            float cx2, float cy2, float cz2, float clipW2, float uu2, float vv2, int c2,
            Texture2D texture,
            CompositingMode compositing,
            PolygonMode polygonMode,
            IntArgbImage color,
            float[] depth,
            int vx,
            int vy,
            int vw,
            int vh
    ) {
        // Caller must near-clip; still guard against W≈0.
        final float wEps = 1e-3f;
        if (clipW0 <= wEps || clipW1 <= wEps || clipW2 <= wEps) {
            return;
        }
        float x0 = cx0 / clipW0, y0 = cy0 / clipW0, z0 = cz0 / clipW0;
        float x1 = cx1 / clipW1, y1 = cy1 / clipW1, z1 = cz1 / clipW1;
        float x2 = cx2 / clipW2, y2 = cy2 / clipW2, z2 = cz2 / clipW2;

        // Back-face culling is intentionally not applied yet: Asphalt mixes
        // strip winding with a Y-flipped viewport, and aggressive NDC culling
        // removed large parts of the track in device frame dumps. Depth test
        // still resolves overlaps.

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
        // Always perspective-correct UV when enabled. Affine fallback on high
        // tiling (Asphalt roads, v≫1) produced the radial/horizontal streaks;
        // float32 handles attr/w for spans ~100 after subtracting a common base.
        boolean perspectiveUV = polygonMode == null || polygonMode.isPerspectiveCorrectionEnabled();

        float[] uv0 = new float[] { uu0, vv0 };
        float[] uv1 = new float[] { uu1, vv1 };
        float[] uv2 = new float[] { uu2, vv2 };

        // Perspective-correct attribs: interpolate (attr/w) then divide by (1/w).
        float iw0 = 1f / clipW0, iw1 = 1f / clipW1, iw2 = 1f / clipW2;
        float z0w = z0 * iw0, z1w = z1 * iw1, z2w = z2 * iw2;

        float u0f = uv0[0], v0f = uv0[1];
        float u1f = uv1[0], v1f = uv1[1];
        float u2f = uv2[0], v2f = uv2[1];
        // Common integer-ish base keeps (u/w) magnitudes smaller.
        float uBase = (float) Math.floor(Math.min(u0f, Math.min(u1f, u2f)));
        float vBase = (float) Math.floor(Math.min(v0f, Math.min(v1f, v2f)));
        u0f -= uBase; u1f -= uBase; u2f -= uBase;
        v0f -= vBase; v1f -= vBase; v2f -= vBase;
        float u0w = u0f * iw0, v0w = v0f * iw0;
        float u1w = u1f * iw1, v1w = v1f * iw1;
        float u2w = u2f * iw2, v2w = v2f * iw2;

        int wrapS = texture != null ? texture.getWrappingS() : Texture2D.WRAP_REPEAT;
        int wrapT = texture != null ? texture.getWrappingT() : Texture2D.WRAP_REPEAT;
        int filter = texture != null ? texture.getImageFilter() : Texture2D.FILTER_NEAREST;
        int blend = texture != null ? texture.getBlending() : Texture2D.FUNC_REPLACE;
        int blendColor = texture != null ? texture.getBlendColor() : 0;
        int compBlend = compositing != null ? compositing.getBlending() : CompositingMode.REPLACE;
        float alphaThresh = compositing != null ? compositing.getAlphaThreshold() : 0f;
        boolean depthTest = compositing == null || compositing.isDepthTestEnabled();
        boolean depthWrite = compositing == null || compositing.isDepthWriteEnabled();
        boolean colorWrite = compositing == null || compositing.isColorWriteEnabled();

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
                if (depth != null && depthTest) {
                    if (z >= depth[di]) {
                        continue;
                    }
                }
                if (depth != null && depthWrite) {
                    depth[di] = z;
                }
                if (!colorWrite) {
                    continue;
                }

                int frag = lerpColor(c0, c1, c2, a0, a1, a2);
                if (texture != null && texture.getImage() != null) {
                    float u;
                    float v;
                    if (perspectiveUV) {
                        u = uBase + (a0 * u0w + a1 * u1w + a2 * u2w) / invW;
                        v = vBase + (a0 * v0w + a1 * v1w + a2 * v2w) / invW;
                    } else {
                        u = a0 * uv0[0] + a1 * uv1[0] + a2 * uv2[0];
                        v = a0 * uv0[1] + a1 * uv1[1] + a2 * uv2[1];
                    }
                    int tex = texture.getImage().sampleARGB(u, v, wrapS, wrapT, filter);
                    frag = applyTextureBlend(blend, tex, frag, blendColor);
                }

                int fa = (frag >>> 24) & 0xFF;
                if (fa < (int) (alphaThresh * 255f)) {
                    continue;
                }
                color.data[di] = applyCompositing(compBlend, frag, color.data[di]);
            }
        }
    }

    /** JSR-184 texture functions. FUNC_BLEND uses Texture2D blend colour. */
    private static int applyTextureBlend(int func, int tex, int frag, int blendRGB) {
        int ta = (tex >>> 24) & 0xFF, tr = (tex >>> 16) & 0xFF, tg = (tex >>> 8) & 0xFF, tb = tex & 0xFF;
        int fa = (frag >>> 24) & 0xFF, fr = (frag >>> 16) & 0xFF, fg = (frag >>> 8) & 0xFF, fb = frag & 0xFF;
        int br = (blendRGB >>> 16) & 0xFF, bg = (blendRGB >>> 8) & 0xFF, bb = blendRGB & 0xFF;
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
                // Cf = Cp*(1-Ct) + Cc*Ct  (Cc = blend colour)
                or = (fr * (255 - tr) + br * tr) / 255;
                og = (fg * (255 - tg) + bg * tg) / 255;
                ob = (fb * (255 - tb) + bb * tb) / 255;
                oa = (ta * fa) / 255;
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
                oa = ta;
                break;
        }
        if (oa == 0 && func == Texture2D.FUNC_REPLACE) {
            oa = 255;
        }
        return (oa << 24) | (or << 16) | (og << 8) | ob;
    }

    private static int applyCompositing(int mode, int src, int dst) {
        int sa = (src >>> 24) & 0xFF, sr = (src >>> 16) & 0xFF, sg = (src >>> 8) & 0xFF, sb = src & 0xFF;
        int da = (dst >>> 24) & 0xFF, dr = (dst >>> 16) & 0xFF, dg = (dst >>> 8) & 0xFF, db = dst & 0xFF;
        switch (mode) {
            case CompositingMode.ALPHA:
                return ((255 << 24)
                    | (((sr * sa + dr * (255 - sa)) / 255) << 16)
                    | (((sg * sa + dg * (255 - sa)) / 255) << 8)
                    | ((sb * sa + db * (255 - sa)) / 255));
            case CompositingMode.ALPHA_ADD:
                return ((255 << 24)
                    | (Math.min(255, sr + (dr * sa) / 255) << 16)
                    | (Math.min(255, sg + (dg * sa) / 255) << 8)
                    | Math.min(255, sb + (db * sa) / 255));
            case CompositingMode.MODULATE:
                return ((255 << 24)
                    | ((sr * dr) / 255 << 16)
                    | ((sg * dg) / 255 << 8)
                    | ((sb * db) / 255));
            case CompositingMode.MODULATE_X2:
                return ((255 << 24)
                    | (Math.min(255, (sr * dr * 2) / 255) << 16)
                    | (Math.min(255, (sg * dg * 2) / 255) << 8)
                    | Math.min(255, (sb * db * 2) / 255));
            case CompositingMode.REPLACE:
            default:
                return src | 0xFF000000;
        }
    }

    private static int vertexColor(VertexArray colors, int index, int fallback) {
        if (colors == null) {
            return fallback;
        }
        float[] tmp = new float[4];
        colors.getFloat(index, tmp);
        int comps = colors.getComponentCount();
        // Byte colours arrive unsigned 0..255 via getFloat.
        int r = (int) tmp[0] & 0xFF;
        int g = comps > 1 ? ((int) tmp[1] & 0xFF) : r;
        int b = comps > 2 ? ((int) tmp[2] & 0xFF) : r;
        int a = comps > 3 ? ((int) tmp[3] & 0xFF) : 255;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int lerpColor(int c0, int c1, int c2, float a0, float a1, float a2) {
        int r = (int) (a0 * ((c0 >>> 16) & 0xFF) + a1 * ((c1 >>> 16) & 0xFF) + a2 * ((c2 >>> 16) & 0xFF));
        int g = (int) (a0 * ((c0 >>> 8) & 0xFF) + a1 * ((c1 >>> 8) & 0xFF) + a2 * ((c2 >>> 8) & 0xFF));
        int b = (int) (a0 * (c0 & 0xFF) + a1 * (c1 & 0xFF) + a2 * (c2 & 0xFF));
        int a = (int) (a0 * ((c0 >>> 24) & 0xFF) + a1 * ((c1 >>> 24) & 0xFF) + a2 * ((c2 >>> 24) & 0xFF));
        r = clamp255(r);
        g = clamp255(g);
        b = clamp255(b);
        a = clamp255(a);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int clamp255(int v) {
        if (v < 0) {
            return 0;
        }
        if (v > 255) {
            return 255;
        }
        return v;
    }

    private static float edge(float ax, float ay, float bx, float by, float cx, float cy) {
        return (cx - ax) * (by - ay) - (cy - ay) * (bx - ax);
    }

    /**
     * Scale/bias texcoords then apply Texture2D composite transform and
     * project by q' (JSR-184 texture pipeline).
     */
    private static void sampleUV(
            VertexArray uvs,
            float[] scaleBias,
            Transform texXform,
            int index,
            float[] out
    ) {
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
        float bz = scaleBias != null && scaleBias.length > 3 ? scaleBias[3] : 0;
        float s = tmp[0] * scale + bx;
        float t = (uvs.getComponentCount() > 1 ? tmp[1] : 0) * scale + by;
        float r = (uvs.getComponentCount() > 2 ? tmp[2] : 0) * scale + bz;
        if (texXform != null) {
            float[] p = new float[] { s, t, r, 1f };
            texXform.transform(p);
            if (p[3] != 0f && p[3] != 1f) {
                s = p[0] / p[3];
                t = p[1] / p[3];
            } else {
                s = p[0];
                t = p[1];
            }
        }
        out[0] = s;
        out[1] = t;
    }
}
