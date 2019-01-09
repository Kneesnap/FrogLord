package net.highwayfrogs.editor.file.mof.prims;

import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Created by Kneesnap on 1/1/2019.
 */
public class MOFPolyFT4 extends MOFPolyTexture {
    public MOFPolyFT4() {
        super(MOFPrimType.FT4, 4, 1);
    }

    @Override
    public void onLoad(DataReader reader) {
        setImageId(reader.readShort());
        loadUV(0, reader);
        setClutId(reader.readShort());
        loadUV(1, reader);
        setTextureId(reader.readShort());
        loadUV(2, reader);
        loadUV(3, reader);
    }

    @Override
    public void onSave(DataWriter writer) {
        writer.writeShort(getImageId());
        getUvs()[0].save(writer);
        writer.writeShort(getClutId());
        getUvs()[1].save(writer);
        writer.writeShort(getTextureId());
        getUvs()[2].save(writer);
        getUvs()[3].save(writer);
    }

    @Override
    public boolean shouldAddInitialPadding() {
        return false;
    }
}
