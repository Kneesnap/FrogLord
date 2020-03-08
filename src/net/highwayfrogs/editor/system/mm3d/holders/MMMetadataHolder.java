package net.highwayfrogs.editor.system.mm3d.holders;

import net.highwayfrogs.editor.system.mm3d.MMDataBlockHeader;
import net.highwayfrogs.editor.system.mm3d.MisfitModel3DObject;
import net.highwayfrogs.editor.system.mm3d.OffsetType;
import net.highwayfrogs.editor.system.mm3d.blocks.MMMetaDataBlock;

/**
 * Holds metadata.
 * Created by Kneesnap on 3/7/2020.
 */
public class MMMetadataHolder extends MMDataBlockHeader<MMMetaDataBlock> {
    public MMMetadataHolder(MisfitModel3DObject parent) {
        super(OffsetType.META_DATA, parent);
    }

    /**
     * Adds a new metadata key value pair.
     * @param key   The key value to add.
     * @param value The value data to add.
     */
    public void addMetadataValue(String key, String value) {
        MMMetaDataBlock newBlock = addNewElement();
        newBlock.setKey(key);
        newBlock.setValue(value);
    }
}
