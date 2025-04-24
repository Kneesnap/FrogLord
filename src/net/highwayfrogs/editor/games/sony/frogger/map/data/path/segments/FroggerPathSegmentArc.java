package net.highwayfrogs.editor.games.sony.frogger.map.data.path.segments;

import javafx.scene.control.TextField;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.file.standard.IVector;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.games.sony.SCMath;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPath;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPathInfo;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPathResult;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPathSegmentType;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapPathManager.FroggerPathPreview;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.system.AbstractStringConverter;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.MathUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents PATH_ARC.
 * This is accurate, even though some Arc paths don't properly connect, such as in FOR2. These disconnected paths can be experienced in-game, and are thus accurate.
 * TODO: Allow changing start positions of each segment type as always updating the end position of the previous one. For ones which can't update the previous one (which aren't null), don't allow changing it.
 *  - This should only occur if the start/end of the segments already match exactly. Pressing a checkbox should also allow separation if they match.
 * Created by Kneesnap on 9/16/2018.
 */
@Getter
@Setter
public class FroggerPathSegmentArc extends FroggerPathSegment {
    private final SVector start = new SVector();
    private final SVector center = new SVector();
    private final SVector normal = FroggerPathSegmentArcOrientation.UP.applyToVector(new SVector()); // This is the "UP" direction of the curve, by default this is towards negative Y. (The circle is flat on the ground)
    private int pitch; // Delta Y in helix frame. (Can be opposite direction of normal)
    private double angle = .5; // How much of a full circle is completed. 0.0 is none, 1.0 is a full circle.

    // TODO: From doc "1st order continuity should be imposed. This equates to the centre points of adjacent segments lying in the plane normal
    //  to the tangent of the arc at that (shared) endpoint. This should also be imposed for arcs joining lines."
    //  ^^ What does this mean? (Apply these features to FrogLord)
    //  - It means an arc following another arc which shares the end/start point must use the same tangent line as the end of the previous arc. (Avoids entities snapping directions weirdly)
    //  - The same thing applies to lines which follow arcs.

    public FroggerPathSegmentArc(FroggerPath path) {
        super(path, FroggerPathSegmentType.ARC);
    }

    @Override
    protected void loadData(DataReader reader) {
        this.start.loadWithPadding(reader);
        this.center.loadWithPadding(reader);
        this.normal.loadWithPadding(reader);

        int readRadius;
        if (getPath().isOldPathFormatEnabled()) {
            readRadius = reader.readUnsignedShortAsInt();
            this.pitch = 0; // Doesn't support pitch.
            this.angle = DataUtils.fixedPointIntToFloatNBits(reader.readUnsignedShortAsInt(), 12);
        } else {
            // Calculate angle.
            // 'angle' was how this data was included in the "Frogger map export revision 18-04-97.doc" file.
            // It is assumed that this is how Mappy stored this data since it's a fairly sensible way to store and edit it,
            //  and an earlier format document is more likely to have data in the terms of how Mappy treated it.
            // Length = pitch + Circumference of Partial Circle = angle * (2 * pi * radius)
            // So, angle = (Length - pitch) / (2 * pi * radius)
            readRadius = reader.readInt();
            this.pitch = reader.readInt();

            // Some paths still don't calculate cleanly, but the fact that this is a real recognized stand-in for Pi and works across a good chunk of paths makes me think it's correct.
            // Though it's possible the ones which don't calculate cleanly were changed via GUI slider, much like what FrogLord supports.
            // I think this is the case since many of the non-even paths don't look right if you put the clean number in.
            this.angle = ((getLength() - Math.abs(this.pitch)) / (2 * SCMath.MAPPY_PI_HALF16 * readRadius));
        }

        // Radius warnings.
        int calculatedRadius = calculateFixedRadius();
        int diff = readRadius - calculatedRadius;
        if (Math.abs(diff) >= 30) // There are only 17 occurrences with a value > 1 in psx-retail-usa. These appear to be outliers, though I don't know why their values are odd.
            getLogger().warning("calculateFixedRadius() was inaccurate! (Read: " + readRadius + "/" + DataUtils.fixedPointIntToFloat4Bit(readRadius) + ", Calculated: " + calculatedRadius + "/" + DataUtils.fixedPointIntToFloat4Bit(calculatedRadius) + ", Difference: " + diff + "/" + DataUtils.fixedPointIntToFloat4Bit(diff) + ", Pitch: " + this.pitch + "/" + DataUtils.fixedPointIntToFloat4Bit(this.pitch) + ", Length: " + getLength() + "/" + DataUtils.fixedPointIntToFloat4Bit(getLength()) + ").");

        // Normal warnings.
        if (FroggerPathSegmentArcOrientation.getDirection(this.normal) == null)
            getLogger().info("Unexpected Arc Normal Vector: " + this.normal + " (Wasn't one of the expected orientations.)");
    }

