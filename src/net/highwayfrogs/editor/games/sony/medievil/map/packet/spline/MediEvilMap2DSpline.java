package net.highwayfrogs.editor.games.sony.medievil.map.packet.spline;

import javafx.scene.control.CheckBox;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.SCMath;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.map.packet.pathchain.MediEvilMapPathChain;
import net.highwayfrogs.editor.games.sony.medievil.map.packet.pathchain.MediEvilMapPathDirection;
import net.highwayfrogs.editor.games.sony.medievil.map.packet.pathchain.ui.MediEvilPathManager;
import net.highwayfrogs.editor.games.sony.shared.spline.MRSplineMatrix;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.components.propertylist.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.StringUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.AppendInfoLoggerWrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a "2D spline" as found in MediEvil. (not sure why it's labelled 2D by the game considering it's actually 3D...)
 * Created by Kneesnap on 2/26/2026.
 */
public class MediEvilMap2DSpline extends SCGameData<MediEvilGameInstance> implements IPropertyListCreator, IMediEvilMapSpline {
    @Getter private final MediEvilMap2DSplinePacket splinePacket;
    private final List<SVector> subDivisions = new ArrayList<>(); // Padding seems to be garbage.
    private final List<SVector> immutableSubDivisions = Collections.unmodifiableList(this.subDivisions);
    @Getter private MediEvilMap3DSpline cameraSpline; // Used by certain camera modes.
    @Getter private MediEvilMapPathChain pathChain;
    @Getter private byte numDeadSubDivsStart;
    @Getter private byte numDeadSubDivsEnd;
    @Getter private byte flags;
    @Getter private short uniqueId; // TODO: Automatically manage/set this.

    // NOTE:
    // My current hypothesis for why the path spline is linked to the camera spline is about progression.
    // The game probably will find the closest subdivision in the path spline to the player, then use the equivalent subdivision in the camera spline.
    // This allows syncing up the paths.

    private int tempSplineSubDivPointer = -1;
    private byte tempCameraSplineId = -1;
    private byte tempParentChainId = -1;

    public static final int FLAG_DEAD_START_JUMP = Constants.BIT_FLAG_0; // Dead subdivisions at a start should jump the camera.
    public static final int FLAG_DEAD_END_JUMP = Constants.BIT_FLAG_1; // Dead subdivisions at an end should jump the camera.
    private static final int FLAG_STATIC_CAMERA = Constants.BIT_FLAG_2; // The camera should be static. (Appears unused)
    private static final int FLAG_CAMERA_START_CONNECT = Constants.BIT_FLAG_3; // The camera spline has connections at the start. (Appears unused)
    private static final int FLAG_CAMERA_END_CONNECT = Constants.BIT_FLAG_4; // The camera spline has connections at the end. (Appears unused)
    public static final int FLAG_NO_CAMERA = Constants.BIT_FLAG_5; // The spline has no camera spline.
    public static final int FLAG_NO_ENTITY = Constants.BIT_FLAG_6; // Entities will not navigate this spline.
    private static final int FLAG_INTERMISSION = Constants.BIT_FLAG_7; // Entities will not navigate this spline. (Flag appears unused)

    public static final int MINIMUM_SUBDIVISION_COUNT = 3;
    public static final int MAXIMUM_SUBDIVISION_COUNT = 17;

    public MediEvilMap2DSpline(MediEvilMap2DSplinePacket splinePacket) {
        super(splinePacket.getGameInstance());
        this.splinePacket = splinePacket;
    }

    /**
     * Gets the ID used to identify this spline when saving/loading.
     * This ID has the ability to change while FrogLord applies changes to files, so it should not be used to identify a spline.
     */
    public int getId() {
        int index = this.splinePacket.getSplines().indexOf(this);
        if (index < 0)
            throw new IllegalArgumentException("The referenced " + getClass().getSimpleName() + " is not registered as part of the MediEvilMapFile.");

        return index;
    }

    @Override
    public void applySplineMatrix(MRSplineMatrix splineMatrix) {
        applySplineMatrixToSubDivisions(splineMatrix);
    }

