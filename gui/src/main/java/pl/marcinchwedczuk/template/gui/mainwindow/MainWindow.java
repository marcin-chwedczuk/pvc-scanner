package pl.marcinchwedczuk.template.gui.mainwindow;

import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Point3D;
import javafx.scene.*;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;
import pl.marcinchwedczuk.template.domain.Util;
import pl.marcinchwedczuk.template.gui.mainwindow.PointsArray2D.PointRef;
import pl.marcinchwedczuk.template.gui.mainwindow.TextureArray2D.TextureRef;

import java.io.IOException;
import java.io.SerializablePermission;
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

        TriangleMesh mesh = new TriangleMesh(VertexFormat.POINT_TEXCOORD);

        final int maxH = 7;
        final int maxA = 12;

        PointsArray2D points = new PointsArray2D(maxA + 1, maxH);
        // PointsArray2D normals = new PointsArray2D(maxA + 1, maxH);
        TextureArray2D texture = new TextureArray2D(maxA + 1, maxH);

        float radiusR = 180f, height = 500f;
        Random r = new Random();

        for (int h = 0; h < maxH; h++) {
            for (int angle = 0; angle < maxA; angle += 1) {
                PointRef pointRef = points.at(angle, h);

                float radius = (radiusR + r.nextFloat() * radiusR / 5.0f) / (float)Math.log(h + 2);

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
        mv.setCullFace(CullFace.NONE);
        Image diffuseMap = new Image(MainWindow.class.getResource("abc.jpg").toExternalForm());
        PhongMaterial earthMaterial = new PhongMaterial();
        // earthMaterial.setDiffuseMap(diffuseMap);
        earthMaterial.setDiffuseColor(Color.RED);
        mv.setMaterial(earthMaterial);


        AxisMarker am = new AxisMarker();
        am.scale(15);

        Group model = new Group();
        model.getChildren().add(new Box(600, 5, 600));
        model.getChildren().add(mv);
        model.getChildren().add(am);
        model.getTransforms().add(new Scale(1, -1, 1));


        Group sceneGroup = new Group();
        Rotate rtx = new Rotate(0, new Point3D(1, 1, 1));
        rtx.angleProperty().bind(rotateSlider.valueProperty());
        sceneGroup.getTransforms().add(rtx);
        sceneGroup.getChildren().addAll(model, am);

        SubScene d3Scene = new SubScene(sceneGroup, 800, 640, true, SceneAntialiasing.DISABLED);

        // default at (0, 0, 0) looking at -z.
        var camera = new PerspectiveCamera();
        d3Scene.setCamera(camera);

        // Make (0,0,0) center in camera view
        camera.setTranslateX(-d3Scene.getWidth() / 2);
        camera.setTranslateY(-d3Scene.getHeight() / 2);
        camera.setTranslateZ(-500);

        // Set cliping rectangle
        camera.setNearClip(1.0);
        camera.setFarClip(10000.0);

        // camera.setTranslateZ(-800);
        d3Scene.setFill(Color.GRAY);
        display3D.getChildren().add(d3Scene);
    }

    @FXML
    private void clicked() {
        boolean has3d = Platform.isSupported(ConditionalFeature.SCENE3D);
        label.setText("3D support: " + has3d);
    }
}
