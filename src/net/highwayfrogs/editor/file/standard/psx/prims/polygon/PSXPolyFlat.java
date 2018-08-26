package net.highwayfrogs.editor.file.standard.psx.prims.polygon;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXColorVector;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Flat shaded polygon.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
public class PSXPolyFlat extends PSXPolygon {
    private PSXColorVector color = new PSXColorVector();

    public PSXPolyFlat(int verticeCount) {
        super(verticeCount);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.color.load(reader);
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        this.color.save(writer);
    }
}
