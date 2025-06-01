package net.highwayfrogs.editor.system.mm3d.holders;

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
}
