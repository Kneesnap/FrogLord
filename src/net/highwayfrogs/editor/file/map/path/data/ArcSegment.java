package net.highwayfrogs.editor.file.map.path.data;

import lombok.Getter;
import net.highwayfrogs.editor.file.map.path.PathInfo;
import net.highwayfrogs.editor.file.map.path.PathSegment;
import net.highwayfrogs.editor.file.map.path.PathType;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents PATH_ARC.
 * Created by Kneesnap on 9/16/2018.
 */
@Getter
public class ArcSegment extends PathSegment {
    private SVector start;
    private SVector center;
    private SVector normal;
    private int radius;
    private int pitch; // Delta Y in helix frame. (Can be opposite direction of normal)

    public ArcSegment() {
        super(PathType.ARC);
    }

    @Override
    protected void loadData(DataReader reader) {
        this.start = SVector.readWithPadding(reader);
        this.center = SVector.readWithPadding(reader);
        this.normal = SVector.readWithPadding(reader);
        this.radius = reader.readInt();
        this.pitch = reader.readInt();
    }

    @Override
    protected void saveData(DataWriter writer) {
        this.start.saveWithPadding(writer);
        this.center.saveWithPadding(writer);
        this.normal.saveWithPadding(writer);
        writer.writeInt(this.radius);
        writer.writeInt(this.pitch);
    }

    @Override // TODO: This is inaccurate.
    protected SVector calculatePosition(PathInfo info) {
        SVector vector = new SVector(center);
        vector.subtract(start);
        vector.multiply(info.getSegmentDistance() / getLength());
        vector.add(start);
        return vector;
    }
}
