package net.highwayfrogs.editor.games.sony.shared.model.primitive;

import lombok.Getter;
import net.highwayfrogs.editor.games.psx.math.PSXMatrix;
import net.highwayfrogs.editor.games.psx.math.vector.IVector;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.games.sony.shared.model.PTModel;
import net.highwayfrogs.editor.games.sony.shared.model.staticmesh.PTStaticPartCel;

/**
 * The transformed vertex scratch buffer shared by a model's parts.
 * PT control primitives write slots in order; polygons consume their current contents.
 * Created by Kneesnap on 5/20/2024.
 */
@Getter
public class PTPrimitiveRenderState {
    private final IVector[] vertexBuffer = new IVector[256];

    public PTPrimitiveRenderState() {
        for (int i = 0; i < this.vertexBuffer.length; i++)
            this.vertexBuffer[i] = new IVector();
    }

    /**
     * Clears the vertex buffer.
     */
    public void clearVertexBuffer() {
        for (int i = 0; i < this.vertexBuffer.length; i++)
            this.vertexBuffer[i].clear();
    }

    /**
     * Executes a position control using the issuing part's transform, as in pvPTMPrimRenderCONTROL.
     * Slots not written by this control retain vertices from earlier controls, including other parts.
     */
    public void applyControl(PTModel model, PTStaticPartCel partCel, PTPrimitiveControl control) {
        if (!control.getControlType().isPosition())
            return;

        int transformId = partCel.getParentPart().getTransformId();
        PSXMatrix matrix = transformId < model.getTransformData().size()
                ? model.getTransformData().get(transformId).getMatrix() : null;
        for (int i = 0; i < control.getVertexCount(); i++) {
            SVector source = control.getVector(model, partCel, i);
            IVector target = this.vertexBuffer[control.getTargetTransformIndex() + i];
            if (matrix != null) {
                PSXMatrix.MRApplyMatrix(matrix, source, target);
            } else {
                target.setValues(source); // The model's skeleton has not been resolved or isn't valid.
            }
        }
    }
}