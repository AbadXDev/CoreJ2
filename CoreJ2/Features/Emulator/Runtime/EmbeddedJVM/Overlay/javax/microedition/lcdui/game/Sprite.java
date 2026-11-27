package javax.microedition.lcdui.game;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/**
 * Clean-room MIDP {@code Sprite} (JSR-118 Game API).
 * Covers methods referenced by Diamond Rush and similar titles.
 */
public class Sprite extends Layer {

    public static final int TRANS_NONE = 0;
    public static final int TRANS_ROT90 = 5;
    public static final int TRANS_ROT180 = 3;
    public static final int TRANS_ROT270 = 6;
    public static final int TRANS_MIRROR = 2;
    public static final int TRANS_MIRROR_ROT90 = 7;
    public static final int TRANS_MIRROR_ROT180 = 1;
    public static final int TRANS_MIRROR_ROT270 = 4;

    public Image sprite;
    public int rowCount;
    public int colCount;

    private int frameWidth;
    private int frameHeight;
    private int frame;
    private int[] sequence;
    private int sequenceIndex;
    private int transform = TRANS_NONE;
    private int refX;
    private int refY;
    private int collX, collY, collW, collH;
    private boolean collDefined;

    public Sprite() {
    }

    public Sprite(Image image) {
        setImage(image, image.getWidth(), image.getHeight());
    }

    public Sprite(Image image, int frameWidth, int frameHeight) {
        setImage(image, frameWidth, frameHeight);
    }

    public Sprite(Sprite s) {
        setImage(s.sprite, s.frameWidth, s.frameHeight);
        setFrame(s.getFrame());
        setTransform(s.transform);
        setPosition(s.x, s.y);
        defineReferencePixel(s.refX, s.refY);
    }

    public void setImage(Image img, int frameWidth, int frameHeight) {
        sprite = img;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        width = frameWidth;
        height = frameHeight;
        colCount = img.getWidth() / frameWidth;
        rowCount = img.getHeight() / frameHeight;
        if (colCount < 1) colCount = 1;
        if (rowCount < 1) rowCount = 1;
        layerImage = Image.createImage(frameWidth, frameHeight);
        frame = 0;
        sequence = null;
        sequenceIndex = 0;
        collX = 0;
        collY = 0;
        collW = frameWidth;
        collH = frameHeight;
        collDefined = true;
    }

    public void paint(Graphics g) {
        if (!visible || sprite == null) {
            return;
        }
        int f = getFrame();
        int sx = (f % colCount) * frameWidth;
        int sy = (f / colCount) * frameHeight;
        int dx = x - refX;
        int dy = y - refY;
        g.drawRegion(sprite, sx, sy, frameWidth, frameHeight, transform, dx, dy, Graphics.TOP | Graphics.LEFT);
    }

    public int getFrame() {
        if (sequence != null && sequence.length > 0) {
            return sequence[sequenceIndex];
        }
        return frame;
    }

    public void setFrame(int sequenceFrameIndex) {
        if (sequence != null) {
            sequenceIndex = sequenceFrameIndex;
        } else {
            frame = sequenceFrameIndex;
        }
    }

    public void nextFrame() {
        if (sequence != null && sequence.length > 0) {
            sequenceIndex = (sequenceIndex + 1) % sequence.length;
        } else {
            int raw = getRawFrameCount();
            if (raw > 0) {
                frame = (frame + 1) % raw;
            }
        }
    }

    public void prevFrame() {
        if (sequence != null && sequence.length > 0) {
            sequenceIndex = sequenceIndex == 0 ? sequence.length - 1 : sequenceIndex - 1;
        } else {
            int raw = getRawFrameCount();
            if (raw > 0) {
                frame = frame == 0 ? raw - 1 : frame - 1;
            }
        }
    }

    public int getRawFrameCount() {
        return rowCount * colCount;
    }

    public int getFrameSequenceLength() {
        return sequence == null ? getRawFrameCount() : sequence.length;
    }

    public void setFrameSequence(int[] sequence) {
        this.sequence = sequence;
        sequenceIndex = 0;
    }

    public void setTransform(int transform) {
        this.transform = transform;
    }

    public void defineReferencePixel(int x, int y) {
        refX = x;
        refY = y;
    }

    public void setRefPixelPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getRefPixelX() { return x; }
    public int getRefPixelY() { return y; }

    public void defineCollisionRectangle(int x, int y, int width, int height) {
        collX = x;
        collY = y;
        collW = width;
        collH = height;
        collDefined = true;
    }

    public boolean collidesWith(Sprite s, boolean pixelLevel) {
        return aabb(s.x + s.collX - s.refX, s.y + s.collY - s.refY, s.collW, s.collH);
    }

    public boolean collidesWith(TiledLayer t, boolean pixelLevel) {
        return aabb(t.getX(), t.getY(), t.getWidth(), t.getHeight());
    }

    public boolean collidesWith(Image image, int x, int y, boolean pixelLevel) {
        return aabb(x, y, image.getWidth(), image.getHeight());
    }

    private boolean aabb(int ox, int oy, int ow, int oh) {
        int ax = x + collX - refX;
        int ay = y + collY - refY;
        return ax < ox + ow && ax + collW > ox && ay < oy + oh && ay + collH > oy;
    }
}
