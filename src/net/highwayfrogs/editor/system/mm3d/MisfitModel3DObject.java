package net.highwayfrogs.editor.system.mm3d;

import lombok.Getter;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.reader.FileSource;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.file.writer.FileReceiver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a mm3d file.
 * Format Version: 1.6
 * Specification: http://www.misfitcode.com/misfitmodel3d/olh_mm3dformat.html
 * Created by Kneesnap on 2/28/2019. TODO: Allow accessing headers via fields. Upon saving, determine which should and shouldn't be saved.
 */
@Getter
public class MisfitModel3DObject extends GameObject {
    private short modelFlags;
    private List<MMDataBlockHeader> segments = new ArrayList<>();

    private static final String SIGNATURE = "MISFIT3D";
    private static final short MAJOR_VERSION = 0x01;
    private static final short MINOR_VERSION = 0x06;

    @Override
    public void load(DataReader reader) {
        reader.verifyString(SIGNATURE);
        short majorVersion = reader.readUnsignedByteAsShort();
        short minorVersion = reader.readUnsignedByteAsShort();
        this.modelFlags = reader.readUnsignedByteAsShort();
        short dataSegmentCount = reader.readUnsignedByteAsShort();

        Utils.verify(majorVersion == MAJOR_VERSION && minorVersion == MINOR_VERSION, "Unknown Version: [" + majorVersion + ", " + minorVersion + "]");

        for (int i = 0; i < dataSegmentCount; i++) {
            int offsetType = reader.readUnsignedShortAsInt();
            OffsetType type = OffsetType.getOffsetType(offsetType);
            int offsetAddress = reader.readInt();

            if (type == null) {
                System.out.println("Unknown OffsetType: " + Utils.toHexString(offsetType));
                continue;
            }

            if (type == OffsetType.END_OF_FILE)
                break; // Reached end.

            reader.jumpTemp(offsetAddress);
            MMDataBlockHeader header = new MMDataBlockHeader(type);
            header.load(reader);
            this.segments.add(header);
            reader.jumpReturn();
        }
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeStringBytes(SIGNATURE);
        writer.writeUnsignedByte(MAJOR_VERSION);
        writer.writeUnsignedByte(MINOR_VERSION);
        writer.writeUnsignedByte(this.modelFlags);
        writer.writeUnsignedByte((short) (this.segments.size() + 1)); // Add 1 to account for EOF.

        Map<MMDataBlockHeader, Integer> headerAddressMap = new HashMap<>();
        for (MMDataBlockHeader header : getSegments()) {
            writer.writeUnsignedShort(header.getOffsetType().getTypeCode());
            headerAddressMap.put(header, writer.writeNullPointer());
        }

        // Write EOF.
        writer.writeUnsignedShort(OffsetType.END_OF_FILE.getTypeCode());
        int eofHeaderAddress = writer.writeNullPointer();

        for (MMDataBlockHeader header : getSegments()) {
            writer.writeAddressTo(headerAddressMap.remove(header));
            header.save(writer);
        }

        // Write EOF Pointer.
        writer.writeAddressTo(eofHeaderAddress);
    }

    public static void performTest() throws IOException {
        File birdInput = new File("debug\\BIRD_REAL.mm3d");
        File birdOutput = new File("debug\\BIRD_FAKE.mm3d");

        DataReader reader = new DataReader(new FileSource(birdInput));
        MisfitModel3DObject object = new MisfitModel3DObject();
        object.load(reader);

        Utils.deleteFile(birdOutput);

        DataWriter writer = new DataWriter(new FileReceiver(birdOutput));
        object.save(writer);
        writer.closeReceiver();

        System.out.println("Ran model test.");
    }
}