    /**
     * Sets the subdivision count to the new number
     * @param newSubDivisionCount the new amount of subdivisions to apply
     */
    public void setSubdivisionCount(int newSubDivisionCount) {
        if (newSubDivisionCount < MINIMUM_SUBDIVISION_COUNT || newSubDivisionCount > MAXIMUM_SUBDIVISION_COUNT)
            throw new IllegalArgumentException("Invalid subDivisionCount: " + newSubDivisionCount);

        while (this.subDivisions.size() > newSubDivisionCount)
            this.subDivisions.remove(this.subDivisions.size() - 1);
        while (newSubDivisionCount > this.subDivisions.size())
            this.subDivisions.add(new SVector());
    }

    @Override
    public void setupEditor(MediEvilPathManager manager, GUIEditorGrid editorGrid) {
        // Allow changing the number of subDivisions.
        editorGrid.addIntegerSlider("Subdivisions", this.subDivisions.size(), newValue -> {
            MRSplineMatrix tempMatrix = SCMath.createBezierCurve(getGameInstance(), this.subDivisions).toSplineMatrix();
            setSubdivisionCount(newValue);
            applySplineMatrixToSubDivisions(tempMatrix);
            manager.updatePreview(this);
        }, MINIMUM_SUBDIVISION_COUNT, MAXIMUM_SUBDIVISION_COUNT);


        if (testDeadStartSubDivsEnabled()) {
            editorGrid.addSignedShortField("Dead Start Subdivisions", DataUtils.byteToUnsignedShort(this.numDeadSubDivsStart),
                    testValue -> testValue >= 0 && testValue < this.subDivisions.size(), newValue -> {
                this.numDeadSubDivsStart = DataUtils.unsignedShortToByte(newValue);
                manager.updatePreview(this);
            });
        }

        if (testDeadEndSubDivsEnabled()) {
            editorGrid.addSignedShortField("Dead End Subdivisions", DataUtils.byteToUnsignedShort(this.numDeadSubDivsEnd),
                    testValue -> testValue >= 0 && testValue < this.subDivisions.size(), newValue -> {
                this.numDeadSubDivsEnd = DataUtils.unsignedShortToByte(newValue);
                manager.updatePreview(this);
            });
        }

        editorGrid.addBoldLabel("Flags:");
        addFlagCheckBox(editorGrid, "Dead Start Jump", FLAG_DEAD_START_JUMP, "Dead subdivisions at a start should jump the camera.").setDisable(!canHaveDeadStartJumpFlag());
        addFlagCheckBox(editorGrid, "Dead End Jump", FLAG_DEAD_END_JUMP, "Dead subdivisions at an end should jump the camera.").setDisable(!canHaveDeadEndJumpFlag());
        addFlagCheckBox(editorGrid, "Static Camera", FLAG_STATIC_CAMERA, "The camera should be static. (Appears unused)").setDisable(true);
        addFlagCheckBox(editorGrid, "Camera Start Connect", FLAG_CAMERA_START_CONNECT, "The camera spline has connections at the start. (Appears unused)").setDisable(true); // TODO: Auto-generated?
        addFlagCheckBox(editorGrid, "Camera End Connect", FLAG_CAMERA_END_CONNECT, "The camera spline has connections at the end. (Appears unused)").setDisable(true); // TODO: Auto-generated?
        addFlagCheckBox(editorGrid, "No Camera Spline", FLAG_NO_CAMERA, "The spline has no camera spline.").setDisable(true);
        addFlagCheckBox(editorGrid, "Disable Navigation", FLAG_NO_ENTITY, "Entities will not navigate this spline.");
        addFlagCheckBox(editorGrid, "Intermission", FLAG_INTERMISSION, "Used for an intermission. Appears unused.").setDisable(true);
    }

    /**
     * Test if either the dead sub divs are dead at the end or the start.
     * @return testDeadSubDivs
     */
    public boolean testDeadSubDivsEnabled() {
        return testDeadStartSubDivsEnabled() || testDeadEndSubDivsEnabled();
    }

