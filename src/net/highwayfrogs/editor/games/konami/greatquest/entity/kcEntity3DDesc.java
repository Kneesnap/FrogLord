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
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

import java.io.File;

/**
 * Represents the 'kcEntity3DDesc' struct.
 * Created by Kneesnap on 8/21/2023.
 */
@Getter
@Setter
public abstract class kcEntity3DDesc extends kcBaseDesc implements kcIGenericResourceData, IConfigData {
    @NonNull private final kcEntityDescType entityDescriptionType;
    private int instanceFlags; // This doesn't appear to be used, it seems to be the default flags value for which kcEntity3DInst will pull its default value from.
    // This is NOT the collision proxy, but instead a bounding sphere to easily eliminate most collision candidates. kcCProxyCapsule::Intersect does the sphere check before doing the more expensive proxy tests.
    // NOTE: Waypoints with bounding boxes appear to ignore the bounding sphere, as massive bounding boxes such as the ones in Joy Towers have a radius of one.
    private final kcSphere boundingSphere = new kcSphere(0, 0, 0, 1F); // Positioned relative to entity position.
    private static final int PADDING_VALUES = 3;
    private static final int PADDING_VALUES_3D = 4;
    private static final int CLASS_ID = GreatQuestUtils.hash("kcCEntity3D");
    private static final String ENTITY_DESC_FILE_PATH_KEY = "entityDescCfgFilePath";
    private static final SavedFilePath ENTITY_DESC_EXPORT_PATH = new SavedFilePath(ENTITY_DESC_FILE_PATH_KEY, "Select the directory to export the entity description to", Config.DEFAULT_FILE_TYPE);
    private static final SavedFilePath ENTITY_DESC_IMPORT_PATH = new SavedFilePath(ENTITY_DESC_FILE_PATH_KEY, "Select the directory to import entity description from", Config.DEFAULT_FILE_TYPE);

    protected kcEntity3DDesc(@NonNull kcCResourceGeneric resource, @NonNull kcEntityDescType descriptionType) {
        super(resource);
        this.entityDescriptionType = descriptionType;
        this.boundingSphere.setRadius(1F); // Default radius is 1.
    }

    @Override
    protected int getTargetClassID() {
        return getEntityDescriptionType().getClassId().getClassId();
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);

        // Main Data
        this.instanceFlags = reader.readInt();
        reader.skipBytesRequireEmpty(PADDING_VALUES * Constants.INTEGER_SIZE);
        this.boundingSphere.load(reader);
        reader.skipBytesRequireEmpty(PADDING_VALUES_3D * Constants.INTEGER_SIZE);
    }

    @Override
    public void saveData(DataWriter writer) {
        // Main Data
        writer.writeInt(this.instanceFlags);
        writer.writeNull(PADDING_VALUES * Constants.INTEGER_SIZE);
        this.boundingSphere.save(writer);
        writer.writeNull(PADDING_VALUES_3D * Constants.INTEGER_SIZE);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        builder.append(padding).append("Flags: ").append(kcEntityInstanceFlag.getAsOptionalArguments(this.instanceFlags).getNamedArgumentsAsCommaSeparatedString()).append(Constants.NEWLINE);
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
            this.fromConfig(entityCfg);
            getLogger().info("Loaded '%s' from '%s'.", getResource().getName(), inputFile.getName());
        });
    }

    @Override
    public final void toConfig(Config output) {
        toConfig(output, getParentFile().createScriptDisplaySettings());
    }

    public static final String CONFIG_KEY_DESC_TYPE = "type";
    private static final String CONFIG_KEY_FLAGS = "flags";
    private static final String CONFIG_KEY_BOUNDING_SPHERE_POS = "boundingSpherePos";
    private static final String CONFIG_KEY_BOUNDING_SPHERE_RADIUS = "boundingSphereRadius";

    @Override
    public void fromConfig(Config input) {
        kcEntityDescType descType = input.getKeyValueNodeOrError(CONFIG_KEY_DESC_TYPE).getAsEnumOrError(kcEntityDescType.class);
        if (descType != getEntityDescriptionType())
            throw new RuntimeException("The entity description reported itself as " + descType + ", which is incompatible with " + getEntityDescriptionType() + ".");

        OptionalArguments arguments = OptionalArguments.parseCommaSeparatedNamedArguments(input.getKeyValueNodeOrError(CONFIG_KEY_FLAGS).getAsString());
        this.instanceFlags = kcEntityInstanceFlag.getValueFromArguments(arguments);
        arguments.warnAboutUnusedArguments(getResource().getLogger());

        this.boundingSphere.getPosition().parse(input.getKeyValueNodeOrError(CONFIG_KEY_BOUNDING_SPHERE_POS).getAsString());
        this.boundingSphere.setRadius(input.getKeyValueNodeOrError(CONFIG_KEY_BOUNDING_SPHERE_RADIUS).getAsFloat());
    }

    /**
     * Resolves a resource from a config node.
     * @param node the node to resolve the resource from
     * @param resourceClass the type of resource to resolve
     * @param hashObj the hash object to apply the result to
     * @param <TResource> the type of resource to resolve
     */
    protected <TResource extends kcHashedResource> void resolve(ConfigValueNode node, Class<TResource> resourceClass, GreatQuestHash<TResource> hashObj) {
        int nodeHash = GreatQuestUtils.getAsHash(node, hashObj.isNullZero() ? 0 : -1, hashObj);
        GreatQuestUtils.resolveResourceHash(resourceClass, getParentFile(), getResource(), hashObj, nodeHash, true);
    }

    /**
     * Saves entity description information to a config node.
     * @param output the config node to save the data to
     * @param settings the settings to save with
     */
    public void toConfig(Config output, kcScriptDisplaySettings settings) {
        output.getOrCreateKeyValueNode(CONFIG_KEY_DESC_TYPE).setAsEnum(getEntityDescriptionType());
        output.getOrCreateKeyValueNode(CONFIG_KEY_FLAGS).setAsString(kcEntityInstanceFlag.getAsOptionalArguments(this.instanceFlags).getNamedArgumentsAsCommaSeparatedString());

        output.getOrCreateKeyValueNode(CONFIG_KEY_BOUNDING_SPHERE_POS).setAsString(this.boundingSphere.getPosition().toParseableString());
        output.getOrCreateKeyValueNode(CONFIG_KEY_BOUNDING_SPHERE_RADIUS).setAsFloat(this.boundingSphere.getRadius());
    }
}