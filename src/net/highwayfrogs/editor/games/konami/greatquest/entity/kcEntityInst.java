package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.generic.data.GameData;
import net.highwayfrogs.editor.games.konami.IConfigData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.entity.CharacterParams.CharacterType;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric.kcCResourceGenericTypeGroup;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScript;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptList;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.manager.GreatQuestEntityManager;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.manager.entity.GreatQuestMapEditorEntityDisplay;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.components.propertylist.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.system.Config.ConfigValueNode;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.BasicWrappedLogger;
import net.highwayfrogs.editor.utils.objects.StringNode;

import java.util.ArrayList;
import java.util.Map;

/**
 * Represents the 'kcEntityInst' struct.
 * Created by Kneesnap on 8/24/2023.
 */
@Getter
public class kcEntityInst extends GameData<GreatQuestInstance> implements IConfigData, IPropertyListCreator {
    private final kcCResourceEntityInst resource;
    private final GreatQuestHash<kcCResourceGeneric> descriptionRef; // Resolved by kcCGameSystem::CreateInstance(), kcCGameSystem::CreateInstance(), kcCEntity::Reset, kcCEntity::Init
    @Setter private int scriptIndex = -1;
    private final GreatQuestHash<kcCResourceEntityInst> targetEntityRef; // Observed both in raw data, but also kcCEntity::OnCommand[action=9], kcCEntity::ResetInt.

    public static final int SIZE_IN_BYTES = 28;
    public static final String PLAYER_ENTITY_NAME = "FrogInst001";
    public static final int PLAYER_ENTITY_HASH = GreatQuestUtils.hash(PLAYER_ENTITY_NAME);
    public static final int DEFAULT_PRIORITY = 1;
    public static final int PLAYER_PRIORITY = 2;
    private static final StringNode DEFAULT_TARGET_ENTITY_NODE = new StringNode(PLAYER_ENTITY_NAME);

    public kcEntityInst(kcCResourceEntityInst resource) {
        super(resource.getGameInstance());
        this.resource = resource;
        this.descriptionRef = new GreatQuestHash<>();
        this.targetEntityRef = new GreatQuestHash<>(PLAYER_ENTITY_NAME); // Everything seems to target FrogInst001 by default.
    }

    @Override
    public ILogger getLogger() {
        return this.resource != null ? this.resource.getLogger() : super.getLogger();
    }

    @Override
    public void load(DataReader reader) {
        reader.skipInt(); // Size in bytes.
        int descriptionHash = reader.readInt();
        reader.skipInt(); // Runtime pointer to description.
        int readPriority = reader.readInt();
        reader.skipBytesRequireEmpty(Constants.INTEGER_SIZE); // group. Seems to always be zero. Used by kcCGameSystem::Insert(kcCGameSystem*, kcCEntity*)
        this.scriptIndex = reader.readInt();
        int targetEntityHash = reader.readInt();

        if (this.resource != null)
            GreatQuestUtils.resolveLevelResourceHash(kcCResourceGenericTypeGroup.ENTITY_DESCRIPTION, this.resource.getParentFile(), this.resource, this.descriptionRef, descriptionHash, !this.resource.doesNameMatch("DummyParticleInst001", "clover-2ProxyDesc", "ConeTreeM-1Inst004", "Sbpile2Inst001")); // There are a handful of cases where this doesn't resolve, but in almost all situations it does resolve. Not sure how entities work when it doesn't resolve though.
        GreatQuestUtils.resolveLevelResourceHash(kcCResourceEntityInst.class, this.resource, this.targetEntityRef, targetEntityHash, showTargetEntityWarning(targetEntityHash));

        // Test after resolving entity reference.
        int expectedPriority = calculatePriority();
        if (readPriority != expectedPriority)
            getLogger().warning("The entity's priority value was expected to be %d, but was actually read from the data to be %d!", expectedPriority, readPriority);
    }

    @Override
    public final void save(DataWriter writer) {
        int sizePtr = writer.writeNullPointer();
        saveData(writer);
        writer.writeIntAtPos(sizePtr, writer.getIndex() - sizePtr);
    }

    /**
     * Saves data to the writer.
     * @param writer The writer to save data to.
     */
    public void saveData(DataWriter writer) {
        writer.writeInt(this.descriptionRef.getHashNumber());
        writer.writeInt(0); // Runtime pointer to description.
        writer.writeInt(calculatePriority());
        writer.writeInt(0); // group. Seems to always be zero.
        writer.writeInt(this.scriptIndex);
        writer.writeInt(this.targetEntityRef.getHashNumber());
    }

