package net.highwayfrogs.editor.games.konami.greatquest.generic;

import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.data.GameData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.KCResourceID;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResource;
import net.highwayfrogs.editor.games.konami.greatquest.entity.*;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.kcClassID;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModelDesc;
import net.highwayfrogs.editor.games.konami.greatquest.proxy.kcEmitterDesc;
import net.highwayfrogs.editor.games.konami.greatquest.proxy.kcProxyCapsuleDesc;
import net.highwayfrogs.editor.games.konami.greatquest.proxy.kcProxyDesc;
import net.highwayfrogs.editor.games.konami.greatquest.proxy.kcProxyTriMeshDesc;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.Utils;

import java.util.function.BiFunction;

/**
 * Represents a generic Frogger TGQ game chunk.
 * Holds data representing the 'kcCGeneric' data struct.
 * Created by Kneesnap on 3/23/2020.
 */
public class kcCResourceGeneric extends kcCResource {
    private int tag = -1; // This is the ID identifying the type of resource this is.
    private byte[] rawResourceData;
    @Getter private GameData<GreatQuestInstance> cachedObject;

    public kcCResourceGeneric(GreatQuestChunkedFile parentFile) {
        super(parentFile, KCResourceID.GENERIC);
    }

    public kcCResourceGeneric(GreatQuestChunkedFile parentFile, kcCResourceGenericType resourceType) {
        this(parentFile);
        this.tag = resourceType.getTag();
        this.cachedObject = loadAsObject();
    }

    public kcCResourceGeneric(GreatQuestChunkedFile parentFile, kcCResourceGenericType resourceType, GameData<GreatQuestInstance> resource) {
        this(parentFile);
        this.tag = resourceType.getTag();
        this.cachedObject = resource;
    }

    @Override
    protected String getExtraLoggerInfo() {
        if (this.tag != -1) {
            return super.getExtraLoggerInfo() + "|" + getResourceType();
        } else {
            return super.getExtraLoggerInfo();
        }
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.tag = reader.readInt();
        int sizeInBytes = reader.readInt();
        reader.skipPointer(); // Pointer.
        this.rawResourceData = reader.readBytes(sizeInBytes);

        // Clear & load the cached object.
        this.cachedObject = null;
        loadAsObject();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeInt(this.tag);
        int dataLengthPos = writer.writeNullPointer();
        writer.writeNullPointer(); // Pointer

        if (this.cachedObject != null) {
            int dataWriteStart = writer.getIndex();
            this.cachedObject.save(writer);
            writer.writeIntAtPos(dataLengthPos, writer.getIndex() - dataWriteStart);
        } else if (this.rawResourceData != null) {
            writer.writeBytes(this.rawResourceData);
            writer.writeIntAtPos(dataLengthPos, this.rawResourceData.length);
        }
    }

    @Override
    public void handleDoubleClick() {
        if (this.cachedObject instanceof kcBaseDesc)
            ((kcBaseDesc) this.cachedObject).handleDoubleClick();
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("Generic Resource Type", getResourceType());
        if (this.cachedObject instanceof IPropertyListCreator)
            propertyList = ((IPropertyListCreator) this.cachedObject).addToPropertyList(propertyList);

        return propertyList;
    }

    @Override
    public String getCollectionViewDisplayName() {
        kcCResourceGenericType type = getResourceType();
        if (type != null && type.getDisplayName() != null) {
            return super.getCollectionViewDisplayName() + " (" + type.getDisplayName() + ")";
        } else {
            return super.getCollectionViewDisplayName();
        }
    }

    @Override
    public Image getCollectionViewIcon() {
        kcCResourceGenericType type = getResourceType();
        ImageResource imageResource = type != null ? type.getImageResource() : null;
        if (imageResource != null) {
            return imageResource.getFxImage();
        } else {
            return super.getCollectionViewIcon();
        }
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
        return loadAsObject(kcCResourceGenericType.ACTOR_BASE_DESCRIPTION, (resource, classID) -> {
            kcClassID kcClass = kcClassID.getClassById(classID);
            switch (kcClass) {
                case ACTOR:
                    return new kcActorDesc(resource);
                case ACTOR_BASE:
                    return new kcActorBaseDesc(resource);
                case CHARACTER:
                    return new CharacterParams(resource);
                default:
                    throw new RuntimeException("Not sure what actor description class to use for " + NumberUtils.to0PrefixedHexString(classID) + "/" + kcClass + ".");
            }
        });
    }

    /**
     * Get the data in this resource as an emitter description, if it is one.
     */
    public kcEmitterDesc getAsEmitterDescription() {
        return loadAsObject(kcCResourceGenericType.EMITTER_DESCRIPTION, (resource, classID) -> new kcEmitterDesc(resource));
    }

