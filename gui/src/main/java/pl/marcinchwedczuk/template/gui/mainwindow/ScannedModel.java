package pl.marcinchwedczuk.template.gui.mainwindow;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Translate;
import pl.marcinchwedczuk.template.gui.mainwindow.PointsArray2D.PointRef;
import pl.marcinchwedczuk.template.gui.mainwindow.TextureArray2D.TextureRef;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

import static javafx.scene.shape.VertexFormat.POINT_TEXCOORD;

/**
 * In this class we use classical mathematical coords system with Z going
 * out of the screen and Y going up and X going to the right.
 */
public class ScannedModel {
    private final int angles;
    private final int layers;
    private final float layerHeight;

    private int partialLayerResultsPosition = 0;
    private final float[] partialLayerResults;

    private final TriangleMesh mesh = new TriangleMesh(POINT_TEXCOORD);
    private final MeshView meshView = new MeshView();

    private final Group previewPoints = new Group();

    private final Group model3DNode = new Group();

    public ScannedModel(int angles, int layers, float layerHeight) {
        if (angles <= 0) throw new IllegalArgumentException("angles");
        if (layers <= 0) throw new IllegalArgumentException("layers");
        if (layerHeight <= 0) throw new IllegalArgumentException("layerHeight");

        this.angles = angles;
        this.layers = layers;
        this.layerHeight = layerHeight;
        this.partialLayerResults = new float[angles];

        mesh.getPoints().clear();
        mesh.getTexCoords().clear();
        mesh.getFaces().clear();

        meshView.setMesh(mesh);
        meshView.setCullFace(CullFace.NONE);
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(Color.RED);
        meshView.setMaterial(material);

        model3DNode.getChildren().addAll(previewPoints, meshView);
    }

    public int angles() {
        return angles;
    }

    public int layers() {
        return layers;
    }

    public float layerHeight() {
        return layerHeight;
    }

    public Group model3DNode() {
        return model3DNode;
    }

    public void addScanPoint(float distanceFromCenter) {
        if (distanceFromCenter < 0) throw new IllegalArgumentException();

        // Add preview point
        Sphere previewPoint = new Sphere(4);
        previewPoint.setMaterial(new PhongMaterial(Color.YELLOW));
        float currentAngleRadians = (float) Math.toRadians(partialLayerResultsPosition * 360.0 / angles);

        float x = distanceFromCenter * (float)Math.cos(currentAngleRadians);
        float y = layerHeight * getNumberOfLayers();
        float z = distanceFromCenter * -(float)Math.sin(currentAngleRadians);
        previewPoint.getTransforms().add(new Translate(x, y, z));

        previewPoints.getChildren().add(previewPoint);

        // Save data
        partialLayerResults[partialLayerResultsPosition++] = distanceFromCenter;

        // If we have entire layer add layer and clear preview
        if (partialLayerResultsPosition >= angles) {
            partialLayerResultsPosition = 0;
            addLayer(partialLayerResults);
            Arrays.fill(partialLayerResults, 0.0f);

            if (getNumberOfLayers() >= 2) {
                previewPoints.getChildren().clear();
            }
        }
    }

