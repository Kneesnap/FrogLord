package net.highwayfrogs.editor.games.sony.shared.collprim;

import javafx.scene.shape.Cylinder;

/**
 * Represents a collprim shape adapter for a Cylinder.
 * Created by Kneesnap on 9/26/2023.
 */
public class CylinderCollprimShapeAdapter extends CollprimShapeAdapter<Cylinder> {
    public CylinderCollprimShapeAdapter(ICollprim collprim, Cylinder cylinder) {
        super(collprim, cylinder);
    }

    @Override
    public void onLengthXUpdate(float newX) {
        if (isCollprimType(MRCollprim.CollprimType.CYLINDER_X))
            getShape().setHeight(Math.max(0.05, newX * 2));
    }

    @Override
    public void onLengthYUpdate(float newY) {
        if (isCollprimType(MRCollprim.CollprimType.CYLINDER_Y) || isCollprimType(PTCollprim.CollprimType.CYLINDER))
            getShape().setHeight(Math.max(0.05, newY * 2));
    }

    @Override
    public void onLengthZUpdate(float newZ) {
        if (isCollprimType(MRCollprim.CollprimType.CYLINDER_Z))
            getShape().setHeight(Math.max(0.05, newZ * 2));
    }

    @Override
    public void onRadiusSquaredUpdate(float newRadiusSquared) {
        getShape().setRadius(Math.max(0.05, Math.sqrt(newRadiusSquared)));
    }
}