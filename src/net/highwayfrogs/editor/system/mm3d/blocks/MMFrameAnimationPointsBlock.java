package net.highwayfrogs.editor.system.mm3d.blocks;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.mm3d.MMDataBlockBody;
import net.highwayfrogs.editor.system.mm3d.MisfitModel3DObject;

import java.util.ArrayList;
import java.util.List;

/**
 * The frame animations points section describes the location and rotation of points in frame animations.
 * Created by Kneesnap on 2/28/2019.
 */
@Getter
public class MMFrameAnimationPointsBlock extends MMDataBlockBody {
    private int flags;
    private long animIndex; // Index of frame animation.
    private long frameCount; // Number of frames that follow.
    private List<FrameAnimationPoint> points = new ArrayList<>();

    public MMFrameAnimationPointsBlock(MisfitModel3DObject parent) {
        super(parent);
    }

    @Override
    public void load(DataReader reader) {
        this.flags = reader.readUnsignedShortAsInt();
        this.animIndex = reader.readUnsignedIntAsLong();
        this.frameCount = reader.readUnsignedIntAsLong();

        long totalPoints = frameCount; //TODO: Supposedly this is multiplied by POINT_COUNT?
        for (long i = 0; i < totalPoints; i++) {
            FrameAnimationPoint point = new FrameAnimationPoint();
            point.load(reader);
            this.points.add(point);
        }
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.flags);
        writer.writeUnsignedInt(this.animIndex);
        writer.writeUnsignedInt(this.frameCount);
        points.forEach(point -> point.save(writer));
    }

    @Getter
    public static class FrameAnimationPoint extends GameObject {
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
