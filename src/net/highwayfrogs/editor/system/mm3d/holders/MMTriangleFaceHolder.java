package net.highwayfrogs.editor.system.mm3d.holders;

import net.highwayfrogs.editor.system.mm3d.MMDataBlockHeader;
import net.highwayfrogs.editor.system.mm3d.MisfitModel3DObject;
import net.highwayfrogs.editor.system.mm3d.OffsetType;
import net.highwayfrogs.editor.system.mm3d.blocks.MMTriangleFaceBlock;

/**
 * Holds face data.
 * Created by Kneesnap on 3/2/2019.
 */
public class MMTriangleFaceHolder extends MMDataBlockHeader<MMTriangleFaceBlock> {
    public MMTriangleFaceHolder(MisfitModel3DObject model) {
        super(OffsetType.TRIANGLES, model);
    }

    /**
     * Add a triangle face to this.
     * @param v1 One of the vertices to add.
     * @param v2 One of the vertices to add.
     * @param v3 One of the vertices to add.
     */
    public MMTriangleFaceBlock addTriangle(int v1, int v2, int v3, short flags) {
        MMTriangleFaceBlock vertex = addNewElement();
        vertex.setFlags(flags);
        vertex.getVertices()[0] = v1;
        vertex.getVertices()[1] = v2;
        vertex.getVertices()[2] = v3;
        return vertex;
    }
}