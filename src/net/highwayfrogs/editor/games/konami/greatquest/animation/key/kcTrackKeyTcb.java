package net.highwayfrogs.editor.games.konami.greatquest.animation.key;

import lombok.Getter;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.animation.kcControlType;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceSkeleton.kcNode;
import net.highwayfrogs.editor.games.konami.greatquest.math.kcQuat;
import net.highwayfrogs.editor.games.konami.greatquest.math.kcVector3;
import net.highwayfrogs.editor.games.konami.greatquest.math.kcVector4;
import net.highwayfrogs.editor.system.math.Vector3f;
import net.highwayfrogs.editor.system.math.Vector4f;

/**
 * Represents a TCB animation track key. (Tension, Continuity, Bias)
 * Most likely Frogger: The Great Quest used 3DS Max, and thus, the data seen here is likely a direct reflection of 3DS Max's capabilities.
 * <a href="https://help.autodesk.com/view/3DSMAX/2023/ENU/?guid=GUID-24693B38-20BD-435D-9816-BE5BDA528506"/>Documentation Reference</a>
 * <a href="https://www.cubic.org/docs/hermite.htm"/>Documentation Reference</a>
 * The following is copied from the above references for preservation.
 * <p/>
 * Tension: How sharply does the curve bend?
 * How stiff is the curve? Higher tension results in a sharper curve while lower tension leads to a more rounded curve.
 * Controls the amount of curvature in the animation curve.
 * High tension produces a linear curve. It also has a slight ease to / ease from effect.
 * Low tension produces a very wide, rounded, curve. It also has a negative ease to & ease from effect.
 * <p/>
 * Continuity: How rapid is the change in speed and direction?
 * This impacts the smoothness of the transition between segments. It can help maintain the tangent direction between two keyframes.
 * Controls the tangential property of the curve at the key.
 * High continuity values create curved overshoot on both sides of the key.
 * Low continuity values create a linear animation curve.
 * Low continuity creates a linear curve similar to high tension except without the Ease To and Ease From side effect.
 * <p/>
 * Bias: What is the direction of the curve as it passes through the keypoint?
 * High bias pushes the curve beyond the key, producing a linear curve coming into the key and an exaggerated curve leaving the key.
 * Low bias pulls the curve before the key. This produces an exaggerated curve coming into the key and a linear curve leaving the key.
 * High bias pushes the curve beyond the key, producing a linear curve coming into the key and an exaggerated curve leaving the key.
 * <p/>
 * These algorithms appear to be the same as the hermite basis functions? H0(t) = 2t^3 - 3t^2 + 1, H1(t) = -2t^3+3t^2, etc.
 * This is because TCB is a kind of hermite spline.
 * TODO: In the future it would be nice to have methods for calculating the tension, continuity, and bias. NOTE: Those are not actually vectors, they are single floating point values.
 *  - It would also be nice to rename the vectors to have proper names.
 * Created by Kneesnap on 10/18/2024.
 */
@Getter
public abstract class kcTrackKeyTcb extends kcTrackKey<kcTrackKeyTcb> {
    protected final kcVector4 vector01 = new kcVector4(0, 0, 0, 1); // Represents the position, at least for TCB_POSITION. The fourth value is some kind of angle for TCB_ROTATION, but is 1.0 for TCB_POSITION.
    protected final kcVector4 vector05 = new kcVector4();
    protected final kcVector3 unused = new kcVector3(); // Almost always 0.0, 0.0, 0.0 in PS2 NTSC. There are some of these which have the X component set. As far as I'm aware, this vector remains unused.

    // Slows the velocity of the animation curve as it approaches the key. Default: 0
    // A high value will cause the animation to decelerate as it approaches the key.
    // Observed as always 0.0 in PS2 NTSC.
    protected float easeTo;

    // Slows the velocity of the animation curve as it leaves the key. Default: 0
    // A high value will cause the animation to start slow and accelerate as it leaves the key.
    // Observed as always 0.0 in PS2 NTSC.
    protected float easeFrom;

    protected final kcVector4 vector0E = new kcVector4();
    protected final kcVector4 vector12 = new kcVector4();

    private static final float PI = 3.1415927f; // Pi for the purposes of this class.
    private static final kcVector4 DEFAULT_VECTOR = new kcVector4(0, 0, 0, 1);

