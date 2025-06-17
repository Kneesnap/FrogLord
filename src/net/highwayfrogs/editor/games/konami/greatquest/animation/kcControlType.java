package net.highwayfrogs.editor.games.konami.greatquest.animation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.animation.key.kcTrackKey;
import net.highwayfrogs.editor.games.konami.greatquest.animation.key.kcTrackKeyBezier.kcTrackKeyBezierPosition;
import net.highwayfrogs.editor.games.konami.greatquest.animation.key.kcTrackKeyDummy;
import net.highwayfrogs.editor.games.konami.greatquest.animation.key.kcTrackKeyTcb.kcTrackKeyTcbPosition;
import net.highwayfrogs.editor.games.konami.greatquest.animation.key.kcTrackKeyTcb.kcTrackKeyTcbRotation;
import net.highwayfrogs.editor.games.konami.greatquest.animation.key.kcTrackKeyVector.kcTrackKeyLinearPosition;
import net.highwayfrogs.editor.games.konami.greatquest.animation.key.kcTrackKeyVector.kcTrackKeyLinearRotation;
import net.highwayfrogs.editor.games.konami.greatquest.animation.key.kcTrackKeyVector.kcTrackKeyLinearScale;
import net.highwayfrogs.editor.utils.Utils;

import java.util.function.Function;

/**
 * A registry of different kcControl types.
 * Unless otherwise specified, the control type is never seen in any release.
 * The functions the game uses to interpolate are found in the 'sInterpFcnTbl':
 *  - USER: StubInterpFcn (Do nothing)
 *  - LINEAR_FLOAT: LinInterpFlt (Lerp between two float values)
 *  - LINEAR_ROTATION: LinInterpRot (Slerps between two quaternion rotation vectors)
 *  - LINEAR_POSITION: LinInterpPnt3 (Linearly interpolate between two XYZ coordinates)
 *  - LINEAR_SCALE: LinInterpPnt3 (Linearly interpolate between two XYZ coordinates)
 *  - TCB_FLT: TcbInterpFlt
 *  - TCB_ROTATION: TcbInterpRot
 *  - TCB_POSITION: TcbInterpPnt3
 *  - TCB_SCALE: TcbInterpPnt3
 *  - BEZIER_FLT: BezInterpFlt
 *  - BEZIER_POSITION: BezInterpPnt3
 *  - BEZIER_SCALE: BezInterpPnt3
 *  - POSITION_ROTATION_SCALE: StubInterpFcn (Do nothing)
 *  - CAMERA: StubInterpFcn (Do nothing)
 *  - LIGHT: StubInterpFcn (Do nothing)
 *  - STD: StubInterpFcn (Do nothing)
 *  - FLT: StubInterpFcn (Do nothing)
 *  - POSITION: StubInterpFcn (Do nothing)
 *  - ROTATION: StubInterpFcn (Do nothing)
 *  - SCALE: StubInterpFcn (Do nothing)
 *  - HIERARCHY: StubInterpFcn (Do nothing)
 * Created by Kneesnap on 4/16/2024.
 */
