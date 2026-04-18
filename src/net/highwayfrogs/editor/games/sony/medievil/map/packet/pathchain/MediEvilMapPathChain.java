package net.highwayfrogs.editor.games.sony.medievil.map.packet.pathchain;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.map.packet.spline.MediEvilMap2DSpline;
import net.highwayfrogs.editor.gui.components.propertylist.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.AppendInfoLoggerWrapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an actual path chain node.
 * A path chain node contains a start/end-point.
 * Any number of splines can be used to connect the start/end-point, which are considered sequential/connected to each other.
 * Then, new chain nodes can connect to either the start/end-point (up to eight per point).
 * Created by Kneesnap on 2/3/2026.
 */
public class MediEvilMapPathChain extends SCGameData<MediEvilGameInstance> implements IPropertyListCreator {
    @Getter private final MediEvilMapPathChainPacket pathChainPacket;
    @Getter @NonNull private MediEvilMapPathGroupType groupType = MediEvilMapPathGroupType.ENTITY;
    @Getter private final MediEvilMapPathChainConnections previousChains;
    @Getter private final MediEvilMapPathChainConnections nextChains;
    @Getter private final List<MediEvilMap2DSpline> pathSplines = new ArrayList<>(); // Each spline in this list is NOT guaranteed to start where the previous one ends. I'm not sure I entirely understand how/why.

    private int tempPathSplineIdOffset = -1;
    private int tempCameraSplineIdOffset = -1;
    private byte[] tempPathSplineIds;
    private byte[] tempCameraSplineIds;

    public MediEvilMapPathChain(MediEvilMapPathChainPacket pathChainPacket) {
        super(pathChainPacket.getGameInstance());
        this.pathChainPacket = pathChainPacket;
        this.previousChains = new MediEvilMapPathChainConnections(this, "previous");
        this.nextChains = new MediEvilMapPathChainConnections(this, "next");
    }

    /**
     * Returns true iff the groupType field is supported in this version of MediEvil.
     */
    private boolean isGroupTypeSupported() {
        return !getGameInstance().getVersionConfig().isAtOrBeforeEctsAlpha();
    }

    /**
     * Gets the ID used to identify this spline when saving/loading.
     * This ID has the ability to change while FrogLord applies changes to files, so it should not be used to identify a spline.
     */
    public int getId() {
        int index = this.pathChainPacket.pathChainNodes.indexOf(this);
        if (index < 0)
            throw new IllegalArgumentException("The referenced " + getClass().getSimpleName() + " is not registered as part of the MediEvilMapFile.");

        return index;
    }

    @Override
    public ILogger getLogger() {
        return new AppendInfoLoggerWrapper(this.pathChainPacket.getLogger(), getClass().getSimpleName() + "[" + this.pathChainPacket.pathChainNodes.indexOf(this) + "]", AppendInfoLoggerWrapper.TEMPLATE_OVERRIDE_AT_ORIGINAL);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + this.pathChainPacket.pathChainNodes.indexOf(this) + "@" + this.pathChainPacket.getParentFile().getFileDisplayName() + "}";
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        if (isGroupTypeSupported())
            propertyList.addEnum("Path Group Type", this.groupType, MediEvilMapPathGroupType.VALID_TYPES, newType -> this.groupType = newType);
        this.previousChains.addNestedPropertyList(propertyList);
        this.nextChains.addNestedPropertyList(propertyList);
        propertyList.addString(this::addPathSplines, "Path Splines", String.valueOf(this.pathSplines.size()));
    }

    private void addPathSplines(PropertyListNode propertyList) {
        addSplines(propertyList, "pathSplines", this.pathSplines);
    }

    private void addSplines(PropertyListNode propertyList, String name, List<MediEvilMap2DSpline> splines) {
        for (int i = 0; i < splines.size(); i++) {
            MediEvilMap2DSpline spline = splines.get(i);
            propertyList.addString(spline, name + "[" + i + "]", "ID: " + spline.getId());
        }
    }

