package net.highwayfrogs.editor.games.konami.greatquest.animation.key;

import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.animation.kcTrack;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.animation.kcControlType;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceSkeleton.kcNode;
import net.highwayfrogs.editor.games.konami.greatquest.math.kcQuat;
import net.highwayfrogs.editor.games.konami.greatquest.math.kcVector4;
import net.highwayfrogs.editor.system.math.Vector3f;
import net.highwayfrogs.editor.system.math.Vector4f;

/**
 * Represents a linear track key.
 * Created by Kneesnap on 5/2/2024.
 */
@Getter
public abstract class kcTrackKeyVector extends kcTrackKey<kcTrackKeyVector> {
    protected final kcVector4 vector = new kcVector4(0, 0, 0, 1); // Default value.
    // Rotation: Quaternion. I think I determined this just by looking at the data.
    // Position / Scale - Xyz Scalar

    protected static final kcAnimState TEMP_ANIM_STATE = new kcAnimState();

    public kcTrackKeyVector(GreatQuestInstance instance, kcControlType controlType) {
        super(instance, controlType);
    }

    @Override
    protected void loadKeyData(DataReader reader, int dataEndIndex) {
        this.vector.load(reader);
    }

    @Override
    protected void saveKeyData(DataWriter writer, int dataEndIndex) {
        this.vector.save(writer);
    }

    @Override
    protected void copyValueFromImpl(kcNode node, kcTrackKeyVector otherKey) {
        getVector().setXYZW(otherKey.getVector());
    }

    @Override
    public void writeInfo(StringBuilder builder) {
        super.writeInfo(builder);
        builder.append(", ");
        this.vector.writeInfo(builder);
    }

    /**
     * Implements the linear rotation track key.
     */
    public static class kcTrackKeyLinearRotation extends kcTrackKeyVector {
        public kcTrackKeyLinearRotation(GreatQuestInstance instance) {
            super(instance, kcControlType.LINEAR_ROTATION);
        }

        @Override
        protected void applyInterpolateValueImpl(kcNode node, kcTrackKeyVector previousKey, kcAnimState state, float t) {
            Vector4f prevRotation = previousKey != null ? previousKey.getVector() : node.getLocalRotation();
            kcQuat.kcQuatSlerp(prevRotation, getVector(), t, state.getRotation());
        }

        @Override
        protected void setupNewNextKeyImpl(kcTrack track, kcNode node, kcTrackKeyVector oldNextKey, kcTrackKeyVector newNextKey) {
            kcAnimState state = TEMP_ANIM_STATE;
            state.reset(node);
            state.evaluate(node, newNextKey.getTick(), track, false);
            newNextKey.getVector().setXYZW(state.getRotation());
        }
    }

    /**
     * Implements the 3D XYZ vector track key.
     */
    public static class kcTrackKeyLinearPosition extends kcTrackKeyVector {
        private static final Vector3f TEMP_PREV_POSITION = new Vector3f();
        private static final Vector3f TEMP_CURR_POSITION = new Vector3f();

        public kcTrackKeyLinearPosition(GreatQuestInstance instance) {
            super(instance, kcControlType.LINEAR_POSITION);
        }

        @Override
        protected void applyInterpolateValueImpl(kcNode node, kcTrackKeyVector previousKey, kcAnimState state, float t) {
            Vector3f prevPosition = TEMP_PREV_POSITION;
            if (previousKey != null) {
                Vector4f prevPosition4 = previousKey.getVector();
                prevPosition.setXYZ(prevPosition4.getX(), prevPosition4.getY(), prevPosition4.getZ());
            } else {
                prevPosition = node.getLocalPosition();
            }

            Vector3f currPosition = TEMP_CURR_POSITION.setXYZ(getVector().getX(), getVector().getY(), getVector().getZ());
            Vector3f.lerp(prevPosition, currPosition, t, state.getPosition());
        }

        @Override
        protected void setupNewNextKeyImpl(kcTrack track, kcNode node, kcTrackKeyVector oldNextKey, kcTrackKeyVector newNextKey) {
            kcAnimState state = TEMP_ANIM_STATE;
            state.reset(node);
            state.evaluate(node, newNextKey.getTick(), track, false);
            newNextKey.getVector().setX(state.getPosition().getX());
            newNextKey.getVector().setY(state.getPosition().getY());
            newNextKey.getVector().setZ(state.getPosition().getZ());
        }
    }

    /**
     * Implements the 3D XYZ vector track key.
     */
    public static class kcTrackKeyLinearScale extends kcTrackKeyVector {
        private static final Vector3f TEMP_PREV_SCALE = new Vector3f();
        private static final Vector3f TEMP_CURR_SCALE = new Vector3f();

        public kcTrackKeyLinearScale(GreatQuestInstance instance) {
            super(instance, kcControlType.LINEAR_SCALE);
        }

        @Override
        protected void applyInterpolateValueImpl(kcNode node, kcTrackKeyVector previousKey, kcAnimState state, float t) {
            Vector3f prevScale = TEMP_PREV_SCALE;
            if (previousKey != null) {
                Vector4f prevScale4 = previousKey.getVector();
                prevScale.setXYZ(prevScale4.getX(), prevScale4.getY(), prevScale4.getZ());
            } else {
                prevScale = node.getLocalScale();
            }

            Vector3f currScale = TEMP_CURR_SCALE.setXYZ(getVector().getX(), getVector().getY(), getVector().getZ());
            Vector3f.lerp(prevScale, currScale, t, state.getScale());
        }

        @Override
        protected void setupNewNextKeyImpl(kcTrack track, kcNode node, kcTrackKeyVector oldNextKey, kcTrackKeyVector newNextKey) {
            kcAnimState state = TEMP_ANIM_STATE;
            state.reset(node);
            state.evaluate(node, newNextKey.getTick(), track, false);
            newNextKey.getVector().setX(state.getScale().getX());
            newNextKey.getVector().setY(state.getScale().getY());
            newNextKey.getVector().setZ(state.getScale().getZ());
        }
    }
}
