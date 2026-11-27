package javax.microedition.m3g;

/** Clean-room JSR-184 {@code Appearance}. */
public class Appearance extends Object3D {
    private CompositingMode compositingMode;
    private PolygonMode polygonMode;
    private Material material;
    private Fog fog;
    private final Texture2D[] textures = new Texture2D[2];
    private int layer;

    public CompositingMode getCompositingMode() { return compositingMode; }
    public void setCompositingMode(CompositingMode compositingMode) { this.compositingMode = compositingMode; }
    public PolygonMode getPolygonMode() { return polygonMode; }
    public void setPolygonMode(PolygonMode polygonMode) { this.polygonMode = polygonMode; }
    public Material getMaterial() { return material; }
    public void setMaterial(Material material) { this.material = material; }
    public Fog getFog() { return fog; }
    public void setFog(Fog fog) { this.fog = fog; }
    public Texture2D getTexture(int index) {
        if (index < 0 || index >= textures.length) return null;
        return textures[index];
    }
    public void setTexture(int index, Texture2D texture) {
        if (index < 0 || index >= textures.length) throw new IndexOutOfBoundsException();
        textures[index] = texture;
    }
    public int getLayer() { return layer; }
    public void setLayer(int layer) { this.layer = layer; }
}
