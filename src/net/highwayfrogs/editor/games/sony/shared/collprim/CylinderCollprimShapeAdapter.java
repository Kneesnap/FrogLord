package net.highwayfrogs.editor.games.sony.shared.collprim;

import javafx.scene.shape.Cylinder;
import net.highwayfrogs.editor.games.sony.shared.collprim.MRCollprim.CollprimType;

/**
 * Represents a collprim shape adapter for a Cylinder.
 * Created by Kneesnap on 9/26/2023.
 */
public class CylinderCollprimShapeAdapter extends CollprimShapeAdapter<Cylinder> {
    public CylinderCollprimShapeAdapter(MRCollprim collprim, Cylinder cylinder) {
        super(collprim, cylinder);
    }

    @Override
    public void onLengthXUpdate(float newX) {
        if (getCollprim().getType() == CollprimType.CYLINDER_X)
            getShape().setHeight(Math.max(0.05, newX * 2));
    }

    @Override
    public void onLengthYUpdate(float newY) {
        if (getCollprim().getType() == CollprimType.CYLINDER_Y)
            getShape().setHeight(Math.max(0.05, newY * 2));
    }

    @Override
    public void onLengthZUpdate(float newZ) {
        if (getCollprim().getType() == CollprimType.CYLINDER_Z)
            getShape().setHeight(Math.max(0.05, newZ * 2));
    }

    @Override
    public void onRadiusSquaredUpdate(float newRadiusSquared) {
        getShape().setRadius(Math.max(0.05, Math.sqrt(newRadiusSquared)));
    }
}