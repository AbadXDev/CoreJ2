package javax.microedition.m3g;

/** CoreJ2 overlay — triangle strip indices. */
public class TriangleStripArray extends IndexBuffer {
    private final int[] indices;
    private final int[] stripLengths;

    public TriangleStripArray(int[] indices, int[] stripLengths) {
        this.indices = indices == null ? new int[0] : (int[]) indices.clone();
        this.stripLengths = stripLengths == null ? new int[0] : (int[]) stripLengths.clone();
    }

    public TriangleStripArray(int firstIndex, int[] stripLengths) {
        this.stripLengths = stripLengths == null ? new int[0] : (int[]) stripLengths.clone();
        int total = 0;
        for (int i = 0; i < this.stripLengths.length; i++) total += this.stripLengths[i];
        this.indices = new int[total];
        for (int i = 0; i < total; i++) this.indices[i] = firstIndex + i;
    }

    public int getIndexCount() { return indices.length; }

    public void getIndices(int[] out) {
        System.arraycopy(indices, 0, out, 0, Math.min(indices.length, out.length));
    }

    public int[] stripLengths() { return stripLengths; }
    public int[] indices() { return indices; }
}
