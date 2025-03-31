package net.highwayfrogs.editor.games.sony.beastwars.map.data;

import lombok.Getter;
import net.highwayfrogs.editor.file.mof.MOFHolder;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.psx.PSXMath;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.beastwars.BeastWarsInstance;
import net.highwayfrogs.editor.games.sony.beastwars.map.BeastWarsMapFile;
import net.highwayfrogs.editor.games.sony.beastwars.ui.BeastWarsObjectManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.AppendInfoLoggerWrapper;

/**
 * Represents an object / entity created inside a Beast Wars map.
 * TODO: arch on discord was helping figure out the various values here. Review that conversation.
 * Created by Kneesnap on 9/22/2023.
 */
@Getter
public class BeastWarsMapObject extends SCGameData<BeastWarsInstance> {
    private final BeastWarsMapFile mapFile;
    private final short[][] matrix = new short[3][3]; // 3x3 matrix usable with PSX library functions & the gte.
    private short matrixPadding; // Generally zero.
    private int positionX;
    private int positionY;
    private int positionZ;
    private short formId;
    private short linkedZoneId = -1; // -1 indicates no zone. TODO: Can also link to line.
    private byte field3_24; // Usually 0, but not always.
    private byte field4_25; // Usually 0, but not always.
    private short field5_26; // TODO: ALWAYS ZERO OR ONE? (The code seems to support 2 as well..?)
    private short typeFlags;
    private byte field7_2A; // Player ID less than 4?
    private byte field8_2B; // Usually 0, but not always.
    private short unsureZeroToFiveFlags; // TODO: Seems to be one. Does this ever differ?
    private short unsureFlags; // TODO: Seems to be 0010 or 0000 sometimes... Perhaps indicates "first object in file". Or perhaps it indicates "player character"... etc.
    // TODO: IDEAS - LINES, SPLINES, LINKED ZONES, IS IT PASSED TO ENTITY FUNCTIONS?

    // TODO TOSS [Ideas]
    /*private int entityId = -1;
    private int formId = -1;
    private short subFormId = 0xFF;
    private short baseGenericData; // Seems to mean different things in different entities? not 100% sure.
    private long triggerData;
    private int initFlags;
    private int destroyFlags;
    private final int[] genericData = new int[4]; // Seems it's just like old/pre-recode Frogger.*/

    // TODO: 0x8000 might mean world relative pos?
    // TODO: 0x4000 might mean entity is reserved? -> field19_2A = player ID field17_26 == 1
    // TODO: 0x4000 might mean entity is reserved? -> field19_2A = 0? field17_26 == 2 -> SUB TYPE?

    public static final int SIZE_IN_BYTES = 48;

    public BeastWarsMapObject(BeastWarsMapFile mapFile) {
        super(mapFile.getGameInstance());
        this.mapFile = mapFile;
    }

    @Override
    public ILogger getLogger() {
        return new AppendInfoLoggerWrapper(this.mapFile.getLogger(), "object=" + Utils.getLoadingIndex(this.mapFile.getObjects(), this), AppendInfoLoggerWrapper.TEMPLATE_COMMA_SEPARATOR);
    }

    @Override
    public void load(DataReader reader) {
        for (int i = 0; i < this.matrix.length; i++)
            for (int j = 0; j < this.matrix[i].length; j++)
                this.matrix[i][j] = reader.readShort();
        this.matrixPadding = reader.readShort();

        this.positionX = reader.readInt(); // 0x14/4 - X Position Integer
        this.positionY = reader.readInt(); // 0x18/4 - Y Position Integer
        this.positionZ = reader.readInt(); // 0x1C/4 - Z Position Integer
        this.formId = reader.readShort(); // 0x20/2 - Form ID
        this.linkedZoneId = reader.readShort(); // 0x22/2 - Linked Zone ID
        this.field3_24 = reader.readByte(); // 0x24/1 - ???
        this.field4_25 = reader.readByte(); // 0x25/1 - ???
        this.field5_26 = reader.readShort(); // 0x26/2 - ??? Checked to be 1 or 2 in 80032350 PSX PAL.
        this.typeFlags = reader.readShort(); // 0x28/2 - Type Flags? 0xC000
        this.field7_2A = reader.readByte(); // 0x2A/1 - ??? Checked to be Less than 4 in 80032350 PSX PAL. Then used to index into global array. Could this be some kind of player ID?
        this.field8_2B = reader.readByte(); // 0x2B/1 - ???
        this.unsureZeroToFiveFlags = reader.readShort();  // 0x2C/2 - Unknown Between [0, 4].
        this.unsureFlags = reader.readShort(); // 0x2E/2 - Unsure Flags? [0x100, 0x80]

        // TODO: TOSS
        if (this.field3_24 != 0)
            getLogger().warning("field3_24 = %02X", this.field3_24);
        if (this.field4_25 != 0)
            getLogger().warning("field4_25 = %02X", this.field4_25);
        if (this.field5_26 != 0 && this.field5_26 != 1)
            getLogger().warning("field5_26 = %02X", this.field5_26);
    }

