package net.highwayfrogs.editor.games.konami.greatquest.generic;

import javafx.scene.control.ContextMenu;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.KCResourceID;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResource;
import net.highwayfrogs.editor.games.konami.greatquest.entity.*;
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
import net.highwayfrogs.editor.utils.data.reader.ArraySource;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents a generic Frogger TGQ game chunk.
 * Holds data representing the 'kcCGeneric' data struct.
 * Created by Kneesnap on 3/23/2020.
 */
@Setter
@Getter
public class kcCResourceGeneric extends kcCResource {
    private kcIGenericResourceData resourceData;

    public kcCResourceGeneric(GreatQuestChunkedFile parentFile) {
        super(parentFile, KCResourceID.GENERIC);
    }

    public kcCResourceGeneric(GreatQuestChunkedFile parentFile, kcCResourceGenericType resourceType) {
        this(parentFile, resourceType, null);
    }

    public kcCResourceGeneric(GreatQuestChunkedFile parentFile, kcCResourceGenericType resourceType, kcClassID kcClass) {
        this(parentFile);
        if (resourceType != null)
            this.resourceData = createResourceDataObject(resourceType, kcClass != null ? kcClass.getClassId() : 0);
    }

    @Override
    protected String getExtraLoggerInfo() {
        if (this.resourceData != null) {
            return super.getExtraLoggerInfo() + "|" + getResourceType();
        } else {
            return super.getExtraLoggerInfo();
        }
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        int tag = reader.readInt();
        int sizeInBytes = reader.readInt();
        reader.skipPointer(); // Pointer.
        byte[] rawData = reader.readBytes(sizeInBytes);

        // Create & load the resource data object.
        kcCResourceGenericType resourceType = kcCResourceGenericType.getType(tag, true);
        if (resourceType == null) {
            getLogger().warning("Couldn't identify the kcCResourceGenericType for the tag " + NumberUtils.toHexString(tag) + ".");
            this.resourceData = new kcCResourceGenericDummy(this, tag, rawData);
        } else {
            this.resourceData = createResourceDataObject(resourceType, rawData);
            if (this.resourceData != null) {
                if (this.resourceData.getResourceType() != resourceType)
                    getLogger().warning("Attempted to create a resource of type " + resourceType + ", but the resulting resource reported itself as " + this.resourceData.getResourceType() + "!");

                // Load the resource data.
                DataReader resourceReader = new DataReader(new ArraySource(rawData));
                try {
                    this.resourceData.load(resourceReader);
                } catch (Throwable th) {
                    Utils.handleError(getLogger(), th, false, "Failed to load kcCResourceGeneric '%s' as %s.", getName(), resourceType);
                    this.resourceData = new kcCResourceGenericDummy(this, tag, rawData); // The resource is treated as raw dummy data now.
                }

                if (resourceReader.hasMore())
                    getLogger().warning("Resource '" + getName() + "'/" + resourceType + " read only " + resourceReader.getIndex() + " bytes, leaving " + resourceReader.getRemaining() + " unread.");
            } else {
                // The resource was recognized, but couldn't be created, usually because it's in an outdated format.
                this.resourceData = new kcCResourceGenericDummy(this, tag, rawData);
            }
        }
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeInt(getTag());
        int dataLengthPos = writer.writeNullPointer();
        writer.writeNullPointer(); // Pointer

        int dataStartIndex = writer.getIndex();
        this.resourceData.save(writer);
        writer.writeIntAtPos(dataLengthPos, writer.getIndex() - dataStartIndex);
    }

    @Override
    public void handleDoubleClick() {
        super.handleDoubleClick();
        if (this.resourceData != null)
            this.resourceData.handleDoubleClick();
    }

