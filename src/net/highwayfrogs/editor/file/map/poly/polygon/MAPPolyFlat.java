package net.highwayfrogs.editor.file.map.poly.polygon;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.standard.psx.PSXColorVector;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapPolygon;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Flat shaded polygon.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
@Setter
public class MAPPolyFlat extends MAPPolygon {
    private PSXColorVector color = new PSXColorVector();

    public MAPPolyFlat(MAPPolygonType type, int verticeCount) {
        super(type, verticeCount);
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

    @Override
    public FroggerMapPolygon convertToNewFormat(FroggerMapFile mapFile) {
        FroggerMapPolygon polygon = super.convertToNewFormat(mapFile);
        if (polygon.getColors().length != 1)
            throw new RuntimeException("Invalid number of colors! Expected one, got " + polygon.getColors().length + "! (" + getType() + "/" + polygon.getPolygonType() + ")");

        polygon.getColors()[0] = this.color.toCVector(null);
        return polygon;
    }
}