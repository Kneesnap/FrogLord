package net.highwayfrogs.editor.games.konami.greatquest.script.cause;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents entity actions shared between ACTOR / PLAYER types.
 * Created by Kneesnap on 8/19/2023.
 */
@Getter
@AllArgsConstructor
public enum kcScriptCauseEntityAction {
    UNKNOWN_0(0, "Never", false, false), // Rolling Rapids Creek - Hides a bug. (Separate from the script to hide the bug in the tutorial.)
    INTERACT(1, "When the player interacts with the attached entity", true, false), // Target Triggers: CFrogCtl::OnBeginAction, CFrogCtl::CheckForHealthBug
    BUMPS(2, "When the player collides/bumps into the attached entity", true, true), // Target Triggers: CProp::TriggerHitCallback, CCharacter::BumpCallback
    ATTACK(5, "When the player targets the attached entity for an attack", true, false), // Target Triggers: CFrogCtl::Spit, CFrogCtl::OnBeginMissile, CFrogCtl::OnBeginMagicStone, and, CFrogCtl::OnBeginMelee
    PICKUP_ITEM(7, "When the player picks up the attached entity", true, false), // Target Trigger: CCharacter::PickupCallback
    TAKE_DAMAGE(8, "When the attached entity takes damage.", false, true), // Target Trigger: kcCActor::OnDamage
    UNKNOWN_9(9, "Never", false, false), // Bog Town - Plays Random Frogger Hurt Noise
    DEATH(10, "When the attached entity dies", false, true), // Target Trigger: kcCActor::OnDamage, Player deaths are also broadcast from the actor cause, but not from the player cause.
    UNKNOWN_13(13, "Never", false, false); // Rolling Rapids Creek - Starts unused copy of tutorial sequence from after opening the locked door.

    private final int value;
    private final String playerDescription;
    private final String actorDescription;
    private final boolean implementedForPlayer; // If this message is broadcast by an unmodified copy of the game for players.
    private final boolean implementedForActor; // If this message is broadcast by an unmodified copy of the game for actors.

    kcScriptCauseEntityAction(int value, String description, boolean implementedForPlayer, boolean implementedForActor) {
        this(value, getDescription(value, description, implementedForPlayer, false), getDescription(value, description, implementedForActor, true), implementedForPlayer, implementedForActor);
    }

    /**
     * Gets the kcScriptCauseEntityAction corresponding to the provided value.
     * @param value     The value to lookup.
     * @param allowNull If null is allowed.
     * @return entityAction
     */
    public static kcScriptCauseEntityAction getAction(int value, boolean allowNull) {
        for (int i = 0; i < values().length; i++) {
            kcScriptCauseEntityAction type = values()[i];
            if (type.value == value)
                return type;
        }

        if (!allowNull)
            throw new RuntimeException("Couldn't determine the entity action type from value " + value + ".");
        return null;
    }

    private static String getDescription(int value, String description, boolean enabled, boolean actor) {
        if (enabled)
            return description;

        String type = actor ? "Actor" : "Player";
        return ("Never (" + type + " Action #" + value + " is not supported by the game code.)");
    }
}