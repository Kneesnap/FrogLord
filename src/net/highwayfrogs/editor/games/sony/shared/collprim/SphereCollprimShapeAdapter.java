package net.highwayfrogs.editor.games.sony.shared.collprim;

import javafx.scene.shape.Sphere;

/**
 * Represents a collprim shape adapter for a sphere.
 * Created by Kneesnap on 9/26/2023.
 */
public class SphereCollprimShapeAdapter extends CollprimShapeAdapter<Sphere> {
    public SphereCollprimShapeAdapter(MRCollprim collprim, Sphere sphere) {
        super(collprim, sphere);
    }

    @Override
    public void onLengthXUpdate(float newX) {
        // Do nothing, this field does not impact spheres.
    }

    @Override
    public void onLengthYUpdate(float newY) {
        // Do nothing, this field does not impact spheres.
    }

    @Override
    public void onLengthZUpdate(float newZ) {
        // Do nothing, this field does not impact spheres.
    }

    @Override
    public void onRadiusSquaredUpdate(float newRadiusSquared) {
        getShape().setRadius(Math.max(0.05, Math.sqrt(newRadiusSquared)));
    }
}