    /**
     * Get the data in this resource as an item description, if it is one.
     */
    public CItemDesc getAsItemDescription() {
        return loadAsObject(kcCResourceGenericType.ITEM_DESCRIPTION, (resource, classID) -> {
            kcClassID kcClass = kcClassID.getClassById(classID);
            switch (kcClass) {
                case COIN:
                    return new CCoinDesc(resource);
                case HONEY_POT:
                    return new CHoneyPotDesc(resource);
                case GEM:
                    return new CGemDesc(resource);
                case MAGIC_STONE:
                    return new CMagicStoneDesc(resource);
                case OBJ_KEY:
                    return new CObjKeyDesc(resource);
                case UNIQUE_ITEM:
                    return new CUniqueItemDesc(resource);
                default:
                    throw new RuntimeException("Not sure what item description class to use for " + NumberUtils.to0PrefixedHexString(classID) + "/" + kcClass + ".");
            }
        });
    }

    /**
     * Get the data in this resource as launcher params, if it is one.
     */
    public LauncherParams getAsLauncherParams() {
        return loadAsObject(kcCResourceGenericType.LAUNCHER_DESCRIPTION, (resource, classID) -> new LauncherParams(resource));
    }

    /**
     * Get the data in this resource as a model description, if it is one.
     */
    public kcModelDesc getAsModelDescription() {
        return loadAsObject(kcCResourceGenericType.MODEL_DESCRIPTION, (resource, classID) -> new kcModelDesc(resource));
    }

    /**
     * Get the data in this resource as a particle emitter param, if it is one.
     */
    public kcParticleEmitterParam getAsParticleEmitterParam() {
        return loadAsObject(kcCResourceGenericType.PARTICLE_EMITTER_PARAM, (resource, classID) -> {
            if (classID == kcClassID.PARTICLE_EMITTER.getAlternateClassId()) { // Impacts: "CoinGrab", "GooberHit", "UnderWaterSpit"
                if (resource.getSelfHash().getOriginalString() == null)
                    resource.getSelfHash().setOriginalString(resource.getName() + kcParticleEmitterParam.NAME_SUFFIX);

                return null; // This appears to be old data in a format we don't understand. Data is unused.
            }

            return new kcParticleEmitterParam(resource);
        });
    }

    /**
     * Get the data in this resource as a prop description, if it is one.
     */
    public CPropDesc getAsPropDescription() {
        return loadAsObject(kcCResourceGenericType.PROP_DESCRIPTION, (resource, classID) -> new CPropDesc(resource));
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
        return loadAsObject(kcCResourceGenericType.PROXY_CAPSULE_DESCRIPTION, (resource, classID) -> {
            if (classID == kcClassID.PROXY_CAPSULE.getAlternateClassId()) // Impacts: Two unnamed files from Mushroom Valley.
                return null; // This appears to be old data in a format we don't understand. (Likely outdated format)

            return new kcProxyCapsuleDesc(resource);
        });
    }

    /**
     * Get the data in this resource as a proxy tri mesh description, if it is one.
     */
    public kcProxyTriMeshDesc getAsProxyTriMeshDescription() {
        return loadAsObject(kcCResourceGenericType.PROXY_TRI_MESH_DESCRIPTION, (resource, classID) -> new kcProxyTriMeshDesc(resource));
    }

    /**
     * Get the data in this resource as a resource path, if it is one.
     */
    public kcCResourcePath getAsResourcePath() {
        return loadAsObject(kcCResourceGenericType.RESOURCE_PATH, (resource, classID) -> new kcCResourcePath(resource));
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
        return loadAsObject(kcCResourceGenericType.STRING_RESOURCE, (resource, classID) -> new kcCResourceString(resource.getGameInstance()));
    }

    /**
     * Get the data in this resource as a waypoint description, if it is one.
     */
    public kcWaypointDesc getAsWaypointDescription() {
        return loadAsObject(kcCResourceGenericType.WAYPOINT_DESCRIPTION, (resource, classID) -> new kcWaypointDesc(resource));
    }

    private <T extends GameData<GreatQuestInstance>> T loadAsObject(kcCResourceGenericType desiredType, BiFunction<kcCResourceGeneric, Integer, T> maker) {
        kcCResourceGenericType genericType = getResourceType();
        if (genericType != desiredType)
            throw new RuntimeException("Expected the generic resource type to be " + desiredType + ", but was " + genericType + " instead.");

        if (this.cachedObject != null)
            return (T) this.cachedObject;

        // Not all resources actually use this. But, it's helpful to provide it for the ones which do.
        int classID = this.rawResourceData != null && this.rawResourceData.length >= Constants.INTEGER_SIZE ? DataUtils.readIntFromBytes(this.rawResourceData, 0) : 0;

        T newObject = maker.apply(this, classID);
        if (newObject == null)
            return null;

        this.cachedObject = newObject;
        if (this.rawResourceData != null) {
            DataReader reader = new DataReader(new ArraySource(this.rawResourceData));
            try {
                newObject.load(reader);
            } catch (Throwable th) {
                Utils.handleError(getLogger(), th, false, "Failed to load kcCResourceGeneric '%s' as %s.", getName(), genericType);
            }

            if (reader.hasMore())
                getLogger().warning("Resource '" + getName() + "'/" + genericType + " read only " + reader.getIndex() + " bytes, leaving " + reader.getRemaining() + " unread.");
        }

        return newObject;
    }

