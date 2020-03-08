package net.highwayfrogs.editor.system.mm3d.holders;

import net.highwayfrogs.editor.file.mof.prims.MOFPolyTexture;
import net.highwayfrogs.editor.file.standard.psx.ByteUV;
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
     * @param polyTex Textured polygon.
     */
    public void addMofPolygon(int face, MOFPolyTexture polyTex) {
        if (polyTex.isQuadFace()) {
            addRectangle(face, polyTex);
        } else if (polyTex.isTriFace()) {
            addTriangle(face, polyTex);
        } else {
            throw new RuntimeException("Failed to add MOF Texture Coordinates.");
        }
    }

    /**
     * Add a triangle's texture coordinates to this.
     */
    public void addTriangle(int face, MOFPolyTexture poly) {
        MMTextureCoordinatesBlock block = addNewElement();
        block.setTriangle(face);

        for (int i = 0; i < MMTextureCoordinatesBlock.COORDINATES_COUNT; i++)
            loadUV(poly, poly.getUvs().length - i - 1, block, i);
    }

    /**
     * Add a rectangle polygon's texture coords to this.
     */
    public void addRectangle(int face, MOFPolyTexture poly) {
        MMTextureCoordinatesBlock block1 = addNewElement();
        block1.setTriangle(face);
        loadUV(poly, 0, block1, 0);
        loadUV(poly, 3, block1, 1);
        loadUV(poly, 1, block1, 2);

        MMTextureCoordinatesBlock block2 = addNewElement();
        block2.setTriangle(face + 1);
        loadUV(poly, 1, block2, 0);
        loadUV(poly, 3, block2, 1);
        loadUV(poly, 2, block2, 2);
    }

    private void loadUV(MOFPolyTexture poly, int index, MMTextureCoordinatesBlock loadTo, int loadIndex) {
        ByteUV uv = poly.getUvs()[index];
        loadTo.getXCoordinates()[loadIndex] = uv.getFloatU();
        loadTo.getYCoordinates()[loadIndex] = uv.getFloatV();
    }
}