@Getter
@RequiredArgsConstructor
public enum kcControlType {
    USER(false, kcControlClassType.STANDARD, (ITrackKeyCreator) null, 0xC), // 0
    LINEAR_FLOAT(false, kcControlClassType.STANDARD, (ITrackKeyCreator) null, 0x14), // 1
    LINEAR_ROTATION(false, kcControlClassType.STANDARD, kcTrackKeyLinearRotation::new, 0x14), // 2, PC, PS2
    LINEAR_POSITION(false, kcControlClassType.STANDARD, kcTrackKeyLinearPosition::new, 0x14), // 3, PC, PS2
    LINEAR_SCALE(false, kcControlClassType.STANDARD, kcTrackKeyLinearScale::new, 0x14), // 4, PC, PS2
    TCB_FLT(true, kcControlClassType.STANDARD, (ITrackKeyCreator) null, 0x58), // 5, FLAT? FLOAT? kcTrackTcbPrepareFlt
    TCB_ROTATION(true, kcControlClassType.STANDARD, kcTrackKeyTcbRotation::new, 0x58), // 6 kcTrackTcbPrepareRot, PC, PS2
    TCB_POSITION(true, kcControlClassType.STANDARD, kcTrackKeyTcbPosition::new, 0x58), // 7 kcTrackTcbPreparePnt3, PC, PS2
    TCB_SCALE(true, kcControlClassType.STANDARD, (ITrackKeyCreator) null, 0x58), // 8 kcTrackTcbPreparePnt3
    BEZIER_FLT(true, kcControlClassType.STANDARD, (ITrackKeyCreator) null, 0x38), // 9, FLAT? FLOAT? kcTrackBezPrepareFlt
    BEZIER_POSITION(true, kcControlClassType.STANDARD, kcTrackKeyBezierPosition::new, 0x38), // 10 kcTrackBezPreparePnt3 PC, PS2
    BEZIER_SCALE(true, kcControlClassType.STANDARD, (ITrackKeyCreator) null, 0x38), // 11 kcTrackBezPreparePnt3
    POSITION_ROTATION_SCALE(false, kcControlClassType.PRS, (ITrackKeyCreator) null, 0x40), // 12 3 kcVector4s probably.
    CAMERA(false, null, (ITrackKeyCreator) null, 0), // 13
    LIGHT(false, null, (ITrackKeyCreator) null, 0), // 14
    STD(false, null, (ITrackKeyCreator) null, 0), // 15, STANDARD? -> StdControlInit() in kcControlCreate()
    FLT(false, null, (ITrackKeyCreator) null, 0), // 16, FLAT? FLOAT? -> PrsControlInit() in kcControlCreate()
    POSITION(false, null, (ITrackKeyCreator) null, 0), // 17
    ROTATION(false, null, (ITrackKeyCreator) null, 0), // 18
    SCALE(false, null, (ITrackKeyCreator) null, 0), // 19
    HIERARCHY(false, null, (ITrackKeyCreator) null, 0), // 20
    INVALID(false, null, (ITrackKeyCreator) null, 0); // 21

    private final boolean supported;
    private final kcControlClassType classType;
    private final ITrackKeyCreator trackKeyCreator;
    private final int probablySize;
    private boolean warningShown;

    kcControlType(boolean supported, kcControlClassType classType, Function<GreatQuestInstance, kcTrackKey<?>> creator, int probablySize) {
        this.supported = supported;
        this.classType = classType;
        this.probablySize = probablySize;
        this.trackKeyCreator = (instance, controlType) -> {
            kcTrackKey<?> newKey = creator.apply(instance);
            if (newKey.getControlType() != controlType)
                throw new RuntimeException("Expected a(n) " + controlType + ", but got a(n) " + Utils.getSimpleName(newKey) + "/" + newKey.getControlType() + " instead.");

            return newKey;
        };
    }

    // NOTE: TCB stands for 'Tension, Continuity, and Bias', which is a curve-based animation system, much like Bezier.

    /**
     * Creates a new kcTrackKey object instance.
     * @param gameInstance the game instance to create it for
     * @return newKey
     */
    public kcTrackKey<?> createKey(GreatQuestInstance gameInstance) {
        if (this.trackKeyCreator != null) {
            return this.trackKeyCreator.apply(gameInstance, this);
        } else {
            if (!this.warningShown) {
                gameInstance.getLogger().warning("The kcControlType '%s' is used but unimplemented!", name());
                this.warningShown = true;
            }

            return new kcTrackKeyDummy(gameInstance, this);
        }
    }

    private interface ITrackKeyCreator {
        kcTrackKey<?> apply(GreatQuestInstance instance, kcControlType controlType);
    }
}