    @Override
    protected void saveData(DataWriter writer) {
        this.start.saveWithPadding(writer);
        this.center.saveWithPadding(writer);
        this.normal.saveWithPadding(writer);
        if (getPath().isOldPathFormatEnabled()) {
            writer.writeUnsignedShort(calculateFixedRadius());
            writer.writeUnsignedShort(DataUtils.floatToFixedPointInt((float) this.angle, 12));
        } else {
            writer.writeInt(calculateFixedRadius());
            writer.writeInt(this.pitch);
        }
    }

    @Override
    protected String getCalculatedIncorrectLengthString() {
        return "Angle Slider: " + this.angle;
    }

    @Override
    protected int getIncorrectLengthTolerance() {
        // There are only 16 occurrences > 6, and all of them appear to be ones where it looks like Mappy made the path too short, potentially suggesting a bug in Mappy, or potentially an intentional manual edit?
        return 100; // (2 * pi * radius tolerance)
    }

    @Override
    public FroggerPathResult calculatePosition(FroggerPathInfo info) {
        int segmentDistance = info.getSegmentDistance();

        IVector vec = new IVector(this.start.getX() - this.center.getX(), this.start.getY() - this.center.getY(), this.start.getZ() - this.center.getZ());
        final IVector vec2 = new IVector(this.normal.getX(), this.normal.getY(), this.normal.getZ());
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

        int radius = calculateFixedRadius();
        final int c = radius * 0x6487;
        final int t = (segmentDistance << 12) / c;
        final int a = ((segmentDistance << 18) - (t * c)) / (radius * 0x192);

        int cos = SCMath.rcos(a);
        int sin = SCMath.rsin(a);
        svec.setX((short) ((cos * radius) >> 12));
        svec.setY((short) ((-getPitch() * segmentDistance) / getLength()));
        svec.setZ((short) ((sin * radius) >> 12));

        PSXMatrix.MRApplyRotMatrix(matrix, svec, vec);
        vec.add(this.center);
        svec.setValues((short) -sin, (short) 0, (short) cos);

        return new FroggerPathResult(new SVector(vec), PSXMatrix.MRApplyRotMatrix(matrix, svec, new IVector()));
    }

    @Override
    public int calculateFixedPointLength() {
        // Length = pitch + Circumference of Partial Circle = pitch + angle * (2 * pi * radius)
        return (int) Math.round((this.angle * 2 * SCMath.MAPPY_PI_HALF16 * calculateFixedRadius()) + Math.abs(this.pitch));
    }

    @Override
    public SVector getStartPosition() {
        return getStart();
    }

    @Override
    public void setupEditor(FroggerPathPreview pathPreview, GUIEditorGrid editor) {
        super.setupEditor(pathPreview, editor);

        TextField radiusField = editor.addFloatField("Arc Radius:", DataUtils.fixedPointIntToFloat4Bit(calculateFixedRadius()), null, null); // Read-Only.
        editor.addFloatVector("Start", getStart(), () -> {
            onUpdate(pathPreview);
            radiusField.setText(String.valueOf(DataUtils.fixedPointIntToFloat4Bit(calculateFixedRadius())));
        }, pathPreview.getController());
        editor.addFloatVector("Center", getCenter(), () -> {
            onUpdate(pathPreview);
            radiusField.setText(String.valueOf(DataUtils.fixedPointIntToFloat4Bit(calculateFixedRadius())));
        }, pathPreview.getController());

        // Add normal editor.
        FroggerPathSegmentArcOrientation orientation = FroggerPathSegmentArcOrientation.getDirection(this.normal);
        if (orientation != null && orientation != FroggerPathSegmentArcOrientation.CUSTOM) { // If the orientation is recognized, .
            editor.addEnumSelector("Circle Orientation", orientation, FroggerPathSegmentArcOrientation.values(), false, newOrientation -> {
                newOrientation.applyToVector(this.normal);
                onUpdate(pathPreview);
            }).setConverter(new AbstractStringConverter<>(FroggerPathSegmentArcOrientation::getDisplayName));
        } else {
            editor.addSVector("Normal:", 12, getNormal(), () -> onUpdate(pathPreview));
        }

        // Maps such as QB.MAP show Mappy was capable of making the angle go beyond 1.0, so a textbox is necessary to support this.
        editor.addDoubleField("Angle", this.angle, newAngle -> {
            this.angle = newAngle;
            onUpdate(pathPreview);
        }, newAngle -> newAngle >= 1 / 16D && newAngle <= (16 - (1D / SCMath.FIXED_POINT_ONE)));
        editor.addDoubleSlider("Angle:", this.angle, newAngle -> {
            this.angle = newAngle;
            onUpdate(pathPreview);
        }, .01, 1, false, null).setDisable(this.angle > 1D);

        // Old paths don't support pitch.
        if (!getPath().isOldPathFormatEnabled()) {
            editor.addFloatField("Pitch:", DataUtils.fixedPointIntToFloat4Bit(getPitch()), newValue -> {
                this.pitch = DataUtils.floatToFixedPointInt4Bit(newValue);
                onUpdate(pathPreview);
            }, null);
        }
    }