    /**
     * Test if the spline appears to have dead subdivisions enabled at the start of the spline.
     */
    public boolean testDeadStartSubDivsEnabled() {
        if ((this.tempCameraSplineId == -1) ? (this.cameraSpline == null) : ((this.flags & FLAG_NO_CAMERA) == FLAG_NO_CAMERA))
            return false;

        // current spline
        //  -> connected next splines (Only 0 or 1 unless at junction)
        //  -> connected previous splines (Only 0 or 1 unless at junction)
        //  -> Also track xStart for each connected spline, which is just the direction as tracked by the CURRENT path chain node.
        //  -> This also directly maps to which subDivision (first/last) was picked.

        if (this.pathChain.getPreviousChains().size() > 1 && this.pathChain.getPathSplines().get(0) == this)
            for (int i = 0; i < this.pathChain.getPreviousChains().size(); i++)
                if (this.pathChain.getPreviousChains().getDirection(i) == MediEvilMapPathDirection.PATH_STARTS)
                    return true;

        if (this.pathChain.getNextChains().size() > 1 && this.pathChain.getPathSplines().get(this.pathChain.getPathSplines().size() - 1) == this)
            for (int i = 0; i < this.pathChain.getNextChains().size(); i++)
                if (this.pathChain.getNextChains().getDirection(i) == MediEvilMapPathDirection.PATH_STARTS)
                    return true;


        return false;
    }

    /**
     * Test if the spline appears to have dead subdivisions enabled at the end of the spline.
     */
    public boolean testDeadEndSubDivsEnabled() {
        if ((this.tempCameraSplineId == -1) ? (this.cameraSpline == null) : ((this.flags & FLAG_NO_CAMERA) == FLAG_NO_CAMERA))
            return false;

        // current spline
        //  -> connected next splines (Only 0 or 1 unless at junction)
        //  -> connected previous splines (Only 0 or 1 unless at junction)
        //  -> Also track xStart for each connected spline, which is just the direction as tracked by the CURRENT path chain node.
        //  -> This also directly maps to which subDivision (first/last) was picked.

        if (this.pathChain.getPreviousChains().size() > 1 && this.pathChain.getPathSplines().get(0) == this)
            for (int i = 0; i < this.pathChain.getPreviousChains().size(); i++)
                if (this.pathChain.getPreviousChains().getDirection(i) == MediEvilMapPathDirection.PATH_ENDS)
                    return true;

        if (this.pathChain.getNextChains().size() > 1 && this.pathChain.getPathSplines().get(this.pathChain.getPathSplines().size() - 1) == this)
            for (int i = 0; i < this.pathChain.getNextChains().size(); i++)
                if (this.pathChain.getNextChains().getDirection(i) == MediEvilMapPathDirection.PATH_ENDS)
                    return true;


        return false;
    }


    /**
     * Test if it is possible to have the flag for dead start jump.
     */
    public boolean canHaveDeadStartJumpFlag() {
        return testDeadStartSubDivsEnabled() && this.numDeadSubDivsStart != 0;
    }

    /**
     * Test if it is possible to have the flag for dead end jump.
     */
    public boolean canHaveDeadEndJumpFlag() {
        return testDeadEndSubDivsEnabled() && this.numDeadSubDivsEnd != 0;
    }

    private void updateFlags() {
        // TODO: Finish this function.
        if (!canHaveDeadStartJumpFlag())
            this.flags &= ~FLAG_DEAD_START_JUMP;
        if (!canHaveDeadEndJumpFlag())
            this.flags &= ~FLAG_DEAD_END_JUMP;

        if (calculateNoCameraSplineFlag()) {
            this.flags |= FLAG_NO_CAMERA;
        } else {
            this.flags &= ~FLAG_NO_CAMERA;
        }
    }

    private boolean calculateCameraStartConnectFlag() {
        if (this.cameraSpline == null)
            return false;

        if (this.pathChain.getPathSplines().get(0) == this) {
            for (int i = 0; i < this.pathChain.getPreviousChains().size(); i++) {
                MediEvilMapPathChain pathChain = this.pathChain.getPreviousChains().get(i);
                MediEvilMap2DSpline otherSpline = pathChain.getPathSplines().get(pathChain.getPathSplines().size() - 1);
                if (otherSpline.getCameraSpline() != null || otherSpline.tempCameraSplineId != -1)
                    return true;
            }

            return false;
        } else {
            MediEvilMap2DSpline otherSpline = this.pathChain.getPathSplines().get(this.pathChain.getPathSplines().indexOf(this) - 1);
            return otherSpline.getCameraSpline() != null || otherSpline.tempCameraSplineId != -1;
        }
    }

