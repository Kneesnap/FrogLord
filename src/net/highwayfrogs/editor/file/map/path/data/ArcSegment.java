package net.highwayfrogs.editor.file.map.path.data;

import javafx.geometry.Point3D;
import javafx.scene.transform.Affine;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.path.Path;
import net.highwayfrogs.editor.file.map.path.PathInfo;
import net.highwayfrogs.editor.file.map.path.PathSegment;
import net.highwayfrogs.editor.file.map.path.PathType;
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

        // TODO: Fix this - needs more work, some (as yet unknown) issues with math [AndyEder]
        // DO NOT DELETE, THANKS! {AndyEder]
        //>>
        /*
        double segmentDistanceF = Utils.fixedPointIntToFloat4Bit(segmentDistance);

        double[] vecF = new double[3];
        vecF[0] = start.getFloatX() - center.getFloatX();
        vecF[1] = start.getFloatY() - center.getFloatY();
        vecF[2] = start.getFloatZ() - center.getFloatZ();

        double mag = Math.sqrt((vecF[0] * vecF[0]) + (vecF[1] * vecF[1]) + (vecF[2] * vecF[2]));
        vecF[0] /= mag;
        vecF[1] /= mag;
        vecF[2] /= mag;

        double[] vec2F = new double[3];
        vec2F[0] = normal.getFloatNormalX();
        vec2F[1] = normal.getFloatNormalY();
        vec2F[2] = normal.getFloatNormalZ();

        double[] vec3F = new double[3];
        vec3F[0] = (vecF[1] * vec2F[2]) - (vecF[2] * vec2F[1]);
        vec3F[1] = (vecF[2] * vec2F[0]) - (vecF[0] * vec2F[2]);
        vec3F[2] = (vecF[0] * vec2F[1]) - (vecF[1] * vec2F[0]);

        double cF = Utils.fixedPointIntToFloat4Bit(radius) * 491;
        double tF = segmentDistanceF / cF;
        double aF = (segmentDistanceF - (tF * cF)) / (cF * 16.0);

        double yF = (-Utils.fixedPointIntToFloat4Bit(pitch) * segmentDistanceF) / Utils.fixedPointIntToFloat4Bit(getLength());

        double cosF = Math.cos(aF);
        double sinF = Math.sin(aF);

        double[] svecF = new double[3];
        svecF[0] = cosF * Utils.fixedPointIntToFloat4Bit(radius);
        svecF[1] = yF;
        svecF[2] = sinF * Utils.fixedPointIntToFloat4Bit(radius);

        Affine xform = new Affine();
        xform.setMxx(vecF[0]);
        xform.setMyx(vecF[1]);
        xform.setMzx(vecF[2]);
        xform.setMxy(-vec2F[0]);
        xform.setMyy(-vec2F[1]);
        xform.setMzy(-vec2F[2]);
        xform.setMxz(-vec3F[0]);
        xform.setMyz(-vec3F[1]);
        xform.setMzz(-vec3F[2]);

        Point3D res = xform.transform(svecF[0], svecF[1], svecF[2]);
        vecF[0] = res.getX();
        vecF[1] = res.getY();
        vecF[2] = res.getZ();

        vecF[0] += center.getFloatX();
        vecF[1] += center.getFloatY();
        vecF[2] += center.getFloatZ();

        return new SVector((short)(vecF[0] * 16), (short)(vecF[1] * 16), (short)(vecF[2] * 16));
        */
        //>>

        IVector vec = new IVector(start.getX() - center.getX(), start.getY() - center.getY(), start.getZ() - center.getZ());
        IVector vec2 = new IVector();
        IVector vec3 = new IVector();
        SVector svec = new SVector();

        vec.normalise();    // Normalise - equivalent to MRNormaliseVEC ?? <- Check this! [AndyEder]
        vec2.vecEqualsSvec(normal);

        // Equivalent to MROuterProduct12 ?? <- Check this! [AndyEder]
        vec3.setX(((vec.getY() * vec2.getZ()) - (vec.getZ() * vec2.getY())) >> 12);
        vec3.setY(((vec.getZ() * vec2.getX()) - (vec.getX() * vec2.getZ())) >> 12);
        vec3.setZ(((vec.getX() * vec2.getY()) - (vec.getY() * vec2.getX())) >> 12);

        PSXMatrix matrix = new PSXMatrix();
        matrix.getMatrix()[0][0] = (short)vec.getX();
        matrix.getMatrix()[1][0] = (short)vec.getY();
        matrix.getMatrix()[2][0] = (short)vec.getZ();
        matrix.getMatrix()[0][1] = (short)-vec2.getX();
        matrix.getMatrix()[1][1] = (short)-vec2.getY();
        matrix.getMatrix()[2][1] = (short)-vec2.getZ();
        matrix.getMatrix()[0][2] = (short)-vec3.getX();
        matrix.getMatrix()[1][2] = (short)-vec3.getY();
        matrix.getMatrix()[2][2] = (short)-vec3.getZ();

        int c = radius * 0x6487;
        int t = (segmentDistance << 12) / c;
        int a = ((segmentDistance << 18) - (t * c)) / (radius * 0x192);

        int y = (-pitch * segmentDistance) / getLength();

        int cos = getConfig().rcos(a);
        int sin = getConfig().rsin(a);

        svec.setX((short)((cos * radius) >> 12));
        svec.setY((short)y);
        svec.setZ((short)((sin * radius) >> 12));

        PSXMatrix.MRApplyRotMatrix(matrix, svec, vec);
        vec.add(center);

        return new SVector((short)vec.getX(), (short)vec.getY(), (short)vec.getZ());

        /*
        //TODO: X and Z aren't accurate. It could be because there is no rotation matrix.
        // - Methods needed to make this accurate: MRNormaliseVEC, MROuterProduct12.
        SVector vector = new SVector(start).subtract(center);
        vector.multiply(segmentDistance / (double) getLength());
        vector.setX((short) ((cos * getRadius()) >> 12));
        vector.setY((short) ((-getPitch() * segmentDistance) / getLength()));
        vector.setZ((short) ((sin * getRadius()) >> 12));
        // PSXMatrix.MRApplyRotMatrix(null, vector, null); TODO: Fix this.
        return vector.add(center);
        */
    }

    @Override
    public void setupEditor(Path path, MapUIController controller, GUIEditorGrid editor) {
        super.setupEditor(path, controller, editor);
        editor.addFloatSVector("Start:", getStart(), () -> controller.getController().resetEntities());
        editor.addFloatSVector("Center:", getCenter(), () -> controller.getController().resetEntities());

        editor.addFloatNormalSVector("Normal:", getNormal());
        editor.addFloatField("Radius:", Utils.fixedPointIntToFloat4Bit(getRadius()));
        editor.addFloatField("Pitch:", Utils.fixedPointIntToFloat4Bit(getPitch()));
    }
}