    public kcTrackKeyTcb(GreatQuestInstance instance, kcControlType controlType) {
        super(instance, controlType);
    }

    @Override
    protected void loadKeyData(DataReader reader, int dataEndIndex) {
        this.vector01.load(reader);
        this.vector05.load(reader);
        this.unused.load(reader);
        this.easeTo = reader.readFloat();
        this.easeFrom = reader.readFloat();
        this.vector0E.load(reader);
        this.vector12.load(reader);
        if (this.unused.getY() != 0 || this.unused.getZ() != 0)
            getLogger().warning("Expected unused vector to be all zeros, but actually was: " + this.unused);
        if (this.easeFrom != 0 || this.easeTo != 0)
            getLogger().warning("Expected TCB ease values to be 0.0, but found [Value 1: " + this.easeFrom + ", Value 2: " + this.easeTo + "]");
    }

    @Override
    protected void saveKeyData(DataWriter writer, int dataEndIndex) {
        this.vector01.save(writer);
        this.vector05.save(writer);
        this.unused.save(writer);
        writer.writeFloat(this.easeTo);
        writer.writeFloat(this.easeFrom);
        this.vector0E.save(writer);
        this.vector12.save(writer);
    }

    @Override
    public void writeInfo(StringBuilder builder) {
        super.writeInfo(builder);
        builder.append(", Vector 01: ");
        this.vector01.writeInfo(builder);
        builder.append(", Vector 05: ");
        this.vector05.writeInfo(builder);
        builder.append(", Vector 0E: ");
        this.vector0E.writeInfo(builder);
        builder.append(", Vector 12: ");
        this.vector12.writeInfo(builder);

        // Write extra data.
        if (this.easeFrom != 0)
            builder.append(", Ease 1: ").append(this.easeFrom);
        if (this.easeTo != 0)
            builder.append(", Ease 2: ").append(this.easeTo);
        if (this.unused.getX() != 0 || this.unused.getY() != 0 || this.unused.getZ() != 0)
            builder.append(", Unused Vector: ").append(this.unused);
    }

    // Reverse engineered from Ease().
    // All this does is try to start & stop gently. I'm not sure why this easing algorithm has a/b though.
    private static float ease(float t, float a, float b) {
        float total = a + b;
        if (t == 0 || t == 1 || total == 0)
            return t;

        if (total > 1F) {
            a /= total;
            b /= total;
        }

        float inverse = 1F / (2 - a - b);
        if (a > t) {
            return t * (inverse / a);
        } else if (b - 1f > t) {
            return inverse * ((2 * t) - a);
        } else {
            return 1f - ((1f - t) * (inverse / b));
        }
    }

    public static class kcTrackKeyTcbPosition extends kcTrackKeyTcb {
        public kcTrackKeyTcbPosition(GreatQuestInstance instance) {
            super(instance, kcControlType.TCB_POSITION);
        }

        @Override
        protected void applyInterpolateValueImpl(kcNode node, kcTrackKeyTcb previousKey, kcAnimState state, float t) {
            t = kcTrackKeyTcb.ease(t, previousKey != null ? previousKey.getEaseFrom() : 0, this.easeTo);

            float t2 = t * t;
            float t3 = t * t2;
            float h4 = t3 - t2; // Hermite Basis function 4: t^3 - t^2 (Multiplied against tangent2)
            float h3 = t3 - (2 * t2) + t; // Hermite Basis function 3: t^3 - 2t^2 + t (Multiplier against tangent 1)
            float h2 = -(2 * t3) + (3 * t2); // Hermite Basis function 2: -2t^3 + 3t^2 (Multiplied against end position)
            float h1 = -h2 + 1f; // Hermite Basis function 1: 2t^3 - 3t^2 + 1 (Multiplied against start position)

            kcVector4 currVec01 = this.vector01;
            kcVector4 currVec0E = this.vector0E;
            kcVector4 prevVec01 = previousKey != null ? previousKey.getVector01() : DEFAULT_VECTOR;
            kcVector4 prevVec12 = previousKey != null ? previousKey.getVector12() : DEFAULT_VECTOR;
            Vector3f pos = state.getPosition();
            pos.setX((currVec0E.getX() * h4) + (prevVec12.getX() * h3) + (currVec01.getX() * h2) + (prevVec01.getX() * h1));
            pos.setY((currVec0E.getY() * h4) + (prevVec12.getY() * h3) + (currVec01.getY() * h2) + (prevVec01.getY() * h1));
            pos.setZ((currVec0E.getZ() * h4) + (prevVec12.getZ() * h3) + (currVec01.getZ() * h2) + (prevVec01.getZ() * h1));
        }
    }