    @Override
    public void load(DataReader reader) {
        short previousNumber = reader.readUnsignedByteAsShort();
        short nextNumber = reader.readUnsignedByteAsShort();
        short splineCount = reader.readUnsignedByteAsShort();
        if (isGroupTypeSupported()) {
            this.groupType = MediEvilMapPathGroupType.values()[reader.readUnsignedByteAsShort()];
        } else {
            reader.skipByte(); // Garbage data.
        }

        this.previousChains.tempChainIds = new byte[previousNumber];
        this.nextChains.tempChainIds = new byte[nextNumber];
        this.tempCameraSplineIds = new byte[splineCount];
        this.tempPathSplineIds = new byte[splineCount];
        this.previousChains.tempChainIdOffset = reader.readInt();
        this.nextChains.tempChainIdOffset = reader.readInt();
        this.tempPathSplineIdOffset = reader.readInt();
        this.tempCameraSplineIdOffset = reader.readInt();
        if (!Utils.contains(MediEvilMapPathGroupType.VALID_TYPES, this.groupType))
            getLogger().warning("Found unexpected group type: %s", this.groupType);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedByte((short) this.previousChains.size());
        writer.writeUnsignedByte((short) this.nextChains.size());
        writer.writeUnsignedByte((short) this.pathSplines.size());
        writer.writeUnsignedByte((short) this.groupType.ordinal());

        this.previousChains.tempChainIdOffset = writer.writeNullPointer();
        this.nextChains.tempChainIdOffset = writer.writeNullPointer();
        this.tempPathSplineIdOffset = writer.writeNullPointer();
        this.tempCameraSplineIdOffset = writer.writeNullPointer();
    }

    void loadPathSplineIdOffset(DataReader reader) {
        if (this.tempPathSplineIdOffset <= 0) // This is not observed to be 0/empty.
            throw new RuntimeException("Cannot load pathSplineIdOffset, the pointer " + NumberUtils.toHexString(this.tempPathSplineIdOffset) + " is invalid.");

        requireReaderIndex(reader, this.tempPathSplineIdOffset, "Expected pathSplineIdOffset");
        this.tempPathSplineIdOffset = reader.readInt();
    }

    void savePathSplineIdOffset(DataWriter writer) {
        if (this.tempPathSplineIdOffset <= 0) // This is not observed to be 0/empty.
            throw new RuntimeException("Cannot save pathSplineIdOffset, the pointer " + NumberUtils.toHexString(this.tempPathSplineIdOffset) + " is invalid.");

        writer.writeAddressTo(this.tempPathSplineIdOffset);
        this.tempPathSplineIdOffset = writer.writeNullPointer();
    }

    void loadPathSplineIds(DataReader reader) {
        if (this.tempPathSplineIdOffset <= 0) // This is not observed to be 0/empty.
            throw new RuntimeException("Cannot load pathSplineIds, the pointer " + NumberUtils.toHexString(this.tempPathSplineIdOffset) + " is invalid.");

        requireReaderIndex(reader, this.tempPathSplineIdOffset, "Expected pathSplineIds");
        this.tempPathSplineIdOffset = -1;
        reader.readBytes(this.tempPathSplineIds);
    }

    void savePathSplineIds(DataWriter writer) {
        if (this.tempPathSplineIdOffset <= 0) // This is not observed to be 0/empty.
            throw new RuntimeException("Cannot save pathSplineIds, the pointer " + NumberUtils.toHexString(this.tempPathSplineIdOffset) + " is invalid.");

        writer.writeAddressTo(this.tempPathSplineIdOffset);
        this.tempPathSplineIdOffset = -1;
        for (int i = 0; i < this.pathSplines.size(); i++)
            writer.writeUnsignedByte((short) this.pathSplines.get(i).getId());
    }

