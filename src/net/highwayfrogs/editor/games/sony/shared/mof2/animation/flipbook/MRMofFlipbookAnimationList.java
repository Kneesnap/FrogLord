package net.highwayfrogs.editor.games.sony.shared.mof2.animation.flipbook;

import lombok.Getter;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.data.IBinarySerializable;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents "MR_PART_FLIPBOOK", which is a list of flipbook animations/actions.
 * Created by Kneesnap on 1/8/2019.
 */
@Getter
public class MRMofFlipbookAnimationList implements IBinarySerializable {
    private final List<MRMofFlipbookAnimation> animations = new ArrayList<>(); // These are called actions by the game.

    @Override
    public void load(DataReader reader) {
        int flags = reader.readUnsignedShortAsInt();
        if (flags != 0)
            throw new RuntimeException("Flipbook Flags were not zero! (" + flags + ").");

        this.animations.clear();
        int actionCount = reader.readUnsignedShortAsInt();
        for (int i = 0; i < actionCount; i++) {
            MRMofFlipbookAnimation newFrame = new MRMofFlipbookAnimation();
            newFrame.load(reader);
            this.animations.add(newFrame);
        }
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(0);
        writer.writeUnsignedShort(this.animations.size());
        for (int i = 0; i < this.animations.size(); i++)
            this.animations.get(i).save(writer);
    }

    /**
     * Get the PartCel index based on the number.
     * @param animationId The frame number presumably.
     * @param frameCount  "Cel number" (Treated as the number of frames into the animation you are. NOTE: That's animation frames not FPS frames)
     * @return celIndex
     */
    public int getPartCelIndex(int animationId, int frameCount) {
        MRMofFlipbookAnimation action = getAction(animationId);
        if (action == null)
            return 0;

        return action.getPartCelIndex() + (frameCount % action.getPartCelCount());
    }

    /**
     * Gets an animation/action by its id.
     * @param action The index of the animation/action to get.
     * @return flipbookAnimation
     */
    public MRMofFlipbookAnimation getAction(int action) {
        return action >= 0 && action < this.animations.size() ? this.animations.get(action) : null;
    }
}
