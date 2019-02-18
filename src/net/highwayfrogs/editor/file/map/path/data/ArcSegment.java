package net.highwayfrogs.editor.file.map.path.data;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.path.Path;
import net.highwayfrogs.editor.file.map.path.PathInfo;
import net.highwayfrogs.editor.file.map.path.PathSegment;
import net.highwayfrogs.editor.file.map.path.PathType;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
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
        int segmentDistance = info.getSegmentDistance();

        int c = getRadius() * 0x6487;
        int t = (segmentDistance << 12) / c;
        int a = ((segmentDistance << 18) - (t * c)) / (getRadius() * 0x192);
        int cos = getConfig().rcos(a);
        int sin = getConfig().rsin(a);

        //TODO: X and Z aren't accurate. It could be because there is no rotation matrix.
        SVector vector = new SVector(start).subtract(center);
        vector.multiply(segmentDistance / (double) getLength());
        vector.setX((short) ((cos * getRadius()) >> 12));
        vector.setY((short) ((-getPitch() * segmentDistance) / getLength()));
        vector.setZ((short) ((sin * getRadius()) >> 12));
        PSXMatrix.MRApplyRotMatrix(null, vector, null);
        return vector.add(center);
    }

    @Override
    public void setupEditor(Path path, MapUIController controller, GUIEditorGrid editor) {
        super.setupEditor(path, controller, editor);
        editor.addFloatSVector("Start", getStart(), () -> controller.getController().resetEntities());
        editor.addFloatSVector("Center", getCenter(), () -> controller.getController().resetEntities());
        editor.addFloatSVector("Normal", getNormal());
        editor.addIntegerField("Radius", getRadius(), this::setRadius, null);
        editor.addIntegerField("Pitch", getPitch(), this::setPitch, null);
    }
}
