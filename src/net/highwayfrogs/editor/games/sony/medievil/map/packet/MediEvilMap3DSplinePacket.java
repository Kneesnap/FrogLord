package net.highwayfrogs.editor.games.sony.medievil.map.packet;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements the MediEvil map 3D spline packet.
 * Created by Kneesnap on 2/3/2026.
 */
public class MediEvilMap3DSplinePacket extends MediEvilMapPacket {
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
            MediEvilMap3DSpline newSpline = new MediEvilMap3DSpline(getGameInstance());
            this.splines.add(newSpline);
            newSpline.load(reader);
        }

        // Read subdivisions.
        this.emptySubDivisions = readEmptyVectors(reader);
        for (int i = 0; i < this.splines.size(); i++)
            this.splines.get(i).loadSubDivisions(reader);
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

    public static class MediEvilMap3DSpline extends SCGameData<MediEvilGameInstance> {
        private final List<SVector> subDivisions = new ArrayList<>(); // Padding seems to be garbage.
        private byte parentChainId; // TODO: Resolve to object reference.
        private byte pathSplineId; // TODO: Resolve to object reference.
        private short uniqueId;
        private int trailingEmptySubDivisions; // TODO: What's up with these. Can we delete them? Are they important?

        private int tempSplineSubDivPointer = -1;

        public MediEvilMap3DSpline(MediEvilGameInstance instance) {
            super(instance);
        }

        @Override
        public void load(DataReader reader) {
            short subDivCount = reader.readUnsignedByteAsShort();
            this.parentChainId = reader.readByte();
            this.pathSplineId = reader.readByte();
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
            writer.writeByte(this.parentChainId);
            writer.writeByte(this.pathSplineId);
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
