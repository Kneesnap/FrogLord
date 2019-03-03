package net.highwayfrogs.editor.system.mm3d;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.reader.FileSource;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.file.writer.FileReceiver;
import net.highwayfrogs.editor.system.mm3d.blocks.*;
import net.highwayfrogs.editor.system.mm3d.holders.MMTriangleFaceHolder;
import net.highwayfrogs.editor.utils.Utils;

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
 * Created by Kneesnap on 2/28/2019.
 */
@Getter
public class MisfitModel3DObject extends GameObject {
    private short modelFlags;
    private List<MMDataBlockHeader<?>> segments = new ArrayList<>();
    private MMDataBlockHeader<MMExternalTexturesBlock> externalTextures = new MMDataBlockHeader<>(OffsetType.EXTERNAL_TEXTURES, this);
    private MMDataBlockHeader<MMFrameAnimationPointsBlock> frameAnimationPoints = new MMDataBlockHeader<>(OffsetType.FRAME_ANIMATION_POINTS, this);
    private MMDataBlockHeader<MMFrameAnimationsBlock> frameAnimations = new MMDataBlockHeader<>(OffsetType.FRAME_ANIMATIONS, this);
    private MMDataBlockHeader<MMMaterialsBlock> materials = new MMDataBlockHeader<>(OffsetType.MATERIALS, this);
    private MMDataBlockHeader<MMMetaDataBlock> metadata = new MMDataBlockHeader<>(OffsetType.META_DATA, this);
    private MMDataBlockHeader<MMSmoothnessAnglesBlock> smoothnessAngles = new MMDataBlockHeader<>(OffsetType.SMOOTHNESS_ANGLES, this);
    private MMDataBlockHeader<MMTextureCoordinatesBlock> textureCoordinates = new MMDataBlockHeader<>(OffsetType.TEXTURE_COORDINATES, this);
    private MMDataBlockHeader<MMTextureProjectionTrianglesBlock> textureProjectionTriangles = new MMDataBlockHeader<>(OffsetType.TEXTURE_PROJECTIONS_TRIANGLES, this);
    private MMTriangleFaceHolder triangleFaces = new MMTriangleFaceHolder(this);
    private MMDataBlockHeader<MMTriangleGroupsBlock> groups = new MMDataBlockHeader<>(OffsetType.GROUPS, this);
    private MMDataBlockHeader<MMTriangleNormalsBlock> normals = new MMDataBlockHeader<>(OffsetType.TRIANGLE_NORMALS, this);
    private MMDataBlockHeader<MMVerticeBlock> vertices = new MMDataBlockHeader<>(OffsetType.VERTICES, this);
    private MMDataBlockHeader<MMPointsBlock> points = new MMDataBlockHeader<>(OffsetType.POINTS, this);

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

            System.out.println("Reading: " + type.ordinal() + ", " + type.getTypeCode() + ", " + type.name());

            reader.jumpTemp(offsetAddress);
            MMDataBlockHeader<?> header = type.findHeader(this);
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

        // Determine which segments we're gonna save.
        for (OffsetType type : OffsetType.values()) {
            if (type.getFinder() == null)
                continue; // Not implemented yet.

            MMDataBlockHeader<?> header = type.findHeader(this);

            boolean oldState = getSegments().contains(header);
            boolean newState = !header.getDataBlockBodies().isEmpty();

            if (oldState != newState) {
                getSegments().remove(header);
                if (newState)
                    getSegments().add(header);
            }
        }

        // Write headers.
        Map<MMDataBlockHeader, Integer> headerAddressMap = new HashMap<>();
        for (MMDataBlockHeader header : getSegments()) {
            writer.writeUnsignedShort(header.getOffsetType().getTypeCode());
            headerAddressMap.put(header, writer.writeNullPointer());
        }

        // Write EOF.
        writer.writeUnsignedShort(OffsetType.END_OF_FILE.getTypeCode());
        int eofHeaderAddress = writer.writeNullPointer();

        // Write chunks.
        for (MMDataBlockHeader header : getSegments()) {
            writer.writeAddressTo(headerAddressMap.remove(header));
            header.save(writer);
        }

        // Write EOF Pointer.
        writer.writeAddressTo(eofHeaderAddress);
    }

    public static void performTest() throws IOException {
        File birdInput = new File("debug\\BIRD_MODIFIED.mm3d");
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
