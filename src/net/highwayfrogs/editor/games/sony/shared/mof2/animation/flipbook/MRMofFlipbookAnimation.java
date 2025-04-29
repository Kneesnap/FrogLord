package net.highwayfrogs.editor.games.sony.shared.mof2.animation.flipbook;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.data.IBinarySerializable;

/**
 * Represents "MR_PART_FLIPBOOK_ACTION".
 * Flipbook animations are vertex animations which contain different "cels" (snapshots of vertex positions) which are flipped between.
 * The memory impact is high, but CPU computation is near-zero.
 * Flipbook animations were added in June 1997 to the MR API, suggesting they may have been added specifically for use in Frogger.
 * This class is an animation definition despite how little data it contains.
 * It gives a position in the partCel list, and then says how many frames there are to move through in the partCel list.
 * Created by Kneesnap on 1/8/2019.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MRMofFlipbookAnimation implements IBinarySerializable {
    // The game calls the following int a "partCelCount".
    // This may be confusing at first since we treat it more like a frameCount.
    // But, a cel is a snapshot of vertex positions, and so the number of cels is actually the same thing as the number of frames.
    private int partCelCount;
    private int partCelIndex;

    @Override
    public void load(DataReader reader) {
        this.partCelCount = reader.readUnsignedShortAsInt();
        this.partCelIndex = reader.readUnsignedShortAsInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.partCelCount);
        writer.writeUnsignedShort(this.partCelIndex);
    }

    /**
     * This is an easier to understand name for the `partCelCount`.
     * It primarily serves to draw attention to the above comment which describes why partCelCount means the same thing as frameCount.
     * @return frameCount
     */
    public int getFrameCount() {
        return this.partCelCount;
    }
}
