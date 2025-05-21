package net.highwayfrogs.editor.system.mm3d.blocks;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.generic.data.IBinarySerializable;
import net.highwayfrogs.editor.system.mm3d.MMDataBlockBody;
import net.highwayfrogs.editor.system.mm3d.MisfitModel3DObject;
import net.highwayfrogs.editor.system.mm3d.OffsetType;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single skeletal animation.
 * Created by Kneesnap on 3/6/2020.
 */
@Getter
@Setter
public class MMSkeletalAnimationBlock extends MMDataBlockBody {
    private short flags;
    private String name;
    private float fps;
    private final List<List<MMSkeletalAnimationFrame>> frames = new ArrayList<>();

    public static final short FLAG_LOOPING = Constants.BIT_FLAG_0; // 1.7+

    public MMSkeletalAnimationBlock(MisfitModel3DObject parent) {
        super(OffsetType.SKELETAL_ANIMATIONS, parent);
    }

    @Override
    public void load(DataReader reader) {
        this.flags = reader.readShort();
        this.name = reader.readNullTerminatedString();
        this.fps = reader.readFloat();

        int frameCount = reader.readInt();
        for (int i = 0; i < frameCount; i++) {
            int keyFrameCount = reader.readInt();
            List<MMSkeletalAnimationFrame> keyframes = new ArrayList<>(keyFrameCount);

            for (int j = 0; j < keyFrameCount; j++) {
                MMSkeletalAnimationFrame keyframe = new MMSkeletalAnimationFrame();
                keyframe.load(reader);
                keyframes.add(keyframe);
            }

            this.frames.add(keyframes);
        }
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeShort(this.flags);
        writer.writeNullTerminatedString(this.name);
        writer.writeFloat(this.fps);

        // Write frames.
        writer.writeInt(this.frames.size());
        for (List<MMSkeletalAnimationFrame> keyframes : this.frames) {
            writer.writeInt(keyframes.size());
            for (MMSkeletalAnimationFrame keyframe : keyframes)
                keyframe.save(writer);
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MMSkeletalAnimationFrame implements IBinarySerializable {
        private int jointIndex;
        private MMAnimationKeyframeType keyframeType;
        private float x; // Could be a transform, could be a rotation (radians).
        private float y;
        private float z;

        @Override
        public void load(DataReader reader) {
            this.jointIndex = reader.readInt();
            this.keyframeType = MMAnimationKeyframeType.values()[reader.readByte()];
            this.x = reader.readFloat();
            this.y = reader.readFloat();
            this.z = reader.readFloat();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeInt(this.jointIndex);
            writer.writeByte((byte) this.keyframeType.ordinal());
            writer.writeFloat(this.x);
            writer.writeFloat(this.y);
            writer.writeFloat(this.z);
        }
    }

    public enum MMAnimationKeyframeType {
        ROTATION, TRANSLATION
    }
}
