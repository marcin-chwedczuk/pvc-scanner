package pl.marcinchwedczuk.template.gui.mainwindow;

import javafx.scene.Group;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import javafx.scene.transform.Translate;
import pl.marcinchwedczuk.template.gui.mainwindow.PointsArray2D.PointRef;
import pl.marcinchwedczuk.template.gui.mainwindow.TextureArray2D.TextureRef;

import static javafx.scene.shape.VertexFormat.POINT_TEXCOORD;

/**
 * In this class we use classical mathematical coords system with Z going
 * out of the screen and Y going up and X going to the right.
 */
public class ScannedModel {
    private final int angles;
    private final int layers;
    private final float layerHeight;

    private final TriangleMesh mesh = new TriangleMesh(POINT_TEXCOORD);

    public final Group debug = new Group();

    public ScannedModel(int angles, int layers, float layerHeight) {
        if (angles <= 0) throw new IllegalArgumentException("angles");
        if (layers <= 0) throw new IllegalArgumentException("layers");
        if (layerHeight <= 0) throw new IllegalArgumentException("layerHeight");

        this.angles = angles;
        this.layers = layers;
        this.layerHeight = layerHeight;

        mesh.getPoints().clear();
        mesh.getTexCoords().clear();
        mesh.getFaces().clear();
    }

    public void addLayer(float... distancesFromCenter) {
        if (layerHeight <= 0)
            throw new IllegalArgumentException("layerHeight");

        if (distancesFromCenter.length != angles)
            throw new IllegalArgumentException("Incorrect number of distances for a layer.");

        // TODO: Update class state only when the entire op is success
        int previousLayersNumber = getNumberOfLayers();
        if (previousLayersNumber >= layers)
            throw new IllegalStateException("Too many layers");

        float currentHeight = previousLayersNumber * layerHeight + 55;

        // Add points and text coordinates and wrap last point
        var points = new PointsArray2D(angles + 1, 1);
        var texture = new TextureArray2D(angles + 1, 1);
        for (int i = 0; i <= angles; i++) {
            float currentAngleRadians = (float) Math.toRadians(i * 360.0 / angles);

            PointRef pointRef = points.at(i, 0);
            pointRef.setX(distancesFromCenter[i % distancesFromCenter.length] * (float)Math.cos(currentAngleRadians));
            pointRef.setY(currentHeight);
            pointRef.setZ(distancesFromCenter[i % distancesFromCenter.length] * -(float)Math.sin(currentAngleRadians));

            Sphere s = new Sphere(5);
            s.getTransforms().add(new Translate(pointRef.getX(), pointRef.getY(), pointRef.getZ()));
            debug.getChildren().add(s);

            TextureRef textureRef = texture.at(i, 0);
            textureRef.setX((float)i / angles);
            textureRef.setY((float)previousLayersNumber / layers);
        }

        mesh.getPoints().addAll(points.cloneRawData());
        mesh.getTexCoords().addAll(texture.cloneRawData());

        if (previousLayersNumber >= 1) {
            // Wrapper around existing points for easy addressing
            points = new PointsArray2D(angles + 1, (previousLayersNumber + 1), mesh.getPoints().toArray(new float[0]));
            // printArray("Current points: ", points.cloneRawData());
            texture = new TextureArray2D(angles + 1, (previousLayersNumber + 1), mesh.getTexCoords().toArray(new float[0]));
            // printArray("Current textures: ", texture.cloneRawData());

            // Create faces strip
            int[] faces = new int[mesh.getPointElementSize() * 3 * 2 * angles];

            for (int angle = 0; angle < angles; angle++) {
                int idx = mesh.getPointElementSize() * 3 * 2 * angle;
                // after adding points we now have extra layer on top of currentLayersNumber layer
                int h = previousLayersNumber - 1;
                int nextH = previousLayersNumber;
                int nextA = (angle + 1) % (angles + 1);

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

            mesh.getFaces().addAll(faces);

            // update groups to disable smoothing
            mesh.getFaceSmoothingGroups().clear();
            mesh.getFaceSmoothingGroups().addAll(new int[mesh.getFaces().size() / mesh.getFaceElementSize()]);
        }
    }

    private int getNumberOfLayers() {
        int points = mesh.getPoints().size() / mesh.getPointElementSize();
        int pointsPerLayer = angles + 1; // we wrap around the last point
        return points / pointsPerLayer;
    }

    public TriangleMesh mesh() {
        return mesh;
    }

    private static void printArray(String msg, float[] data) {
        System.out.println("DEBUG(" + msg + "): ");
        for (int i = 0; i < data.length; i++) {
            System.out.print(" " + data[i]);
        }
        System.out.println();
    }
}
