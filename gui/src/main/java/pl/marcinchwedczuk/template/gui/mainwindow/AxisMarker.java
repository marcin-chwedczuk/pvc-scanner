package pl.marcinchwedczuk.template.gui.mainwindow;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Mesh;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

public class AxisMarker extends Group {
    public AxisMarker() {
        Sphere centerBall = new Sphere(1.0);
        centerBall.setMaterial(new PhongMaterial(Color.GREENYELLOW));
        add(centerBall);

        final float arrowSize = 20;

        // Transforms order: last -> 0th.

        {
            // Y axis
            PhongMaterial color = new PhongMaterial(Color.GREEN);

            Cylinder yAxisRod = new Cylinder(0.5, arrowSize);
            yAxisRod.getTransforms().add(new Translate(0, arrowSize / 2, 0));
            yAxisRod.setMaterial(color);
            add(yAxisRod);

            ConeMesh yAxis = new ConeMesh(1.0f, 2.0f);
            MeshView yAxisMV = new MeshView(yAxis);
            yAxisMV.getTransforms().add(new Translate(0, arrowSize, 0));
            yAxisMV.setMaterial(color);
            add(yAxisMV);
        }

        {
            // X axis
            PhongMaterial color = new PhongMaterial(Color.RED);

            Cylinder xAxisRod = new Cylinder(0.5, arrowSize);
            xAxisRod.getTransforms().add(new Translate(arrowSize / 2, 0, 0));
            xAxisRod.getTransforms().add(new Rotate(-90, new Point3D(0, 0, 1)));
            xAxisRod.setMaterial(color);
            add(xAxisRod);

            ConeMesh xAxis = new ConeMesh(1.0f, 2.0f);
            MeshView xAxisMV = new MeshView(xAxis);
            xAxisMV.getTransforms().add(new Translate(arrowSize, 0, 0));
            xAxisMV.getTransforms().add(new Rotate(-90, new Point3D(0, 0, 1)));
            xAxisMV.setMaterial(color);
            add(xAxisMV);
        }

        {
            // Z axis
            PhongMaterial color = new PhongMaterial(Color.BLUE);

            Cylinder zAxisRod = new Cylinder(0.5, arrowSize);
            zAxisRod.getTransforms().add(new Translate(0, 0, arrowSize / 2));
            zAxisRod.getTransforms().add(new Rotate(90, new Point3D(1, 0, 0)));
            zAxisRod.setMaterial(color);
            add(zAxisRod);

            ConeMesh zAxis = new ConeMesh(1.0f, 2.0f);
            MeshView zAxisMV = new MeshView(zAxis);
            zAxisMV.getTransforms().add(new Translate(0, 0, arrowSize));
            zAxisMV.getTransforms().add(new Rotate(90, new Point3D(1, 0, 0)));
            zAxisMV.setMaterial(color);
            add(zAxisMV);
        }
    }

    private void add(Node node) {
        getChildren().add(node);
    }

    public void scale(double factor) {
        // Must scale around (0,0) otherwise the entire group will move in 3D scene
        getTransforms().add(new Scale(factor, factor, factor));
    }

    public void translateTo(double x, double y, double z) {
        getTransforms().add(new Translate(x, y, z));
    }
}
