package net.highwayfrogs.editor.games.sony.shared.model.primitive;

import lombok.Getter;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;

import java.util.Arrays;

/**
 * Represents render state.
 * Created by Kneesnap on 5/20/2024.
 */
@Getter
public class PTPrimitiveRenderState {
    private final SVector[] vertexBuffer = new SVector[256];

    public PTPrimitiveRenderState() {
        clearVertexBuffer();
    }

    /**
     * Clears the vertex buffer.
     */
    public void clearVertexBuffer() {
        Arrays.fill(this.vertexBuffer, SVector.EMPTY);
    }
}