package net.highwayfrogs.editor.games.konami.greatquest.script.cause;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScript;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Represents the script cause type.
 * These are processed within kcCScriptMgr::OnCommand where the execution conditions are tested, and if passed, the script effects will be queued for processing.
 * Created by Kneesnap on 6/26/2023.
 */
@Getter
@AllArgsConstructor
public enum kcScriptCauseType {
    LEVEL(Constants.BIT_FLAG_1, "OnLevel", kcScriptCauseLevel::new), // 2|0x02, Global Triggers: EvLevelBegin, EvLevelEnd
    PLAYER(Constants.BIT_FLAG_4, "OnPlayer", kcScriptCausePlayer::new), // 16|0x10, See kcScriptCauseEntityAction for triggers
    ACTOR(Constants.BIT_FLAG_5, "OnActor", kcScriptCauseActor::new), // 32|0x20, See kcScriptCauseEntityAction for triggers
    DAMAGE(Constants.BIT_FLAG_6, "OnDamage", kcScriptCauseDamage::new), // 64|0x40, Target Trigger: kcCActorBase::OnDamage
    TIMER(Constants.BIT_FLAG_8, "OnAlarm", kcScriptCauseTimer::new), // 256|0x100, Target Trigger: kcCEntity::AlarmCallback
    PROMPT(Constants.BIT_FLAG_9, "OnPrompt", kcScriptCausePrompt::new), // 512|0x200, Target Trigger: kcCActorBase::OnCommand[PROMPT]
    EVENT(Constants.BIT_FLAG_11, "OnEvent", kcScriptCauseEvent::new), // 2048|0x800, Global Trigger: kcCEventMgr::Trigger
    DIALOG(Constants.BIT_FLAG_12, "OnDialog", kcScriptCauseDialog::new), // 4096|0x1000, Target Triggers: EvDialogBegin, EvDialogAdvance, EvDialogEnd (I don't believe EvDialogEnd is possible to trigger)
    NUMBER(Constants.BIT_FLAG_13, "OnNumber", kcScriptCauseNumber::new), // 8192|0x2000, Target Trigger: kcCEntity::OnNumber
    WHEN_ITEM(Constants.BIT_FLAG_14, "OnPlayerHasItem", kcScriptCauseWhenItem::new), // 16384|0x4000, Target Triggers: CCharacter::OnWithItem, CProp::OnWithItem
    ENTITY_3D(Constants.BIT_FLAG_15, "OnEntity", kcScriptCauseEntity3D::new), // 32768|0x8000, Target Triggers: kcCEntity3D::Notify, sSendWaypointStatus triggers for the non-waypoint entity
    WAYPOINT(Constants.BIT_FLAG_19, "OnWaypointVisitor", kcScriptCauseWaypoint::new); // 524288|0x80000, Target Trigger: sSendWaypointStatus triggers for the waypoint itself

    // Unimplemented:
    // Bit Flag 3|0x08 has runtime support, but doesn't appear to ever be fired. It validates a single matches a value, in addition to the normal subCause check.
    // It's not clear which one it is either.
    //
    // These are a list of causes which existed (PS2 debug symbols) but don't appear to be implemented:
    // - Audio (uint mSoundHandle)
    // - Cheat
    // - Hud
    // - Sequence
    // - System

    private final int value;
    private final String displayName;
    private final Function<kcScript, kcScriptCause> maker;

    private static final Map<String, kcScriptCauseType> TYPES_BY_NAME = new HashMap<>();

    /**
     * Creates a new instance of a cause for this particular type.
     */
    public kcScriptCause createNew(kcScript script) {
        if (this.maker != null) {
            kcScriptCause newCause = this.maker.apply(script);
            if (newCause != null)
                return newCause;
        }

        return new kcScriptCauseDummy(script, this);
    }

    /**
     * Gets the kcScriptCauseType corresponding to the provided value.
     * @param value     The value to lookup.
     * @param allowNull If null is allowed.
     * @return instructionType
     */
    public static kcScriptCauseType getCauseType(int value, boolean allowNull) {
        for (int i = 0; i < values().length; i++) {
            kcScriptCauseType type = values()[i];
            if (type.value == value)
                return type;
        }

        if (!allowNull)
            throw new RuntimeException("Couldn't determine cause type from value " + value + ".");
        return null;
    }

    /**
     * Gets the kcScriptCauseType corresponding to the provided display name.
     * @param displayName The displayName to lookup.
     * @return causeType
     */
    public static kcScriptCauseType getCauseType(String displayName) {
        return TYPES_BY_NAME.get(displayName);
    }

     static {
        for (kcScriptCauseType type : values())
            TYPES_BY_NAME.put(type.getDisplayName(), type);
     }
}