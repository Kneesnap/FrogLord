package net.highwayfrogs.editor.file.map.path.data;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.map.path.Path;
import net.highwayfrogs.editor.file.map.path.PathInfo;
import net.highwayfrogs.editor.file.map.path.PathSegment;
import net.highwayfrogs.editor.file.map.path.PathType;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.MapUIController;

/**
 * Represents PATH_ARC.
 * Created by Kneesnap on 9/16/2018.
 */
@Getter
@Setter
public class ArcSegment extends PathSegment {
    private SVector start = new SVector();
    private SVector center = new SVector();
    private SVector normal = new SVector();
    private int radius;
    private int pitch; // Delta Y in helix frame. (Can be opposite direction of normal)

    public ArcSegment() {
        super(PathType.ARC);
    }

    @Override
    protected void loadData(DataReader reader) {
        this.start.loadWithPadding(reader);
        this.center.loadWithPadding(reader);
        this.normal.loadWithPadding(reader);
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

    @Override
    protected SVector calculatePosition(PathInfo info) {
        short segmentDistance = (short) info.getSegmentDistance();
        float distanceCovered = Utils.fixedPointShortToFloat412(segmentDistance);

        SVector vector = new SVector(start).subtract(center);
        vector.multiply(segmentDistance / (double) getLength());
        //TODO: X and Z aren't accurate.
        vector.setY(Utils.floatToFixedPointShort412(-Utils.fixedPointIntToFloat2012(getPitch()) * (distanceCovered / (float) getLength())));
        vector.add(center);
        return vector;
    }

    @Override
    public void setupEditor(Path path, MapUIController controller, GUIEditorGrid editor) {
        super.setupEditor(path, controller, editor);
        editor.addSVector("Start", getStart());
        editor.addSVector("Center", getCenter());
        editor.addSVector("Normal", getNormal());
        editor.addIntegerField("Radius", getRadius(), this::setRadius, null);
        editor.addIntegerField("Pitch", getPitch(), this::setPitch, null);
    }
}
