package net.highwayfrogs.editor.file.mof.prims;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.mof.MOFPart;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Created by Kneesnap on 1/1/2019.
 */
public class MOFPolyGT4 extends MOFPolyTexture {
    public MOFPolyGT4(MOFPart parent) {
        super(parent, MOFPrimType.GT4, 4, 4);
    }

    @Override
    public void onLoad(DataReader reader) {
        super.onLoad(reader);
        reader.skipShort(); // Padding.
    }

    @Override
    public void onSave(DataWriter writer) {
        super.onSave(writer);
        writer.writeNull(Constants.SHORT_SIZE); // Padding.
    }
}
