package pl.marcinchwedczuk.template.gui.mainwindow;

import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;

import java.util.Random;

/**
 * Bottom located in X-Z plane.
 * Growing along Y axis. The tip located at (0, height, 0).
 */
public class ConeMesh extends TriangleMesh {
    private static final int DIVISIONS_HEIGHT = 5;
    private static final int DIVISIONS_CIRCUMFERENCE = 8;

    private final float radius;
    private final float height;

    public ConeMesh(float radius, float height) {
        super(VertexFormat.POINT_TEXCOORD);

        this.radius = radius;
        this.height = height;

        generateMesh();
    }

    private void generateMesh() {
        // Need to use +1 for circumference to have an inclusive 0 - 1 range for texture parameters.
        PointsArray2D points = new PointsArray2D(DIVISIONS_CIRCUMFERENCE + 1, DIVISIONS_HEIGHT);
        TextureArray2D texture = new TextureArray2D(DIVISIONS_CIRCUMFERENCE + 1, DIVISIONS_HEIGHT);

        // Define vertices
        for (int h = 0; h < DIVISIONS_HEIGHT; h++) {
            for (int angle = 0; angle <= DIVISIONS_CIRCUMFERENCE; angle++) {
                PointsArray2D.PointRef pointRef = points.at(angle, h);

                // for h = DIVISIONS_HEIGHT-1 we make the bottom circle
                float layerRadius = radius * ((float)h / (DIVISIONS_HEIGHT-1));
                float angleRadians = (float) Math.toRadians(-angle * 360.0f / DIVISIONS_CIRCUMFERENCE);

                // points wounded in CCW fashion, we need to flip the angle sign
                pointRef.setX(layerRadius * (float)Math.cos(angleRadians));
                pointRef.setY(height * (DIVISIONS_HEIGHT - 1 - h) / (DIVISIONS_HEIGHT - 1));
                pointRef.setZ(layerRadius * (float)Math.sin(angleRadians));

                TextureArray2D.TextureRef textureRef = texture.at(angle, h);
                textureRef.setX(1.0f * angle / DIVISIONS_CIRCUMFERENCE);
                textureRef.setY(1.0f * h / DIVISIONS_HEIGHT);
            }
        }

        // Define triangles
        int[] faces = new int[getPointElementSize() * 3 * 2 * (DIVISIONS_HEIGHT - 1) * DIVISIONS_CIRCUMFERENCE];
        for (int h = 0; h < DIVISIONS_HEIGHT - 1; h++) {
            for (int angle = 0; angle < DIVISIONS_CIRCUMFERENCE; angle++) {
                int idx = getPointElementSize() * 3 * 2 * (h * DIVISIONS_CIRCUMFERENCE + angle);
                int nextH = (h + 1);
                int nextA = (angle + 1) % (DIVISIONS_CIRCUMFERENCE + 1);

                // top triangle
                faces[idx++] = points.facesIndexOf(angle, nextH);
                faces[idx++] = texture.facesIndexOf(angle, nextH);

                faces[idx++] = points.facesIndexOf(nextA, h);
                faces[idx++] = texture.facesIndexOf(nextA, h);

                faces[idx++] = points.facesIndexOf(nextA, nextH);
                faces[idx++] = texture.facesIndexOf(nextA, nextH);

                // bottom triangle
                faces[idx++] = points.facesIndexOf(angle, h);
                faces[idx++] = texture.facesIndexOf(angle, h);

                faces[idx++] = points.facesIndexOf(nextA, h);
                faces[idx++] = texture.facesIndexOf(nextA, h);

                faces[idx++] = points.facesIndexOf(angle, nextH);
                faces[idx++] = texture.facesIndexOf(angle, nextH);
            }
        }

        getPoints().addAll(points.cloneRawData());
        getTexCoords().addAll(texture.cloneRawData());
        getFaces().addAll(faces);

        // Disable smoothing
        getFaceSmoothingGroups().clear();
        getFaceSmoothingGroups().addAll(new int[getFaces().size() / getFaceElementSize()]);
    }
}
