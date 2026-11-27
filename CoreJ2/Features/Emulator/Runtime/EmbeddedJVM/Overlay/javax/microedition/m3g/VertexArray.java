package javax.microedition.m3g;

/** CoreJ2 overlay — stores vertex component data. */
public class VertexArray extends Object3D {
    private final int vertexCount;
    private final int componentCount;
    private final int componentSize;
    private final byte[] bytes;
    private final short[] shorts;

    public VertexArray(int numVertices, int numComponents, int componentSize) {
        this.vertexCount = numVertices;
        this.componentCount = numComponents;
        this.componentSize = componentSize;
        if (componentSize == 1) {
            bytes = new byte[numVertices * numComponents];
            shorts = null;
        } else {
            bytes = null;
            shorts = new short[numVertices * numComponents];
        }
    }

    public void get(int firstVertex, int numVertices, byte[] values) {
        if (bytes == null) return;
        System.arraycopy(bytes, firstVertex * componentCount, values, 0, numVertices * componentCount);
    }

    public void get(int firstVertex, int numVertices, short[] values) {
        if (shorts == null) return;
        System.arraycopy(shorts, firstVertex * componentCount, values, 0, numVertices * componentCount);
    }

    public int getComponentCount() { return componentCount; }
    public int getComponentType() { return componentSize == 1 ? 1 : 2; }
    public int getVertexCount() { return vertexCount; }

    public void set(int firstVertex, int numVertices, byte[] values) {
        if (bytes == null) return;
        System.arraycopy(values, 0, bytes, firstVertex * componentCount, numVertices * componentCount);
    }

    public void set(int firstVertex, int numVertices, short[] values) {
        if (shorts == null) return;
        System.arraycopy(values, 0, shorts, firstVertex * componentCount, numVertices * componentCount);
    }

    /** Package helper for the software rasterizer. */
    public void getFloat(int vertex, float[] out) {
        int base = vertex * componentCount;
        for (int i = 0; i < componentCount; i++) {
            if (bytes != null) out[i] = bytes[base + i];
            else out[i] = shorts[base + i];
        }
    }
}
