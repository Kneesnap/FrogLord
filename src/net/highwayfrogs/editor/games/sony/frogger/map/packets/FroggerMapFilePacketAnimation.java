package net.highwayfrogs.editor.games.sony.frogger.map.packets;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.animation.FroggerMapAnimation;
import net.highwayfrogs.editor.games.sony.frogger.map.data.animation.FroggerMapAnimationTargetPolygon;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapPolygon;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents Frogger map animations.
 * Created by Kneesnap on 5/25/2024.
 */
public class FroggerMapFilePacketAnimation extends FroggerMapFilePacket {
    public static final String IDENTIFIER = "ANIM";
    public static final short TEXTURE_ID_LIST_TERMINATOR = (short) -1;
    @Getter private final List<FroggerMapAnimation> animations = new ArrayList<>();
    @Getter private final List<Short> unusedTextureIds = new ArrayList<>();
    private final Map<FroggerMapPolygon, List<FroggerMapAnimationTargetPolygon>> animationTargetsByPolygon = new IdentityHashMap<>(); // This is significantly faster than HashMap according to profiling, and we need every cycle we can get for animations...

    public FroggerMapFilePacketAnimation(FroggerMapFile parentFile, boolean require) {
        super(parentFile, IDENTIFIER, require, PacketSizeType.NO_SIZE);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        this.animations.clear();
        int mapAnimationCount = reader.readInt();
        int mapAnimationStartAddress = reader.readInt();
        reader.requireIndex(getLogger(), mapAnimationStartAddress, "Expected FroggerMapAnimation");
        for (int i = 0; i < mapAnimationCount; i++) {
            FroggerMapAnimation newAnimation = new FroggerMapAnimation(getParentFile());
            this.animations.add(newAnimation);
            newAnimation.load(reader);
        }

        // Read texture id lists.
        for (int i = 0; i < this.animations.size(); i++)
            this.animations.get(i).readTextureIdList(reader);

        // Read remaining texture ID list.
        short unusedTextureId;
        this.unusedTextureIds.clear();
        int polygonTargetStartAddress = this.animations.size() > 0 ? this.animations.get(0).getTargetPolygonListPointerAddress() : reader.getSize();
        while (polygonTargetStartAddress > reader.getIndex() && (unusedTextureId = reader.readShort()) != TEXTURE_ID_LIST_TERMINATOR)
            this.unusedTextureIds.add(unusedTextureId);

        // Read target polygon lists.
        for (int i = 0; i < this.animations.size(); i++)
            this.animations.get(i).readTargetPolygonList(reader);
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        writer.writeInt(this.animations.size());
        writer.writeAddressTo(writer.writeNullPointer());

        // Write animation data.
        for (int i = 0; i < this.animations.size(); i++)
            this.animations.get(i).save(writer);

        // Write texture id lists.
        for (int i = 0; i < this.animations.size(); i++)
            this.animations.get(i).writeTextureIdList(writer);

        // Write unused texture ids.
        for (int i = 0; i < this.unusedTextureIds.size(); i++)
            writer.writeShort(this.unusedTextureIds.get(i));
        writer.align(Constants.INTEGER_SIZE, (byte) 0xFF); // Mark end of texture id list.

        // Write animation id lists.
        for (int i = 0; i < this.animations.size(); i++)
            this.animations.get(i).writeTargetPolygonList(writer);
    }

    @Override
    public int getKnownStartAddress() {
        return getParentFile().getGraphicalPacket().getAnimationPacketAddress();
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList.add("Animation Count", this.animations.size());
        return propertyList;
    }

    /**
     * Gets the animation target for a specific polygon.
     * @param polygon the polygon to get the animation target from
     * @return animationTarget, or null if there is none
     */
    public FroggerMapAnimationTargetPolygon getAnimationTarget(FroggerMapPolygon polygon) {
        List<FroggerMapAnimationTargetPolygon> targets = this.animationTargetsByPolygon.get(polygon);
        return targets != null && targets.size() > 0 ? targets.get(targets.size() - 1) : null;
    }

    /**
     * Called when a polygon is applied to an animation polygon target.
     * @param targetPolygon the target polygon object
     * @param oldPolygon the previously tracked polygon
     * @param newPolygon the newly tracked polygon
     */
    public void onPolygonTargetSet(FroggerMapAnimationTargetPolygon targetPolygon, FroggerMapPolygon oldPolygon, FroggerMapPolygon newPolygon) {
        if (oldPolygon == newPolygon)
            return;

        // Remove old polygon from old tracking.
        if (oldPolygon != null) {
            List<FroggerMapAnimationTargetPolygon> targets = this.animationTargetsByPolygon.get(oldPolygon);
            if (targets.remove(targetPolygon) && targets.isEmpty())
                this.animationTargetsByPolygon.remove(oldPolygon, targets);
        }

        // Add new polygon to tracking.
        if (newPolygon != null) {
            List<FroggerMapAnimationTargetPolygon> targets = this.animationTargetsByPolygon.computeIfAbsent(newPolygon, key -> new ArrayList<>());
            targets.add(targetPolygon);
        }
    }
}