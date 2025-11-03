package net.highwayfrogs.editor.games.konami.greatquest.entity;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.konami.IConfigData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash.kcHashedResource;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntityFlag.kcEntityInstanceFlag;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric.IkcCResourceGenericTypeGroup;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric.kcCResourceGenericType;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcIGenericResourceData;
import net.highwayfrogs.editor.games.konami.greatquest.math.kcSphere;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.system.Config.ConfigValueNode;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.FileUtils.SavedFilePath;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;
import net.highwayfrogs.editor.utils.objects.StringNode;

import java.io.File;

/**
 * Represents the 'kcEntity3DDesc' struct.
 * Created by Kneesnap on 8/21/2023.
 */
@Getter
@Setter
public abstract class kcEntity3DDesc extends kcBaseDesc implements kcIGenericResourceData, IConfigData {
    @NonNull private final kcEntityDescType entityDescriptionType;
    private int defaultFlags; // This doesn't appear to be used, it seems to be the default flags value for which kcEntity3DInst will pull its default value from.
    // This is NOT the collision proxy, but is still used for collision purposes, especially checking for entities near each other.
    // NOTE: Waypoints with bounding boxes appear to ignore the bounding sphere, as massive bounding boxes such as the ones in Joy Towers have a radius of one.
    // Copied from kcEntity3DDesc::mSphere to kcCEntity3D::mSphere by kcCEntity3D::Init.
    // Places used:
    //  - kcCOTAEntity3D::Update (Generates a bounding box to refresh where in the oct tree the entity is placed)
    //  - kcCEntity3D::AngleToX/kcCEntity3D::AngleToY/kcCEntity3D::AngleToZ (Everything from checking to health bugs to AI wandering to seeking rotation to entities)
    //  - kcCEntity3D::Intersects (Tests if two entities bounding spheres overlap. Overridden in kcCWaypoint to handle OBB testing.)
    //  - kcCActorBase::SeekRotationToTarget
    //  - kcCOctTreeSceneMgr::RenderProjectedTextureShadow (Seems to be the position the shadow is placed at, although I'm not 100%)
    //  - kcCWaypoint::IntersectsExpanded
    //  - MonsterClass::Do_Guard (Seems to be unused code that wasn't optimized out. Not sure)
    //  - There are more, but I didn't think it was worth investigating.
    //  - But perhaps most interestingly, CCharacter::LookAtTarget. When the target entity's skeleton has 0 has no head target (eg: no bone named 'Bip01 Head' or 'Bip02 Head'), the target position will be the bounding sphere position.
    private final kcSphere boundingSphere = DEFAULT_BOUNDING_SPHERE.clone(); // Positioned relative to entity position. (Not on any bone)
    private static final int PADDING_VALUES = 3;
    private static final int PADDING_VALUES_3D = 4;
    private static final int CLASS_ID = GreatQuestUtils.hash("kcCEntity3D");
    private static final String ENTITY_DESC_FILE_PATH_KEY = "entityDescCfgFilePath";
    private static final SavedFilePath ENTITY_DESC_EXPORT_PATH = new SavedFilePath(ENTITY_DESC_FILE_PATH_KEY, "Select the directory to export the entity description to", Config.DEFAULT_FILE_TYPE);
    private static final SavedFilePath ENTITY_DESC_IMPORT_PATH = new SavedFilePath(ENTITY_DESC_FILE_PATH_KEY, "Select the directory to import entity description from", Config.DEFAULT_FILE_TYPE);
    private static final kcSphere DEFAULT_BOUNDING_SPHERE = new kcSphere(0, 0, 0, 1F);

    protected kcEntity3DDesc(@NonNull kcCResourceGeneric resource, @NonNull kcEntityDescType descriptionType) {
        super(resource);
        this.entityDescriptionType = descriptionType;
    }

    @Override
    protected int getTargetClassID() {
        return getEntityDescriptionType().getClassId().getClassId();
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);

