package javax.microedition.lcdui.game;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/**
 * Clean-room MIDP {@code Layer} (JSR-118 Game API).
 * {@link #getLayerImage()}/{@link #render()} support {@link LayerManager}.
 */
public abstract class Layer {

    protected int x;
    protected int y;
    protected int width;
    protected int height;
    protected boolean visible = true;
    protected Image layerImage;

    public Layer() {
    }

    public int getHeight() { return height; }
    public int getWidth() { return width; }
    public int getX() { return x; }
    public int getY() { return y; }
    public boolean isVisible() { return visible; }

    public void move(int dx, int dy) {
        x += dx;
        y += dy;
    }

    public abstract void paint(Graphics g);

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public Image getLayerImage() {
        return layerImage;
    }

    public void render() {
        if (layerImage == null || !visible) {
            return;
        }
        Graphics g = layerImage.getGraphics();
        paint(g);
    }
}
