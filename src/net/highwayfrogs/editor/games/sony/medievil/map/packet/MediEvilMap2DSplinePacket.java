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
 * Implements the MediEvil map 2D spline packet.
 * Created by Kneesnap on 2/3/2026.
 */
public class MediEvilMap2DSplinePacket extends MediEvilMapPacket {
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
            MediEvilMap2DSpline newSpline = new MediEvilMap2DSpline(getGameInstance());
            this.splines.add(newSpline);
            newSpline.load(reader);
        }

        // Read subdivisions.
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
        for (int i = 0; i < this.splines.size(); i++)
            this.splines.get(i).saveSubDivisions(writer);
    }

    @Override
    public void clear() {
        this.splines.clear();
    }

    public static class MediEvilMap2DSpline extends SCGameData<MediEvilGameInstance> {
        private final List<SVector> subDivisions = new ArrayList<>();
        private byte cameraSplineId; // TODO: Resolve to object reference.
        private byte parentChainId; // TODO: Resolve to object reference.
        private byte numDeadSubDivsStart;
        private byte numDeadSubDivsEnd;
        private byte flags;
        private short uniqueId;

        private int tempSplineSubDivPointer = -1;

        public MediEvilMap2DSpline(MediEvilGameInstance instance) {
            super(instance);
        }

        @Override
        public void load(DataReader reader) {
            short subDivCount = reader.readUnsignedByteAsShort();
            this.cameraSplineId = reader.readByte();
            this.parentChainId = reader.readByte();
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
            writer.writeByte(this.cameraSplineId);
            writer.writeByte(this.parentChainId);
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
                this.subDivisions.get(i).loadWithPadding(reader);
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
    }
}
