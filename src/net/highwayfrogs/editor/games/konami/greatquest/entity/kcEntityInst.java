package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter.IMultiLineInfoWriter;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScript;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptList;
import net.highwayfrogs.editor.games.konami.greatquest.toc.kcCResourceEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.manager.GreatQuestEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

import java.util.Map;

/**
 * Represents the 'kcEntityInst' struct.
 * Created by Kneesnap on 8/24/2023.
 */
@Getter
@Setter
@NoArgsConstructor
public class kcEntityInst extends GameObject implements IMultiLineInfoWriter {
    private transient kcCResourceEntityInst resource;
    private int descriptionHash;
    private int priority;
    private int group;
    private int scriptIndex;
    private int targetEntityHash;

    public static final int SIZE_IN_BYTES = 28;

    public kcEntityInst(kcCResourceEntityInst resource) {
        this.resource = resource;
    }

    @Override
    public void load(DataReader reader) {
        reader.skipInt(); // Size in bytes.
        this.descriptionHash = reader.readInt();
        reader.skipInt(); // Runtime pointer to description.
        this.priority = reader.readInt();
        this.group = reader.readInt();
        this.scriptIndex = reader.readInt();
        this.targetEntityHash = reader.readInt();
    }

    @Override
    public final void save(DataWriter writer) {
        int sizePtr = writer.writeNullPointer();
        saveData(writer);
        writer.writeAddressAt(sizePtr, writer.getIndex() - sizePtr);
    }

    /**
     * Saves data to the writer.
     * @param writer The writer to save data to.
     */
    public void saveData(DataWriter writer) {
        writer.writeInt(this.descriptionHash);
        writer.writeInt(0); // Runtime pointer to description.
        writer.writeInt(this.priority);
        writer.writeInt(this.group);
        writer.writeInt(this.scriptIndex);
        writer.writeInt(this.targetEntityHash);
    }

    /**
     * Sets up an editor for the entity data.
     * @param grid the ui creator
     */
    public void setupEditor(GreatQuestEntityManager manager, GUIEditorGrid grid) {
        GreatQuestChunkedFile chunkedFile = this.resource != null ? this.resource.getParentFile() : null;
        setupMainEditor(manager, grid, chunkedFile);

        // Add basic entity data.
        GreatQuestChunkedFile.writeAssetLine(grid, chunkedFile, "Target Entity", this.targetEntityHash);
        grid.addIntegerField("Priority", this.priority, newValue -> this.priority = newValue, null);
        grid.addIntegerField("Group", this.group, newValue -> this.group = newValue, null);
        grid.addIntegerField("Script Index", this.scriptIndex, newValue -> this.scriptIndex = newValue, null);

        // Add script data, if it exists.
        kcScriptList scriptList = chunkedFile != null ? chunkedFile.getScriptList() : null;
        kcScript script = scriptList != null && this.scriptIndex >= 0 && scriptList.getScripts().size() > this.scriptIndex ? scriptList.getScripts().get(this.scriptIndex) : null;
        if (script != null) {
            grid.addSeparator();
            grid.addBoldLabel("Entity Script:");

            // Generate script string.
            Map<Integer, String> nameMap = manager.getMap().calculateLocalHashes();
            kcScriptDisplaySettings displaySettings = new kcScriptDisplaySettings(nameMap, true, true);
            GreatQuestUtils.addDefaultHashesToMap(nameMap);
            StringBuilder builder = new StringBuilder();
            script.toString(manager.getMap(), builder, displaySettings);
            for (String str : builder.toString().split("\n"))
                grid.addNormalLabel(str);
        }

        // Write entity description.
        kcCResourceGeneric entityDescription = chunkedFile != null ? chunkedFile.getResourceByHash(this.descriptionHash) : null;
        if (entityDescription != null) {
            kcEntity3DDesc entityDesc = entityDescription.getAsEntityDescription();
            grid.addSeparator();
            grid.addBoldLabel("Description '" + entityDescription.getName() + "':");

            if (entityDesc != null) {
                StringBuilder builder = new StringBuilder();
                entityDesc.writeMultiLineInfo(builder);
                for (String str : builder.toString().split(Constants.NEWLINE))
                    grid.addNormalLabel(str);
            }
        }
    }

    /**
     * Sets up the main information to be edited.
     * @param grid the grid to create the UI inside
     * @param chunkedFile the chunked file to perform hash lookup operations within.
     */
    protected void setupMainEditor(GreatQuestEntityManager manager, GUIEditorGrid grid, GreatQuestChunkedFile chunkedFile) {
        GreatQuestChunkedFile.writeAssetLine(grid, chunkedFile, "Entity Hash", this.descriptionHash);
        GreatQuestChunkedFile.writeAssetLine(grid, chunkedFile, "Entity Description", this.descriptionHash);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        GreatQuestChunkedFile chunkedFile = this.resource != null ? this.resource.getParentFile() : null;

        GreatQuestChunkedFile.writeAssetLine(chunkedFile, builder, padding, "Description", this.descriptionHash);
        builder.append(padding).append("Priority: ").append(this.priority).append(Constants.NEWLINE);
        builder.append(padding).append("Group: ").append(this.group).append(Constants.NEWLINE);
        builder.append(padding).append("Script Index: ").append(this.scriptIndex).append(Constants.NEWLINE);
        GreatQuestChunkedFile.writeAssetLine(chunkedFile, builder, padding, "Target Entity", this.targetEntityHash);
    }
}