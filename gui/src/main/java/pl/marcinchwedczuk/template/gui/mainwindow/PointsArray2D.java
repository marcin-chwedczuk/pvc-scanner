package pl.marcinchwedczuk.template.gui.mainwindow;

public class PointsArray2D {
    private final int maxWidth;
    private final int maxHeight;
    private final float[] data;

    public PointsArray2D(int maxWidth, int maxHeight) {
        if (maxWidth <= 0) throw new IllegalArgumentException();
        if (maxHeight <= 0) throw new IllegalArgumentException();

        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.data = new float[maxWidth * maxHeight * 3];
    }

    public PointsArray2D(int maxWidth, int maxHeight, float[] data) {
        if (maxWidth <= 0) throw new IllegalArgumentException();
        if (maxHeight <= 0) throw new IllegalArgumentException();

        int expectedLength = 3 * maxHeight * maxWidth;
        if (data.length != expectedLength)
            throw new IllegalArgumentException(String.format("expecting data length to be %d but was %d", expectedLength, data.length));

        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.data = data;
    }

    private int rawIndexOf(int w, int h) {
        if (w < 0 || w >= maxWidth)
            throw new IndexOutOfBoundsException("w");

        if (h < 0 || h >= maxHeight)
            throw new IndexOutOfBoundsException("h");

        return 3 * (maxWidth * h + w);
    }

    public int pointsCount() {
        return maxWidth * maxHeight;
    }

    public int facesIndexOf(int w, int h) {
        if (w < 0 || w >= maxWidth)
            throw new IndexOutOfBoundsException("w");

        if (h < 0 || h >= maxHeight)
            throw new IndexOutOfBoundsException(String.format("index should be between [0..%d] but was %d", maxHeight, h));

        return (maxWidth * h + w);
    }

    public PointRef at(int w, int h) {
        return new PointRef(rawIndexOf(w, h));
    }

    public float[] cloneRawData() {
        float[] copy = new float[data.length];
        System.arraycopy(data, 0, copy, 0, data.length);
        return copy;
    }

    public class PointRef {
        private final int rawIndex;

        public PointRef(int rawIndex) {
            this.rawIndex = rawIndex;
        }

        public float getX() {
            return data[rawIndex + 0];
        }
        public void setX(float x) {
            data[rawIndex + 0] = x;
        }

        public float getY() {
            return data[rawIndex + 1];
        }
        public void setY(float y) {
            data[rawIndex + 1] = y;
        }

        public float getZ() {
            return data[rawIndex + 2];
        }
        public void setZ(float z) {
            data[rawIndex + 2] = z;
        }

        @Override
        public String toString() {
            return String.format("[%.2f, %.2f, %.2f]", getX(), getY(), getZ());
        }
    }
}
