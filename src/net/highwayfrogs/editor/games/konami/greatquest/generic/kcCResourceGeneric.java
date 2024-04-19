package net.highwayfrogs.editor.games.konami.greatquest.generic;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.entity.*;
import net.highwayfrogs.editor.games.konami.greatquest.kcClassID;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModelDesc;
import net.highwayfrogs.editor.games.konami.greatquest.proxy.kcEmitterDesc;
import net.highwayfrogs.editor.games.konami.greatquest.proxy.kcProxyCapsuleDesc;
import net.highwayfrogs.editor.games.konami.greatquest.proxy.kcProxyDesc;
import net.highwayfrogs.editor.games.konami.greatquest.proxy.kcProxyTriMeshDesc;
import net.highwayfrogs.editor.games.konami.greatquest.toc.KCResourceID;
import net.highwayfrogs.editor.games.konami.greatquest.toc.kcCResource;
import net.highwayfrogs.editor.utils.Utils;

import java.util.function.BiFunction;

/**
 * Represents a generic Frogger TGQ game chunk.
 * Holds data representing the 'kcCGeneric' data struct.
 * Created by Kneesnap on 3/23/2020.
 */
@Getter
@Setter
public class kcCResourceGeneric extends kcCResource {
    private int tag;
    private byte[] bytes;
    private GameData<GreatQuestInstance> cachedObject;

    public kcCResourceGeneric(GreatQuestChunkedFile parentFile) {
        super(parentFile, KCResourceID.GENERIC);
    }

    public kcCResourceGeneric(GreatQuestChunkedFile parentFile, kcCResourceGenericType resourceType, GameData<GreatQuestInstance> resource) {
        this(parentFile);
        this.tag = resourceType.getTag();
        this.cachedObject = resource;
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.tag = reader.readInt();
        int sizeInBytes = reader.readInt();
        reader.skipInt(); // Pointer.
        this.bytes = reader.readBytes(sizeInBytes);
    }

    /**
     * Gets the resource type tracked by this generic, if we recognize it.
     */
    public kcCResourceGenericType getResourceType() {
        return kcCResourceGenericType.getType(this.tag, true);
    }

    /**
     * Get the resource as an entity description.
     * Returns null if this is not an entity description.
     */
    public kcEntity3DDesc getAsEntityDescription() {
        switch (getResourceType()) {
            case ACTOR_BASE_DESCRIPTION:
                return getAsActorDescription();
            case ITEM_DESCRIPTION:
                return getAsItemDescription();
            case PARTICLE_EMITTER_PARAM:
                return getAsParticleEmitterParam();
            case PROP_DESCRIPTION:
                return getAsPropDescription();
            case WAYPOINT_DESCRIPTION:
                return getAsWaypointDescription();
            default:
                return null;
        }
    }

    /**
     * Get the data in this resource as launcher params, if it is launcher params.
     */
    public kcActorBaseDesc getAsActorDescription() {
        return loadAsObject(kcCResourceGenericType.ACTOR_BASE_DESCRIPTION, (gameInstance, classID) -> {
            kcClassID kcClass = kcClassID.getClassById(classID);
            switch (kcClass) {
                case ACTOR:
                    return new kcActorDesc(gameInstance);
                case ACTOR_BASE:
                    return new kcActorBaseDesc(gameInstance);
                case CHARACTER:
                    return new CharacterParams(gameInstance);
                default:
                    throw new RuntimeException("Not sure what actor description class to use for " + Utils.to0PrefixedHexString(classID) + "/" + kcClass + ".");
            }
        });
    }

    /**
     * Get the data in this resource as an emitter description, if it is one.
     */
    public kcEmitterDesc getAsEmitterDescription() {
        return loadAsObject(kcCResourceGenericType.EMITTER_DESCRIPTION, (gameInstance, classID) -> new kcEmitterDesc(gameInstance));
    }

    /**
     * Get the data in this resource as an item description, if it is one.
     */
    public CItemDesc getAsItemDescription() {
        return loadAsObject(kcCResourceGenericType.ITEM_DESCRIPTION, (gameInstance, classID) -> {
            kcClassID kcClass = kcClassID.getClassById(classID);
            switch (kcClass) {
                case COIN:
                    return new CCoinDesc(gameInstance);
                case HONEY_POT:
                    return new CHoneyPotDesc(gameInstance);
                case GEM:
                    return new CGemDesc(gameInstance);
                case MAGIC_STONE:
                    return new CMagicStoneDesc(gameInstance);
                case OBJ_KEY:
                    return new CObjKeyDesc(gameInstance);
                case UNIQUE_ITEM:
                    return new CUniqueItemDesc(gameInstance);
                default:
                    throw new RuntimeException("Not sure what item description class to use for " + Utils.to0PrefixedHexString(classID) + "/" + kcClass + ".");
            }
        });
    }