    private boolean calculateNoCameraSplineFlag() {
        return this.cameraSpline == null;
    }

    private CheckBox addFlagCheckBox(GUIEditorGrid editorGrid, String flagName, int bitMask, String tooltip) {
        boolean flagState = (this.flags & bitMask) == bitMask;

        CheckBox checkBox = editorGrid.addCheckBox(flagName, flagState, newState -> {
            if (newState) {
                this.flags |= (byte) bitMask;
            } else {
                this.flags &= (byte) ~bitMask;
            }
        });
        if (!StringUtils.isNullOrWhiteSpace(tooltip))
            checkBox.setTooltip(FXUtils.createTooltip(tooltip));

        return checkBox;
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        propertyList.addInteger("Unique ID", this.uniqueId);
        propertyList.addString("Flags", String.format("%02X", this.flags));
        if (testDeadStartSubDivsEnabled())
            propertyList.addInteger("Number of dead sub-division starts", DataUtils.byteToUnsignedShort(this.numDeadSubDivsStart));
        if (testDeadEndSubDivsEnabled())
            propertyList.addInteger("Number of dead sub-division ends", DataUtils.byteToUnsignedShort(this.numDeadSubDivsEnd));

        propertyList.addString(this.pathChain, "Path Chain", this.pathChain != null ? "ID: " + this.pathChain.getId() : "None");
        propertyList.addString(this.cameraSpline, "Camera Spline", this.cameraSpline != null ? "ID: " + this.cameraSpline.getId() : "None");
        propertyList.addString(this::addSubDivisions, "Positions (Subdivisions)", String.valueOf(this.subDivisions.size()));
    }

    private void addSubDivisions(PropertyListNode propertyList) {
        for (int i = 0; i < this.subDivisions.size(); i++)
            propertyList.add("subDivisions[" + i + "]", this.subDivisions.get(i));
    }

    @Override
    public ILogger getLogger() {
        return new AppendInfoLoggerWrapper(this.splinePacket.getLogger(), getClass().getSimpleName()
                + "[" + this.splinePacket.getSplines().indexOf(this) + "]", AppendInfoLoggerWrapper.TEMPLATE_OVERRIDE_AT_ORIGINAL);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + this.splinePacket.getSplines().indexOf(this) + "@" + this.splinePacket.getParentFile().getFileDisplayName() + "}";
    }

    @Override
    public List<SVector> getSubDivisions() {
        return this.immutableSubDivisions;
    }

    @Override
    public void load(DataReader reader) {
        short subDivCount = reader.readUnsignedByteAsShort();
        this.tempCameraSplineId = reader.readByte();
        this.tempParentChainId = reader.readByte();
        this.numDeadSubDivsStart = reader.readByte();
        this.numDeadSubDivsEnd = reader.readByte();
        this.flags = reader.readByte();
        this.uniqueId = reader.readShort();
        this.tempSplineSubDivPointer = reader.readInt();

        // Setup list.
        this.subDivisions.clear();
        for (int i = 0; i < subDivCount; i++)
            this.subDivisions.add(new SVector());
    }

    @Override
    public void save(DataWriter writer) {
        updateFlags();
        writer.writeUnsignedByte((short) this.subDivisions.size());
        writer.writeUnsignedByte((short) (this.cameraSpline != null ? this.cameraSpline.getId() : 0xFF));
        writer.writeUnsignedByte((short) this.pathChain.getId());
        writer.writeByte(this.numDeadSubDivsStart);
        writer.writeByte(this.numDeadSubDivsEnd);
        writer.writeByte(this.flags);
        writer.writeShort(this.uniqueId);
        this.tempSplineSubDivPointer = writer.writeNullPointer();
    }

