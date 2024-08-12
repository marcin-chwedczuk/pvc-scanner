package pl.marcinchwedczuk.template.gui.mainwindow;

import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import javafx.stage.Stage;
import pl.marcinchwedczuk.template.domain.Util;
import pl.marcinchwedczuk.template.gui.mainwindow.PointsArray2D.PointRef;
import pl.marcinchwedczuk.template.gui.mainwindow.TextureArray2D.TextureRef;

import java.io.IOException;
import java.net.URL;
import java.util.Random;
import java.util.ResourceBundle;

public class MainWindow implements Initializable {
    public static MainWindow showOn(Stage window) {
        try {
            FXMLLoader loader = new FXMLLoader(MainWindow.class.getResource("MainWindow.fxml"));

            Scene scene = new Scene(loader.load());
            MainWindow controller = (MainWindow) loader.getController();

            window.setTitle("Main Window");
            window.setScene(scene);
            window.setResizable(true);

            window.show();

            return controller;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @FXML
    private VBox mainWindow;

    @FXML
    private Label label;

    @FXML
    private Slider rotateSlider;

    @FXML
    private StackPane display3D;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        label.setText(Util.quote("Hello, world!"));

        Group g = new Group();
        TriangleMesh mesh = new TriangleMesh(VertexFormat.POINT_TEXCOORD);

        final int maxH = 5;
        final int maxA = 12;

        PointsArray2D points = new PointsArray2D(maxA + 1, maxH);
        // PointsArray2D normals = new PointsArray2D(maxA + 1, maxH);
        TextureArray2D texture = new TextureArray2D(maxA + 1, maxH);

        float radiusR = 180f, height = 500f;
        Random r = new Random();

        for (int h = 0; h < maxH; h++) {
            for (int angle = 0; angle < maxA; angle += 1) {
                PointRef pointRef = points.at(angle, h);

                float radius = radiusR + r.nextFloat() * radiusR / 5.0f;

                // points wounded in CCW fashion
                pointRef.setX(radius * (float)Math.cos(Math.toRadians(-angle*360.0f/maxA)));
                pointRef.setY(height * h / maxH);
                pointRef.setZ(radius * (float)Math.sin(Math.toRadians(-angle*360.0f/maxA)));

                /*
                PointRef normalRef = normals.at(angle, h);

                // points wounded in CCW fashion
                normalRef.setX((float)Math.cos(Math.toRadians(-angle*360.0f/maxA)));
                normalRef.setY(0);
                normalRef.setZ((float)Math.sin(Math.toRadians(-angle*360.0f/maxA)));
                */

                Sphere marker = new Sphere(10.0);
                Color color = Color.hsb(angle * 360.0f / maxA, 1.0, 1.0);
                marker.setMaterial(new PhongMaterial(color));
                marker.setTranslateX(pointRef.getX());
                marker.setTranslateY(pointRef.getY());
                marker.setTranslateZ(pointRef.getZ());
                g.getChildren().add(marker);

                TextureRef textureRef = texture.at(angle, h);
                textureRef.setX(1.0f * angle / maxA);
                textureRef.setY(1.0f * h / maxH);
            }
        }

        // Add faces
        int[] faces = new int[mesh.getPointElementSize() * 3 * 2 * (maxH - 1) * (maxA)];
        for (int h = 0; h < maxH - 1; h++) {
            for (int angle = 0; angle < maxA; angle++) {
                int idx = mesh.getPointElementSize() * 3 * 2 * (h * maxA + angle);
                int nextH = (h + 1);
                int nextA = (angle + 1) % (maxA);
                System.out.printf("Adding triangles for vertice: %d x %d (point: %s)%n", h, angle, points.at(angle, h).toString());

                // top triangle
                faces[idx ++] = points.facesIndexOf(angle, nextH);
                // faces[idx ++] = normals.facesIndexOf(angle, nextH);
                faces[idx ++] = texture.facesIndexOf(angle, nextH);// texture

                faces[idx++] = points.facesIndexOf(nextA, h);
                // faces[idx++] = normals.facesIndexOf(nextA, h);
                faces[idx++] = texture.facesIndexOf(nextA, h); // texture

                faces[idx++] = points.facesIndexOf(nextA, nextH);
                // faces[idx++] = normals.facesIndexOf(nextA, nextH);
                faces[idx++] = texture.facesIndexOf(nextA, nextH); // texture

                // bottom triangle
                faces[idx++] = points.facesIndexOf(angle, h);
                // faces[idx++] = normals.facesIndexOf(angle, h);
                faces[idx++] = texture.facesIndexOf(angle, h);

                faces[idx++] = points.facesIndexOf(nextA, h);
                // faces[idx++] = normals.facesIndexOf(nextA, h);
                faces[idx++] = texture.facesIndexOf(nextA, h);

                faces[idx++] = points.facesIndexOf(angle, nextH);
                // faces[idx++] = normals.facesIndexOf(angle, nextH);
                faces[idx++] = texture.facesIndexOf(angle, nextH);
            }
        }

        System.out.println("Points size: " + points.pointsCount());
        for (int i = 0; i < faces.length; i++) {
            System.out.print(" " + faces[i]);
        }
        System.out.println();

        mesh.getPoints().addAll(points.cloneRawData());
        // mesh.getNormals().addAll(normals.cloneRawData());
        mesh.getTexCoords().addAll(texture.cloneRawData());
        mesh.getFaces().addAll(faces);

        mesh.getFaceSmoothingGroups().clear();
        mesh.getFaceSmoothingGroups().addAll(new int[mesh.getFaces().size() / mesh.getFaceElementSize()]);

        MeshView mv = new MeshView(mesh);
        // mv.setDrawMode(DrawMode.LINE);
        // mv.setCullFace(CullFace.NONE);
        Image diffuseMap = new Image(MainWindow.class.getResource("abc.jpg").toExternalForm());
        PhongMaterial earthMaterial = new PhongMaterial();
        // earthMaterial.setDiffuseMap(diffuseMap);
        earthMaterial.setDiffuseColor(Color.RED);
        mv.setMaterial(earthMaterial);

        mv.setRotationAxis(new Point3D(1, 1, 0));
        mv.rotateProperty().bind(rotateSlider.valueProperty());
        // display3D.getChildren().add(new AmbientLight(Color.GRAY));
        PointLight pl = new PointLight(Color.WHITE);
        pl.setTranslateZ(500);
        //display3D.getChildren().add(pl);

        g.setRotationAxis(mv.getRotationAxis());
        g.rotateProperty().bind(rotateSlider.valueProperty());
        display3D.getChildren().add(g);
        display3D.getChildren().add(mv);
    }

    @FXML
    private void clicked() {
        boolean has3d = Platform.isSupported(ConditionalFeature.SCENE3D);
        label.setText("3D support: " + has3d);
    }
}
