package net.highwayfrogs.editor.file.mof.animation.transform;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.standard.IVector;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;

/**
 * Represents a basic mof transform data holder.
 * Created by Kneesnap on 1/5/2019.
 */
public abstract class TransformObject extends GameObject {

    /**
     * Get transform data.
     * @return transformData
     */
    public abstract short[] getTransform();

    /**
     * Creates a PSXMatrix with the animation data.
     * @return matrix
     */
    public abstract PSXMatrix createMatrix();

    private TransformData calculatePartTransform() {
        PSXMatrix importedPartTransform = null; //TODO: Not null. This may not matter, if 0 0 0 is origin.
        PSXMatrix importedModelTransform = null; //TODO: Not null. This may not matter, if 0 0 0 is origin.

        PSXMatrix psxMatrix = createMatrix();
        PSXMatrix mat34 = psxMatrix;

        SVector tempSVector = new SVector();

        if (importedPartTransform != null) {
            IVector vec = new IVector();

            PSXMatrix.MRMulMatrixABC(mat34, importedPartTransform, psxMatrix);
            tempSVector.svecEqualsVec(importedPartTransform.getTransform());
            PSXMatrix.MRApplyMatrix(mat34, tempSVector, vec);

            tempSVector.setX((short) (vec.getX() + psxMatrix.getTransform()[0]));
            tempSVector.setY((short) (vec.getY() + psxMatrix.getTransform()[1]));
            tempSVector.setZ((short) (vec.getZ() + psxMatrix.getTransform()[2]));
        } else if (importedModelTransform != null) {
            PSXMatrix.MRMulMatrixABC(importedModelTransform, mat34, psxMatrix);
            tempSVector.setX((short) (importedModelTransform.getTransform()[0] + psxMatrix.getTransform()[0]));
            tempSVector.setY((short) (importedModelTransform.getTransform()[1] + psxMatrix.getTransform()[1]));
            tempSVector.setZ((short) (importedModelTransform.getTransform()[2] + psxMatrix.getTransform()[2]));
        } else {
            tempSVector.setX((short) psxMatrix.getTransform()[0]);
            tempSVector.setY((short) psxMatrix.getTransform()[1]);
            tempSVector.setZ((short) psxMatrix.getTransform()[2]);
        }

        return new TransformData(psxMatrix, tempSVector);
    }

    private void updateAnim() {
        PSXMatrix ownerTransform = null; //TODO: This is generated at run-time. I'm not sure it's really needed, 0 0 0 should be just fine?

        TransformData data = calculatePartTransform();

        PSXMatrix lwTransform = data.getTempMatrix();

        // Calculate model -> world transform.
        PSXMatrix.MRMulMatrixABC(ownerTransform, data.getTempMatrix(), lwTransform);

        // Build the new transform.
        PSXMatrix.MRApplyMatrix(ownerTransform, data.getTempSVector(), lwTransform.getTransform());

        // MR_ADD_VEC:
        for (int i = 0; i < lwTransform.getTransform().length; i++)
            lwTransform.getTransform()[i] += ownerTransform.getTransform()[i];
    }

    @Getter
    @AllArgsConstructor
    private static final class TransformData {
        private PSXMatrix tempMatrix;
        private SVector tempSVector;
    }
}