    @Override
    public void save(DataWriter writer) {
        for (int i = 0; i < this.matrix.length; i++)
            for (int j = 0; j < this.matrix[i].length; j++)
                writer.writeShort(this.matrix[i][j]);

        writer.writeShort(this.matrixPadding);
        writer.writeInt(this.positionX);
        writer.writeInt(this.positionY);
        writer.writeInt(this.positionZ);
        writer.writeShort(this.formId);
        writer.writeShort(this.linkedZoneId);
        writer.writeByte(this.field3_24);
        writer.writeByte(this.field4_25);
        writer.writeShort(this.field5_26);
        writer.writeShort(this.typeFlags);
        writer.writeByte(this.field7_2A);
        writer.writeByte(this.field8_2B);
        writer.writeShort(this.unsureZeroToFiveFlags);
        writer.writeShort(this.unsureFlags);
    }

    /**
     * Add entity script data to the editor.
     * @param editor The editor to build on top of.
     * @param manager the entity manager responsible for the data
     */
    public void setupEditor(GUIEditorGrid editor, BeastWarsObjectManager manager) {
        MOFHolder mof = getMof();
        editor.addTextField("Model File", mof != null ? mof.getFileDisplayName() : "None").setEditable(false);
        if (mof == null)
            editor.addTextField("formId", String.format("%04X", this.formId)).setEditable(false);

        // TODO: Add a proper editor once things are figured out.
        // TODO: POSITION
        editor.addFloatVector("Rotation", getRotation(), null, null, 12); // TODO: TOSS?
        editor.addFloatVector("Scale", getScale(), null, null, 12); // TODO: TOSS?
        if (this.matrixPadding != 0)
            editor.addSignedShortField("Matrix Padding", this.matrixPadding, newValue -> this.matrixPadding = newValue);

        editor.addSignedShortField("Linked Zone ID", this.linkedZoneId,
                newValue -> newValue >= -1 && newValue < this.mapFile.getZones().size(),
                newValue -> this.linkedZoneId = newValue);
        editor.addTextField("field15_24", String.format("%02X", this.field3_24)).setEditable(false);
        editor.addTextField("field16_25", String.format("%02X", this.field4_25)).setEditable(false);
        editor.addTextField("field17_26", String.format("%04X", this.field5_26)).setEditable(false);
        editor.addTextField("typeFlags", String.format("%04X", this.typeFlags)).setEditable(false);
        editor.addTextField("field19_2A", String.format("%02X", this.field7_2A)).setEditable(false);
        editor.addTextField("field20_2B", String.format("%02X", this.field8_2B)).setEditable(false);
        editor.addTextField("unsureZeroToFiveFlags", String.format("%04X", this.unsureZeroToFiveFlags)).setEditable(false);
        editor.addTextField("unsureFlags", String.format("%04X", this.unsureFlags)).setEditable(false);
    }

    /**
     * Gets the mof object displayed for this object, if there is one.
     */
    public MOFHolder getMof() {
        /*if ((this.typeFlags & 0xC000) == 0x4000)
            return null;*/ // TODO: Document this better! TODO: Players might actually use this model...

        if (this.formId < 0 || this.formId >= getGameInstance().getModelRemaps().size())
            return null;

        int realId = getGameInstance().getModelRemaps().get(this.formId);
        if (realId == 0)
            return null;

        return getGameInstance().getGameFile(realId);
    }

    /**
     * Gets the world X position as a floating point number.
     */
    public float getWorldPositionX() {
        return DataUtils.fixedPointIntToFloatNBits(this.positionX, 4);
    }

