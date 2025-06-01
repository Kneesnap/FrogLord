package net.highwayfrogs.editor.games.sony.shared.mof2.utils;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRMofPart;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRMofPolygon;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRMofPolygonBlock;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRMofPolygonType;

/**
 * Allows for building various polygon groups before adding them to the
 * Created by Kneesnap on 5/20/2025.
 */
@RequiredArgsConstructor
public class MRMofPartPolygonBuilder {
    @NonNull private final MRMofPart parentPart;
    private final MRMofPolygonBlock[] polygonBlocks = new MRMofPolygonBlock[MRMofPolygonType.values().length];

    /**
     * Adds the polygon.
     * @param polygon the polygon to add
     * @return if the polygon was added successfully
     */
    public boolean addPolygon(MRMofPolygon polygon) {
        if (polygon == null)
            throw new NullPointerException("polygon");

        MRMofPolygonBlock polygonBlock = this.polygonBlocks[polygon.getPolygonType().ordinal()];
        if (polygonBlock == null)
            this.polygonBlocks[polygon.getPolygonType().ordinal()] = polygonBlock = new MRMofPolygonBlock(this.parentPart, polygon.getPolygonType());

        return polygonBlock.addPolygon(polygon);
    }

    /**
     * Apply the polygons to the part, and clear the builder.
     */
    public void applyPolygonsToPart() {
        for (int i = 0; i < this.polygonBlocks.length; i++) {
            MRMofPolygonBlock block = this.polygonBlocks[i];
            if (block != null && !block.getPolygons().isEmpty()) { // empty blocks are okay to keep.
                if (!this.parentPart.addPolygonBlock(block))
                    throw new RuntimeException("Failed to add the polygon block to the mof part!");
                this.polygonBlocks[i] = null;
            }
        }
    }
}