    void resolvePathSplines() {
        if (this.tempPathSplineIds == null || this.tempPathSplineIdOffset != -1)
            throw new RuntimeException("Cannot resolve path splines, the data was not setup properly.");

        this.pathSplines.clear();
        List<MediEvilMap2DSpline> pathSplines = this.pathChainPacket.getParentFile().getSpline2DPacket().getSplines();
        for (int i = 0; i < this.tempPathSplineIds.length; i++) {
            int pathSplineIndex = (this.tempPathSplineIds[i] & 0xFF);
            if (pathSplineIndex >= pathSplines.size())
                throw new IllegalArgumentException("Invalid pathSplineIndex: " + pathSplineIndex);

            MediEvilMap2DSpline pathSpline = pathSplines.get(pathSplineIndex);
            this.pathSplines.add(pathSpline);
        }

        this.tempPathSplineIds = null;
    }

    void loadCameraSplineIdOffset(DataReader reader) {
        if (this.tempCameraSplineIdOffset <= 0) // This is not observed to be 0/empty.
            throw new RuntimeException("Cannot load cameraSplineIdOffset, the pointer " + NumberUtils.toHexString(this.tempCameraSplineIdOffset) + " is invalid.");

        requireReaderIndex(reader, this.tempCameraSplineIdOffset, "Expected cameraSplineIdOffset");
        this.tempCameraSplineIdOffset = reader.readInt();
    }

    void saveCameraSplineIdOffset(DataWriter writer) {
        if (this.tempCameraSplineIdOffset <= 0) // This is not observed to be 0/empty.
            throw new RuntimeException("Cannot save cameraSplineIdOffset, the pointer " + NumberUtils.toHexString(this.tempCameraSplineIdOffset) + " is invalid.");

        writer.writeAddressTo(this.tempCameraSplineIdOffset);
        this.tempCameraSplineIdOffset = writer.writeNullPointer();
    }

    void loadCameraSplineIds(DataReader reader) {
        if (this.tempCameraSplineIdOffset <= 0) // This is not observed to be 0/empty.
            throw new RuntimeException("Cannot load cameraSplineIds, the pointer " + NumberUtils.toHexString(this.tempCameraSplineIdOffset) + " is invalid.");

        requireReaderIndex(reader, this.tempCameraSplineIdOffset, "Expected cameraSplineIds");
        this.tempCameraSplineIdOffset = -1;
        reader.readBytes(this.tempCameraSplineIds);
    }

    void saveCameraSplineIds(DataWriter writer) {
        if (this.tempCameraSplineIdOffset <= 0) // This is not observed to be 0/empty.
            throw new RuntimeException("Cannot save cameraSplineIds, the pointer " + NumberUtils.toHexString(this.tempCameraSplineIdOffset) + " is invalid.");

        writer.writeAddressTo(this.tempCameraSplineIdOffset);
        this.tempCameraSplineIdOffset = -1;
        for (int i = 0; i < this.pathSplines.size(); i++)
            writer.writeUnsignedByte((short) this.pathSplines.get(i).getId());
    }

    void resolveCameraSplines() {
        if (this.tempCameraSplineIds == null || this.tempCameraSplineIdOffset != -1)
            throw new RuntimeException("Cannot resolve camera splines, the data was not setup properly.");

        List<MediEvilMap2DSpline> cameraSplines = this.pathChainPacket.getParentFile().getSpline2DPacket().getSplines();
        for (int i = 0; i < this.tempCameraSplineIds.length; i++) {
            int cameraSplineIndex = (this.tempCameraSplineIds[i] & 0xFF);
            if (cameraSplineIndex >= cameraSplines.size())
                throw new IllegalArgumentException("Invalid cameraSplineIndex: " + cameraSplineIndex);

            MediEvilMap2DSpline cameraSpline = cameraSplines.get(cameraSplineIndex);
            if (cameraSpline != this.pathSplines.get(i))
                throw new IllegalStateException(String.format("pathSplines[%d] (%s) did not match cameraSplines[%d]! (%s)", i, this.pathSplines.get(i), i, cameraSpline));
        }

        this.tempCameraSplineIds = null;
    }
}
