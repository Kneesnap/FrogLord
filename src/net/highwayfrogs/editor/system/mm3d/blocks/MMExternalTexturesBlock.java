package net.highwayfrogs.editor.system.mm3d.blocks;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.mm3d.MMDataBlockBody;
import net.highwayfrogs.editor.system.mm3d.MisfitModel3DObject;
import net.highwayfrogs.editor.system.mm3d.OffsetType;

import java.io.File;

/**
 * Contains information about external textures.
 * Created by Kneesnap on 2/28/2019.
 */
@Getter
@Setter
public class MMExternalTexturesBlock extends MMDataBlockBody {
    private short flags;
    private String path = ""; // File path to texture relative to model (directory separator is backslash)

    public static final char SEPARATOR_CHAR = '\\';

    public MMExternalTexturesBlock(MisfitModel3DObject parent) {
        super(OffsetType.EXTERNAL_TEXTURES, parent);
    }

    @Override
    public void load(DataReader reader) {
        this.flags = reader.readShort();
        this.path = reader.readNullTerminatedString().replace(SEPARATOR_CHAR, File.separatorChar);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeShort(this.flags);
        writer.writeNullTerminatedString(this.path != null ? this.path.replace(File.separatorChar, SEPARATOR_CHAR) : null);
    }
}
