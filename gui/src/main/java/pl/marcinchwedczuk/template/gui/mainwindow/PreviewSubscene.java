package pl.marcinchwedczuk.template.gui.mainwindow;

import javafx.scene.Parent;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

import java.util.concurrent.atomic.AtomicInteger;

public class PreviewSubscene extends SubScene {
    // Camera rotation around X axis
    private final Rotate rtx = new Rotate(0, Rotate.X_AXIS);

    // Model rotation around Y axis
    private final Rotate rty = new Rotate(0, Rotate.Y_AXIS);

    private boolean rotating = false;
    private double lastScreenX = -1;
    private double lastScreenY = -1;
    private double currentXAngle = 0;
    private double currentYAngle = 0;

    public PreviewSubscene(Parent root, double width, double height) {
        super(root, width, height, true, SceneAntialiasing.DISABLED);

        // default at (0, 0, 0) looking at -z.
        var camera = new PerspectiveCamera(true);
        setCamera(camera);

        // Camera is at (0,0,0) be default, move a bit back along Z axis
        camera.getTransforms().add(rtx);

        var cameraTranslate = new Translate(0, 0, -2000);
        cameraTranslate.yProperty().bind(heightProperty().divide(2).negate());
        camera.getTransforms().add(cameraTranslate);

        // Set cliping rectangle
        camera.setNearClip(1.0);
        camera.setFarClip(10000.0);

        setFill(Color.GRAY);

        // Mouse events for rotation
        setOnMousePressed(e -> {
            lastScreenX = e.getScreenX();
            lastScreenY = e.getScreenY();

            currentXAngle = rtx.getAngle();
            currentYAngle = rty.getAngle();

            rotating = true;
        });

        setOnMouseDragged(e -> {
            if (!rotating) return;

            double dx = e.getScreenX() - lastScreenX;
            double dy = e.getScreenY() - lastScreenY;

            rty.setAngle(currentYAngle - dx);
            rtx.setAngle(currentXAngle - dy);
        });

        setOnMouseReleased(e -> {
            rotating = false;
        });

        // Attach transform
        root.getTransforms().add(modelRotationTransform());
    }

    public Rotate modelRotationTransform() {
        return rty;
    }
}