    /**
     * Calculates the radius for this segment.
     * NOTE: This isn't full accurate, but it's close enough for now.
     * @return radius
     */
    public int calculateFixedRadius() {
        int xDiff = this.start.getX() - this.center.getX();
        int yDiff = this.start.getY() - this.center.getY();
        int zDiff = this.start.getZ() - this.center.getZ();
        return MathUtils.fixedSqrt((xDiff * xDiff) + (zDiff * zDiff) + (yDiff * yDiff));
    }

    @Override
    public void setupNewSegment() {
        FroggerPath path = getPath();
        if (path.getSegments().size() > 0) {
            FroggerPathSegment lastSegment = path.getSegments().get(path.getSegments().size() - 1);
            this.start.setValues(lastSegment.calculatePosition(lastSegment.getLength()).getPosition());
        }

        this.pitch = 0;
        this.center.setValues(this.start);
        this.center.add(new SVector(-400, 0, -400));
        this.normal.setFloatY(-1F, 12);
        onUpdate(null);
    }

    @Getter
    @RequiredArgsConstructor
    public enum FroggerPathSegmentArcOrientation {
        UP("Up (Clockwise, -Y)", 0, SCMath.FIXED_POINT_ONE, 0),
        DOWN("Down (Counter-Clockwise, +Y)", 0, -SCMath.FIXED_POINT_ONE, 0),
        NORTH("North (Counter-Clockwise, +Z)", 0, 0, -SCMath.FIXED_POINT_ONE),
        EAST("East (Counter-Clockwise, +X)", -SCMath.FIXED_POINT_ONE, 0, 0),
        SOUTH("South (Clockwise, -Z)", 0, 0, SCMath.FIXED_POINT_ONE),
        WEST("West (Clockwise, -X)", SCMath.FIXED_POINT_ONE, 0, 0),
        CUSTOM("Custom (Make your own)", 0, SCMath.FIXED_POINT_ONE >> 2, -SCMath.FIXED_POINT_ONE >> 2);

        private final String displayName;
        private final short fixedPointX;
        private final short fixedPointY;
        private final short fixedPointZ;

        FroggerPathSegmentArcOrientation(String displayName, int fixedPointX, int fixedPointY, int fixedPointZ) {
            this.displayName = displayName;
            this.fixedPointX = (short) fixedPointX;
            this.fixedPointY = (short) fixedPointY;
            this.fixedPointZ = (short) fixedPointZ;
        }

        /**
         * Applies the vector values to the given vector.
         * @param vector the vector to apply the values to
         * @return vector
         */
        public SVector applyToVector(SVector vector) {
            vector.setValues(this.fixedPointX, this.fixedPointY, this.fixedPointZ);
            return vector;
        }

        /**
         * Gets the FroggerPathSegmentArcOrientation corresponding to the input SVector, if one exists.
         * @param input the input vector to find the value from
         * @return direction or null
         */
        public static FroggerPathSegmentArcOrientation getDirection(SVector input) {
            if (input == null)
                throw new NullPointerException("input");

            // Some arc path segments use value 4095 instead of 4096.
            // There doesn't appear to be any difference since it gets normalized as part of path calculations.
            // So, we might as well treat the vector <0, -4095, 0> the same as <0, -4096, 0>
            if (input.getX() == 0 && input.getZ() == 0 && input.getY() == 1 - SCMath.FIXED_POINT_ONE)
                return DOWN;

            for (int i = 0; i < values().length; i++) {
                FroggerPathSegmentArcOrientation direction = values()[i];
                if (direction.fixedPointX == input.getX() && direction.fixedPointY == input.getY() && direction.fixedPointZ == input.getZ())
                    return direction;
            }

            return null;
        }
    }
}