    public void addLayer(float... distancesFromCenter) {
        if (layerHeight <= 0)
            throw new IllegalArgumentException("layerHeight");

        if (distancesFromCenter.length != angles)
            throw new IllegalArgumentException("Incorrect number of distances for a layer.");

        int previousLayersCount = getNumberOfLayers();
        if (previousLayersCount >= layers)
            throw new IllegalStateException("Too many layers");

        float currentHeight = previousLayersCount * layerHeight;

        // Add points and text coordinates and wrap last point
        var points = new PointsArray2D(angles + 1, 1);
        var texture = new TextureArray2D(angles + 1, 1);
        for (int i = 0; i <= angles; i++) {
            float currentAngleRadians = (float) Math.toRadians(i * 360.0 / angles);

            PointRef pointRef = points.at(i, 0);
            pointRef.setX(distancesFromCenter[i % distancesFromCenter.length] * (float)Math.cos(currentAngleRadians));
            pointRef.setY(currentHeight);
            pointRef.setZ(distancesFromCenter[i % distancesFromCenter.length] * -(float)Math.sin(currentAngleRadians));

            TextureRef textureRef = texture.at(i, 0);
            textureRef.setX((float)i / angles);
            textureRef.setY((float)previousLayersCount / (layers-1));
        }

        mesh.getPoints().addAll(points.cloneRawData());
        mesh.getTexCoords().addAll(texture.cloneRawData());

        if (previousLayersCount >= 1) {
            int anglesCount = angles + 1;
            int layersCount = (previousLayersCount + 1);

            // Create faces strip
            int[] faces = new int[mesh.getFaceElementSize() * 2 * angles];

            for (int angle = 0; angle < angles; angle++) {
                int idx = mesh.getFaceElementSize() * 2 * angle;
                // after adding points we now have extra layer on top of currentLayersNumber layer
                int layer = previousLayersCount - 1;
                int nextL = previousLayersCount;
                int nextA = (angle + 1) % (angles + 1);

                // top triangle
                faces[idx++] = facesIndexOf(angle, nextL, anglesCount, layersCount);
                faces[idx++] = facesIndexOf(angle, nextL, anglesCount, layersCount);

                faces[idx++] = facesIndexOf(nextA, layer, anglesCount, layersCount);
                faces[idx++] = facesIndexOf(nextA, layer, anglesCount, layersCount);

                faces[idx++] = facesIndexOf(nextA, nextL, anglesCount, layersCount);
                faces[idx++] = facesIndexOf(nextA, nextL, anglesCount, layersCount);

                // bottom triangle
                faces[idx++] = facesIndexOf(angle, layer, anglesCount, layersCount);
                faces[idx++] = facesIndexOf(angle, layer, anglesCount, layersCount);

                faces[idx++] = facesIndexOf(nextA, layer, anglesCount, layersCount);
                faces[idx++] = facesIndexOf(nextA, layer, anglesCount, layersCount);

                faces[idx++] = facesIndexOf(angle, nextL, anglesCount, layersCount);
                faces[idx++] = facesIndexOf(angle, nextL, anglesCount, layersCount);
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

    public static int facesIndexOf(int angle, int layer, int anglesCount, int layersCount) {
        if (angle < 0 || angle >= anglesCount)
            throw new IndexOutOfBoundsException("w");

        if (layer < 0 || layer >= layersCount)
            throw new IndexOutOfBoundsException(String.format("index should be between [0..%d] but was %d", layersCount, layer));

        return (anglesCount * layer + angle);
    }

    public void saveToObjFile(java.nio.file.Path path) throws IOException {
        try(BufferedWriter objFile = Files.newBufferedWriter(path)) {
            objFile.write(String.format("# Created by pvc-scanner%n"));

            // Write vertices
            for (int i = 0; i < mesh.getPoints().size(); i += 3) {
                float x = mesh.getPoints().get(i);
                float y = mesh.getPoints().get(i + 1);
                float z = mesh.getPoints().get(i + 2);
                objFile.write(String.format("v %.3f %.3f %.3f%n", x, y, z));
            }

            // Write texture coords
            for (int i = 0; i < mesh.getTexCoords().size(); i += 2) {
                float u = mesh.getTexCoords().get(i);
                float v = mesh.getTexCoords().get(i + 1);
                objFile.write(String.format("vt %.3f %.3f%n", u, v));
            }

            // Write faces
            for (int i = 0; i < mesh.getFaces().size(); i += 6) {
                // Obj uses 1 as the starting index
                int p0 = 1 + mesh.getFaces().get(i);
                int t0 = 1 + mesh.getFaces().get(i + 1);

                int p1 = 1 + mesh.getFaces().get(i + 2);
                int t1 = 1 + mesh.getFaces().get(i + 3);

                int p2 = 1 + mesh.getFaces().get(i + 4);
                int t2 = 1 + mesh.getFaces().get(i + 5);

                objFile.write(String.format("f %d/%d %d/%d %d/%d%n", p0, t0, p1, t1, p2, t2));
            }
        }
    }

    private static void printArray(String msg, float[] data) {
        System.out.println("DEBUG(" + msg + "): ");
        for (float d : data) {
            System.out.print(" " + d);
        }
        System.out.println();
    }

    private static void printArray(String msg, int[] data) {
        System.out.println("DEBUG(" + msg + "): ");
        for (int d : data) {
            System.out.print(" " + d);
        }
        System.out.println();
    }
}
