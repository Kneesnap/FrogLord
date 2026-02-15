package net.highwayfrogs.editor.games.sony.medievil.map.packet;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.games.sony.medievil.map.packet.MediEvilMap2DSplinePacket.MediEvilMap2DSpline;
import net.highwayfrogs.editor.games.sony.medievil.map.packet.MediEvilMapPathChainPacket.MediEvilMapPathChain;
import net.highwayfrogs.editor.gui.components.propertylist.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.AppendInfoLoggerWrapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements the MediEvil map 3D spline packet.
 * Created by Kneesnap on 2/3/2026.
 */
@Getter
public class MediEvilMap3DSplinePacket extends MediEvilMapPacket implements IPropertyListCreator {
    private final List<MediEvilMap3DSpline> splines = new ArrayList<>();
    private int emptySubDivisions; // TODO: What's up with this. Can we delete it? Is it important?

    public static final String IDENTIFIER = "3LPS"; // 'SPL3'

    public MediEvilMap3DSplinePacket(MediEvilMapFile parentFile) {
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
            MediEvilMap3DSpline newSpline = new MediEvilMap3DSpline(this);
            this.splines.add(newSpline);
            newSpline.load(reader);
        }

        // Read subdivisions.
        this.emptySubDivisions = readEmptyVectors(reader);
        for (int i = 0; i < this.splines.size(); i++)
            this.splines.get(i).loadSubDivisions(reader);
    }

    @Override
    protected void loadBodySecondPass(DataReader reader, int endIndex) {
        // Resolve object references.
        for (int i = 0; i < this.splines.size(); i++)
            this.splines.get(i).resolveReferences();

        // Do this here since it's the last packet in the chain to resolve.
        getParentFile().getPathChainPacket().validateReferencesAreValid();
        getParentFile().getSpline2DPacket().validateReferencesAreValid();
        validateReferencesAreValid();
    }

    /**
     * Validates the references found within the splines are valid.
     */
    private void validateReferencesAreValid() {
        for (int i = 0; i < this.splines.size(); i++) {
            MediEvilMap3DSpline spline = this.splines.get(i);
            if (spline.pathSpline != null && spline.pathSpline.getCameraSpline() != spline)
                spline.getLogger().warning("The 3D spline (%s) had an attached 2D path spline (%s) which was not linked to the 3D spline!", spline, spline.pathSpline);
            if (spline.pathSpline != null && spline.parentChain != null && spline.parentChain != spline.pathSpline.getPathChain())
                spline.getLogger().warning("The 3D spline (%s) was attached to a different pathChainNode (%s) than its 2D path spline!", spline, spline.parentChain);
        }
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
        writer.writeNull(SVector.PADDED_BYTE_SIZE * this.emptySubDivisions);
        for (int i = 0; i < this.splines.size(); i++)
            this.splines.get(i).saveSubDivisions(writer);
    }

    @Override
    public void clear() {
        this.splines.clear();
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        propertyList.addString(this::addSplines, "3D Splines", String.valueOf(this.splines.size()));
    }

    private void addSplines(PropertyListNode propertyList) {
        for (int i = 0; i < this.splines.size(); i++)
            propertyList.addProperties("Spline " + i, this.splines.get(i));
    }

    public static class MediEvilMap3DSpline extends SCGameData<MediEvilGameInstance> implements IPropertyListCreator {
        @Getter private final MediEvilMap3DSplinePacket splinePacket;
        @Getter private final List<SVector> subDivisions = new ArrayList<>(); // Padding seems to be garbage.
        @Getter private MediEvilMapPathChain parentChain; // Appears to be unused by the game.
        @Getter private MediEvilMap2DSpline pathSpline; // Appears to be unused by the game.
        @Getter private short uniqueId; // TODO: Research/understand. (Is this separate from the unique IDs seen in 2D splines?)
        @Getter private int trailingEmptySubDivisions; // TODO: What's up with these. Can we delete them? Are they important?

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
            int index = this.splinePacket.splines.indexOf(this);
            if (index < 0)
                throw new IllegalArgumentException("The referenced " + getClass().getSimpleName() + " is not registered as part of the MediEvilMapFile.");

            return index;
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
            writer.writeUnsignedByte((short) this.parentChain.getId());
            writer.writeUnsignedByte((short) this.pathSpline.getId());
            writer.writeNull(3); // Alignment/garbage.
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

            // Skip trailing empty subdivisions.
            this.trailingEmptySubDivisions = readEmptyVectors(reader);
        }

        private void saveSubDivisions(DataWriter writer) {
            if (this.tempSplineSubDivPointer <= 0)
                throw new RuntimeException("Cannot save tempSplineSubDivPointer, the pointer " + NumberUtils.toHexString(this.tempSplineSubDivPointer) + " is invalid.");

            writer.writeAddressTo(this.tempSplineSubDivPointer);
            this.tempSplineSubDivPointer = -1;

            // Write subdivisions.
            for (int i = 0; i < this.subDivisions.size(); i++)
                this.subDivisions.get(i).saveWithPadding(writer);

            // Write empty trailing vectors. (It is not clear if this is necessary)
            writer.writeNull(SVector.PADDED_BYTE_SIZE * this.trailingEmptySubDivisions);
        }

        private void resolveReferences() {
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

            this.parentChain = pathChains.get(pathChainIndex);
            this.tempParentChainId = -1;
        }

        @Override
        public void addToPropertyList(PropertyListNode propertyList) {
            propertyList.addInteger("Unique ID", this.uniqueId);
            propertyList.addString(this.parentChain, "Parent Chain", this.parentChain != null ? "ID: " + this.parentChain.getId() : "None");
            propertyList.addString(this.pathSpline, "Path Spline", this.pathSpline != null ? "ID: " + this.pathSpline.getId() : "None");
            propertyList.addString(this::addSubDivisions, "Positions (Subdivisions)", String.valueOf(this.subDivisions.size()));
            propertyList.addInteger("Trailing Empty Subdivisions", this.trailingEmptySubDivisions);
        }

        private void addSubDivisions(PropertyListNode propertyList) {
            for (int i = 0; i < this.subDivisions.size(); i++)
                propertyList.add("subDivisions[" + i + "]", this.subDivisions.get(i));
        }
    }

    private static int readEmptyVectors(DataReader reader) {
        // Skip trailing empty subdivisions.
        int emptyVectorCount = 0;
        while (reader.getRemaining() >= SVector.PADDED_BYTE_SIZE) {
            int startIndex = reader.getIndex();
            reader.jumpTemp(startIndex);
            int nextValue = reader.readInt();
            short nextValue2 = reader.readShort();
            // Don't test padding.
            reader.jumpReturn();

            if (nextValue != 0 || nextValue2 != 0)
                break; // Found a non-empty vector, abort.

            emptyVectorCount++;
            reader.skipBytes(SVector.PADDED_BYTE_SIZE);
        }

        return emptyVectorCount;
    }
}
