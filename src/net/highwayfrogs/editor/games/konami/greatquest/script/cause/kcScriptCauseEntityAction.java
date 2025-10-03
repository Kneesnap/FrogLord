package net.highwayfrogs.editor.games.konami.greatquest.script.cause;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntityInheritanceGroup;

/**
 * Represents entity actions shared between ACTOR / PLAYER types.
 * Created by Kneesnap on 8/19/2023.
 */
@Getter
@AllArgsConstructor
public enum kcScriptCauseEntityAction {
    UNKNOWN_0(0, "Never", false, false, kcEntityInheritanceGroup.ENTITY), // Rolling Rapids Creek - Hides a bug. (Separate from the script to hide the bug in the tutorial.)
    INTERACT(1, "When the player interacts with the script entity", true, false, kcEntityInheritanceGroup.ACTOR_BASE), // Target Triggers: CFrogCtl::OnBeginAction, CFrogCtl::CheckForHealthBug
    BUMPS(2, "When another entity/player collides/bumps into the script entity", true, true, kcEntityInheritanceGroup.PROP_OR_CHARACTER), // Target Triggers: CProp::TriggerHitCallback, CCharacter::BumpCallback
    TARGET_FOR_ATTACK(5, "When the player targets the script entity for an attack", true, false, kcEntityInheritanceGroup.ACTOR_BASE), // Target Triggers: CFrogCtl::Spit, CFrogCtl::OnBeginMissile, CFrogCtl::OnBeginMagicStone, and, CFrogCtl::OnBeginMelee
    PICKUP_ITEM(7, "When the player picks up the script entity", true, false, kcEntityInheritanceGroup.ITEM), // Target Trigger: CCharacter::PickupCallback
    HEAL(8, "When the script entity heals", false, true, kcEntityInheritanceGroup.ACTOR), // Target Trigger: kcCActor::OnDamage
    UNKNOWN_9(9, "Never", false, false, kcEntityInheritanceGroup.ENTITY), // Bog Town - Plays Random Frogger Hurt Noise
    DEATH(10, "When the script entity dies", false, true, kcEntityInheritanceGroup.ACTOR), // Target Trigger: kcCActor::OnDamage, NOTE: If the --PreventDeath flag is set, it looks like this trigger can still occur, as it's only the animation which doesn't occur, the health still reaches 0. Remove --CanTakeDamage to prevent this.
    UNKNOWN_13(13, "Never", false, false, kcEntityInheritanceGroup.ENTITY); // Rolling Rapids Creek - Starts unused copy of tutorial sequence from after opening the locked door.

    private final int value;
    private final String playerDescription;
    private final String actorDescription;
    private final boolean implementedForPlayer; // If this cause is sent by an unmodified copy of the game for players.
    private final boolean implementedForActor; // If this cause is sent by an unmodified copy of the game for actors.
    private final kcEntityInheritanceGroup entityGroup;

    kcScriptCauseEntityAction(int value, String description, boolean implementedForPlayer, boolean implementedForActor, kcEntityInheritanceGroup category) {
        this(value, getDescription(value, description, implementedForPlayer, false), getDescription(value, description, implementedForActor, true), implementedForPlayer, implementedForActor, category);
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