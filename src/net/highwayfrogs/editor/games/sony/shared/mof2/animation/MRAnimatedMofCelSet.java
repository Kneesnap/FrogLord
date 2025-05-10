package net.highwayfrogs.editor.games.sony.shared.mof2.animation;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.AppendInfoLoggerWrapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the "MR_ANIM_CEL_SET" struct.
 * Each entry is a different animation.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
public class MRAnimatedMofCelSet extends SCSharedGameData {
    @NonNull private final MRAnimatedMofModelSet parentModelSet;
    @NonNull private final List<MRAnimatedMofXarAnimation> animations = new ArrayList<>(); // Each entry is called "Cels" by the game, but I think that's a confusing name, so I'm calling it animations instead.
    private short padding;

    public MRAnimatedMofCelSet(@NonNull MRAnimatedMofModelSet modelSet) {
        super(modelSet.getGameInstance());
        this.parentModelSet = modelSet;
    }

    @Override
    public ILogger getLogger() {
        return new AppendInfoLoggerWrapper(this.parentModelSet.getLogger(), "CelSet", AppendInfoLoggerWrapper.TEMPLATE_COMMA_SEPARATOR);
    }

    @Override
    public void load(DataReader reader) {
        int animationCount = reader.readUnsignedShortAsInt();
        this.padding = reader.readShort(); // Can be non-zero.
        int dataPointer = reader.readInt(); // Points to literally the exact index after reading.

        // Read data.
        this.animations.clear();
        requireReaderIndex(reader, dataPointer, "Expected MRAnimatedMofXarAnimations");
        for (int i = 0; i < animationCount; i++) {
            MRAnimatedMofXarAnimation newAnimation = new MRAnimatedMofXarAnimation(this);
            newAnimation.load(reader);
            this.animations.add(newAnimation);
        }

        // Read animation data.
        for (int i = 0; i < this.animations.size(); i++)
            this.animations.get(i).readAnimationData(reader);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.animations.size());
        writer.writeShort(this.padding);
        writer.writeInt(writer.getIndex() + Constants.POINTER_SIZE);

        // Write animations.
        for (int i = 0; i < this.animations.size(); i++)
            this.animations.get(i).save(writer);

        // Write animation extra data.
        for (int i = 0; i < this.animations.size(); i++)
            this.animations.get(i).writeAnimationData(writer);
    }

    /**
     * Validates the animation data as being valid.
     * Must only be run after static mofs have been loaded.
     */
    void validateAnimations() {
        for (int i = 0; i < this.animations.size(); i++)
            this.animations.get(i).validatePartCount();
    }
}
