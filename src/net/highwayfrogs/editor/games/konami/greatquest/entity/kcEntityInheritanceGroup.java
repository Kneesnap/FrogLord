package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcAction;
import net.highwayfrogs.editor.games.konami.greatquest.script.cause.kcScriptCause;
import net.highwayfrogs.editor.utils.Utils;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * Represents different inheritance groups of kcEntity types.
 * This is mainly used to match scripting behavior.
 * No such enum/data type exists in the debug symbols, this is understood from reverse engineering.
 * Created by Kneesnap on 11/6/2024.
 */
@RequiredArgsConstructor
public enum kcEntityInheritanceGroup {
    ENTITY("kcCEntity", Objects::nonNull),
    ENTITY3D("kcCEntity3D", Objects::nonNull),
    ACTOR_BASE("kcCActorBase", entityDesc -> entityDesc instanceof kcActorBaseDesc),
    ACTOR("kcCActor", entityDesc -> entityDesc instanceof kcActorDesc),
    CHARACTER("CCharacter", entityDesc -> entityDesc instanceof CharacterParams),
    ITEM("CItem", entityDesc -> entityDesc instanceof CItemDesc),
    WAYPOINT("kcCWaypoint", entityDesc -> entityDesc instanceof kcWaypointDesc),
    PROP_OR_CHARACTER("CProp or CCharacter", entityDesc -> entityDesc instanceof CPropDesc || entityDesc instanceof CharacterParams);

    @Getter private final String displayName;
    private final Predicate<kcEntity3DDesc> validator;

    /**
     * Test if the given entity instance is applicable to this group.
     * @param entity the entity to test
     */
    public boolean isApplicable(kcCResourceEntityInst entity) {
        return entity != null && isApplicable(entity.getInstance());
    }

    /**
     * Test if the given entity instance is applicable to this group.
     * @param entity the entity to test
     */
    public boolean isApplicable(kcEntityInst entity) {
        return entity != null && this.validator != null && this.validator.test(entity.getDescription());
    }

    /**
     * Test if the given entity description is applicable to this group.
     * @param entityDesc the entity description to test
     */
    public boolean isApplicable(kcEntity3DDesc entityDesc) {
        return entityDesc != null && this.validator != null && this.validator.test(entityDesc);
    }

    /**
     * Logs warnings about the target entity and its compatibility with this action.
     * @param logger the logger to log the warning to
     * @param scriptCause the script cause to print warnings for
     */
    public void logEntityTypeWarnings(Logger logger, kcScriptCause scriptCause, String subCauseDisplayName) {
        kcCResourceEntityInst entity = scriptCause.getScriptEntity();
        if (entity == null) {
            scriptCause.printWarning(logger, "will never occur because FrogLord could not find the script entity.");
            return;
        }

        kcEntityInst entityInst = entity.getInstance();
        kcEntity3DDesc entityDesc = entityInst != null ? entityInst.getDescription() : null;
        if (entityDesc == null) {
            scriptCause.printWarning(logger, "will never occur because FrogLord could not find the entity description for '" + entity.getName() + "'.");
            return;
        }

        if (!isApplicable(entityDesc))
            scriptCause.printWarning(logger, "will never occur because '" + subCauseDisplayName + "' requires the entity '" + entity.getName() + "' to extend " + this.displayName + ", but the entity was actually a " + Utils.getSimpleName(entityDesc) + ".");
    }

    /**
     * Logs warnings about the target entity and its compatibility with this action.
     * @param logger the logger to log the warning to
     * @param scriptCause the script cause to print warnings for
     */
    public void logEntityTypeWarnings(Logger logger, kcScriptCause scriptCause, GreatQuestHash<kcCResourceEntityInst> otherEntityRef, String subCauseDisplayName) {
        kcCResourceEntityInst entity = otherEntityRef != null ? otherEntityRef.getResource() : null;
        if (entity == null) {
            scriptCause.printWarning(logger, "will never occur because FrogLord could not find the entity referenced as an argument.");
            return;
        }

        kcEntityInst entityInst = entity.getInstance();
        kcEntity3DDesc entityDesc = entityInst != null ? entityInst.getDescription() : null;
        if (entityDesc == null) {
            scriptCause.printWarning(logger, "will never occur because FrogLord could not find the entity description for '" + entity.getName() + "'.");
            return;
        }

        if (!isApplicable(entityDesc))
            scriptCause.printWarning(logger, "will never occur because '" + subCauseDisplayName + "' requires the entity parameter '" + entity.getName() + "' to extend " + this.displayName + ", but the entity was actually a " + Utils.getSimpleName(entityDesc) + ".");
    }

    /**
     * Logs warnings about the target entity and its compatibility with this action.
     * @param logger the logger to log the warning to
     * @param action the action to print warnings for
     * @param relevantData a string to show as relevant to the reason it was skipped.
     */
    public void logEntityTypeWarnings(Logger logger, kcAction action, String relevantData) {
        if (action == null)
            throw new NullPointerException("action");

        kcEntity3DDesc entityDesc = action.getExecutor().getExecutingEntityDescription();
        if (entityDesc == null) {
            action.printWarning(logger, "FrogLord could not find the entity description for the script entity.");
            return;
        }

        if (!isApplicable(entityDesc))
            action.printWarning(logger, "'" + relevantData + "' requires the entity description '" + entityDesc.getResource().getName() + "' to extend " + this.displayName + ", but the entity was actually a " + Utils.getSimpleName(entityDesc) + ".");
    }
}