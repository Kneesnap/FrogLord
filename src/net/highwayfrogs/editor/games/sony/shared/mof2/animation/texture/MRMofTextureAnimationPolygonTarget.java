package net.highwayfrogs.editor.games.sony.shared.mof2.animation.texture;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.generic.data.IBinarySerializable;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRMofPart;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRMofPolygon;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRMofPolygonType;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.Map;

/**
 * Represents "MR_PART_POLY_ANIM".
 * Created by Kneesnap on 1/8/2019.
 */
public class MRMofTextureAnimationPolygonTarget implements IBinarySerializable {
    @Getter @NonNull private final MRMofPart parentPart;
    @Getter private MRMofPolygon polygon;
    @Getter private MRMofTextureAnimation animation;

    private transient int tempAnimAddress = -1;
    private transient int tempPolygonIndex = -1;
    private transient MRMofPolygonType tempPolygonType;

    public MRMofTextureAnimationPolygonTarget(MRMofPart parentPart) {
        this.parentPart = parentPart;
    }

    public MRMofTextureAnimationPolygonTarget(MRMofPart parentPart, MRMofPolygon polygon, MRMofTextureAnimation animation) {
        this(parentPart);
        this.polygon = polygon;
        this.animation = animation;
    }

    @Override
    public void load(DataReader reader) {
        if (this.tempAnimAddress > 0)
            throw new RuntimeException("tempAnimAddress was set, suggesting the MRMofTextureAnimationPolygonTarget may be in an invalid state!");

        this.tempPolygonType = MRMofPolygonType.values()[reader.readInt()];
        this.tempPolygonIndex = reader.readInt();
        reader.skipBytesRequireEmpty(Constants.INTEGER_SIZE); // Runtime.
        this.tempAnimAddress = reader.readInt(); // Animation pointer.
    }

    @Override
    public void save(DataWriter writer) {
        if (this.polygon == null)
            throw new RuntimeException("Cannot save MRMofTextureAnimation with a null polygon!");
        if (this.tempAnimAddress > 0)
            throw new RuntimeException("tempAnimAddress was set, suggesting the MRMofTextureAnimationPolygonTarget may be in an invalid state!");

        writer.writeInt(this.polygon.getPolygonType().ordinal());
        writer.writeInt(this.parentPart.getPolygonIndex(this.polygon)); // OK to use even if polygons have not been written yet.
        writer.writeNullPointer(); // Runtime.
        this.tempAnimAddress = writer.writeNullPointer();
    }

    /**
     * Resolves the texture animation by its pointer.
     * @param animationsByPointer all loaded texture animations by their pointers.
     */
    public void resolveTextureAnimationPointer(Map<Integer, MRMofTextureAnimation> animationsByPointer) {
        if (this.tempAnimAddress < 0)
            throw new RuntimeException("tempAnimAddress was not set, suggesting the MRMofTextureAnimationPolygonTarget has no need to resolve its texture animation!");

        int animationAddress = this.tempAnimAddress;
        this.tempAnimAddress = -1;
        this.animation = animationsByPointer.get(animationAddress);
        if (this.animation == null)
            throw new RuntimeException("Failed to resolve texture animation by its pointer: " + NumberUtils.toHexString(animationAddress));
    }

    /**
     * Write the texture animation pointer, once we know what it is/might be.
     * @param writer The writer to write data to.
     * @param pointersByAnimation allows determining the pointer which a given texture animation was written to.
     */
    public void saveTextureAnimationPointer(DataWriter writer, Map<MRMofTextureAnimation, Integer> pointersByAnimation) {
        if (pointersByAnimation == null)
            throw new NullPointerException("pointersByAnimation");
        if (this.tempAnimAddress < 0)
            throw new RuntimeException("tempAnimAddress was not set, suggesting the MRMofTextureAnimation has no need to its animation pointer!");

        Integer animationPointer = pointersByAnimation.get(this.animation);
        if (animationPointer == null)
            throw new RuntimeException("Could not find the pointer where the MRMofTextureAnimationPolygonTarget's animation was saved.");

        writer.writeIntAtPos(this.tempAnimAddress, animationPointer);
        this.tempAnimAddress = -1;
    }

    /**
     * Resolve the polygon ID loaded earlier (before polygons were loaded) from a binary data stream.
     * Should only be called once polygon data is loaded.
     */
    public void resolvePolygonID() {
        if (this.tempPolygonIndex < 0)
            throw new RuntimeException("tempPolygonIndex was not set, so the polygon target has no need to resolve the polygon!");
        if (this.tempPolygonType == null)
            throw new RuntimeException("tempPolygonType was not set, so the polygon target has no need to resolve the polygon!");

        // Cleanup.
        MRMofPolygonType polygonType = this.tempPolygonType;
        int polygonIndex = this.tempPolygonIndex;
        this.tempPolygonIndex = -1;
        this.tempPolygonType = null;

        // Resolve polygon.
        this.polygon = getParentPart().getPolygon(polygonIndex);
        if (this.polygon == null)
            throw new RuntimeException("Failed to resolve MOF Polygon by ID: " + polygonIndex);
        if (this.polygon.getPolygonType() != polygonType)
            throw new RuntimeException("The resolved MOF polygon was of type " + this.polygon.getPolygonType() + ", but a(n) " + polygonType + " polygon was expected?!");
    }

    /**
     * Sets the targeted polygon.
     * @param newTargetPolygon the new polygon to target
     */
    public void setPolygon(MRMofPolygon newTargetPolygon) {
        if (newTargetPolygon == null)
            throw new NullPointerException("newTargetPolygon");
        if (newTargetPolygon.getMofPart() != this.parentPart)
            throw new IllegalArgumentException("The provided targetPolygon does not belong to the parent mofPart!");
        if (this.tempPolygonIndex >= 0 || this.tempPolygonType != null)
            resolvePolygonID();

        this.polygon = newTargetPolygon;
    }

    /**
     * Gets the frame count of this animation.
     * @return totalFrameCount
     */
    public int getTotalFrameCount() {
        int frame = 0;
        for (MRMofTextureAnimationEntry entry : this.animation.getEntries())
            frame += entry.getDuration();
        return frame;
    }
}
