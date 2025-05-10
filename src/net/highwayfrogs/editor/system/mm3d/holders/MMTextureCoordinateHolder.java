package net.highwayfrogs.editor.system.mm3d.holders;

import net.highwayfrogs.editor.games.sony.shared.SCByteTextureUV;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRMofPolygon;
import net.highwayfrogs.editor.system.mm3d.MMDataBlockHeader;
import net.highwayfrogs.editor.system.mm3d.MisfitModel3DObject;
import net.highwayfrogs.editor.system.mm3d.OffsetType;
import net.highwayfrogs.editor.system.mm3d.blocks.MMTextureCoordinatesBlock;

/**
 * Holds texture coordinate data.
 * Created by Kneesnap on 3/12/2019.
 */
public class MMTextureCoordinateHolder extends MMDataBlockHeader<MMTextureCoordinatesBlock> {
    public MMTextureCoordinateHolder(MisfitModel3DObject parent) {
        super(OffsetType.TEXTURE_COORDINATES, parent);
    }

    /**
     * Adds a mof polygon coordinates.
     * @param polygon Textured polygon.
     */
    public void addMofPolygon(int face, MRMofPolygon polygon) {
        if (polygon.getVertexCount() == 4) {
            addRectangle(face, polygon);
        } else if (polygon.getVertexCount() == 3) {
            addTriangle(face, polygon);
        } else {
            throw new RuntimeException("Failed to add MOF Texture Coordinates.");
        }
    }

    /**
     * Add a triangle's texture coordinates to this.
     */
    public void addTriangle(int face, MRMofPolygon polygon) {
        MMTextureCoordinatesBlock block = addNewElement();
        block.setTriangle(face);

        for (int i = 0; i < MMTextureCoordinatesBlock.COORDINATES_COUNT; i++)
            loadUV(polygon, polygon.getTextureUvs().length - i - 1, block, i);
    }

    /**
     * Add a rectangle polygon's texture coords to this.
     */
    public void addRectangle(int face, MRMofPolygon polygon) {
        MMTextureCoordinatesBlock block1 = addNewElement();
        block1.setTriangle(face);
        loadUV(polygon, 0, block1, 0);
        loadUV(polygon, 3, block1, 1);
        loadUV(polygon, 1, block1, 2);

        MMTextureCoordinatesBlock block2 = addNewElement();
        block2.setTriangle(face + 1);
        loadUV(polygon, 1, block2, 0);
        loadUV(polygon, 3, block2, 1);
        loadUV(polygon, 2, block2, 2);
    }

    private void loadUV(MRMofPolygon polygon, int index, MMTextureCoordinatesBlock loadTo, int loadIndex) {
        SCByteTextureUV uv = polygon.getTextureUvs()[index];
        loadTo.getXCoordinates()[loadIndex] = uv.getFloatU();
        loadTo.getYCoordinates()[loadIndex] = uv.getFloatV();
    }
}
