package net.highwayfrogs.editor.games.sony.shared.model;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.games.sony.SCGameObject.SCSharedGameObject;
import net.highwayfrogs.editor.games.sony.shared.model.staticmesh.PTStaticPart;
import net.highwayfrogs.editor.games.sony.shared.model.staticmesh.PTStaticPartCel;

/**
 * Runtime data for a PTStaticPart.
 * Created by Kneesnap on 5/22/2024.
 */
@Getter
public class PTPartInstanceData extends SCSharedGameObject {
    private final PTStaticPart part;
    private short renderFlags; // Render flags for each part (Takes priority over the mprim header flags.)
    private int currentPartCel; // Start (Interpolation) partCel. ACTUALLY THE CURRENT ONE.
    private int targetPartCel; // End (Interpolation) partCel.
    private int interpolationProgress; // The t value used for interpolation.
    @Setter private int lastInterpolationProgress = -1; // The last interpolation value.
    private SVector[] vectorBlock; // mime, mime-skin and skin vectors (vertices & normals)

    public PTPartInstanceData(PTStaticPart part) {
        super(part.getGameInstance());
        this.part = part;
        resizeVectorBlock();
    }

    /**
     * Resize the vector block.
     */
    public void resizeVectorBlock() {
        // PartCel.
        int maxSkinVectors = 0;
        for (int i = 0; i < this.part.getPartCels().size(); i++) {
            PTStaticPartCel partCel = this.part.getPartCels().get(i);
            if (partCel.getSkinVectorCount() > maxSkinVectors)
                maxSkinVectors = partCel.getSkinVectorCount();
        }

        // Calculate the size of the block and resize the block.
        int calculatedSize = this.part.getMimeVectors() + this.part.getMimeSkinVectors() + maxSkinVectors;
        if (this.vectorBlock == null || this.vectorBlock.length != calculatedSize) {
            SVector[] newVectorBlock = new SVector[calculatedSize];

            // Reuse object instances from the previous block.
            int copySize = this.vectorBlock != null ? Math.min(calculatedSize, this.vectorBlock.length) : 0;
            if (copySize > 0) {
                System.arraycopy(this.vectorBlock, 0, newVectorBlock, 0, copySize);
                for (int i = 0; i < copySize; i++)
                    this.vectorBlock[i].clear();
            }
            for (int i = copySize; i < newVectorBlock.length; i++)
                newVectorBlock[i] = new SVector();

            this.vectorBlock = newVectorBlock;
        }
    }
}