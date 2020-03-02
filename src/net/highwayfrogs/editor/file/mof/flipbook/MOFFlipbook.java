package net.highwayfrogs.editor.file.mof.flipbook;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents "MR_PART_FLIPBOOK".
 * Created by Kneesnap on 1/8/2019.
 */
@Getter
public class MOFFlipbook extends GameObject {
    private List<MOFFlipbookAction> actions = new ArrayList<>();

    @Override
    public void load(DataReader reader) {
        int flags = reader.readUnsignedShortAsInt();
        if (flags != 0)
            throw new RuntimeException("Flipbook Flags were not zero! (" + flags + ").");

        int actionCount = reader.readUnsignedShortAsInt();
        for (int i = 0; i < actionCount; i++) {
            MOFFlipbookAction action = new MOFFlipbookAction();
            action.load(reader);
            actions.add(action);
        }
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(0);
        writer.writeUnsignedShort(this.actions.size());
        this.actions.forEach(action -> action.save(writer));
    }

    /**
     * Get the PartCel index based on the number.
     * @param animationId The frame number presumably.
     * @param frameCount  "Cel number"
     * @return celIndex
     */
    public int getPartCelIndex(int animationId, int frameCount) {
        MOFFlipbookAction action = getAction(animationId);
        return action.getPartcelIndex() + (frameCount % action.getFrameCount());
    }

    /**
     * Gets an action by its id.
     * @param action The action id to get.
     * @return flipbookAction
     */
    public MOFFlipbookAction getAction(int action) {
        return getActions().get(action);
    }
}
