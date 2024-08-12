package pl.marcinchwedczuk.template.gui.mainwindow;

public class TextureArray2D {
    private final int maxWidth;
    private final int maxHeight;
    private final float[] data;

    public TextureArray2D(int maxWidth, int maxHeight) {
        if (maxWidth <= 0) throw new IllegalArgumentException();
        if (maxHeight <= 0) throw new IllegalArgumentException();

        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.data = new float[maxWidth * maxHeight * 2];
    }

    public TextureArray2D(int maxWidth, int maxHeight, float[] data) {
        if (maxWidth <= 0) throw new IllegalArgumentException();
        if (maxHeight <= 0) throw new IllegalArgumentException();
        if (data.length != (2 * maxHeight * maxWidth)) throw new IllegalArgumentException();

        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.data = data;
    }

    private int rawIndexOf(int w, int h) {
        if (w < 0 || w >= maxWidth)
            throw new IndexOutOfBoundsException("w");

        if (h < 0 || h >= maxHeight)
            throw new IndexOutOfBoundsException("h");

        return 2*(maxWidth * h + w);
    }

    public int facesIndexOf(int w, int h) {
        if (w < 0 || w >= maxWidth)
            throw new IndexOutOfBoundsException("w");

        if (h < 0 || h >= maxHeight)
            throw new IndexOutOfBoundsException("h");

        return maxWidth * h + w;
    }

    public TextureRef at(int w, int h) {
        return new TextureRef(rawIndexOf(w, h));
    }

    public float[] cloneRawData() {
        float[] copy = new float[data.length];
        System.arraycopy(data, 0, copy, 0, data.length);
        return copy;
    }

    public class TextureRef {
        private final int rawIndex;

        public TextureRef(int rawIndex) {
            this.rawIndex = rawIndex;
        }

        public void setX(float x) {
            data[rawIndex + 0] = x;
        }

        public void setY(float y) {
            data[rawIndex + 1] = y;
        }
    }
}