        // Main Data
        this.defaultFlags = reader.readInt();
        reader.skipBytesRequireEmpty(PADDING_VALUES * Constants.INTEGER_SIZE);
        this.boundingSphere.load(reader);
        reader.skipBytesRequireEmpty(PADDING_VALUES_3D * Constants.INTEGER_SIZE);
    }

    @Override
    public void saveData(DataWriter writer) {
        // Main Data
        writer.writeInt(this.defaultFlags);
        writer.writeNull(PADDING_VALUES * Constants.INTEGER_SIZE);
        this.boundingSphere.save(writer);
        writer.writeNull(PADDING_VALUES_3D * Constants.INTEGER_SIZE);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        builder.append(padding).append("Flags: ").append(kcEntityInstanceFlag.getAsOptionalArguments(this.defaultFlags).getNamedArgumentsAsCommaSeparatedString()).append(Constants.NEWLINE);
        this.boundingSphere.writePrefixedMultiLineInfo(builder, "Bounding Sphere", padding);
    }

    @Override
    public kcCResourceGeneric getResource() {
        return (kcCResourceGeneric) super.getResource();
    }

    @Override
    public abstract kcCResourceGenericType getResourceType();

    @Override
    public void handleDoubleClick() {
        // Do nothing by default.
    }

    @Override
    public void setupRightClickMenuItems(ContextMenu contextMenu) {
        MenuItem exportEntityItem = new MenuItem("Export Entity Description");
        contextMenu.getItems().add(exportEntityItem);
        exportEntityItem.setOnAction(event -> {
            File outputFile = FileUtils.askUserToSaveFile(getGameInstance(), ENTITY_DESC_EXPORT_PATH, getResource().getName() + "." + Config.DEFAULT_EXTENSION, true);
            if (outputFile != null) {
                toConfig().saveTextFile(outputFile);
                getLogger().info("Saved '%s' as '%s'.", getResource().getName(), outputFile.getName());
            }
        });

        MenuItem importEntityItem = new MenuItem("Import Entity Description");
        contextMenu.getItems().add(importEntityItem);
        importEntityItem.setOnAction(event -> {
            File inputFile = FileUtils.askUserToOpenFile(getGameInstance(), ENTITY_DESC_IMPORT_PATH);
            if (inputFile == null)
                return;

            Config entityCfg = Config.loadConfigFromTextFile(inputFile, false);
            this.fromConfig(getLogger(), entityCfg);
            getLogger().info("Loaded '%s' from '%s'.", getResource().getName(), inputFile.getName());
        });
    }

    @Override
    public final void toConfig(Config output) {
        toConfig(output, getParentFile().createScriptDisplaySettings());
    }

    public static final String CONFIG_KEY_DESC_TYPE = "type";
    private static final String CONFIG_KEY_FLAGS = "defaultFlags";
    private static final String CONFIG_KEY_BOUNDING_SPHERE_POS = "boundingSpherePos";
    protected static final String CONFIG_KEY_BOUNDING_SPHERE_RADIUS = "boundingSphereRadius";

    @Override
    public void fromConfig(ILogger logger, Config input) {
        kcEntityDescType descType = input.getKeyValueNodeOrError(CONFIG_KEY_DESC_TYPE).getAsEnumOrError(kcEntityDescType.class);
        if (descType != getEntityDescriptionType())
            throw new RuntimeException("The entity description type was configured to be a(n) " + descType + ", but '" + getResourceName() + "' already exists as a(n) " + getEntityDescriptionType() + " description instead.");

        OptionalArguments arguments = OptionalArguments.parseCommaSeparatedNamedArguments(input.getKeyValueNodeOrError(CONFIG_KEY_FLAGS).getAsString());
        this.defaultFlags = kcEntityInstanceFlag.getValueFromArguments(arguments);
        arguments.warnAboutUnusedArguments(logger);

        ConfigValueNode boundingSpherePosNode = input.getOptionalKeyValueNode(CONFIG_KEY_BOUNDING_SPHERE_POS);
        if (boundingSpherePosNode != null) {
            this.boundingSphere.getPosition().parse(boundingSpherePosNode.getAsString());
        } else {
            this.boundingSphere.getPosition().setXYZ(DEFAULT_BOUNDING_SPHERE.getPosition());
        }

        ConfigValueNode boundingSphereRadiusNode = input.getOptionalKeyValueNode(CONFIG_KEY_BOUNDING_SPHERE_RADIUS);
        if (boundingSphereRadiusNode != null) {
            this.boundingSphere.setRadius(boundingSphereRadiusNode.getAsFloat());
        } else {
            this.boundingSphere.setRadius(DEFAULT_BOUNDING_SPHERE.getRadius());
        }
    }

    /**
     * Resolves a resource from a config node.
     * @param node the node to resolve the resource from
     * @param resourceClass the type of resource to resolve
     * @param hashObj the hash object to apply the result to
     * @param <TResource> the type of resource to resolve
     */
    protected <TResource extends kcHashedResource> void resolveResource(ILogger logger, StringNode node, Class<TResource> resourceClass, GreatQuestHash<TResource> hashObj) {
        GreatQuestUtils.resolveLevelResource(logger, node, resourceClass, getParentFile(), getResource(), hashObj, true);
    }

    /**
     * Resolves a resource from a config node.
     * @param node the node to resolve the resource from
     * @param resourceType the type of resource to resolve
     * @param hashObj the hash object to apply the result to
     */
    protected void resolveResource(ILogger logger, StringNode node, IkcCResourceGenericTypeGroup resourceType, GreatQuestHash<kcCResourceGeneric> hashObj) {
        GreatQuestUtils.resolveLevelResource(logger, node, resourceType, getParentFile(), getResource(), hashObj, true);
    }

    /**
     * Saves entity description information to a config node.
     * @param output the config node to save the data to
     * @param settings the settings to save with
     */
    public void toConfig(Config output, kcScriptDisplaySettings settings) {
        output.getOrCreateKeyValueNode(CONFIG_KEY_DESC_TYPE).setAsEnum(getEntityDescriptionType());
        output.getOrCreateKeyValueNode(CONFIG_KEY_FLAGS).setAsString(kcEntityInstanceFlag.getAsOptionalArguments(this.defaultFlags).getNamedArgumentsAsCommaSeparatedString());

        if (!DEFAULT_BOUNDING_SPHERE.getPosition().equals(this.boundingSphere.getPosition()))
            output.getOrCreateKeyValueNode(CONFIG_KEY_BOUNDING_SPHERE_POS).setAsString(this.boundingSphere.getPosition().toParseableString());
        if (DEFAULT_BOUNDING_SPHERE.getRadius() != this.boundingSphere.getRadius())
            output.getOrCreateKeyValueNode(CONFIG_KEY_BOUNDING_SPHERE_RADIUS).setAsFloat(this.boundingSphere.getRadius());
    }
}