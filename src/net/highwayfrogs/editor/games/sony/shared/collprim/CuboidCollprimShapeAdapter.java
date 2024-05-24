package net.highwayfrogs.editor.games.sony.shared.collprim;

import javafx.scene.shape.Box;

/**
 * Represents a collprim shape adapter for a cuboid / Box.
 * Created by Kneesnap on 9/26/2023.
 */
public class CuboidCollprimShapeAdapter extends CollprimShapeAdapter<Box> {
    public CuboidCollprimShapeAdapter(ICollprim collprim, Box box) {
        super(collprim, box);
    }

    @Override
    public void onLengthXUpdate(float newX) {
        getShape().setWidth(newX * 2);
    }

    @Override
    public void onLengthYUpdate(float newY) {
        getShape().setHeight(newY * 2);
    }

    @Override
    public void onLengthZUpdate(float newZ) {
        getShape().setDepth(newZ * 2);
    }

    @Override
    public void onRadiusSquaredUpdate(float newRadiusSquared) {
        // Do nothing, this field does not impact cuboids.
    }
}