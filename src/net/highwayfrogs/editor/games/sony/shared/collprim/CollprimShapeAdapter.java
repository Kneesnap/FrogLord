package net.highwayfrogs.editor.games.sony.shared.collprim;

import javafx.scene.shape.Shape3D;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * A collprim shape adapter is the bridge between a collprim and a 3D shape.
 * It allows the changes in collprim values to immediately update a 3D shape.
 * Created by Kneesnap on 9/26/2023.
 */
@Getter
@AllArgsConstructor
public abstract class CollprimShapeAdapter<TShape extends Shape3D> {
    private final ICollprim collprim;
    private final TShape shape;

    /**
     * Called when the xLength value is updated.
     * @param newX The new xLength value
     */
    public abstract void onLengthXUpdate(float newX);

    /**
     * Called when the yLength value is updated.
     * @param newY The new yLength value
     */
    public abstract void onLengthYUpdate(float newY);

    /**
     * Called when the z Length value is updated.
     * @param newZ The new zLength value
     */
    public abstract void onLengthZUpdate(float newZ);

    /**
     * Called when the radius squared value is updated.
     * @param newRadiusSquared The new radius squared value.
     */
    public abstract void onRadiusSquaredUpdate(float newRadiusSquared);

    /**
     * Test if this adapter is adapted to a collprim of the given type.
     * @param collprimType the type to test.
     * @return true iff the collprim is of the given type.
     */
    protected boolean isCollprimType(MRCollprim.CollprimType collprimType) {
        return (getCollprim() instanceof MRCollprim) && ((MRCollprim) getCollprim()).getType() == collprimType;
    }

    /**
     * Test if this adapter is adapted to a collprim of the given type.
     * @param collprimType the type to test.
     * @return true iff the collprim is of the given type.
     */
    protected boolean isCollprimType(PTCollprim.CollprimType collprimType) {
        return (getCollprim() instanceof PTCollprim) && ((PTCollprim) getCollprim()).getType() == collprimType;
    }
}