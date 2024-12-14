package net.highwayfrogs.editor.system.mm3d.blocks;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.mm3d.MMDataBlockBody;
import net.highwayfrogs.editor.system.mm3d.MisfitModel3DObject;
import net.highwayfrogs.editor.system.mm3d.OffsetType;

/**
 * Holds ascii metadata.
 * Version: 1.4+
 * Created by Kneesnap on 2/28/2019.
 */
@Getter
@Setter
public class MMMetaDataBlock extends MMDataBlockBody {
    private String key;
    private String value;

    public MMMetaDataBlock(MisfitModel3DObject parent) {
        super(OffsetType.META_DATA, parent);
    }

    @Override
    public void load(DataReader reader) {
        this.key = reader.readNullTerminatedString();
        this.value = reader.readNullTerminatedString();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeNullTerminatedString(this.key);
        writer.writeNullTerminatedString(this.value);
    }
}
