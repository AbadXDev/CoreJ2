package javax.microedition.m3g;

/** CoreJ2 overlay — retains positions/normals/texcoords/colors. */
public class VertexBuffer extends Object3D {
    private VertexArray positions;
    private float positionScale = 1f;
    private float[] positionBias = new float[] {0, 0, 0};
    private VertexArray normals;
    private VertexArray colors;
    private VertexArray[] texCoords = new VertexArray[2];
    private float[] texScale = new float[] {1, 1};
    private float[][] texBias = new float[][] {{0,0,0},{0,0,0}};
    private int defaultColor = 0xFFFFFFFF;
    private int vertexCount;

    public VertexBuffer() {}

    public VertexArray getColors() { return colors; }
    public int getDefaultColor() { return defaultColor; }
    public VertexArray getNormals() { return normals; }

    public VertexArray getPositions(float[] scaleBias) {
        if (scaleBias != null && scaleBias.length >= 4) {
            scaleBias[0] = positionScale;
            scaleBias[1] = positionBias[0];
            scaleBias[2] = positionBias[1];
            scaleBias[3] = positionBias[2];
        }
        return positions;
    }

    public VertexArray getTexCoords(int index, float[] scaleBias) {
        if (index < 0 || index >= texCoords.length) return null;
        if (scaleBias != null && scaleBias.length >= 4) {
            scaleBias[0] = texScale[index];
            scaleBias[1] = texBias[index][0];
            scaleBias[2] = texBias[index][1];
            scaleBias[3] = texBias[index][2];
        }
        return texCoords[index];
    }

    public int getVertexCount() {
        return positions != null ? positions.getVertexCount() : vertexCount;
    }

    public void setColors(VertexArray colors) { this.colors = colors; }
    public void setDefaultColor(int ARGB) { defaultColor = ARGB; }
    public void setNormals(VertexArray normals) { this.normals = normals; }

    public void setPositions(VertexArray positions, float scale, float[] bias) {
        this.positions = positions;
        this.positionScale = scale;
        if (bias != null) {
            for (int i = 0; i < 3 && i < bias.length; i++) positionBias[i] = bias[i];
        }
        if (positions != null) vertexCount = positions.getVertexCount();
    }

    public void setTexCoords(int index, VertexArray texCoords, float scale, float[] bias) {
        if (index < 0 || index >= this.texCoords.length) return;
        this.texCoords[index] = texCoords;
        this.texScale[index] = scale;
        if (bias != null) {
            for (int i = 0; i < 3 && i < bias.length; i++) this.texBias[index][i] = bias[i];
        }
    }
}
