package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import net.highwayfrogs.editor.games.konami.greatquest.entity.kcActorBaseDesc;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcActorDesc;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcHealthDesc.kcDamageType;
import net.highwayfrogs.editor.games.konami.greatquest.script.*;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

/**
 * Gives damage to the actor.
 * The entity will only take the damage if it is negative (heal) OR (entity->health->mImmune & weaponMask) == 0.
 * In other words, if the entity is immune to even a single masked bit in the weapon mask, they will not take the damage.
 * Reference: kcCHealth::ApplyDamage().
 * Created by Kneesnap on 10/30/2024.
 */
public class kcActionGiveDamage extends kcActionTemplate {
    private static final kcArgument[] DAMAGE_ARGUMENTS = kcArgument.make(kcParamType.HEX_INTEGER, "weaponMask", kcParamType.INT, "attackStrength");
    private static final kcArgument[] GIVE_DAMAGE_ARGUMENTS = kcArgument.make(kcParamType.INT, "attackStrength", kcParamType.HEX_INTEGER, "weaponMask");

    public kcActionGiveDamage(kcActionExecutor executor, kcActionID action) {
        super(executor, action);
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        switch (getActionID()) {
            case DAMAGE:
                return DAMAGE_ARGUMENTS;
            case GIVE_DAMAGE:
                return GIVE_DAMAGE_ARGUMENTS;
            default:
                throw new RuntimeException("Unsupported kcActionID: " + getActionID());
        }
    }

    @Override
    public int getGqsArgumentCount(kcArgument[] argumentTemplates) {
        return 1; // Loading is done manually by us, so this is only how many arguments there should be (attack strength)
    }

    @Override
    protected void loadArguments(ILogger logger, OptionalArguments arguments) {
        getAttackStrength().setValue(arguments.useNext().getAsInteger());
        getWeaponMask().setValue(kcDamageType.getValueFromArguments(arguments));
    }

    @Override
    protected void saveArguments(ILogger logger, OptionalArguments arguments, kcScriptDisplaySettings settings) {
        arguments.createNext().setAsInteger(getAttackStrength().getAsInteger());
        kcDamageType.addFlags(getWeaponMask().getAsInteger(), arguments);
    }

    @Override
    public void printWarnings(ILogger logger) {
        super.printWarnings(logger);
        int attackStrength = getAttackStrength().getAsInteger();
        int weaponMask = getWeaponMask().getAsInteger();
        if (attackStrength == 0) {
            printWarning(logger, "the attackStrength is zero!");
        } else if (weaponMask == 0) { // This even occurs when negative attack strength (healing) provided.
            // kcCActor::OnDamage() verifies that weaponMask != 0 before applying or healing.
            printWarning(logger, "no weaponMask (damage flags) were specified!");
        } else if (attackStrength >= 0) { // When the attackStrength is less than zero, it will cause healing, which doesn't do the immunity test. (But the weaponMask still must not be zero!)
            // Test that the target entity can even receive the damage.
            kcActorBaseDesc actorDesc = getExecutor() != null ? getExecutor().getExecutingActorBaseDescription() : null;
            if (actorDesc instanceof kcActorDesc) {
                int immuneMask = ((kcActorDesc) actorDesc).getHealth().getImmuneMask();
                int overlappingMask = (weaponMask & immuneMask);
                if (overlappingMask != 0)
                    printWarning(logger, "the target (" + actorDesc.getResource().getName() + ") is immune to the damage flags: " + kcDamageType.getFlagsAsString(overlappingMask));
            }
        }
    }

    /**
     * Gets the kcParam holding the weapon mask value.
     */
    public kcParam getWeaponMask() {
        switch (getActionID()) {
            case DAMAGE:
                return getOrCreateParam(0);
            case GIVE_DAMAGE:
                return getOrCreateParam(1);
            default:
                throw new RuntimeException("Unsupported kcActionID: " + getActionID());
        }
    }

    /**
     * Gets the kcParam holding the attack strength value.
     */
    public kcParam getAttackStrength() {
        switch (getActionID()) {
            case DAMAGE:
                return getOrCreateParam(1);
            case GIVE_DAMAGE:
                return getOrCreateParam(0);
            default:
                throw new RuntimeException("Unsupported kcActionID: " + getActionID());
        }
    }
}
