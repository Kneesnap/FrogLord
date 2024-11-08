package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.data.GameData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter.IMultiLineInfoWriter;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScript;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptList;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.manager.GreatQuestEntityManager;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.manager.entity.GreatQuestMapEditorEntityDisplay;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.Map;

/**
 * Represents the 'kcEntityInst' struct.
 * Created by Kneesnap on 8/24/2023.
 */
@Getter
@Setter
public class kcEntityInst extends GameData<GreatQuestInstance> implements IMultiLineInfoWriter {
    private final kcCResourceEntityInst resource;
    private final GreatQuestHash<kcCResourceGeneric> descriptionRef; // Resolved by kcCGameSystem::CreateInstance(), kcCGameSystem::CreateInstance(), kcCEntity::Reset, kcCEntity::Init
    private int priority = 1; // Usually 1, but sometimes two. This is used by kcCEntityMsgStore::RouteMessage() for placement in a priority queue. Basically, it allows overriding the order messages are processed/stored as.
    private int scriptIndex = -1;
    private final GreatQuestHash<kcCResourceEntityInst> targetEntityRef; // Observed both in raw data, but also kcCEntity::OnCommand[action=9], kcCEntity::ResetInt.

    public static final int SIZE_IN_BYTES = 28;
    public static final String PLAYER_ENTITY_NAME = "FrogInst001";
    public static final int PLAYER_ENTITY_HASH = GreatQuestUtils.hash(PLAYER_ENTITY_NAME);

    public kcEntityInst(kcCResourceEntityInst resource) {
        super(resource.getGameInstance());
        this.resource = resource;
        this.descriptionRef = new GreatQuestHash<>();
        this.targetEntityRef = new GreatQuestHash<>(PLAYER_ENTITY_NAME); // Everything seems to target FrogInst001 by default.
    }

    @Override
    public void load(DataReader reader) {
        reader.skipInt(); // Size in bytes.
        int descriptionHash = reader.readInt();
        reader.skipInt(); // Runtime pointer to description.
        this.priority = reader.readInt();
        reader.skipBytesRequireEmpty(Constants.INTEGER_SIZE); // group. Seems to always be zero. Used by kcCGameSystem::Insert(kcCGameSystem*, kcCEntity*)
        this.scriptIndex = reader.readInt();
        int targetEntityHash = reader.readInt();

        GreatQuestUtils.resolveResourceHash(kcCResourceGeneric.class, this.resource, this.descriptionRef, descriptionHash, this.resource == null || !this.resource.doesNameMatch("DummyParticleInst001", "clover-2ProxyDesc", "ConeTreeM-1Inst004", "Sbpile2Inst001")); // There are a handful of cases where this doesn't resolve, but in almost all situations it does resolve. Not sure how entities work when it doesn't resolve though.
        GreatQuestUtils.resolveResourceHash(kcCResourceEntityInst.class, this.resource, this.targetEntityRef, targetEntityHash, true);
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
        writer.writeInt(this.priority);
        writer.writeInt(0); // group. Seems to always be zero.
        writer.writeInt(this.scriptIndex);
        writer.writeInt(this.targetEntityRef.getHashNumber());
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
        GreatQuestChunkedFile.writeAssetLine(grid, chunkedFile, "Target Entity", this.targetEntityRef);
        grid.addSignedIntegerField("Priority", this.priority, newValue -> this.priority = newValue);
        grid.addSignedIntegerField("Script Index", this.scriptIndex, newValue -> this.scriptIndex = newValue);

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
            String name = entityDescription.getResource() != null ? entityDescription.getResource().getName() : null;
            grid.addBoldLabel("Description" + (name != null ? " '" + name + "'" : "") + " (" + Utils.getSimpleName(entityDescription) + "):");

            StringBuilder builder = new StringBuilder();
            entityDescription.writeMultiLineInfo(builder);
            for (String str : builder.toString().split(Constants.NEWLINE))
                grid.addNormalLabel(str);
        }
    }

    /**
     * Loads script functions from the config to this entity, creating a new script if necessary.
     * @param scriptList The script list to resolve/create scripts with.
     * @param config The config to load script functions from
     * @param sourceName The source name (usually a file name) representing where the scripts came from.
     */
    public void addScriptFunctions(kcScriptList scriptList, Config config, String sourceName) {
        if (scriptList == null)
            throw new NullPointerException("scriptList");
        if (config == null)
            throw new NullPointerException("config");

        // Get or create a script for ourselves.
        kcScript script;
        if (this.scriptIndex >= 0 && this.scriptIndex < scriptList.getScripts().size()) {
            script = scriptList.getScripts().get(this.scriptIndex);
        } else {
            this.scriptIndex = scriptList.getScripts().size();
            script = new kcScript(getGameInstance(), scriptList, this.resource, new ArrayList<>());
            scriptList.getScripts().add(script);
        }

        script.addFunctionsFromConfigNode(config, sourceName);
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
     * Sets up the main information to be edited.
     * @param grid the grid to create the UI inside
     */
    protected void setupMainEditor(GreatQuestEntityManager manager, GUIEditorGrid grid, GreatQuestMapEditorEntityDisplay entityDisplay) {
        GreatQuestChunkedFile.writeAssetLine(grid, manager.getMap(), "Entity Description", this.descriptionRef);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        GreatQuestChunkedFile chunkedFile = this.resource != null ? this.resource.getParentFile() : null;

        GreatQuestChunkedFile.writeAssetLine(chunkedFile, builder, padding, "Description", this.descriptionRef);
        builder.append(padding).append("Priority: ").append(this.priority).append(Constants.NEWLINE);
        builder.append(padding).append("Script Index: ").append(this.scriptIndex).append(Constants.NEWLINE);
        GreatQuestChunkedFile.writeAssetLine(chunkedFile, builder, padding, "Target Entity", this.targetEntityRef);
    }
}