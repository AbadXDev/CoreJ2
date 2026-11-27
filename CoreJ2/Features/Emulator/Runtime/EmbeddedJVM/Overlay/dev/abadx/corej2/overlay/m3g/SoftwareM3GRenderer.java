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
        float w0 = eye[i0 * 4 + 3];
        float w1 = eye[i1 * 4 + 3];
        float w2 = eye[i2 * 4 + 3];
        if (w0 == 0 || w1 == 0 || w2 == 0) {
            return;
        }
        float x0 = eye[i0 * 4] / w0, y0 = eye[i0 * 4 + 1] / w0, z0 = eye[i0 * 4 + 2] / w0;
        float x1 = eye[i1 * 4] / w1, y1 = eye[i1 * 4 + 1] / w1, z1 = eye[i1 * 4 + 2] / w1;
        float x2 = eye[i2 * 4] / w2, y2 = eye[i2 * 4 + 1] / w2, z2 = eye[i2 * 4 + 2] / w2;

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

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                float w0b = edge(sx1, sy1, sx2, sy2, x + 0.5f, y + 0.5f) / area;
                float w1b = edge(sx2, sy2, sx0, sy0, x + 0.5f, y + 0.5f) / area;
                float w2b = edge(sx0, sy0, sx1, sy1, x + 0.5f, y + 0.5f) / area;
                if (w0b < 0 || w1b < 0 || w2b < 0) {
                    continue;
                }
                float z = w0b * z0 + w1b * z1 + w2b * z2;
                int di = y * color.width + x;
                if (depth != null) {
                    if (z >= depth[di]) {
                        continue;
                    }
                    depth[di] = z;
                }
                int argb = defaultColor;
                if (texture != null && texture.getImage() != null) {
                    float u = w0b * uv0[0] + w1b * uv1[0] + w2b * uv2[0];
                    float v = w0b * uv0[1] + w1b * uv1[1] + w2b * uv2[1];
                    argb = texture.getImage().sampleARGB(u, v);
                }
                color.data[di] = argb | 0xFF000000;
            }
        }
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