    void loadSubDivisions(DataReader reader) {
        if (this.tempSplineSubDivPointer <= 0)
            throw new RuntimeException("Cannot load tempSplineSubDivPointer, the pointer " + NumberUtils.toHexString(this.tempSplineSubDivPointer) + " is invalid.");

        requireReaderIndex(reader, this.tempSplineSubDivPointer, "Expected subDivisions");
        this.tempSplineSubDivPointer = -1;

        // Read subdivisions.
        for (int i = 0; i < this.subDivisions.size(); i++)
            this.subDivisions.get(i).loadWithPadding(reader); // Padding seems to be garbage.
    }

    void saveSubDivisions(DataWriter writer) {
        if (this.tempSplineSubDivPointer <= 0)
            throw new RuntimeException("Cannot save tempSplineSubDivPointer, the pointer " + NumberUtils.toHexString(this.tempSplineSubDivPointer) + " is invalid.");

        writer.writeAddressTo(this.tempSplineSubDivPointer);
        this.tempSplineSubDivPointer = -1;

        // Write subdivisions.
        for (int i = 0; i < this.subDivisions.size(); i++)
            this.subDivisions.get(i).saveWithPadding(writer);
    }

    void resolveReferences() {
        resolveCameraSpline();
        resolvePathChain();

        // Validate flags now that things have been resolved.
        if ((this.flags & FLAG_DEAD_START_JUMP) == FLAG_DEAD_START_JUMP && !canHaveDeadStartJumpFlag())
            getLogger().warning("Found FLAG_DEAD_START_JUMP, despite not seeming to be capable of having it.");
        if ((this.flags & FLAG_DEAD_END_JUMP) == FLAG_DEAD_END_JUMP && !canHaveDeadEndJumpFlag())
            getLogger().warning("Found FLAG_DEAD_END_JUMP, despite not seeming to be capable of having it.");
        if (((this.flags & FLAG_INTERMISSION) == FLAG_INTERMISSION))
            getLogger().warning("Found FLAG_INTERMISSION, which previously has not been observed to be set for any spline.");
        if (calculateNoCameraSplineFlag() ^ ((this.flags & FLAG_NO_CAMERA) == FLAG_NO_CAMERA))
            getLogger().warning("Flag FLAG_NO_CAMERA was %b, but the spline %s actually have a camera spline.", ((this.flags & FLAG_NO_CAMERA) == FLAG_NO_CAMERA), (calculateNoCameraSplineFlag() ? "DID NOT" : "DID"));
        if (calculateCameraStartConnectFlag() ^ ((this.flags & FLAG_CAMERA_START_CONNECT) == FLAG_CAMERA_START_CONNECT))
            getLogger().warning("Flag FLAG_CAMERA_START_CONNECT was %b, which did not match FrogLord's calculation.", ((this.flags & FLAG_CAMERA_START_CONNECT) == FLAG_CAMERA_START_CONNECT));
        // TODO: Finish
        //  - FLAG_CAMERA_START_CONNECT The camera spline has connections at the start. (Appears unused)
        //  - FLAG_CAMERA_END_CONNECT
    }

    private void resolveCameraSpline() {
        if (this.tempCameraSplineId == -1) {
            this.cameraSpline = null;
            return;
        }

        int splineIndex = (this.tempCameraSplineId & 0xFF);
        List<MediEvilMap3DSpline> splines = this.splinePacket.getParentFile().getSpline3DPacket().getSplines();
        if (splineIndex >= splines.size())
            throw new IllegalArgumentException("Invalid splineIndex: " + splineIndex);

        this.cameraSpline = splines.get(splineIndex);
        this.tempCameraSplineId = -1;
    }

    private void resolvePathChain() {
        if (this.tempParentChainId == -1)
            throw new RuntimeException("Cannot resolve parentChainId, the ID " + this.tempParentChainId + " is invalid.");

        int pathChainIndex = (this.tempParentChainId & 0xFF);
        List<MediEvilMapPathChain> pathChains = this.splinePacket.getParentFile().getPathChainPacket().getPathChainNodes();
        if (pathChainIndex >= pathChains.size())
            throw new IllegalArgumentException("Invalid pathChainIndex: " + pathChainIndex);

        this.pathChain = pathChains.get(pathChainIndex);
        this.tempParentChainId = -1;
    }
}