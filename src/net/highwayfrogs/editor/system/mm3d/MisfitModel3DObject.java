package net.highwayfrogs.editor.system.mm3d;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.mm3d.blocks.*;
import net.highwayfrogs.editor.system.mm3d.holders.MMExternalTextureHolder;
import net.highwayfrogs.editor.system.mm3d.holders.MMMetadataHolder;
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
    @Setter private short majorVersion = 1;
    @Setter private short minorVersion = 6;
    private short modelFlags;
    private List<MMDataBlockHeader<?>> segments = new ArrayList<>();
    private MMExternalTextureHolder externalTextures = new MMExternalTextureHolder(this);
    private MMDataBlockHeader<MMFrameAnimationPointsBlock> frameAnimationPoints = new MMDataBlockHeader<>(OffsetType.FRAME_ANIMATION_POINTS, this);
    private MMDataBlockHeader<MMFrameAnimationsBlock> frameAnimations = new MMDataBlockHeader<>(OffsetType.FRAME_ANIMATIONS, this);
    private MMDataBlockHeader<MMMaterialsBlock> materials = new MMDataBlockHeader<>(OffsetType.MATERIALS, this);
    private MMMetadataHolder metadata = new MMMetadataHolder(this);
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

    @Override
    public void load(DataReader reader) {
        reader.verifyString(SIGNATURE);
        this.majorVersion = reader.readUnsignedByteAsShort();
        this.minorVersion = reader.readUnsignedByteAsShort();
        this.modelFlags = reader.readUnsignedByteAsShort();
        short dataSegmentCount = reader.readUnsignedByteAsShort();

        Utils.verify(majorVersion == 1 && (minorVersion >= 4 && minorVersion <= 7), "Unsupported Version: (" + majorVersion + "." + minorVersion + ")");

        // First Reading Round.
        reader.jumpTemp(reader.getIndex());
        readRound(reader, dataSegmentCount, true);
        reader.jumpReturn();

        // Main Round.
        readRound(reader, dataSegmentCount, false);
    }

    private void readRound(DataReader reader, int dataSegmentCount, boolean firstRound) {
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

            if (FIRST_ROUND_READ.contains(type) == firstRound) {
//                System.out.println((firstRound ? "First" : "Main") + " Reading: " + type.ordinal() + "/" + type.name());
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

    /**
     * Gets the first metadata value with a given key.
     * Returns null if not found.
     * @param key The key to search for.
     */
    public String getFirstMetadataValue(String key) {
        for (MMMetaDataBlock metaDataBlock : getMetadata().getBlocks())
            if (metaDataBlock.getKey().equals(key))
                return metaDataBlock.getValue();
        return null;
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeStringBytes(SIGNATURE);
        writer.writeUnsignedByte(this.majorVersion);
        writer.writeUnsignedByte(this.minorVersion);
        writer.writeUnsignedByte(this.modelFlags);

        // Determine which segments we're gonna save.
        for (OffsetType type : OffsetType.values()) {
            if (type.getFinder() == null)
                continue; // Not implemented yet.

            MMDataBlockHeader<?> header = type.findHeader(this);

            boolean oldState = getSegments().contains(header);
            boolean newState = !header.getBlocks().isEmpty();

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

    /**
     * Check if the file version is at least a certain version.
     * @param majorVersion The major version to test.
     * @param minorVersion The minor version to test.
     */
    @SuppressWarnings("unused")
    public boolean isVersionAtLeast(int majorVersion, int minorVersion) {
        return (majorVersion >= this.majorVersion) && (majorVersion > this.majorVersion || minorVersion >= this.minorVersion);
    }
}
