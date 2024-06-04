package net.highwayfrogs.editor.games.konami.greatquest.animation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.animation.key.kcTrackKey;
import net.highwayfrogs.editor.games.konami.greatquest.animation.key.kcTrackKeyDummy;
import net.highwayfrogs.editor.games.konami.greatquest.animation.key.kcTrackKeyLinear;

import java.util.function.BiFunction;

/**
 * A registry of different kcControl types.
 * Created by Kneesnap on 4/16/2024.
 */
@Getter
@AllArgsConstructor
public enum kcControlType {
    USER(false, kcControlClassType.STANDARD, null, 0xC), // 0
    LINEAR_FLT(false, kcControlClassType.STANDARD, kcTrackKeyLinear::new, 0x14), // 1, FLAT? FLOAT?
    LINEAR_ROTATION(false, kcControlClassType.STANDARD, kcTrackKeyLinear::new, 0x14), // 2 TODO: PC
    LINEAR_POSITION(false, kcControlClassType.STANDARD, kcTrackKeyLinear::new, 0x14), // 3 TODO: PC
    LINEAR_SCALE(false, kcControlClassType.STANDARD, kcTrackKeyLinear::new, 0x14), // 4 TODO: PC
    TCB_FLT(true, kcControlClassType.STANDARD, null, 0x58), // 5, FLAT? FLOAT? kcTrackTcbPrepareFlt
    TCB_ROTATION(true, kcControlClassType.STANDARD, null, 0x58), // 6 kcTrackTcbPrepareRot TODO: PC
    TCB_POSITION(true, kcControlClassType.STANDARD, null, 0x58), // 7 kcTrackTcbPreparePnt3 TODO: PC
    TCB_SCALE(true, kcControlClassType.STANDARD, null, 0x58), // 8 kcTrackTcbPreparePnt3
    BEZIER_FLT(true, kcControlClassType.STANDARD, null, 0x38), // 9, FLAT? FLOAT? kcTrackBezPrepareFlt
    BEZIER_POSITION(true, kcControlClassType.STANDARD, null, 0x38), // 10 kcTrackBezPreparePnt3 TODO: PC
    BEZIER_SCALE(true, kcControlClassType.STANDARD, null, 0x38), // 11 kcTrackBezPreparePnt3
    POSITION_ROTATION_SCALE(false, kcControlClassType.PRS, null, 0x40), // 12 3 kcVector4s probably.
    CAMERA(false, null, null, 0), // 13
    LIGHT(false, null, null, 0), // 14
    STD(false, null, null, 0), // 15, STANDARD?
    FLT(false, null, null, 0), // 16, FLAT? FLOAT?
    POSITION(false, null, null, 0), // 17
    ROTATION(false, null, null, 0), // 18
    SCALE(false, null, null, 0), // 19
    HIERARCHY(false, null, null, 0), // 20
    INVALID(false, null, null, 0); // 21

    private final boolean supported;
    private final kcControlClassType classType;
    private final BiFunction<GreatQuestInstance, kcControlType, kcTrackKey> trackKeyCreator;
    private final int probablySize;

    // NOTE: TCB stands for 'Tension, Continuity, and Bias', which is a curve-based animation system, much like Bezier.

    /**
     * Creates a new kcTrackKey object instance.
     * @param gameInstance the game instance to create it for
     * @return newKey
     */
    public kcTrackKey createKey(GreatQuestInstance gameInstance) {
        return this.trackKeyCreator != null ? this.trackKeyCreator.apply(gameInstance, this) : new kcTrackKeyDummy(gameInstance, this);
    }
}