    /**
     * Returns a value representing what FrogLord expects the entity priority to be.
     * For scripts, entities with larger priorities have their effects processed after all other entities, and only call their update function afterward too.
     * @return expectedEntityPriority
     */
    public int calculatePriority() {
        kcEntity3DDesc entityDescription = getDescription();

        // CCharacter::Init calls CFrogCtl::Init if the character type is PLAYER.
        // CFrogCtl::Init calls CFrogCtl::Reset, which forces the player's priority value to be 2.
        // This means the game always expects the player's priority value to be two, thus causing the player to be updated after all other entities.
        if (entityDescription instanceof CharacterParams && ((CharacterParams) entityDescription).getCharacterType() == CharacterType.PLAYER)
            return PLAYER_PRIORITY;

        return DEFAULT_PRIORITY;
    }

    /**
     * Gets the entity description
     * @return description or null
     */
    public kcEntity3DDesc getDescription() {
        return this.descriptionRef.getResource() != null ? this.descriptionRef.getResource().getAsEntityDescription() : null;
    }

    /**
     * Sets up an editor for the entity data.
     * @param grid the ui creator
     */
    public void setupEditor(GreatQuestEntityManager manager, GUIEditorGrid grid, GreatQuestMapEditorEntityDisplay entityDisplay) {
        GreatQuestChunkedFile chunkedFile = this.resource != null ? this.resource.getParentFile() : null;
        setupMainEditor(manager, grid, entityDisplay);

        // Add basic entity data.
        this.targetEntityRef.addEditorUI(grid, chunkedFile, "Target Entity", kcCResourceEntityInst.class);
        grid.addSignedIntegerField("Script Index", this.scriptIndex, newValue -> this.scriptIndex = newValue).setDisable(true);

        // Add script data, if it exists.
        kcScriptList scriptList = chunkedFile != null ? chunkedFile.getScriptList() : null;
        kcScript script = scriptList != null && this.scriptIndex >= 0 && scriptList.getScripts().size() > this.scriptIndex ? scriptList.getScripts().get(this.scriptIndex) : null;
        if (script != null) {
            grid.addSeparator();
            grid.addBoldLabel("Entity Script:");

            // Generate script string.
            Map<Integer, String> nameMap = manager.getMap().calculateLocalHashes();
            kcScriptDisplaySettings displaySettings = new kcScriptDisplaySettings(getGameInstance(), this.resource != null ? this.resource.getParentFile() : null, nameMap, true, true);
            GreatQuestUtils.addDefaultHashesToMap(nameMap);
            StringBuilder builder = new StringBuilder();
            script.toString(manager.getMap(), builder, displaySettings);
            grid.addTextArea(builder.toString());
        }

        // Write entity description.
        kcEntity3DDesc entityDescription = getDescription();
        if (entityDescription != null) {
            grid.addSeparator();
            String name = entityDescription.getResource() != null ? entityDescription.getResourceName() : null;
            grid.addBoldLabel("Description" + (name != null ? " '" + name + "'" : "") + " (" + Utils.getSimpleName(entityDescription) + "):");

            StringBuilder builder = new StringBuilder();
            entityDescription.createPropertyList().toStringChildEntries(builder, " ", 0);
            for (String str : builder.toString().split(Constants.NEWLINE))
                grid.addNormalLabel(str);
        }
    }

    /**
     * Loads script functions from the config to this entity, creating a new script if necessary.
     * @param logger the logger to write script information/warnings to
     * @param scriptList The script list to resolve/create scripts with.
     * @param config The config to load script functions from
     * @param sourceName The source name (usually a file name) representing where the scripts came from.
     * @param clearExistingFunctions if true, any existing functions will be wiped.
     * @param sharedScript Iff true, the script will be applied to more than one entity.
     */
    public void addScriptFunctions(ILogger logger, kcScriptList scriptList, Config config, String sourceName, boolean clearExistingFunctions, boolean sharedScript) {
        if (scriptList == null)
            throw new NullPointerException("scriptList");
        if (config == null)
            throw new NullPointerException("config");

        // Use a logger linked to the entity.
        if (logger == null) {
            logger = getResource() != null ? getResource().getLogger() : getLogger();
        } else if (getResource() != null) {
            ILogger entityLogger = getResource().getLogger();
            logger = new BasicWrappedLogger(logger, entityLogger.getName(), entityLogger.getLoggerInfo());
        }

        // Get or create a script for ourselves.
        kcScript script;
        if (this.scriptIndex >= 0 && this.scriptIndex < scriptList.getScripts().size()) {
            script = scriptList.getScripts().get(this.scriptIndex);
        } else {
            this.scriptIndex = scriptList.getScripts().size();
            script = new kcScript(getGameInstance(), scriptList, this.resource, new ArrayList<>());
            scriptList.getScripts().add(script);
        }

        if (clearExistingFunctions)
            script.getFunctions().clear();

        script.addFunctionsFromConfigNode(logger, config, sourceName, sharedScript);
    }

    /**
     * Gets the script for this entity from the script list.
     * @param scriptList The script list to get the script from.
     * @return script
     */
    public kcScript getScript(kcScriptList scriptList) {
        if (scriptList == null)
            throw new NullPointerException("scriptList");

        return this.scriptIndex >= 0 ? scriptList.getScripts().get(this.scriptIndex) : null;
    }

