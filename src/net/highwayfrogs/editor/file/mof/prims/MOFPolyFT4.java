package net.highwayfrogs.editor.file.mof.prims;

import net.highwayfrogs.editor.file.mof.MOFPart;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Created by Kneesnap on 1/1/2019.
 */
public class MOFPolyFT4 extends MOFPolyTexture {
    public MOFPolyFT4(MOFPart parent) {
        super(parent, MOFPrimType.FT4, 4, 1);
    }

    @Override
    public void onLoad(DataReader reader) {
        setImageId(reader.readShort());
        loadUV(0, reader);
        short clutId = reader.readShort();
        loadUV(1, reader);
        short texId = reader.readShort();
        loadUV(2, reader);
        loadUV(3, reader);
        if (clutId != 0)
            throw new RuntimeException("MOFPolyFT4 had clut id which was not zero! (" + clutId + ").");
        if (texId != 0)
            throw new RuntimeException("MOFPolyFT4 had texture id which was not zero! (" + texId + ").");
    }

    @Override
    public void onSave(DataWriter writer) {
        writer.writeShort(getImageId());
        getUvs()[0].save(writer);
        writer.writeShort((short) 0);
        getUvs()[1].save(writer);
        writer.writeShort((short) 0);
        getUvs()[2].save(writer);
        getUvs()[3].save(writer);
    }

    @Override
    public boolean shouldAddInitialPadding() {
        return false;
    }
}
