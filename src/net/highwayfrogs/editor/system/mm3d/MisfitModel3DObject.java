package net.highwayfrogs.editor.system.mm3d;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.mm3d.blocks.*;
import net.highwayfrogs.editor.system.mm3d.holders.MMExternalTextureHolder;
import net.highwayfrogs.editor.system.mm3d.holders.MMTextureCoordinateHolder;
import net.highwayfrogs.editor.system.mm3d.holders.MMTriangleFaceHolder;
import net.highwayfrogs.editor.utils.Utils;

import java.util.*;

/**
 * Represents a mm3d file.
 * Format Version: 1.6
 * Specification: http://www.misfitcode.com/misfitmodel3d/olh_mm3dformat.html
 * New Spec: https://clover.moe/mm3d_manual/olh_mm3dformat.html#overview
 * Created by Kneesnap on 2/28/2019.
 */
@Getter
public class MisfitModel3DObject extends GameObject {
    private short modelFlags;
    private List<MMDataBlockHeader<?>> segments = new ArrayList<>();
    private MMExternalTextureHolder externalTextures = new MMExternalTextureHolder(this);
    private MMDataBlockHeader<MMFrameAnimationPointsBlock> frameAnimationPoints = new MMDataBlockHeader<>(OffsetType.FRAME_ANIMATION_POINTS, this);
    private MMDataBlockHeader<MMFrameAnimationsBlock> frameAnimations = new MMDataBlockHeader<>(OffsetType.FRAME_ANIMATIONS, this);
    private MMDataBlockHeader<MMMaterialsBlock> materials = new MMDataBlockHeader<>(OffsetType.MATERIALS, this);
    private MMDataBlockHeader<MMMetaDataBlock> metadata = new MMDataBlockHeader<>(OffsetType.META_DATA, this);
    private MMDataBlockHeader<MMSmoothnessAnglesBlock> smoothnessAngles = new MMDataBlockHeader<>(OffsetType.SMOOTHNESS_ANGLES, this);
    private MMTextureCoordinateHolder textureCoordinates = new MMTextureCoordinateHolder(this);
    private MMDataBlockHeader<MMTextureProjectionTrianglesBlock> textureProjectionTriangles = new MMDataBlockHeader<>(OffsetType.TEXTURE_PROJECTIONS_TRIANGLES, this);
    private MMTriangleFaceHolder triangleFaces = new MMTriangleFaceHolder(this);
    private MMDataBlockHeader<MMTriangleGroupsBlock> groups = new MMDataBlockHeader<>(OffsetType.GROUPS, this);
    private MMDataBlockHeader<MMTriangleNormalsBlock> normals = new MMDataBlockHeader<>(OffsetType.TRIANGLE_NORMALS, this);
    private MMDataBlockHeader<MMVerticeBlock> vertices = new MMDataBlockHeader<>(OffsetType.VERTICES, this);
    private MMDataBlockHeader<MMPointsBlock> points = new MMDataBlockHeader<>(OffsetType.POINTS, this);
    private MMDataBlockHeader<MMCanvasBackgroundImage> canvasBackgroundImages = new MMDataBlockHeader<>(OffsetType.CANVAS_BACKGROUND_IMAGES, this);
    private MMDataBlockHeader<MMSkeletalAnimationBlock> skeletalAnimations = new MMDataBlockHeader<>(OffsetType.SKELETAL_ANIMATIONS, this);
    private MMDataBlockHeader<MMJointsBlock> joints = new MMDataBlockHeader<>(OffsetType.JOINTS, this);
    private MMDataBlockHeader<MMJointVerticesBlock> jointVertices = new MMDataBlockHeader<>(OffsetType.JOINT_VERTICES, this);
    private MMDataBlockHeader<MMTextureProjectionsBlock> textureProjections = new MMDataBlockHeader<>(OffsetType.TEXTURE_PROJECTIONS, this);
    private MMDataBlockHeader<MMWeightedInfluencesBlock> weightedInfluences = new MMDataBlockHeader<>(OffsetType.WEIGHTED_INFLUENCES, this);

    private static final Set<OffsetType> FIRST_ROUND_READ = new HashSet<>(Arrays.asList(OffsetType.VERTICES, OffsetType.POINTS)); // These values need to be read before the rest, since information about them are used in other places.

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

        // First Reading Round.
        reader.jumpTemp(reader.getIndex());
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

            if (FIRST_ROUND_READ.contains(type)) {
//                System.out.println("Round 1 Reading: " + type.ordinal() + "/" + type.name());
                loadHeader(reader, type, offsetAddress);
            }
        }

        reader.jumpReturn();

        // Main Reading Round.
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

            if (!FIRST_ROUND_READ.contains(type)) {
//                System.out.println("Main Reading: " + type.ordinal() + "/" + type.name());
                loadHeader(reader, type, offsetAddress);
            }
        }
    }

    private void loadHeader(DataReader reader, OffsetType type, int offsetAddress) {
        reader.jumpTemp(offsetAddress);
        MMDataBlockHeader<?> header = type.findHeader(this);
        header.load(reader);
        this.segments.add(header);
        reader.jumpReturn();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeStringBytes(SIGNATURE);
        writer.writeUnsignedByte(MAJOR_VERSION);
        writer.writeUnsignedByte(MINOR_VERSION);
        writer.writeUnsignedByte(this.modelFlags);

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

        // Write the count after calculating which segments are being written.
        writer.writeUnsignedByte((short) (this.segments.size() + 1)); // Add 1 to account for EOF.

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
}
