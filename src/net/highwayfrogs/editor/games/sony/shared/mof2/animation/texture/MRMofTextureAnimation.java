package net.highwayfrogs.editor.games.sony.shared.mof2.animation.texture;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.games.generic.data.IBinarySerializable;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRMofPart;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds texture animation entries, thus representing a full texture animation.
 * Created by Kneesnap on 1/9/2019.
 */
@Getter
public class MRMofTextureAnimation implements IBinarySerializable {
    @NonNull private final MRMofPart parentPart;
    private final List<MRMofTextureAnimationEntry> entries = new ArrayList<>();

    public MRMofTextureAnimation(MRMofPart parentPart) {
        this.parentPart = parentPart;
    }

    @Override
    public void load(DataReader reader) {
        this.entries.clear();

        int entryCount = reader.readInt();
        for (int i = 0; i < entryCount; i++) {
            MRMofTextureAnimationEntry entry = new MRMofTextureAnimationEntry();
            entry.load(reader);
            this.entries.add(entry);
        }
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.entries.size());
        for (int i = 0; i < this.entries.size(); i++)
            this.entries.get(i).save(writer);
    }

    /**
     * Gets the entry for the given frame ID.
     * @param frame the frame ID to get the entry for
     * @return entryForFrame
     */
    public MRMofTextureAnimationEntry getEntryForFrame(int frame) {
        int frameCount = getTotalFrameCount();
        if (frameCount <= 0)
            return null;

        frame %= frameCount;
        if (frame < 0)
            frame += frameCount;

        for (int i = 0; i < this.entries.size(); i++) {
            MRMofTextureAnimationEntry entry = this.entries.get(i);
            if (entry.getDuration() > frame)
                return entry;

            frame -= entry.getDuration();
        }

        return null;
    }

    /**
     * Gets the total number of frames in the texture animation before it resets.
     * @return totalFrameCount
     */
    public int getTotalFrameCount() {
        int frameCount = 0;
        for (int i = 0; i < this.entries.size(); i++)
            frameCount += this.entries.get(i).getDuration();

        return frameCount;
    }
}
