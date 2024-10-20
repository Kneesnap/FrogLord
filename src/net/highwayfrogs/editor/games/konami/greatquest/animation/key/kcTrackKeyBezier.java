package net.highwayfrogs.editor.games.konami.greatquest.animation.key;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.animation.kcControlType;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceSkeleton.kcNode;
import net.highwayfrogs.editor.games.konami.greatquest.math.kcVector4;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents a cubic Bézier curve.
 * Created by Kneesnap on 10/15/2024.
 */
@Getter
public abstract class kcTrackKeyBezier extends kcTrackKey<kcTrackKeyBezier> {
    protected final kcVector4 endPosition = new kcVector4();
    protected final kcVector4 startPosition = new kcVector4();
    protected final kcVector4 controlPosition = new kcVector4();
    protected int flags = 2 << 7 | 2 << 10; // Unknown.

    // These values appear to be used for preparation? Not sure if we need this...
    // bits [00, 06]: Purpose Unknown, possibly unused
    // bits [07, 09]: Start Position Mode [Used]
    // bits [10, 12]: Mode Selection? [Used]
    // bits [13, 15]: Unknown potentially unused
    private static final int FLAG_VALIDATION = 0b1_111_111_111_1111;

    protected kcTrackKeyBezier(GreatQuestInstance instance, kcControlType controlType) {
        super(instance, controlType);
    }

    @Override
    protected void loadKeyData(DataReader reader, int dataEndIndex) {
        this.endPosition.load(reader);
        this.startPosition.load(reader);
        this.controlPosition.load(reader);
        this.flags = reader.readInt();
        warnAboutInvalidBitFlags(this.flags, FLAG_VALIDATION, "kcTrackKeyBezier");

        if (this.endPosition.getW() != 1)
            getLogger().severe("Unexpected End Position W: " + this.endPosition.getW());
        if (this.startPosition.getW() != 1)
            getLogger().severe("Unexpected Start Position W: " + this.startPosition.getW());
        if (this.controlPosition.getW() != 1)
            getLogger().severe("Unexpected Control Position W: " + this.controlPosition.getW());
    }

    @Override
    protected void saveKeyData(DataWriter writer, int dataEndIndex) {
        this.endPosition.save(writer);
        this.startPosition.save(writer);
        this.controlPosition.save(writer);
        writer.writeInt(this.flags);
    }

    @Override
    public void writeInfo(StringBuilder builder) {
        super.writeInfo(builder);
        builder.append(", Flags: ");
        builder.append(Utils.toHexString(getFlags()));
        builder.append(", Start Position: ");
        this.startPosition.writeInfo(builder);
        builder.append(", End Position: ");
        this.endPosition.writeInfo(builder);
        builder.append(", Control Position: ");
        this.controlPosition.writeInfo(builder);
    }

    public static class kcTrackKeyBezierPosition extends kcTrackKeyBezier {
        public kcTrackKeyBezierPosition(GreatQuestInstance instance) {
            super(instance, kcControlType.BEZIER_POSITION);
        }

        @Override
        protected void applyInterpolateValueImpl(kcNode node, kcTrackKeyBezier lastKey, kcAnimState state, float t) {
            if (lastKey == null) {
                // I added this myself, this was not reverse engineered from BezInterpPnt3.
                if (t >= .5f) {
                    state.getPosition().setXYZ(this.endPosition.getX(), this.endPosition.getY(), this.endPosition.getZ());
                } else {
                    state.getPosition().setXYZ(this.startPosition.getX(), this.startPosition.getY(), this.startPosition.getZ());
                }
            } else if (((lastKey.getFlags() >> 10 & 7) == 2) || ((getFlags() >> 7 & 7) == 2)) {
                if (t == 1.0) {
                    state.getPosition().setXYZ(this.endPosition.getX(), this.endPosition.getY(), this.endPosition.getZ());
                } else {
                    state.getPosition().setXYZ(lastKey.getEndPosition().getX(), lastKey.getEndPosition().getY(), lastKey.getEndPosition().getZ());
                }
            } else {
                float coefficient = (getTick() - lastKey.getTick()) / 3f;
                float c1ControlX = lastKey.getControlPosition().getX(), c1ControlY = lastKey.getControlPosition().getY(), c1ControlZ = lastKey.getControlPosition().getZ();
                float c1EndX = lastKey.getEndPosition().getX(), c1EndY = lastKey.getEndPosition().getY(), c1EndZ = lastKey.getEndPosition().getZ();
                float c2StartX = this.startPosition.getX(), c2StartY = this.startPosition.getY(), c2StartZ = this.startPosition.getZ();
                float c2EndX = this.endPosition.getX(), c2EndY = this.endPosition.getY(), c2EndZ = this.endPosition.getZ();
                float invT = 1f - t;
                float t2 = t * t;
                float t3 = t * t * t;
                state.getPosition().setX(c2EndX * t3 +
                        invT * (3 * t2 * (c2EndX + c2StartX * coefficient) +
                        invT * (invT * c1EndX + 3 * t * (c1EndX + c1ControlX * coefficient))));
                state.getPosition().setY(c2EndY * t3 +
                        invT * (3 * t2 * (c2EndY + c2StartY * coefficient) +
                        invT * (3 * t * (c1EndY + c1ControlY * coefficient) + invT * c1EndY)));
                state.getPosition().setZ(c2EndZ * t3 +
                        invT * (3 * t2 * (c2EndZ + c2StartZ * coefficient) +
                        invT * (3 * t * (c1EndZ + c1ControlZ * coefficient) + invT * c1EndZ)));
            }
        }
    }
}