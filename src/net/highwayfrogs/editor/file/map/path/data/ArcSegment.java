package net.highwayfrogs.editor.file.map.path.data;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.path.*;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.IVector;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.MapUIController;
import net.highwayfrogs.editor.utils.Utils;

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
    private int pitch; // Delta Y in helix frame. (Can be opposite direction of normal)

    public ArcSegment() {
        super(PathType.ARC, false);
    }

    @Override
    protected void loadData(DataReader reader) {
        this.start.loadWithPadding(reader);
        this.center.loadWithPadding(reader);
        this.normal.loadWithPadding(reader);
        int readRadius = reader.readInt();
        this.pitch = reader.readInt();

        float diff = Math.abs(Utils.fixedPointIntToFloat4Bit(readRadius - getRadius()));
        if (diff >= 3)
            throw new RuntimeException("getRadius() was too inaccurate in ArcSegment. (" + diff + ")");
    }

    @Override
    protected void saveData(DataWriter writer) {
        this.start.saveWithPadding(writer);
        this.center.saveWithPadding(writer);
        this.normal.saveWithPadding(writer);
        writer.writeInt(getRadius());
        writer.writeInt(this.pitch);
    }

    @Override
    protected PathResult calculatePosition(PathInfo info) {
        int segmentDistance = info.getSegmentDistance();

        IVector vec = new IVector(start.getX() - center.getX(), start.getY() - center.getY(), start.getZ() - center.getZ());
        final IVector vec2 = new IVector(normal.getX(), normal.getY(), normal.getZ());
        IVector vec3 = new IVector();
        SVector svec = new SVector();

        vec.normalise();                // Equivalent to MRNormaliseVEC ?? <- Check this! [AndyEder]
        vec3.outerProduct12(vec, vec2); // Equivalent to MROuterProduct12 ?? <- Check this! [AndyEder]

        PSXMatrix matrix = new PSXMatrix();
        matrix.getMatrix()[0][0] = (short) vec.getX();
        matrix.getMatrix()[1][0] = (short) vec.getY();
        matrix.getMatrix()[2][0] = (short) vec.getZ();
        matrix.getMatrix()[0][1] = (short) -vec2.getX();
        matrix.getMatrix()[1][1] = (short) -vec2.getY();
        matrix.getMatrix()[2][1] = (short) -vec2.getZ();
        matrix.getMatrix()[0][2] = (short) -vec3.getX();
        matrix.getMatrix()[1][2] = (short) -vec3.getY();
        matrix.getMatrix()[2][2] = (short) -vec3.getZ();

        int radius = getRadius();
        final int c = radius * 0x6487;
        final int t = (segmentDistance << 12) / c;
        final int a = ((segmentDistance << 18) - (t * c)) / (radius * 0x192);

        int cos = getConfig().rcos(a);
        int sin = getConfig().rsin(a);
        svec.setX((short) ((cos * radius) >> 12));
        svec.setY((short) ((-pitch * segmentDistance) / getLength()));
        svec.setZ((short) ((sin * radius) >> 12));

        PSXMatrix.MRApplyRotMatrix(matrix, svec, vec);
        vec.add(center);
        svec.setValues((short) -sin, (short) 0, (short) cos);
        return new PathResult(new SVector(vec), PSXMatrix.MRApplyRotMatrix(matrix, svec, new IVector()));
    }

    @Override
    public void recalculateLength() {
        setLength(Utils.floatToFixedPointInt4Bit(Utils.fixedPointIntToFloat4Bit(getRadius()) * (float) (Math.PI / 2)));
    }

    @Override
    public void setupEditor(Path path, MapUIController controller, GUIEditorGrid editor) {
        super.setupEditor(path, controller, editor);
        editor.addFloatVector("Start:", getStart(), () -> onUpdate(controller), controller.getController());
        editor.addFloatVector("Center:", getCenter(), () -> onUpdate(controller), controller.getController());

        editor.addSVector("Normal:", 12, getNormal(), () -> onUpdate(controller));

        editor.addFloatField("Pitch:", Utils.fixedPointIntToFloat4Bit(getPitch()), newValue -> {
            this.pitch = Utils.floatToFixedPointInt4Bit(newValue);
            onUpdate(controller);
        }, null);
    }

    /**
     * Gets the radius for this segment.
     * NOTE: This isn't full accurate, but it's close enough for now.
     * @return radius
     */
    public int getRadius() {
        double xDiff = (start.getFloatX() - center.getFloatX());
        double zDiff = (start.getFloatZ() - center.getFloatZ());
        return Utils.floatToFixedPointInt4Bit((float) Math.sqrt((xDiff * xDiff) + (zDiff * zDiff)));
    }
}
