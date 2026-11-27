package javax.microedition.lcdui.game;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/** Clean-room MIDP {@code TiledLayer} (JSR-118) — minimal for Sprite.collidesWith. */
public class TiledLayer extends Layer {

    private Image image;
    private int[][] cells;
    private int rows;
    private int cols;
    private int tileW;
    private int tileH;

    public TiledLayer(int columns, int rows, Image image, int tileWidth, int tileHeight) {
        this.cols = columns;
        this.rows = rows;
        this.image = image;
        this.tileW = tileWidth;
        this.tileH = tileHeight;
        this.width = columns * tileWidth;
        this.height = rows * tileHeight;
        cells = new int[rows][columns];
        layerImage = Image.createImage(width, height);
    }

    public void setCell(int col, int row, int tileIndex) {
        cells[row][col] = tileIndex;
    }

    public int getCell(int col, int row) {
        return cells[row][col];
    }

    public void fillCells(int col, int row, int numCols, int numRows, int tileIndex) {
        for (int r = row; r < row + numRows; r++) {
            for (int c = col; c < col + numCols; c++) {
                cells[r][c] = tileIndex;
            }
        }
    }

    public int getCellWidth() { return tileW; }
    public int getCellHeight() { return tileH; }
    public int getColumns() { return cols; }
    public int getRows() { return rows; }

    public void paint(Graphics g) {
        if (!visible || image == null) {
            return;
        }
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int tile = cells[r][c];
                if (tile <= 0) {
                    continue;
                }
                int idx = tile - 1;
                int tw = image.getWidth() / tileW;
                if (tw < 1) tw = 1;
                int sx = (idx % tw) * tileW;
                int sy = (idx / tw) * tileH;
                g.drawRegion(image, sx, sy, tileW, tileH, 0,
                        x + c * tileW, y + r * tileH, Graphics.TOP | Graphics.LEFT);
            }
        }
    }
}
