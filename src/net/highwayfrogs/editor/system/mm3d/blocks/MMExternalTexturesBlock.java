package net.highwayfrogs.editor.system.mm3d.blocks;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.system.mm3d.MMDataBlockBody;
import net.highwayfrogs.editor.system.mm3d.MisfitModel3DObject;
import net.highwayfrogs.editor.system.mm3d.OffsetType;
import net.highwayfrogs.editor.utils.StringUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.io.File;

/**
 * Contains information about external textures.
 * Created by Kneesnap on 2/28/2019.
 */
@Getter
public class MMExternalTexturesBlock extends MMDataBlockBody {
    @Setter private short flags;
    @NonNull private String relativeFilePath = ""; // File path to texture relative to model (directory separator is backslash)

    public static final char SEPARATOR_CHAR = '\\';

    public MMExternalTexturesBlock(MisfitModel3DObject parent) {
        super(OffsetType.EXTERNAL_TEXTURES, parent);
    }

    @Override
    public void load(DataReader reader) {
        this.flags = reader.readShort();
        this.relativeFilePath = convertToRuntimeForm(reader.readNullTerminatedString());
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeShort(this.flags);
        writer.writeNullTerminatedString(convertToWrittenForm(this.relativeFilePath));
    }

    /**
     * Returns just the file name portion of the path.
     */
    public String getFileName() {
        int separatorIndex = this.relativeFilePath.lastIndexOf(File.separatorChar);
        return this.relativeFilePath.substring(separatorIndex + 1);
    }

    /**
     * Sets the relative file path used to resolve any textures.
     * @param newFilePath the new file path to apply
     */
    public void setRelativeFilePath(String newFilePath) {
        if (StringUtils.isNullOrWhiteSpace(newFilePath)) {
            this.relativeFilePath = "";
        } else {
            this.relativeFilePath = convertToRuntimeForm(newFilePath);
        }
    }

    private static String convertToRuntimeForm(String input) {
        return input.replace('/', File.separatorChar)
                .replace(SEPARATOR_CHAR, File.separatorChar);
    }

    private static String convertToWrittenForm(String input) {
        return input.replace(File.separatorChar, SEPARATOR_CHAR);
    }
}
