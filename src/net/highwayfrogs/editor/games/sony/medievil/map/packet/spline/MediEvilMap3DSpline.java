package net.highwayfrogs.editor.games.sony.medievil.map.packet.spline;

import lombok.Getter;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.map.packet.pathchain.MediEvilMapPathChain;
import net.highwayfrogs.editor.games.sony.medievil.map.packet.pathchain.ui.MediEvilPathManager;
import net.highwayfrogs.editor.games.sony.shared.spline.MRSplineMatrix;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.components.propertylist.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.AppendInfoLoggerWrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a "3D" camera spline in MediEvil.
 * Created by Kneesnap on 2/26/2026.
 */
public class MediEvilMap3DSpline extends SCGameData<MediEvilGameInstance> implements IPropertyListCreator, IMediEvilMapSpline {
    @Getter private final MediEvilMap3DSplinePacket splinePacket;
    private final List<SVector> subDivisions = new ArrayList<>(); // Padding seems to be garbage.
    private final List<SVector> immutableSubDivisions = Collections.unmodifiableList(this.subDivisions);
    @Getter private MediEvilMapPathChain pathChain; // Appears to be unused by the game.
    @Getter private MediEvilMap2DSpline pathSpline; // Appears to be unused by the game.
    @Getter private short uniqueId; // TODO: Research/understand. (Is this separate from the unique IDs seen in 2D splines?)

    private int tempSplineSubDivPointer = -1;
    private byte tempParentChainId = -1;
    private byte tempPathSplineId = -1;

    public MediEvilMap3DSpline(MediEvilMap3DSplinePacket splinePacket) {
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
    public void setupEditor(MediEvilPathManager manager, GUIEditorGrid editorGrid) {
        // Not much to edit here really.
    }

    @Override
    public void applySplineMatrix(MRSplineMatrix splineMatrix) {
        // 1) Ensure the subDivisionCount is based on the 2D spline.
        // The path spline shouldn't really ever be null, and if it does happen, it's probably from FrogLord edits which have unlinked it from the spline (Such as the spline being deleted).
        // It should be safe to preserve the data as-is.
        if (this.pathSpline != null) {
            int subDivCount = this.pathSpline.getSubDivisions().size();
            while (this.subDivisions.size() > subDivCount)
                this.subDivisions.remove(this.subDivisions.size() - 1);
            while (subDivCount > this.subDivisions.size())
                this.subDivisions.add(new SVector());
        }

        // 2) Apply the spline curve to the subdivisions.
        applySplineMatrixToSubDivisions(splineMatrix);
    }

    @Override
    public ILogger getLogger() {
        return new AppendInfoLoggerWrapper(this.splinePacket.getLogger(), getClass().getSimpleName() + "[" + this.splinePacket.getSplines().indexOf(this) + "]", AppendInfoLoggerWrapper.TEMPLATE_OVERRIDE_AT_ORIGINAL);
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
        this.tempParentChainId = reader.readByte();
        this.tempPathSplineId = reader.readByte();
        reader.skipBytes(3); // Garbage.
        this.uniqueId = reader.readShort();
        this.tempSplineSubDivPointer = reader.readInt();

        // Setup list.
        this.subDivisions.clear();
        for (int i = 0; i < subDivCount; i++)
            this.subDivisions.add(new SVector());
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedByte((short) this.subDivisions.size());
        writer.writeUnsignedByte((short) this.pathChain.getId());
        writer.writeUnsignedByte((short) this.pathSpline.getId());
        writer.writeNull(3); // Alignment/garbage.
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

        // Skip trailing empty subdivisions.
        int expectedTrailingEmptySubDivisions = calculateEmptyTrailingSubDivisions();
        int actualTrailingEmptySubDivisions = MediEvilMap3DSplinePacket.readEmptyVectors(reader);
        if (actualTrailingEmptySubDivisions != expectedTrailingEmptySubDivisions && this.splinePacket.shouldWarnAboutEmptySubDivisions())
            getLogger().warning("The number of empty trailing subDivisions was %d, when %d were expected.", actualTrailingEmptySubDivisions, expectedTrailingEmptySubDivisions);
    }

     void saveSubDivisions(DataWriter writer) {
        if (this.tempSplineSubDivPointer <= 0)
            throw new RuntimeException("Cannot save tempSplineSubDivPointer, the pointer " + NumberUtils.toHexString(this.tempSplineSubDivPointer) + " is invalid.");

        writer.writeAddressTo(this.tempSplineSubDivPointer);
        this.tempSplineSubDivPointer = -1;

        // Write subdivisions.
        for (int i = 0; i < this.subDivisions.size(); i++)
            this.subDivisions.get(i).saveWithPadding(writer);

        // Write empty trailing vectors. (It is not clear if this is necessary)
        writer.writeNull(SVector.PADDED_BYTE_SIZE * calculateEmptyTrailingSubDivisions());
    }

    void resolveReferences() {
        resolveCameraSpline();
        resolvePathChain();
    }

    private void resolveCameraSpline() {
        if (this.tempPathSplineId == -1)
            throw new RuntimeException("Cannot resolve pathSplineId, the ID " + this.tempPathSplineId + " is invalid.");

        int splineIndex = (this.tempPathSplineId & 0xFF);
        List<MediEvilMap2DSpline> splines = this.splinePacket.getParentFile().getSpline2DPacket().getSplines();
        if (splineIndex >= splines.size())
            throw new IllegalArgumentException("Invalid splineIndex: " + splineIndex);

        this.pathSpline = splines.get(splineIndex);
        this.tempPathSplineId = -1;
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

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        propertyList.addInteger("Unique ID", this.uniqueId);
        propertyList.addString(this.pathChain, "Parent Chain", this.pathChain != null ? "ID: " + this.pathChain.getId() : "None");
        propertyList.addString(this.pathSpline, "Path Spline", this.pathSpline != null ? "ID: " + this.pathSpline.getId() : "None");
        propertyList.addString(this::addSubDivisions, "Positions (Subdivisions)", String.valueOf(this.subDivisions.size()));
    }

    private void addSubDivisions(PropertyListNode propertyList) {
        for (int i = 0; i < this.subDivisions.size(); i++)
            propertyList.add("subDivisions[" + i + "]", this.subDivisions.get(i));
    }


    private int calculateEmptyTrailingSubDivisions() {
        List<MediEvilMap3DSpline> splines = this.splinePacket.getSplines();
        if (splines.size() > 0 && splines.get(splines.size() - 1) == this) {
            return MediEvilMap3DSplinePacket.EXPECTED_START_END_TRAILING_EMPTY_SUB_DIVISIONS;
        } else {
            return 4;
        }
    }
}