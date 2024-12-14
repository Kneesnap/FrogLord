package net.highwayfrogs.editor.system.mm3d.blocks;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.data.IBinarySerializable;
import net.highwayfrogs.editor.system.mm3d.MMDataBlockBody;
import net.highwayfrogs.editor.system.mm3d.MisfitModel3DObject;
import net.highwayfrogs.editor.system.mm3d.OffsetType;

import java.util.ArrayList;
import java.util.List;

/**
 * The frame animations points section describes the location and rotation of points in frame animations.
 * Version: 1.6+
 * Created by Kneesnap on 2/28/2019.
 */
@Getter
@Setter
public class MMFrameAnimationPointsBlock extends MMDataBlockBody {
    private short flags;
    private int animIndex; // Index of frame animation this applies to.
    private int frameCount; // Number of frames that follow.
    private List<FrameAnimationPoint> points = new ArrayList<>();

    public MMFrameAnimationPointsBlock(MisfitModel3DObject parent) {
        super(OffsetType.FRAME_ANIMATION_POINTS, parent);
    }

    @Override
    public void load(DataReader reader) {
        this.flags = reader.readShort();
        this.animIndex = reader.readInt();
        this.frameCount = reader.readInt();

        int totalPoints = frameCount * getParent().getPoints().size();
        for (int i = 0; i < totalPoints; i++) {
            FrameAnimationPoint point = new FrameAnimationPoint();
            point.load(reader);
            this.points.add(point);
        }
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeShort(this.flags);
        writer.writeInt(this.animIndex);
        writer.writeInt(this.frameCount);
        points.forEach(point -> point.save(writer));
    }

    @Getter
    public static class FrameAnimationPoint implements IBinarySerializable {
        private float rotX; // NOTE: In radians!
        private float rotY;
        private float rotZ;
        private float translateX;
        private float translateY;
        private float translateZ;

        @Override
        public void load(DataReader reader) {
            this.rotX = reader.readFloat();
            this.rotY = reader.readFloat();
            this.rotZ = reader.readFloat();
            this.translateX = reader.readFloat();
            this.translateY = reader.readFloat();
            this.translateZ = reader.readFloat();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeFloat(this.rotX);
            writer.writeFloat(this.rotY);
            writer.writeFloat(this.rotZ);
            writer.writeFloat(this.translateX);
            writer.writeFloat(this.translateY);
            writer.writeFloat(this.translateZ);
        }
    }
}
