package net.highwayfrogs.editor.games.sony.oldfrogger.map.packet;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;

import java.util.Arrays;

/**
 * Contains the "STANDARD" chunk data.
 * Created by Kneesnap on 12/8/2023.
 */
@Getter
public class OldFroggerMapStandardPacket extends OldFroggerMapPacket {
    public static final String IDENTIFIER = "STND";

    private final SVector cameraOffset = new SVector();
    private short halfHeight;
    private short frontClipPlane;
    private short backClipPlane;
    private short horizClipPlaneOffset;
    private short cameraLeftExtreme;
    private short cameraBottomExtreme;
    private short cameraRightExtreme;
    private short cameraTopExtreme;
    private final long[] racePathID = new long[5];

    public OldFroggerMapStandardPacket(OldFroggerMapFile parentFile) {
        super(parentFile, IDENTIFIER, PacketSizeType.NO_SIZE);
        Arrays.fill(this.racePathID, -1);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        this.cameraOffset.loadWithPadding(reader);
        this.halfHeight = reader.readShort();
        this.frontClipPlane = reader.readShort();
        this.backClipPlane = reader.readShort();
        this.horizClipPlaneOffset = reader.readShort();
        this.cameraLeftExtreme = reader.readShort();
        this.cameraBottomExtreme = reader.readShort();
        this.cameraRightExtreme = reader.readShort();
        this.cameraTopExtreme = reader.readShort();
        for (int i = 0; i < this.racePathID.length; i++)
            this.racePathID[i] = reader.readUnsignedIntAsLong();
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        this.cameraOffset.saveWithPadding(writer);
        writer.writeShort(this.halfHeight);
        writer.writeShort(this.frontClipPlane);
        writer.writeShort(this.backClipPlane);
        writer.writeShort(this.horizClipPlaneOffset);
        writer.writeShort(this.cameraLeftExtreme);
        writer.writeShort(this.cameraBottomExtreme);
        writer.writeShort(this.cameraRightExtreme);
        writer.writeShort(this.cameraTopExtreme);
        for (int i = 0; i < this.racePathID.length; i++)
            writer.writeUnsignedInt(this.racePathID[i]);
    }
}