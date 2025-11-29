package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.kcClassID;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A registry of different entityDesc types.
 * Created by Kneesnap on 11/9/2024.
 */
@RequiredArgsConstructor
public enum kcEntityDescType {
    COIN(kcClassID.COIN, "CCoinDesc", CCoinDesc::new),
    GEM(kcClassID.GEM, "CGemDesc", CGemDesc::new),
    CHARACTER(kcClassID.CHARACTER, "CharacterParams", CharacterParams::new),
    HONEY_POT(kcClassID.HONEY_POT, "CCoinDesc", CHoneyPotDesc::new),
    ITEM(kcClassID.ITEM, "CItemDesc", CItemDesc::new),
    MAGIC_STONE(kcClassID.MAGIC_STONE, "CMagicStoneDesc", CMagicStoneDesc::new),
    OBJ_KEY(kcClassID.OBJ_KEY, "CObjKeyDesc", CObjKeyDesc::new),
    PROP(kcClassID.PROP, "CPropDesc", CPropDesc::new),
    UNIQUE_ITEM(kcClassID.UNIQUE_ITEM, "CUniqueItemDesc", CUniqueItemDesc::new),
    ACTOR_BASE(kcClassID.ACTOR_BASE, "kcActorBaseDesc", kcActorBaseDesc::new),
    ACTOR(kcClassID.ACTOR, "kcActorDesc", kcActorDesc::new),
    PARTICLE_EMITTER(kcClassID.PARTICLE_EMITTER, "kcParticleEmitterParam", kcParticleEmitterParam::new), // NOTE: This cannot be used in kcCResourceEntityInst, see kcCResourceEntityInst::Prepare() to see that it will not work.
    WAYPOINT(kcClassID.WAYPOINT, "kcWaypointDesc", kcWaypointDesc::new);

    @Getter private final kcClassID classId;
    @Getter private final String descriptionName;
    private final Function<kcCResourceGeneric, kcEntity3DDesc> descriptionCreator;
    private final BiFunction<kcCResourceGeneric, kcEntityDescType, kcEntity3DDesc> descriptionTypeCreator;

    kcEntityDescType(kcClassID classId, String descriptionName, Function<kcCResourceGeneric, kcEntity3DDesc> descriptionCreator) {
        this(classId, descriptionName, descriptionCreator, null);
    }

    kcEntityDescType(kcClassID classId, String descriptionName, BiFunction<kcCResourceGeneric, kcEntityDescType, kcEntity3DDesc> descriptionCreator) {
        this(classId, descriptionName, null, descriptionCreator);
    }

    /**
     * Create a new description instance.
     * @param genericResource the generic resource to create the description for
     * @return newInstance
     */
    public kcEntity3DDesc createNewInstance(kcCResourceGeneric genericResource) {
        if (genericResource == null)
            throw new NullPointerException("genericResource");

        if (this.descriptionTypeCreator != null) {
            return this.descriptionTypeCreator.apply(genericResource, this);
        } else if (this.descriptionCreator != null) {
            return this.descriptionCreator.apply(genericResource);
        } else {
            throw new RuntimeException("Cannot create new instance of " + this + ".");
        }
    }
}