    /**
     * Get the data in this resource as launcher params, if it is one.
     */
    public LauncherParams getAsLauncherParams() {
        return loadAsObject(kcCResourceGenericType.LAUNCHER_DESCRIPTION, (gameInstance, classID) -> new LauncherParams(getParentFile()));
    }

    /**
     * Get the data in this resource as a model description, if it is one.
     */
    public kcModelDesc getAsModelDescription() {
        return loadAsObject(kcCResourceGenericType.MODEL_DESCRIPTION, (gameInstance, classID) -> new kcModelDesc(gameInstance));
    }

    /**
     * Get the data in this resource as a particle emitter param, if it is one.
     */
    public kcParticleEmitterParam getAsParticleEmitterParam() {
        return loadAsObject(kcCResourceGenericType.PARTICLE_EMITTER_PARAM, (gameInstance, classID) -> {
            if (classID == kcClassID.PARTICLE_EMITTER.getAlternateClassId())
                return null; // This appears to be old data in a format we don't understand. Data is unused. Example: 1st GEN chunk "CoinGrab" in Level 06 Fairy Town Spring.

            return new kcParticleEmitterParam(gameInstance);
        });
    }

    /**
     * Get the data in this resource as a prop description, if it is one.
     */
    public CPropDesc getAsPropDescription() {
        return loadAsObject(kcCResourceGenericType.PROP_DESCRIPTION, (gameInstance, classID) -> new CPropDesc(gameInstance));
    }

    /**
     * Get the data in this resource as a proxy description, if it is one.
     */
    public kcProxyDesc getAsProxyDescription() {
        switch (getResourceType()) {
            case PROXY_CAPSULE_DESCRIPTION:
                return getAsProxyCapsuleDescription();
            case PROXY_TRI_MESH_DESCRIPTION:
                return getAsProxyTriMeshDescription();
            default:
                return null;
        }
    }

    /**
     * Get the data in this resource as a proxy capsule description, if it is one.
     */
    public kcProxyCapsuleDesc getAsProxyCapsuleDescription() {
        return loadAsObject(kcCResourceGenericType.PROXY_CAPSULE_DESCRIPTION, (gameInstance, classID) -> {
            if (classID == kcClassID.PROXY_CAPSULE.getAlternateClassId())
                return null; // This appears to be old data in a format we don't understand. (Likely outdated format)

            return new kcProxyCapsuleDesc(gameInstance);
        });
    }

    /**
     * Get the data in this resource as a proxy tri mesh description, if it is one.
     */
    public kcProxyTriMeshDesc getAsProxyTriMeshDescription() {
        return loadAsObject(kcCResourceGenericType.PROXY_TRI_MESH_DESCRIPTION, (gameInstance, classID) -> new kcProxyTriMeshDesc(gameInstance));
    }

    /**
     * Get the data in this resource as a resource path, if it is one.
     */
    public kcCResourcePath getAsResourcePath() {
        return loadAsObject(kcCResourceGenericType.RESOURCE_PATH, (gameInstance, classID) -> new kcCResourcePath(gameInstance));
    }

    /**
     * Get the data in this resource as a string, if it is one.
     */
    public String getAsString() {
        return getAsStringResource().getValue();
    }

    /**
     * Get the data in this resource as a string resource, if it is one.
     */
    public kcCResourceString getAsStringResource() {
        return loadAsObject(kcCResourceGenericType.STRING_RESOURCE, (gameInstance, classID) -> new kcCResourceString(gameInstance));
    }

    /**
     * Get the data in this resource as a waypoint description, if it is one.
     */
    public kcWaypointDesc getAsWaypointDescription() {
        return loadAsObject(kcCResourceGenericType.WAYPOINT_DESCRIPTION, (gameInstance, classID) -> new kcWaypointDesc(gameInstance));
    }

