package net.highwayfrogs.editor.games.sony.medievil.map.packet;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.games.sony.medievil.map.packet.MediEvilMap3DSplinePacket.MediEvilMap3DSpline;
import net.highwayfrogs.editor.games.sony.medievil.map.packet.MediEvilMapPathChainPacket.MediEvilMapPathChain;
import net.highwayfrogs.editor.gui.components.propertylist.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.AppendInfoLoggerWrapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements the MediEvil map 2D spline packet.
 * Created by Kneesnap on 2/3/2026.
 */
@Getter
public class MediEvilMap2DSplinePacket extends MediEvilMapPacket implements IPropertyListCreator {
    private final List<MediEvilMap2DSpline> splines = new ArrayList<>();

    public static final String IDENTIFIER = "2LPS"; // 'SPL2'

    public MediEvilMap2DSplinePacket(MediEvilMapFile parentFile) {
        super(parentFile, IDENTIFIER);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        short splineCount = reader.readUnsignedByteAsShort();
        reader.align(Constants.INTEGER_SIZE); // There seems to be garbage data here.
        int splineDataStartIndex = reader.readInt();

        reader.requireIndex(getLogger(), splineDataStartIndex, "Expected splines");
        this.splines.clear();
        for (int i = 0; i < splineCount; i++) {
            MediEvilMap2DSpline newSpline = new MediEvilMap2DSpline(this);
            this.splines.add(newSpline);
            newSpline.load(reader);
        }

        // Read subdivisions.
        for (int i = 0; i < this.splines.size(); i++)
            this.splines.get(i).loadSubDivisions(reader);
    }

    @Override
    protected void loadBodySecondPass(DataReader reader, int endIndex) {
        // Resolve object references.
        for (int i = 0; i < this.splines.size(); i++)
            this.splines.get(i).resolveReferences();
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        writer.writeUnsignedByte((short) this.splines.size());
        writer.align(Constants.INTEGER_SIZE); // There seems to be garbage data here.
        int splineDataStartIndex = writer.writeNullPointer();

        // Write splines.
        writer.writeAddressTo(splineDataStartIndex);
        for (int i = 0; i < this.splines.size(); i++)
            this.splines.get(i).save(writer);

        // Write subdivisions.
        for (int i = 0; i < this.splines.size(); i++)
            this.splines.get(i).saveSubDivisions(writer);
    }

    /**
     * Validates the references found within the splines are valid.
     */
    void validateReferencesAreValid() {
        for (int i = 0; i < this.splines.size(); i++) {
            MediEvilMap2DSpline spline = this.splines.get(i);
            if (spline.cameraSpline != null && spline.cameraSpline.getPathSpline() != spline)
                spline.getLogger().warning("The 2D spline (%s) had an attached camera spline (%s) which was not linked to the spline!", spline, spline.cameraSpline);
            if (spline.pathChain != null && !spline.pathChain.getPathSplines().contains(spline) && !spline.pathChain.getPathSplines().contains(spline))
                spline.getLogger().warning("The 2D spline (%s) was attached to a path chain node (%s) which did not seem to know about the spline!", spline, spline.pathChain);
        }
    }

    @Override
    public void clear() {
        this.splines.clear();
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        propertyList.addString(this::addSplines, "2D Splines", String.valueOf(this.splines.size()));
    }

    private void addSplines(PropertyListNode propertyList) {
        for (int i = 0; i < this.splines.size(); i++)
            propertyList.addProperties("Spline " + i, this.splines.get(i));
    }

    public static class MediEvilMap2DSpline extends SCGameData<MediEvilGameInstance> implements IPropertyListCreator {
        @Getter private final MediEvilMap2DSplinePacket splinePacket;
        @Getter private final List<SVector> subDivisions = new ArrayList<>(); // Padding seems to be garbage.
        @Getter private MediEvilMap3DSpline cameraSpline; // Used by certain camera modes.
        @Getter private MediEvilMapPathChain pathChain;
        @Getter private byte numDeadSubDivsStart; // TODO: Research/understand.
        @Getter private byte numDeadSubDivsEnd; // TODO: Research/understand.
        @Getter private byte flags; // TODO: Allow editing?
        @Getter private short uniqueId; // TODO: Automatically manage. Is this an index? Probably not.

        // NOTE:
        // My current hypothesis for why the path spline is linked to the camera spline is about progression.
        // The game probably will find the closest subdivision in the path spline to the player, then use the equivalent subdivision in the camera spline.
        // This allows syncing up the paths.

