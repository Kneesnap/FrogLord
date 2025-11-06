package net.highwayfrogs.editor.games.konami.greatquest.animation.key;

import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.animation.kcTrack;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceSkeleton.kcNode;
import net.highwayfrogs.editor.games.konami.greatquest.math.kcMatrix;
import net.highwayfrogs.editor.system.math.Matrix4x4f;
import net.highwayfrogs.editor.system.math.Vector3f;
import net.highwayfrogs.editor.system.math.Vector4f;

import java.util.List;

/**
 * Represents the animation state.
 * Created by Kneesnap on 10/11/2024.
 */
@Getter
public class kcAnimState {
    private final Vector3f position = new Vector3f();
    private final Vector4f rotation = Vector4f.UNIT_W.clone();
    private final Vector3f scale = new Vector3f(1, 1, 1);

    private static final Matrix4x4f TEMP_SCALE_MATRIX = new Matrix4x4f();

    /**
     * Resets the animation state back to the default pose.
     */
    public void reset(kcNode bone) {
        if (bone.getTag() != 0) { // As seen in EnumEval()
            this.position.setXYZ(bone.getLocalPosition());
        } else {
            this.position.setXYZ(0, 0, 0);
        }
        this.rotation.setXYZW(bone.getLocalRotation());
        this.scale.setXYZ(1, 1, 1);
    }

    /**
     * Gets the local offset matrix usable for matrix calculations.
     * @param result the matrix to store the results within
     * @return localOffsetMatrix
     */
    public Matrix4x4f getLocalOffsetMatrix(Matrix4x4f result) {
        kcMatrix.kcQuatToMatrix(this.rotation, result);
        result = result.setTranslation(this.position);

        // I didn't quite find where this is applied in the original game, but scaling is used for various animations, so I think this is ok.
        if (this.scale.getX() != 1 || this.scale.getY() != 1 || this.scale.getZ() != 1) {
            Matrix4x4f scaleMatrix = Matrix4x4f.initialiseScaleMatrix(TEMP_SCALE_MATRIX, this.scale);
            result = result.multiply(scaleMatrix, result);
        }

        return result;
    }

    /**
     * Evaluates the animation
     * @param bone the bone which is getting evaluated
     * @param tick the animation tick to evaluate
     * @param tracks the animation tracks to apply
     * @return true if the animation is occurring, or false if the end of the animation has been reached / no future changes will occur.
     */
    public boolean evaluate(kcNode bone, double tick, List<kcTrack> tracks, boolean reverseAnimation) {
        if (!Double.isFinite(tick))
            throw new IllegalArgumentException("Invalid animation tick provided: " + tick);
        if (tracks == null || tracks.isEmpty())
            return false;

        boolean reachedEndOfAllTracks = true;
        for (int i = 0; i < tracks.size(); i++) {
            kcTrack track = tracks.get(i);
            int trackKeyIndex = track.getKeyIndexForTick((int) tick);
            if (trackKeyIndex < 0)
                continue;

            kcTrackKey<?> trackKey = track.getKeyList().get(trackKeyIndex);
            kcTrackKey<?> lastKey = trackKeyIndex - 1 >= 0 ? track.getKeyList().get(trackKeyIndex - 1) : null;
            kcTrackKey<?> nextKey = track.getKeyList().size() > trackKeyIndex + 1 ? track.getKeyList().get(trackKeyIndex + 1) : null;

            if (tick < trackKey.getTick() && lastKey == null) { // Happens when playing animations in reverse.
                trackKey.applyInterpolateValue(bone, trackKey, this, 0f);
                if (!reverseAnimation) // If playing in the regular direction, don't stop playing until after the end is reached.
                    reachedEndOfAllTracks = false;
            } else if (nextKey != null) {
                float t = (float) (tick - trackKey.getTick()) / (nextKey.getTick() - trackKey.getTick());
                nextKey.applyInterpolateValue(bone, trackKey, this, t);
                reachedEndOfAllTracks = false;
            } else {
                trackKey.applyInterpolateValue(bone, lastKey, this, 1f);
                if (reverseAnimation) // If playing in the reverse direction, don't stop playing until after the start is reached.
                    reachedEndOfAllTracks = false;
            }
        }

        return !reachedEndOfAllTracks;
    }
}