    /**
     * Gets the world Y position as a floating point number.
     */
    public float getWorldPositionY() {
        return DataUtils.fixedPointIntToFloatNBits(this.positionY, 4);
    }

    /**
     * Gets the world X position as a floating point number.
     */
    public float getWorldPositionZ() {
        return DataUtils.fixedPointIntToFloatNBits(this.positionZ, 4);
    }

    /**
     * Calculates the rotation of the object.
     * @return a 4.12 fixed point vector where 1 << 12 represents 2pi radians
     */
    public SVector getRotation() {
        return getRotation(null);
    }

    /**
     * Calculates the rotation of the object.
     * Based on the function at 0x80034998 in the PSX PAL release of Beast Wars.
     * @return a 4.12 fixed point vector where 1 << 12 represents 2pi radians
     */
    public SVector getRotation(SVector output) {
        if (output == null)
            output = new SVector();

        if ((this.matrix[1][1] > PSXMath.FIXED_PT12_ONE - 50) && (this.matrix[1][1] < PSXMath.FIXED_PT12_ONE + 50)) {
            output.setX((short) 0);
            output.setY(PSXMath.ratan2(-this.matrix[2][0], this.matrix[0][0]));
            output.setZ((short) 0);
        } else if (this.matrix[0][2] >= PSXMath.FIXED_PT12_ONE) {
            output.setX((short) -PSXMath.ratan2(-this.matrix[2][1], this.matrix[1][1]));
            output.setY((short) 0x400);
            output.setZ((short) 0);
        } else {
            short tableIndex = PSXMath.ratan2(this.matrix[0][1], this.matrix[0][0]);
            output.setX((short) -PSXMath.ratan2(this.matrix[1][2], this.matrix[2][2]));
            output.setZ((short) -tableIndex);

            int temp;
            int angle = PSXMath.RCOSSIN_TBL[((tableIndex & (PSXMath.FIXED_PT12_ONE - 1)) * 2) + 1];
            if (Math.abs(angle) < 0x19A) {
                temp = (this.matrix[0][1] << 12) / PSXMath.RCOSSIN_TBL[(tableIndex & (PSXMath.FIXED_PT12_ONE - 1)) * 2];
            } else {
                temp = (this.matrix[0][0] << 12) / angle;
            }

            output.setY(PSXMath.ratan2(this.matrix[0][2], temp));
        }

        return output;
    }

    /**
     * Calculates the scaling of the object
     * @return a 4.12 fixed point vector where 1 << 12 represents a scale factor of 1.0
     */
    public SVector getScale() {
        return getScale(null);
    }

    public SVector getScale(SVector output) {
        if (output == null)
            output = new SVector();

        /*
        The original matrix started as a rotation matrix, then was scaled.
        Given we've just recreated the original unscaled matrix, that should be enough to solve for the scaling matrix.
        But how would scaling work? The original scaling matrix is created from a scaling vector [x, y, z]:
        scaleMatrix = S =
        [x 0 0]
        [0 y 0]
        [0 0 z]

        Then, let the original unscaled matrix be:
        unscaledMatrix = O =
        [a b c]
        [d e f]
        [h i j]

        this.matrix = O * S =
        [ax by cz]
        [dx ey fz]
        [hx iy jz]
         */

        // Regenerate unscaled rotation matrix from angles.
        getRotation(output);
        short[] rotationAngles = {output.getX(), output.getY(), output.getZ()};
        short[][] unscaledMatrix = PSXMath.createRotationMatrix(rotationAngles);

        // Generate output.
        // This isn't perfectly accurate to the original data, most likely caused by lost precision, but I don't know for sure.
        // It's very close though.
        for (int i = 0; i < 3; i++) {
            int totalCount = 0;
            float fullScalar = 0;
            for (int j = 0; j < 3; j++) {
                short scaled = this.matrix[i][j];
                short unscaled = unscaledMatrix[i][j];
                if (scaled != 0 && unscaled != 0) {
                    totalCount++;
                    fullScalar += (float) scaled / unscaled;
                }
            }

            if (totalCount > 0)
                rotationAngles[i] = (short) ((fullScalar / totalCount) * PSXMath.FIXED_PT12_ONE);
        }

        output.setValues(rotationAngles[0], rotationAngles[1], rotationAngles[2]);
        return output;
    }
}