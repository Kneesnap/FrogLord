package net.highwayfrogs.editor.games.sony.frogger.map.data.animation;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapPolygon;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.FroggerMapFilePacketAnimation;
import net.highwayfrogs.editor.games.sony.shared.SCByteTextureUV;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.Utils;

import java.util.List;

/**
 * This represents a polygon targetted by an animation.
 * Represents the 'MAP_UV_INFO' struct in mapdisp.H
 * Created by Kneesnap on 6/15/2024.
 */
@Getter
public class FroggerMapAnimationTargetPolygon extends SCGameData<FroggerGameInstance> {
    private final transient FroggerMapAnimation animation;
    private FroggerMapPolygon polygon;

    private static final int UV_COUNT = 4;
    private static final int TOTAL_UV_BLOCK_SIZE = UV_COUNT * SCByteTextureUV.BYTE_SIZE;
    public static final int BYTE_SIZE = Constants.POINTER_SIZE + TOTAL_UV_BLOCK_SIZE;

    public FroggerMapAnimationTargetPolygon(FroggerMapAnimation animation) {
        super(animation != null ? animation.getGameInstance() : null);
        this.animation = animation;
    }

    public FroggerMapAnimationTargetPolygon(FroggerMapAnimation animation, FroggerMapPolygon polygon) {
        this(animation);
        setPolygon(polygon);
    }

    @Override
    public void load(DataReader reader) {
        int polygonPointer = reader.readInt();
        reader.skipBytesRequireEmpty(TOTAL_UV_BLOCK_SIZE); // There are a bunch of uvs, but they're run-time only.

        // Resolve polygon.
        List<FroggerMapPolygon> mapPolygons = this.animation.getMapFile().getPolygonPacket().getPolygons();
        int polygonIndex = Utils.binarySearch(mapPolygons, polygonPointer, FroggerMapPolygon::getLastReadAddress);
        FroggerMapPolygon polygon = polygonIndex >= 0 ? mapPolygons.get(polygonIndex) : null;
        if (polygon == null)
            throw new RuntimeException("No polygon was loaded from " + NumberUtils.toHexString(polygonPointer) + ".");

        setPolygon(polygon);
    }

    @Override
    public void save(DataWriter writer) {
        if (this.polygon == null || this.polygon.getLastWriteAddress() <= 0)
            throw new RuntimeException("Animation cannot target a polygon which wasn't saved!");

        writer.writeInt(this.polygon.getLastWriteAddress());
        writer.writeNull(TOTAL_UV_BLOCK_SIZE); // Run-time UV data.
    }

    /**
     * Sets the polygon currently active.
     * @param polygon the polygon to apply to this target
     */
    public void setPolygon(FroggerMapPolygon polygon) {
        if (this.polygon == polygon)
            return; // No change.

        FroggerMapPolygon oldPolygon = this.polygon;
        this.polygon = polygon;

        FroggerMapFilePacketAnimation animationPacket = this.animation != null && this.animation.getMapFile() != null
                ? this.animation.getMapFile().getAnimationPacket() : null;

        if (animationPacket != null)
            animationPacket.onPolygonTargetSet(this, oldPolygon, polygon);
    }
}