    public static class kcTrackKeyTcbRotation extends kcTrackKeyTcb {
        private static final Vector4f TEMP_VECTOR1 = new Vector4f();
        private static final Vector4f TEMP_VECTOR2 = new Vector4f();
        private static final Vector4f TEMP_VECTOR3 = new Vector4f();
        private static final Vector4f TEMP_VECTOR4 = new Vector4f();

        public kcTrackKeyTcbRotation(GreatQuestInstance instance) {
            super(instance, kcControlType.TCB_ROTATION);
        }

        @Override
        protected void applyInterpolateValueImpl(kcNode node, kcTrackKeyTcb previousKey, kcAnimState state, float t) {
            t = kcTrackKeyTcb.ease(t, previousKey != null ? previousKey.getEaseFrom() : 0, this.easeTo);

            float calcAngle = 0f;
            float angle = this.vector05.getW();
            float halfAngle = angle * 0.5f;
            Vector4f prevVector01 = previousKey != null ? previousKey.getVector01() : DEFAULT_VECTOR;
            Vector4f prevVector12 = previousKey != null ? previousKey.getVector12() : DEFAULT_VECTOR;

            if (halfAngle <= PI) {
                kcQuat.kcQuatSlerp(prevVector01, this.vector01, t, TEMP_VECTOR1);
                kcQuat.kcQuatSlerp(prevVector12, this.vector0E, t, TEMP_VECTOR2);
                kcQuat.kcQuatSlerp(TEMP_VECTOR1, TEMP_VECTOR2, (1.0f - t) * 2.0f * t, state.getRotation());
                return;
            }

            while (halfAngle > 3.1415918) {
                calcAngle += 1f;
                halfAngle -= 3.1415927f;
            }
            if (halfAngle < 0.0)
                halfAngle = 0.0f;

            Vector4f temp = TEMP_VECTOR1.setXYZW(this.vector05.getX(), this.vector05.getY(), this.vector05.getZ(), 0);
            float normalisedAngle = (t * angle) / 3.1415927f;
            if (normalisedAngle < 1.0) {
                kcQuat.kcQuatMul(prevVector01, temp, TEMP_VECTOR2);
                kcQuat.kcQuatSlerp(prevVector01, TEMP_VECTOR2, normalisedAngle, TEMP_VECTOR3);
                kcQuat.kcQuatSlerp(prevVector12, TEMP_VECTOR2, normalisedAngle, TEMP_VECTOR4);
                kcQuat.kcQuatSlerp(TEMP_VECTOR3, TEMP_VECTOR4, (1f - normalisedAngle) * 2f * normalisedAngle, state.getRotation());
            } else {
                calcAngle = (normalisedAngle + 1f) - (calcAngle + halfAngle / PI) * 2f;
                if (calcAngle <= 0.0) {
                    while (normalisedAngle >= 2.0)
                        normalisedAngle -= 2f;

                    kcQuat.kcQuatMul(prevVector01, temp, TEMP_VECTOR2);
                    kcQuat.kcQuatSlerp(prevVector01, TEMP_VECTOR2, normalisedAngle, state.getRotation());
                } else {
                    kcQuat.kcQuatMul(this.vector01, temp, TEMP_VECTOR2);
                    TEMP_VECTOR2.negate(); // Equivalent to kcQuatInvertQ()
                    kcQuat.kcQuatSlerp(TEMP_VECTOR2, this.vector01, calcAngle, TEMP_VECTOR3);
                    kcQuat.kcQuatSlerp(TEMP_VECTOR2, this.vector0E, calcAngle, TEMP_VECTOR4);
                    kcQuat.kcQuatSlerp(TEMP_VECTOR3, TEMP_VECTOR4, (1f - calcAngle) * 2f * calcAngle, state.getRotation());
                }
            }
        }
    }
}
