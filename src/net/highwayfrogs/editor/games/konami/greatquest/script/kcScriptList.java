package net.highwayfrogs.editor.games.konami.greatquest.script;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.KCResourceID;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResource;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcInterimScriptEffect;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcScriptListInterim;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcScriptTOC;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScript.kcScriptFunction;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.FileUtils.SavedFilePath;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a list of scripts.
 * The logic has been determined by reading kcCScriptMgr::Load() from the PS2 PAL version.
 * Created by Kneesnap on 6/26/2023.
 */
public class kcScriptList extends kcCResource {
    @Getter private final List<kcScript> scripts = new ArrayList<>();
    private transient kcScriptListInterim interim;

    public static final String GLOBAL_SCRIPT_NAME = "scriptdata";
    public static final int GLOBAL_SCRIPT_NAME_HASH = GreatQuestUtils.hash(GLOBAL_SCRIPT_NAME);
    private static final String SCRIPT_FILE_PATH_KEY = "scriptFilePath";
    private static final SavedFilePath SCRIPT_EXPORT_PATH = new SavedFilePath(SCRIPT_FILE_PATH_KEY, "Select the directory to export scripts to");
    private static final SavedFilePath SCRIPT_IMPORT_PATH = new SavedFilePath(SCRIPT_FILE_PATH_KEY, "Select the directory to import scripts from");

    public kcScriptList(GreatQuestChunkedFile parentFile) {
        super(parentFile, KCResourceID.RAW);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);

