package javax.microedition.m3g;

/** CoreJ2 overlay — stores vertex/index/appearance submeshes. */
public class Mesh extends Node {
    private VertexBuffer vertexBuffer;
    private IndexBuffer[] indexBuffers;
    private Appearance[] appearances;

    public Mesh() {}

    public Mesh(VertexBuffer vertices, IndexBuffer[] submeshes, Appearance[] appearances) {
        this.vertexBuffer = vertices;
        this.indexBuffers = submeshes;
        this.appearances = appearances;
        setRenderingEnable(true);
    }

    public Mesh(VertexBuffer vertices, IndexBuffer submesh, Appearance appearance) {
        this.vertexBuffer = vertices;
        this.indexBuffers = new IndexBuffer[] { submesh };
        this.appearances = new Appearance[] { appearance };
        setRenderingEnable(true);
    }

    public Appearance getAppearance(int index) {
        if (appearances == null || index < 0 || index >= appearances.length) return null;
        return appearances[index];
    }

    public IndexBuffer getIndexBuffer(int index) {
        if (indexBuffers == null || index < 0 || index >= indexBuffers.length) return null;
        return indexBuffers[index];
    }

    public int getSubmeshCount() {
        return indexBuffers == null ? 0 : indexBuffers.length;
    }

    public VertexBuffer getVertexBuffer() { return vertexBuffer; }

    public void setAppearance(int index, Appearance a) {
        if (appearances == null) appearances = new Appearance[Math.max(1, index + 1)];
        if (index >= appearances.length) {
            Appearance[] n = new Appearance[index + 1];
            System.arraycopy(appearances, 0, n, 0, appearances.length);
            appearances = n;
        }
        appearances[index] = a;
    }
}