    private <T extends GameData<GreatQuestInstance>> T loadAsObject(kcCResourceGenericType desiredType, BiFunction<GreatQuestInstance, Integer, T> maker) {
        kcCResourceGenericType genericType = getResourceType();
        if (genericType != desiredType)
            throw new RuntimeException("Expected the generic resource type to be " + desiredType + ", but was " + genericType + " instead.");

        if (this.cachedObject != null)
            return (T) this.cachedObject;

        DataReader reader = new DataReader(new ArraySource(this.bytes));
        reader.jumpTemp(reader.getIndex());
        int classID = reader.getRemaining() >= Constants.INTEGER_SIZE ? reader.readInt() : 0;
        reader.jumpReturn();

        T newObject = maker.apply(getGameInstance(), classID);
        if (newObject == null)
            return null;

        if (newObject instanceof kcBaseDesc) {
            ((kcBaseDesc) newObject).setParentFile(getParentFile());
            ((kcBaseDesc) newObject).setGenericResourceParent(this);
        }

        newObject.load(reader);
        this.cachedObject = newObject;
        if (reader.hasMore())
            System.out.println("Resource '" + getName() + "'/" + genericType + " read only " + reader.getIndex() + " bytes, leaving " + reader.getRemaining() + " unread.");

        return newObject;
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeInt(this.tag);
        writer.writeInt(this.bytes != null ? this.bytes.length : 0);
        writer.writeInt(0); // Pointer
        if (this.cachedObject != null) {
            this.cachedObject.save(writer);
        } else if (this.bytes != null) {
            writer.writeBytes(this.bytes);
        }
    }

    @Getter
    public enum kcCResourceGenericType {
        ACTOR_BASE_DESCRIPTION("ActorBaseDesc", 0x46E460D7), // Real Struct: kcActorBaseDesc
        EMITTER_DESCRIPTION("EmitterDesc", 0x3224F1ED), // Real Struct: kcEmitterDesc
        ITEM_DESCRIPTION("ItemDesc", 0xE23B225D), // Real Struct: CUniqueItemDesc
        LAUNCHER_DESCRIPTION("LauncherDesc", 0x5E2E846B), // Real Struct: kcLauncherParams
        MODEL_DESCRIPTION("", 0x00000000), // Real Struct: "kcModelDesc". The class tag is created from string "", and it is not known why.
        PARTICLE_EMITTER_PARAM("ParticleParam", 0x5A800152), // Real Struct: kcParticleEmitterParam.
        PROP_DESCRIPTION("PropDesc", 0x7486225C), // Real Struct: CPropDesc
        PROXY_CAPSULE_DESCRIPTION("ProxyCapsule", 0xF56C372A), // Real Struct: kcProxyCapsuleDesc
        PROXY_TRI_MESH_DESCRIPTION("ProxyTriMesh", 0xE344C6D7), // Real Struct: kcProxyDesc but with an extra hash value at the end for the model file name. Let's call it kcProxyTriMeshDesc.
        RESOURCE_PATH("ResourcePath", 0x24592470), // It appears this is unused and there is no corresponding class/struct in debug symbols.
        STRING_RESOURCE(0x691277A3), // kcResUtil.cpp/kcCreateStringResource.
        WAYPOINT_DESCRIPTION("WaypointDesc", 0x9F9934B5); // Real Struct: kcWaypointDesc


        private final String name;
        private final int tag;

        kcCResourceGenericType(int tag) {
            this(null, tag);
        }

        kcCResourceGenericType(String name, int tag) {
            this.name = name;
            this.tag = tag;
            if (name != null && GreatQuestUtils.hash(name) != tag)
                throw new RuntimeException("Generic Resource type '" + name + "'/" + name() + " hashes to " + Utils.to0PrefixedHexString(GreatQuestUtils.hash(name)) + ", instead of " + Utils.to0PrefixedHexString(tag));
        }

        /**
         * Gets the kcCResourceGenericKnownType corresponding to the provided tag.
         * @param tag       The tag to lookup.
         * @param allowNull If null is allowed.
         * @return genericType
         */
        public static kcCResourceGenericType getType(int tag, boolean allowNull) {
            for (int i = 0; i < values().length; i++) {
                kcCResourceGenericType type = values()[i];
                if (type.tag == tag)
                    return type;
            }

            if (!allowNull)
                throw new RuntimeException("Couldn't determine the generic type from tag " + Utils.to0PrefixedHexString(tag) + ".");
            return null;
        }
    }
}