    /**
     * Detach the script from the entity.
     * No cleanup of any sort is performed on the script, use with caution.
     */
    public void removeScriptIndex() {
        this.scriptIndex = -1;
    }

    /**
     * Decrease the index of the script by one.
     * Used when a script previously in the list is removed, and indices need to be updated to reflect that change.
     */
    public void decrementScriptIndex() {
        this.scriptIndex--;
    }

    /**
     * Sets up the main information to be edited.
     * @param grid the grid to create the UI inside
     */
    protected void setupMainEditor(GreatQuestEntityManager manager, GUIEditorGrid grid, GreatQuestMapEditorEntityDisplay entityDisplay) {
        GreatQuestChunkedFile.writeAssetLine(grid, manager.getMap(), "Entity Description", this.descriptionRef);
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        GreatQuestChunkedFile parentFile = getResource() != null ? getResource().getParentFile() : null;
        this.descriptionRef.addToPropertyList(propertyList, "Entity Description", parentFile, kcCResourceGenericTypeGroup.ENTITY_DESCRIPTION);
        propertyList.add("Script ID", this.scriptIndex != -1 ? this.scriptIndex : "None");
        this.targetEntityRef.addToPropertyList(propertyList, "Target Entity", parentFile, kcCResourceEntityInst.class);
    }

    private static final String CONFIG_KEY_ENTITY_DESC = "description";
    private static final String CONFIG_KEY_TARGET_ENTITY = "targetEntity";
    public static final String CONFIG_SECTION_SCRIPT = "Script";

    @Override
    public void fromConfig(ILogger logger, Config input) {
        if (this.resource == null)
            throw new NullPointerException("this.resource");

        GreatQuestChunkedFile chunkedFile = this.resource.getParentFile();
        if (chunkedFile == null)
            throw new NullPointerException("chunkedFile");

        ConfigValueNode entityDescNode = input.getKeyValueNodeOrError(CONFIG_KEY_ENTITY_DESC);
        GreatQuestUtils.resolveLevelResource(logger, entityDescNode, kcCResourceGenericTypeGroup.ENTITY_DESCRIPTION, chunkedFile, this.resource, this.descriptionRef, true);

        StringNode targetEntityNode = input.getOptionalKeyValueNode(CONFIG_KEY_TARGET_ENTITY);
        if (targetEntityNode == null) // Default to FrogInst001 if not specified.
            targetEntityNode = DEFAULT_TARGET_ENTITY_NODE;

        GreatQuestUtils.resolveLevelResource(logger, targetEntityNode, kcCResourceEntityInst.class, chunkedFile, this.resource, this.targetEntityRef, true);
    }

    @Override
    public final void toConfig(Config output) {
        if (this.resource == null)
            throw new NullPointerException("this.resource");

        GreatQuestChunkedFile chunkedFile = this.resource.getParentFile();
        if (chunkedFile == null)
            throw new NullPointerException("chunkedFile");

        toConfig(output, chunkedFile.getScriptList(), chunkedFile.createScriptDisplaySettings());
    }

    /**
     * Writes the entity instance data to a config.
     * @param output the output configuration
     * @param scriptList the script list to update the script from
     * @param settings the script display settings to write entity information with
     */
    public void toConfig(Config output, kcScriptList scriptList, kcScriptDisplaySettings settings) {
        if (output == null)
            throw new NullPointerException("output");
        if (scriptList == null)
            throw new NullPointerException("scriptList");

        if (this.resource != null)
            output.setSectionName(this.resource.getName());

        output.getOrCreateKeyValueNode(CONFIG_KEY_ENTITY_DESC)
                .setComment("The name of the description (template) describing what kind of entity this is.")
                .setAsString(this.descriptionRef.getAsGqsString(settings));

        if (this.targetEntityRef.getHashNumber() != PLAYER_ENTITY_HASH) {
            output.getOrCreateKeyValueNode(CONFIG_KEY_TARGET_ENTITY)
                    .setComment("The entity to move towards or attack, or for use with AI.")
                    .setAsString(this.targetEntityRef.getAsGqsString(settings));
        }

        Config oldScript = output.getChildConfigByName(CONFIG_SECTION_SCRIPT);
        if (oldScript != null)
            output.removeChildConfig(oldScript);

        kcScript script = getScript(scriptList);
        if (script != null) {
            Config scriptData = script.toConfigNode(getLogger(), settings);
            scriptData.setSectionName(CONFIG_SECTION_SCRIPT);
            output.addChildConfig(scriptData);
        }
    }

    private boolean showTargetEntityWarning(int hash) {
        return (!"FrogInst001".equals(this.resource.getName()) || hash != 0xAF201672); // BarrelInst001.
    }
}