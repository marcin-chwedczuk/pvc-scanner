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
        TriangleMesh mesh = new TriangleMesh(VertexFormat.POINT_NORMAL_TEXCOORD);

        final int maxH = 5;
        final int maxA = 12;

        PointsArray2D points = new PointsArray2D(maxA + 1, maxH);
        PointsArray2D normals = new PointsArray2D(maxA + 1, maxH);
        TextureArray2D texture = new TextureArray2D(maxA + 1, maxH);

        float radius = 180f, height = 500f;

        for (int h = 0; h < maxH; h++) {
            for (int angle = 0; angle <= maxA; angle += 1) {
                PointRef pointRef = points.at(angle, h);

                // points wounded in CCW fashion
                pointRef.setX(radius * (float)Math.cos(Math.toRadians(-angle*360.0f/maxA)));
                pointRef.setY(height * h / maxH);
                pointRef.setZ(radius * (float)Math.sin(Math.toRadians(-angle*360.0f/maxA)));

                PointRef normalRef = normals.at(angle, h);

                // points wounded in CCW fashion
                normalRef.setX((float)Math.cos(Math.toRadians(-angle*360.0f/maxA)));
                normalRef.setY(0);
                normalRef.setZ((float)Math.sin(Math.toRadians(-angle*360.0f/maxA)));

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
        int[] faces = new int[3 * (maxH - 1) * (maxA) * 6];
        for (int h = 0; h < maxH - 1; h++) {
            for (int angle = 0; angle < maxA; angle++) {
                int idx = 3 * 6 * (h * maxA + angle);
                int nextH = (h + 1);
                int nextA = (angle + 1) % (maxA + 1);
                System.out.printf("Adding triangles for vertice: %d x %d (point: %s)%n", h, angle, points.at(angle, h).toString());

                // top triangle
                faces[idx + 0] = points.facesIndexOf(angle, nextH);
                faces[idx + 1] = normals.facesIndexOf(angle, nextH);
                faces[idx + 2] = texture.facesIndexOf(angle, nextH);// texture

                faces[idx + 3] = points.facesIndexOf(nextA, h);
                faces[idx + 4] = normals.facesIndexOf(nextA, h);
                faces[idx + 5] = texture.facesIndexOf(nextA, h); // texture

                faces[idx + 6] = points.facesIndexOf(nextA, nextH);
                faces[idx + 7] = normals.facesIndexOf(nextA, nextH);
                faces[idx + 8] = texture.facesIndexOf(nextA, nextH); // texture

                // bottom triangle
                idx += 9;

                System.out.println("faces Index at " + angle + ", " + h + " = " + points.facesIndexOf(angle, h));

                faces[idx + 0] = points.facesIndexOf(angle, h);
                faces[idx + 1] = normals.facesIndexOf(angle, h);
                faces[idx + 2] = texture.facesIndexOf(angle, h);

                faces[idx + 3] = points.facesIndexOf(nextA, h);
                faces[idx + 4] = normals.facesIndexOf(nextA, h);
                faces[idx + 5] = texture.facesIndexOf(nextA, h);

                faces[idx + 6] = points.facesIndexOf(angle, nextH);
                faces[idx + 7] = normals.facesIndexOf(angle, nextH);
                faces[idx + 8] = texture.facesIndexOf(angle, nextH);
            }
        }

        System.out.println("Points size: " + points.pointsCount());
        for (int i = 0; i < faces.length; i++) {
            System.out.print(" " + faces[i]);
        }
        System.out.println();

        mesh.getPoints().addAll(points.cloneRawData());
        mesh.getNormals().addAll(normals.cloneRawData());
        mesh.getTexCoords().addAll(texture.cloneRawData());
        mesh.getFaces().addAll(faces);

        MeshView mv = new MeshView(mesh);
        // mv.setDrawMode(DrawMode.LINE);
        // mv.setCullFace(CullFace.NONE);
        Image diffuseMap = new Image(MainWindow.class.getResource("abc.jpg").toExternalForm());
        PhongMaterial earthMaterial = new PhongMaterial();
        earthMaterial.setDiffuseMap(diffuseMap);

        mv.setMaterial(earthMaterial);
        // mv.setTranslateX(70);
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