        private int tempSplineSubDivPointer = -1;
        private byte tempCameraSplineId = -1;
        private byte tempParentChainId = -1;

        public static final int FLAG_DEAD_START_JUMP = Constants.BIT_FLAG_0; // Dead subdivisions at a start should jump the camera.
        public static final int FLAG_DEAD_END_JUMP = Constants.BIT_FLAG_1; // Dead subdivisions at an end should jump the camera.
        public static final int FLAG_STATIC_CAMERA = Constants.BIT_FLAG_2; // The camera should be static. (Appears unused)
        public static final int FLAG_CAMERA_START_CONNECT = Constants.BIT_FLAG_3; // The camera spline has connections at the start. (Appears unused)
        public static final int FLAG_CAMERA_END_CONNECT = Constants.BIT_FLAG_4; // The camera spline has connections at the end. (Appears unused)
        public static final int FLAG_NO_CAMERA = Constants.BIT_FLAG_5; // The spline has no camera spline.
        public static final int FLAG_NO_ENTITY = Constants.BIT_FLAG_6; // Entities will not navigate this spline.
        public static final int FLAG_INTERMISSION = Constants.BIT_FLAG_7; // Entities will not navigate this spline. (Flag appears unused)

        public MediEvilMap2DSpline(MediEvilMap2DSplinePacket splinePacket) {
            super(splinePacket.getGameInstance());
            this.splinePacket = splinePacket;
        }

        /**
         * Gets the ID used to identify this spline when saving/loading.
         * This ID has the ability to change while FrogLord applies changes to files, so it should not be used to identify a spline.
         */
        public int getId() {
            int index = this.splinePacket.splines.indexOf(this);
            if (index < 0)
                throw new IllegalArgumentException("The referenced " + getClass().getSimpleName() + " is not registered as part of the MediEvilMapFile.");

            return index;
        }

        @Override
        public void addToPropertyList(PropertyListNode propertyList) {
            propertyList.addInteger("Unique ID", this.uniqueId);
            propertyList.addString("Flags", String.format("%02X", this.flags));
            propertyList.addInteger("Number of dead sub-division starts", DataUtils.byteToUnsignedShort(this.numDeadSubDivsStart));
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
            return new AppendInfoLoggerWrapper(this.splinePacket.getLogger(), getClass().getSimpleName() + "[" + this.splinePacket.splines.indexOf(this) + "]", AppendInfoLoggerWrapper.TEMPLATE_OVERRIDE_AT_ORIGINAL);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" + this.splinePacket.splines.indexOf(this) + "@" + this.splinePacket.getParentFile().getFileDisplayName() + "}";
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
            writer.writeUnsignedByte((short) this.subDivisions.size());
            writer.writeUnsignedByte((short) (this.cameraSpline != null ? this.cameraSpline.getId() : 0xFF));
            writer.writeUnsignedByte((short) this.pathChain.getId());
            writer.writeByte(this.numDeadSubDivsStart);
            writer.writeByte(this.numDeadSubDivsEnd);
            writer.writeByte(this.flags);
            writer.writeShort(this.uniqueId);
            this.tempSplineSubDivPointer = writer.writeNullPointer();
        }

        private void loadSubDivisions(DataReader reader) {
            if (this.tempSplineSubDivPointer <= 0)
                throw new RuntimeException("Cannot load tempSplineSubDivPointer, the pointer " + NumberUtils.toHexString(this.tempSplineSubDivPointer) + " is invalid.");

            requireReaderIndex(reader, this.tempSplineSubDivPointer, "Expected subDivisions");
            this.tempSplineSubDivPointer = -1;

            // Read subdivisions.
            for (int i = 0; i < this.subDivisions.size(); i++)
                this.subDivisions.get(i).loadWithPadding(reader); // Padding seems to be garbage.
        }

        private void saveSubDivisions(DataWriter writer) {
            if (this.tempSplineSubDivPointer <= 0)
                throw new RuntimeException("Cannot save tempSplineSubDivPointer, the pointer " + NumberUtils.toHexString(this.tempSplineSubDivPointer) + " is invalid.");

            writer.writeAddressTo(this.tempSplineSubDivPointer);
            this.tempSplineSubDivPointer = -1;

            // Write subdivisions.
            for (int i = 0; i < this.subDivisions.size(); i++)
                this.subDivisions.get(i).saveWithPadding(writer);
        }

        private void resolveReferences() {
            resolveCameraSpline();
            resolvePathChain();
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
}
