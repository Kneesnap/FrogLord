package net.highwayfrogs.editor.system.mm3d.holders;

import net.highwayfrogs.editor.system.mm3d.MMDataBlockHeader;
import net.highwayfrogs.editor.system.mm3d.MisfitModel3DObject;
import net.highwayfrogs.editor.system.mm3d.OffsetType;
import net.highwayfrogs.editor.system.mm3d.blocks.MMExternalTexturesBlock;

/**
 * Holds external texture data.
 * Created by Kneesnap on 3/12/2019.
 */
public class MMExternalTextureHolder extends MMDataBlockHeader<MMExternalTexturesBlock> {
    public MMExternalTextureHolder(MisfitModel3DObject parent) {
        super(OffsetType.EXTERNAL_TEXTURES, parent);
    }

    /**
     * Add a texture with a given path.
     */
    public void addTexture(String path) {
        addNewElement().setPath(path);
    }
}