    @Override
    public void setupRightClickMenuItems(ContextMenu contextMenu) {
        super.setupRightClickMenuItems(contextMenu);
        if (this.resourceData != null)
            this.resourceData.setupRightClickMenuItems(contextMenu);
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("Generic Resource Type", getResourceType() + " (" + Utils.getSimpleName(this.resourceData) + ")");
        if (this.resourceData instanceof IPropertyListCreator)
            propertyList = ((IPropertyListCreator) this.resourceData).addToPropertyList(propertyList);

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
     * Gets the resource type representing the type of resource data.
     */
    public kcCResourceGenericType getResourceType() {
        return this.resourceData != null ? this.resourceData.getResourceType() : null;
    }

    /**
     * Gets the resource type tag.
     */
    public int getTag() {
        if (this.resourceData instanceof kcCResourceGenericDummy) {
            return ((kcCResourceGenericDummy) this.resourceData).getTag();
        } else {
            return getResourceType().getTag();
        }
    }

    /**
     * Get the resource as an entity description.
     * Returns null if this is not an entity description.
     */
    public kcEntity3DDesc getAsEntityDescription() {
        kcCResourceGenericType genericType = getResourceType();
        if (genericType == null)
            return null;

        switch (genericType) {
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
        return getResourceDataAs(kcCResourceGenericType.ACTOR_BASE_DESCRIPTION);
    }

    /**
     * Get the data in this resource as an emitter description, if it is one.
     */
    public kcEmitterDesc getAsEmitterDescription() {
        return getResourceDataAs(kcCResourceGenericType.EMITTER_DESCRIPTION);
    }

    /**
     * Get the data in this resource as an item description, if it is one.
     */
    public CItemDesc getAsItemDescription() {
        return getResourceDataAs(kcCResourceGenericType.ITEM_DESCRIPTION);
    }

    /**
     * Get the data in this resource as launcher params, if it is one.
     */
    public LauncherParams getAsLauncherParams() {
        return getResourceDataAs(kcCResourceGenericType.LAUNCHER_DESCRIPTION);
    }

    /**
     * Get the data in this resource as a model description, if it is one.
     */
    public kcModelDesc getAsModelDescription() {
        return getResourceDataAs(kcCResourceGenericType.MODEL_DESCRIPTION);
    }

    /**
     * Get the data in this resource as a particle emitter param, if it is one.
     */
    public kcParticleEmitterParam getAsParticleEmitterParam() {
        return getResourceDataAs(kcCResourceGenericType.PARTICLE_EMITTER_PARAM);
    }

    /**
     * Get the data in this resource as a prop description, if it is one.
     */
    public CPropDesc getAsPropDescription() {
        return getResourceDataAs(kcCResourceGenericType.PROP_DESCRIPTION);
    }

    /**
     * Get the data in this resource as a proxy description, if it is one.
     */
    public kcProxyDesc getAsProxyDescription() {
        kcCResourceGenericType genericType = getResourceType();
        if (genericType == null)
            return null;

        switch (genericType) {
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
        return getResourceDataAs(kcCResourceGenericType.PROXY_CAPSULE_DESCRIPTION);
    }

    /**
     * Get the data in this resource as a proxy tri mesh description, if it is one.
     */
    public kcProxyTriMeshDesc getAsProxyTriMeshDescription() {
        return getResourceDataAs(kcCResourceGenericType.PROXY_TRI_MESH_DESCRIPTION);
    }

    /**
     * Get the data in this resource as a resource path, if it is one.
     */
    public kcCResourcePath getAsResourcePath() {
        return getResourceDataAs(kcCResourceGenericType.RESOURCE_PATH);
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
        return getResourceDataAs(kcCResourceGenericType.STRING_RESOURCE);
    }

    /**
     * Get the data in this resource as a waypoint description, if it is one.
     */
    public kcWaypointDesc getAsWaypointDescription() {
        return getResourceDataAs(kcCResourceGenericType.WAYPOINT_DESCRIPTION);
    }

    @SuppressWarnings("unchecked")
    private <T extends kcIGenericResourceData> T getResourceDataAs(kcCResourceGenericType desiredType) {
        if (desiredType == null)
            throw new NullPointerException("desiredType");

        kcCResourceGenericType genericType = getResourceType();
        if (genericType != desiredType)
            throw new RuntimeException("Expected the generic resource type to be " + desiredType + ", but was " + genericType + " instead.");

        return (T) this.resourceData;
    }

    private kcIGenericResourceData createResourceDataObject(kcCResourceGenericType resourceType, byte[] rawData) {
        int classID = rawData != null && rawData.length >= Constants.INTEGER_SIZE ? DataUtils.readIntFromBytes(rawData, 0) : 0;
        return createResourceDataObject(resourceType, classID);
    }

    private kcIGenericResourceData createResourceDataObject(kcCResourceGenericType resourceType, int classID) {
        kcClassID kcClass = kcClassID.getClassById(classID);

        switch (resourceType) {
            case ACTOR_BASE_DESCRIPTION:
                switch (kcClass) {
                    case ACTOR:
                        return new kcActorDesc(this, kcEntityDescType.ACTOR);
                    case ACTOR_BASE:
                        return new kcActorBaseDesc(this, kcEntityDescType.ACTOR_BASE);
                    case CHARACTER:
                        return new CharacterParams(this);
                    default:
                        throw new RuntimeException("Not sure what actor description class to use for " + NumberUtils.to0PrefixedHexString(classID) + "/" + kcClass + ".");
                }
            case EMITTER_DESCRIPTION:
                return new kcEmitterDesc(this);
            case ITEM_DESCRIPTION:
                switch (kcClass) {
                    case COIN:
                        return new CCoinDesc(this);
                    case HONEY_POT:
                        return new CHoneyPotDesc(this);
                    case GEM:
                        return new CGemDesc(this);
                    case MAGIC_STONE:
                        return new CMagicStoneDesc(this);
                    case OBJ_KEY:
                        return new CObjKeyDesc(this);
                    case UNIQUE_ITEM:
                        return new CUniqueItemDesc(this);
                    default:
                        throw new RuntimeException("Not sure what item description class to use for " + NumberUtils.to0PrefixedHexString(classID) + "/" + kcClass + ".");
                }
            case LAUNCHER_DESCRIPTION:
                return new LauncherParams(this);
            case MODEL_DESCRIPTION:
                return new kcModelDesc(this);
            case PARTICLE_EMITTER_PARAM:
                if (classID == kcClassID.PARTICLE_EMITTER.getAlternateClassId()) { // Impacts: "CoinGrab", "GooberHit", "UnderWaterSpit"
                    if (getSelfHash().getOriginalString() == null)
                        getSelfHash().setOriginalString(getName() + kcParticleEmitterParam.NAME_SUFFIX);

                    return null; // This appears to be old data in a format we don't understand. Data is unused.
                }

                return new kcParticleEmitterParam(this);
            case PROP_DESCRIPTION:
                return new CPropDesc(this);
            case PROXY_CAPSULE_DESCRIPTION:
                if (classID == kcClassID.PROXY_CAPSULE.getAlternateClassId()) // Impacts: Two unnamed files from Mushroom Valley.
                    return null; // This appears to be old data in a format we don't understand. (Likely outdated format)

                return new kcProxyCapsuleDesc(this);
            case PROXY_TRI_MESH_DESCRIPTION:
                return new kcProxyTriMeshDesc(this);
            case RESOURCE_PATH:
                return new kcCResourcePath(this);
            case STRING_RESOURCE:
                return new kcCResourceString(this);
            case WAYPOINT_DESCRIPTION:
                return new kcWaypointDesc(this);
            default:
                throw new UnsupportedOperationException("Unsupported kcCResourceGenericType: " + resourceType);
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