package net.highwayfrogs.editor.system.mm3d;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.system.mm3d.blocks.*;

import java.util.function.Supplier;

/**
 * A registry of different offset types.
 * Created by Kneesnap on 2/28/2019.
 */
@Getter
@AllArgsConstructor
public enum OffsetType {
    META_DATA(0x1001, MMMetaDataBlock::new),
    GROUPS(0x0101, MMTriangleGroupsBlock::new),
    EMBEDDED_TEXTURES(0x141, null), // Not implemented in the actual mm3d yet.
    EXTERNAL_TEXTURES(0x0142, MMExternalTexturesBlock::new),
    MATERIALS(0x0161, MMMaterialsBlock::new),
    TEXTURE_PROJECTIONS_TRIANGLES(0x16C, MMTextureProjectionTrianglesBlock::new),
    CANVAS_BACKGROUND_IMAGES(0x0191, null),
    SKELETAL_ANIMATIONS(0x0301, null),
    FRAME_ANIMATIONS(0x321, MMFrameAnimationsBlock::new),
    FRAME_ANIMATION_POINTS(0x326, MMFrameAnimationPointsBlock::new),
    FRAME_RELATIVE_ANIMATIONS(0x341, null), // Not implemented in the actual mm3d yet.
    END_OF_FILE(0x3FFF, null),

    VERTICES(0x8001, MMVerticeBlock::new),
    TRIANGLES(0x8021, MMTriangleBlock::new),
    TRIANGLE_NORMALS(0x8026, MMTriangleNormalsBlock::new),
    JOINTS(0x8041, null),
    JOINT_VERTICES(0x8046, null),
    POINTS(0x8061, null),
    SMOOTHNESS_ANGLES(0x8106, MMSmoothnessAnglesBlock::new),
    WEIGHTED_INFLUENCES(0x8146, null),
    TEXTURE_PROJECTIONS(0x8168, null), // (Sphere / cylinder map)
    TEXTURE_COORDINATES(0x8121, MMTextureCoordinatesBlock::new);

    private final int typeCode;
    private final Supplier<? extends MMDataBlockBody> maker;

    /**
     * Test if this offset is a constant size.
     * @return isConstantSize
     */
    public boolean isTypeB() {
        return (this.typeCode & 0x8000) == 0x8000;
    }

    /**
     * Test if this offset is type A.
     * @return typeA
     */
    public boolean isTypeA() {
        return !isTypeB();
    }

    /**
     * Make a new data body instance.
     * @return new
     */
    public MMDataBlockBody makeNew() {
        if (maker == null)
            throw new RuntimeException(name() + " is not a supported data block type yet.");
        return maker.get();
    }

    /**
     * Get the OffsetType based on its type number.
     * @param offsetType The type number to get an offset type for.
     * @return offsetType
     */
    public static OffsetType getOffsetType(int offsetType) {
        for (OffsetType type : values())
            if (type.getTypeCode() == offsetType)
                return type;
        return null;
    }
}
