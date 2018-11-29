package net.highwayfrogs.editor.file.standard.psx.prims.polygon;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXColorVector;
import net.highwayfrogs.editor.file.vlo.TextureMap;
import net.highwayfrogs.editor.file.vlo.TextureMap.TextureEntry;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents polygons with gouraud shading.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
public class PSXPolyGouraud extends PSXPolygon {
    private PSXColorVector[] colors;

    public PSXPolyGouraud(int verticeCount) {
        super(verticeCount);
        this.colors = new PSXColorVector[verticeCount];
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);

        // Read colors
        for (int i = 0; i < this.colors.length; i++) {
            PSXColorVector vector = new PSXColorVector();
            vector.load(reader);
            this.colors[i] = vector;
        }
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        for (PSXColorVector vector : colors)
            vector.save(writer);
    }

    @Override
    public TextureEntry getEntry(TextureMap map) {
        return map.getVertexColorMap().get(getSecondaryHashCode());
    }

    @Override
    public int getSecondaryHashCode() {
        int hash = 0;
        for (PSXColorVector color : this.colors)
            hash += (color.hashCode() / this.colors.length);
        return hash;
    }
}
