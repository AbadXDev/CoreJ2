package javax.microedition.m3g;

/** CoreJ2 overlay — abstract index buffer with storage hooks. */
public abstract class IndexBuffer extends Object3D {
    public abstract int getIndexCount();
    public abstract void getIndices(int[] indices);
}