        // Read interim list.
        this.interim = new kcScriptListInterim(getParentFile());
        this.interim.load(reader);
    }

    @Override
    public void save(DataWriter writer) {
        loadScriptsFromInterim();
        super.save(writer);

        // Convert scripts to interim data.
        List<kcScriptTOC> entries = new ArrayList<>();
        List<Integer> statusData = new ArrayList<>();
        List<kcInterimScriptEffect> effects = new ArrayList<>();
        for (int i = 0; i < this.scripts.size(); i++) {
            kcScript script = this.scripts.get(i);

            // Create new entry.
            kcScriptTOC newEntry = new kcScriptTOC(script.calculateCauseTypes(), statusData.size(), script.getFunctions().size(), script.getEffectCount());
            entries.add(newEntry);

            // Add 'cause' & 'effect' data.
            for (int j = 0; j < script.getFunctions().size(); j++) {
                kcScriptFunction function = script.getFunctions().get(j);
                function.saveCauseData(statusData, effects.size() * kcInterimScriptEffect.SIZE_IN_BYTES);
                for (int k = 0; k < function.getEffects().size(); k++)
                    effects.add(function.getEffects().get(k).toInterimScriptEffect());
            }
        }

        // Write interim data.
        kcScriptListInterim interim = new kcScriptListInterim(getParentFile(), entries, statusData, effects);
        interim.save(writer);
    }

    /**
     * Loads the scripts from their interim format.
     * This should be called after the other chunks in the file, so that we can identify which scripts belong to which entities.
     */
    public void loadScriptsFromInterim() {
        if (this.interim == null)
            return;

        Map<Integer, kcCResourceEntityInst> entityScriptMapping = new HashMap<>();
        for (kcCResource testChunk : getParentFile().getChunks()) {
            if (!(testChunk instanceof kcCResourceEntityInst))
                continue;

            kcCResourceEntityInst resourceEntity = (kcCResourceEntityInst) testChunk;
            if (resourceEntity.getInstance() == null)
                continue;

            int scriptIndex = resourceEntity.getInstance().getScriptIndex();
            if (scriptIndex >= 0) {
                kcCResourceEntityInst oldEntity = entityScriptMapping.put(scriptIndex, resourceEntity);
                if (oldEntity != null)
                    throw new RuntimeException("There was more than one entity ('" + oldEntity.getName() + "' & '" + resourceEntity.getName() + "') attached to script index " + scriptIndex + "!");
            }
        }

        // Convert interim list to script list.
        this.scripts.clear();
        for (int i = 0; i < this.interim.getEntries().size(); i++)
            this.scripts.add(kcScript.loadScript(this.interim, this, entityScriptMapping.remove(i), this.interim.getEntries().get(i)));

        this.interim = null;
    }

    @Override
    public Image getCollectionViewIcon() {
        return ImageResource.WIN98_TERMINAL_16.getFxImage();
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("Scripts", this.scripts.size());
        return propertyList;
    }

    @Override
    public void setupRightClickMenuItems(ContextMenu contextMenu) {
        super.setupRightClickMenuItems(contextMenu);

        MenuItem exportScriptsItem = new MenuItem("Export Scripts");
        contextMenu.getItems().add(exportScriptsItem);
        exportScriptsItem.setOnAction(event -> {
            File scriptFolder = FileUtils.askUserToSelectFolder(getGameInstance(), SCRIPT_EXPORT_PATH);
            if (scriptFolder == null)
                return;

            int functions = 0;
            kcScriptDisplaySettings settings = getParentFile().createScriptDisplaySettings();
            for (int i = 0; i < this.scripts.size(); i++) {
                kcScript script = this.scripts.get(i);
                Config rootNode = script.toConfigNode(settings);
                rootNode.saveTextFile(new File(scriptFolder, rootNode.getSectionName() + "." + kcScript.EXTENSION));
                functions += rootNode.getChildConfigNodes().size();
            }

            getLogger().info("Saved " + this.scripts.size() + " script" + (this.scripts.size() != 1 ? "s" : "")
                    + " containing " + functions + " function" + (functions != 1 ? "s" : "") + " to a folder named '" + scriptFolder.getName() + "'.");
        });

        MenuItem clearScriptsItem = new MenuItem("Clear Scripts");
        contextMenu.getItems().add(clearScriptsItem);
        clearScriptsItem.setOnAction(event -> {
            if (!FXUtils.makePopUpYesNo("Are you sure you'd like to clear all the scripts in the level?"))
                return;

            // Ensure entities no longer have script indices. (Otherwise they will point to the wrong scripts when we add new ones.)
            for (int i = 0; i < this.scripts.size(); i++) {
                kcScript script = this.scripts.get(i);
                kcCResourceEntityInst entity = script.getEntity();
                if (entity != null && entity.getInstance() != null)
                    entity.getInstance().removeScriptIndex();
            }

            this.scripts.clear();
            getLogger().info("Cleared the script list.");
        });

        MenuItem importScriptsItem = new MenuItem("Import Scripts");
        contextMenu.getItems().add(importScriptsItem);
        importScriptsItem.setOnAction(event -> {
            File scriptFolder = FileUtils.askUserToSelectFolder(getGameInstance(), SCRIPT_IMPORT_PATH);
            if (scriptFolder == null)
                return;

            int filesImported = 0;
            for (File file : FileUtils.listFiles(scriptFolder)) {
                if (!file.getName().endsWith(kcScript.EXTENSION)) {
                    getLogger().warning("Skipping " + file.getName());
                    continue;
                }

                Config scriptCfg = Config.loadConfigFromTextFile(file, false);
                int entityNameHash = GreatQuestUtils.hash(scriptCfg.getSectionName());
                kcCResourceEntityInst entity = getParentFile().getResourceByHash(entityNameHash);
                if (entity == null) {
                    getLogger().warning("Skipping " + scriptCfg.getSectionName() + ", as the entity could not be resolved.");
                    continue;
                }

                kcEntityInst entityInst = entity.getInstance();
                if (entityInst == null) {
                    getLogger().warning("Skipping " + entity.getName() + " because the entity is not valid.");
                    continue;
                }

                filesImported++;
                entityInst.addScriptFunctions(this, scriptCfg, scriptCfg.getSectionName());
            }

            getLogger().info("Imported " + filesImported + " scripts.");
        });
    }

    /**
     * Writes the script list to a string builder.
     * @param level    The level to find any extra data from.
     * @param builder  The builder to write the script to.
     * @param settings The settings used to build the output.
     */
    public void toString(GreatQuestChunkedFile level, StringBuilder builder, kcScriptDisplaySettings settings) {
        for (int i = 0; i < this.scripts.size(); i++) {
            builder.append("// Script #").append(i + 1).append(":\n");
            this.scripts.get(i).toString(level, builder, settings);
            builder.append('\n');
        }
    }
}