    private GameData<GreatQuestInstance> loadAsObject() {
        if (this.cachedObject != null)
            return this.cachedObject;

        kcCResourceGenericType genericType = getResourceType();
        switch (genericType) {
            case ACTOR_BASE_DESCRIPTION:
                return getAsActorDescription();
            case EMITTER_DESCRIPTION:
                return getAsEmitterDescription();
            case ITEM_DESCRIPTION:
                return getAsItemDescription();
            case LAUNCHER_DESCRIPTION:
                return getAsLauncherParams();
            case MODEL_DESCRIPTION:
                return getAsModelDescription();
            case PARTICLE_EMITTER_PARAM:
                return getAsParticleEmitterParam();
            case PROP_DESCRIPTION:
                return getAsPropDescription();
            case PROXY_CAPSULE_DESCRIPTION:
                return getAsProxyCapsuleDescription();
            case PROXY_TRI_MESH_DESCRIPTION:
                return getAsProxyTriMeshDescription();
            case RESOURCE_PATH:
                return getAsResourcePath();
            case STRING_RESOURCE:
                return getAsStringResource();
            case WAYPOINT_DESCRIPTION:
                return getAsWaypointDescription();
            default:
                throw new UnsupportedOperationException("Unsupported kcCResourceGenericType: " + genericType);
        }
    }

    @Getter
    public enum kcCResourceGenericType {
        ACTOR_BASE_DESCRIPTION(ImageResource.GHIDRA_ICON_MONKEY_16, "Actor Entity Data", "ActorBaseDesc", 0x46E460D7), // Real Struct: kcActorBaseDesc
        EMITTER_DESCRIPTION(ImageResource.GHIDRA_ICON_MONKEY_16, "Emitter Entity Data", "EmitterDesc", 0x3224F1ED), // Real Struct: kcEmitterDesc
        ITEM_DESCRIPTION(ImageResource.GHIDRA_ICON_MONKEY_16, "Item Entity Data", "ItemDesc", 0xE23B225D), // Real Struct: CUniqueItemDesc
        LAUNCHER_DESCRIPTION(ImageResource.GHIDRA_ICON_LOCATION_OUT_16, "Launcher Entity Data", "LauncherDesc", 0x5E2E846B), // Real Struct: kcLauncherParams
        MODEL_DESCRIPTION(ImageResource.GEOMETRIC_SHAPES_16, "3D Model Identifier", "", 0x00000000), // Real Struct: "kcModelDesc". The class tag is created from string "", and it is not known why.
        PARTICLE_EMITTER_PARAM(ImageResource.GHIDRA_ICON_MONKEY_16, "Particle Emitter Entity Data", "ParticleParam", 0x5A800152), // Real Struct: kcParticleEmitterParam.
        PROP_DESCRIPTION(ImageResource.GHIDRA_ICON_MONKEY_16, "Prop Entity Data", "PropDesc", 0x7486225C), // Real Struct: CPropDesc
        PROXY_CAPSULE_DESCRIPTION(ImageResource.GEOMETRIC_SHAPES_16, "Collision Capsule Data", "ProxyCapsule", 0xF56C372A), // Real Struct: kcProxyCapsuleDesc
        PROXY_TRI_MESH_DESCRIPTION(ImageResource.GOURAUD_TRIANGLE_LIST_16, "Collision Mesh Data", "ProxyTriMesh", 0xE344C6D7), // Real Struct: kcProxyDesc but with an extra hash value at the end for the model file name. Let's call it kcProxyTriMeshDesc.
        RESOURCE_PATH(ImageResource.GHIDRA_ICON_OPEN_SMALL_FOLDER_16, "Resource File Path", "ResourcePath", 0x24592470), // It appears this is unused and there is no corresponding class/struct in debug symbols.
        STRING_RESOURCE(ImageResource.WIN98_HELP_FAQ_16, "String", 0x691277A3), // kcResUtil.cpp/kcCreateStringResource.
        WAYPOINT_DESCRIPTION(ImageResource.GHIDRA_ICON_FLAG_GREEN_16, "Waypoint Entity Data", "WaypointDesc", 0x9F9934B5); // Real Struct: kcWaypointDesc

        private final ImageResource imageResource;
        private final String displayName;
        private final String name;
        private final int tag; // The hashes are explicitly included in the code to make Ctrl + F searching for hashes feasible.

        kcCResourceGenericType(ImageResource imageResource, String displayName, int tag) {
            this(imageResource, displayName, null, tag);
        }

        kcCResourceGenericType(ImageResource imageResource, String displayName, String name, int tag) {
            this.imageResource = imageResource;
            this.displayName = displayName;
            this.name = name;
            this.tag = tag;
            if (name != null && GreatQuestUtils.hash(name) != tag)
                throw new RuntimeException("Generic Resource type '" + name + "'/" + name() + " hashes to " + NumberUtils.to0PrefixedHexString(GreatQuestUtils.hash(name)) + ", instead of " + NumberUtils.to0PrefixedHexString(tag));
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
                throw new RuntimeException("Couldn't determine the generic type from tag " + NumberUtils.to0PrefixedHexString(tag) + ".");
            return null;
        }
    }
}