package net.highwayfrogs.editor.games.konami.greatquest.chunks;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.IFileExport;
import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter.IMultiLineInfoWriter;
import net.highwayfrogs.editor.games.konami.greatquest.audio.SBRFile;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceNamedHash.HashTableEntry;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntity3DDesc;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntity3DInst;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntityDescType;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestArchiveFile;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestGameFile;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric.kcCResourceGenericType;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourcePath;
import net.highwayfrogs.editor.games.konami.greatquest.loading.kcLoadContext;
import net.highwayfrogs.editor.games.konami.greatquest.map.kcEnvironment;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModelDesc;
import net.highwayfrogs.editor.games.konami.greatquest.proxy.kcProxyDesc;
import net.highwayfrogs.editor.games.konami.greatquest.proxy.kcProxyDescType;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcAction;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcCActionSequence;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScript;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptList;
import net.highwayfrogs.editor.games.konami.greatquest.ui.GreatQuestChunkFileEditor;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.GreatQuestMapMesh;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.GreatQuestMapMeshController;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.utils.*;
import net.highwayfrogs.editor.utils.FileUtils.SavedFilePath;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Handles the Frogger TGQ TOC files. (They are maps, but may also have other data(?))
 * These files may appear sorted, but in PS2 PAL there's a little more to it. "\GameData\Level00Global\Text\globaltext.dat", "\GameData\Level07TreeKnowledge\Level\PS2_Level07.dat", etc, show that there may be more to it.
 * TODO: Over time we should start removing a lot of the export functionality in this class, as it will have been replaced.
 * Created by Kneesnap on 8/25/2019.
 */
public class GreatQuestChunkedFile extends GreatQuestArchiveFile implements IFileExport {
    private final List<kcCResource> chunks = new ArrayList<>();
    private final List<kcCResource> immutableChunks = Collections.unmodifiableList(this.chunks);

    private static final String RESOURCE_PATH_NAME = "chunkedResourceImportExportPath";
    public static final SavedFilePath RESOURCE_IMPORT_PATH = new SavedFilePath(RESOURCE_PATH_NAME, "Please select the folder with the assets to import");
    public static final SavedFilePath RESOURCE_EXPORT_PATH = new SavedFilePath(RESOURCE_PATH_NAME, "Please select the folder to export assets to");

    private static final Comparator<kcCResource> RESOURCE_ORDERING = Comparator
            .comparingInt((kcCResource resource) -> resource.getChunkType().ordinal()) // Sort by resource type.
            .thenComparing(kcCResource::getName, String.CASE_INSENSITIVE_ORDER); // Sort by name (case-insensitive, alphabetically)

    public GreatQuestChunkedFile(GreatQuestInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        this.chunks.clear();

        // Prepare chunks.
        Map<kcCResource, byte[]> cachedRawDataMap = new HashMap<>();
        kcCResourceTOC tocChunk = null;
        int tocPos = 0;
        while (reader.hasMore()) {
            String identifier = reader.readTerminatedString(4);
            int length = reader.readInt() + 0x20; // 0x20 and not 0x24 because we're reading from the start of the data, not the length.
            byte[] readBytes = reader.readBytes(Math.min(reader.getRemaining(), length));

            // Read chunk.
            KCResourceID readType = KCResourceID.getByMagic(identifier);
            kcCResource newChunk = createResource(readType, readBytes, identifier);

            if (newChunk instanceof kcCResourceTOC) {
                // If we encounter a table of contents, use it for reading the upcoming chunks!
                if (this.chunks.size() > 0)
                    throw new IllegalStateException("kcCResourceTOC was not the first chunk in the file!");

                tocChunk = (kcCResourceTOC) newChunk;
                tocChunk.loadFromRawBytes(readBytes);
            } else {
                cachedRawDataMap.put(newChunk, readBytes);
                this.chunks.add(newChunk);

                // Apply the hash from the table of contents.
                if (tocChunk != null && tocChunk.getHashes().size() > tocPos)
                    newChunk.getSelfHash().setHash(tocChunk.getHashes().get(tocPos++));
            }
        }

        // Read the chunks. (Chunk data reading occurs after all chunks have been read, in order to allow resolving of hashes into chunk object references, regardless of if the order they are read.)
        kcCResource lastChunk = null;
        for (int i = 0; i < this.chunks.size(); i++) {
            kcCResource chunk = this.chunks.get(i);
            if (!(chunk instanceof kcCResourceTOC)) {
                chunk.loadFromRawBytes(cachedRawDataMap.remove(chunk));
                if (lastChunk != null && RESOURCE_ORDERING.compare(chunk, lastChunk) < 0)
                    getLogger().warning("The chunk '" + chunk.getName() + "'/" + chunk.getHashAsHexString() + " was expected to be sorted before '" + lastChunk.getName() + "'/" + lastChunk.getHashAsHexString() + ", but it was found after it!");

                lastChunk = chunk;
            }
        }

        // Alert about chunks which are missing names.
        for (int i = 0; i < this.chunks.size(); i++) {
            kcCResource chunk = this.chunks.get(i);
            if (chunk.getSelfHash().getOriginalString() == null && !(chunk instanceof kcCActionSequence) && !kcCResource.DEFAULT_RESOURCE_NAME.equals(chunk.getName())) // Action sequences are skipped because at this point they are unburdened by reality.
                chunk.getLogger().warning("Name hash mismatch! Calculated: " + NumberUtils.to0PrefixedHexString(chunk.calculateHash()) + ", Real: " + chunk.getHashAsHexString());
        }

        // Now that all the data is prepared, load the scripts.
        kcScriptList scriptList = getScriptList();
        if (scriptList != null)
            scriptList.loadScriptsFromInterim();
    }

    @Override
    public void afterLoad1(kcLoadContext context) {
        super.afterLoad1(context);
        for (int i = 0; i < this.chunks.size(); i++)
            this.chunks.get(i).afterLoad1(context);
    }

    @Override
    public void afterLoad2(kcLoadContext context) {
        super.afterLoad2(context);
        for (int i = 0; i < this.chunks.size(); i++)
            this.chunks.get(i).afterLoad2(context);
    }

    @Override
    public void save(DataWriter writer) {
        // Write table of contents.
        kcCResourceTOC tableOfContents = new kcCResourceTOC(this);
        tableOfContents.update(this.chunks);
        writeChunk(writer, tableOfContents);

        // Write chunks.
        for (kcCResource chunk : this.chunks)
            writeChunk(writer, chunk);
    }

    private void writeChunk(DataWriter writer, kcCResource chunk) {
        writer.writeStringBytes(chunk.getChunkIdentifier());
        int lengthAddress = writer.writeNullPointer();

        // Write chunk data.
        writer.pushAnchorPoint(); // Ensure the data aligns properly and such.
        int dataStartIndex = writer.getIndex();
        chunk.save(writer);
        int dataEndIndex = writer.getIndex();
        writer.popAnchorPoint();

        // Write chunk length. (Must do after popping)
        writer.writeIntAtPos(lengthAddress, (dataEndIndex - dataStartIndex) - 0x20);
    }

    @Override
    public Image getCollectionViewIcon() {
        if (getSceneManager() != null) {
            return ImageResource.TREASURE_MAP_16.getFxImage();
        } else {
            return ImageResource.ZIPPED_FOLDER_16.getFxImage();
        }
    }

    @Override
    public GameUIController<?> makeEditorUI() {
        return loadEditor(getGameInstance(), new GreatQuestChunkFileEditor(getGameInstance()), this);
    }

    @Override
    public void handleDoubleClick() {
        openMeshViewer();
    }

    @Override
    public String getDefaultFolderName() {
        return "ChunkedDataFiles";
    }

    /**
     * Create a map of hash numbers to corresponding strings from files present in the chunks.
     * @return localHashes
     */
    public Map<Integer, String> calculateLocalHashes() {
        Map<Integer, String> nameMap = new HashMap<>();
        for (kcCResource testChunk : this.chunks) {
            String realName = testChunk.getSelfHash().getOriginalString();
            if (realName != null && (realName.length() > 0)) // We use the original strings here instead of the section names since we want the strings we use to resolve back to the expected hash values.
                nameMap.put(testChunk.getHash(), realName);

            if (testChunk instanceof kcCResourceNamedHash) {
                kcCResourceNamedHash namedHashChunk = (kcCResourceNamedHash) testChunk;
                for (HashTableEntry entry : namedHashChunk.getEntries())
                    nameMap.put(entry.getKeyHash(), entry.getKeyName());
            }
        }

        return nameMap;
    }

    @Override
    public String getExportFolderName() {
        return FileUtils.stripExtension(getExportName());
    }

    /**
     * Creates display settings for scripts.
     */
    public kcScriptDisplaySettings createScriptDisplaySettings() {
        // Build the name map.
        Map<Integer, String> nameMap = calculateLocalHashes();
        GreatQuestUtils.addDefaultHashesToMap(nameMap);
        return new kcScriptDisplaySettings(getGameInstance(), this, nameMap, true, true);
    }

    @Override
    public void exportToFolder(File folder) throws IOException {
        saveMapObj(folder); // TODO: Here's the slowdown.
        exportChunksToDirectory(folder);

        kcScriptList scriptList = getScriptList();
        kcScriptDisplaySettings settings = createScriptDisplaySettings();
        saveActionSequences(new File(folder, "sequences.txt"), settings);
        saveScripts(new File(folder, "scripts.txt"), settings);

        saveGenericText(new File(folder, "strings.txt"));
        saveAnimationSets(new File(folder, "animation-sets.txt"));
        saveAnimationSkeletons(new File(folder, "animation-skeletons.txt"));
        saveAnimationTracks(new File(folder, "animation-tracks.txt"));
        exportEntities(new File(folder, "entities"), scriptList, settings);
        exportEntityDescriptions(new File(folder, "entity-descriptions"));
        exportCollisionProxies(new File(folder, "proxy-descriptions.txt"));
        saveGenericEmitterInfo(new File(folder, "emitters.txt"));
        saveGenericLauncherInfo(new File(folder, "launchers.txt"));
        saveGenericResourcePaths(new File(folder, "resource-paths.txt"));
        saveGenericModelDescriptions(new File(folder, "model-descriptions.txt"));
        saveOctTreeSceneManager(new File(folder, "oct-tree-scene-manager.txt"));
        saveNamedResourceHashes(new File(folder, "named-hashes.txt"));
        saveInfo(new File(folder, "info.txt"));

        // Save hashes to file.
        Map<Integer, String> nameMap = settings.getNamesByHash();
        if (nameMap.size() > 0) {
            File hashFile = new File(folder, "hashes.txt");

            List<String> lines = nameMap.entrySet().stream()
                    .sorted(Comparator.comparingInt(Entry::getKey))
                    .map(entry -> NumberUtils.to0PrefixedHexString(entry.getKey()) + "=" + entry.getValue())
                    .collect(Collectors.toList());

            Files.write(hashFile.toPath(), lines);
        }
    }

    /**
     * Gets the resource chunks tracked by this chunked file.
     */
    public List<kcCResource> getChunks() {
        return this.immutableChunks;
    }

    /**
     * Opens the mesh viewer for viewing the map, if possible.
     */
    public boolean openMeshViewer() {
        if (getSceneManager() != null) {
            MeshViewController.setupMeshViewer(getGameInstance(), new GreatQuestMapMeshController(), new GreatQuestMapMesh(this));
            return true;
        } else {
            return false;
        }
    }

    /**
     * Creates a .obj file in the given folder of the map 3D model.
     * @param folder The folder to save in.
     */
    public void saveMapObj(File folder) {
        kcCResOctTreeSceneMgr mainModel = null;
        for (kcCResource testChunk : this.chunks)
            if (testChunk instanceof kcCResOctTreeSceneMgr)
                mainModel = (kcCResOctTreeSceneMgr) testChunk;

        if (mainModel != null) {
            try {
                mainModel.exportAsObj(folder, FileUtils.stripExtension(getExportName()));
            } catch (IOException ex) {
                throw new RuntimeException("Failed to export map to .obj", ex);
            }
        }
    }

    /**
     * Saves all the action sequences to a file.
     * @param file     The file to save to.
     * @param settings The settings to decompile the action sequences with.
     */
    public void saveActionSequences(File file, kcScriptDisplaySettings settings) {
        StringBuilder sequenceBuilder = new StringBuilder();
        for (kcCResource testChunk : this.chunks) {
            if (testChunk instanceof kcCActionSequence) {
                kcCActionSequence sequence = (kcCActionSequence) testChunk;

                sequenceBuilder.append(sequence.getName()).append(":\n");
                for (kcAction command : sequence.getActions()) {
                    sequenceBuilder.append(" - ");
                    command.toString(sequenceBuilder, settings);
                    sequenceBuilder.append('\n');
                }

                sequenceBuilder.append('\n');
            }
        }

        // Save sequences to folder.
        saveExport(file, sequenceBuilder);
    }

    /**
     * Gets a resource by its hash.
     * @param hash        The hash to lookup.
     * @param <TResource> The type of resource to return.
     * @return The resource found with the hash, or null.
     */
    @SuppressWarnings("unchecked")
    public <TResource extends kcCResource> TResource getResourceByHash(int hash) {
        if (hash == 0 || hash == -1)
            return null; // TOC chunks conflict since they don't have a hash / aren't loaded.

        for (int i = 0; i < this.chunks.size(); i++) {
            kcCResource resource = this.chunks.get(i);
            if (resource.getHash() == hash)
                return (TResource) resource;
        }

        return null;
    }

    /**
     * Gets the scene manager for this map, if it exists.
     */
    public kcEnvironment getEnvironment() {
        return getResourceByHash(kcEnvironment.LEVEL_RESOURCE_HASH);
    }

    /**
     * Gets the scene manager for this map, if it exists.
     */
    public kcCResOctTreeSceneMgr getSceneManager() {
        return getResourceByHash(kcCResOctTreeSceneMgr.LEVEL_RESOURCE_HASH);
    }

    /**
     * Gets the sound bank file corresponding to this chunked file, if there is one.
     * @return soundBankFile
     */
    public SBRFile getSoundBankFile() {
        String filePath = getFilePath();
        if (filePath == null)
            return null;

        // Extract numeric level ID from the file path.
        StringBuilder builder = new StringBuilder();
        boolean currentlyBuildingNumber = false;
        for (int i = 0; i < filePath.length(); i++) {
            char tempChar = filePath.charAt(i);
            if (Character.isDigit(tempChar)) {
                if (currentlyBuildingNumber) {
                    builder.append(tempChar);
                } else {
                    currentlyBuildingNumber = true;
                    builder.setLength(0);
                    builder.append(tempChar);
                }
            } else {
                currentlyBuildingNumber = false;
            }
        }

        if (builder.length() == 0)
            return null;

        String filePrefix = builder.append('.').toString();
        for (GreatQuestGameFile file : getGameInstance().getLooseFiles())
            if (file instanceof SBRFile && file.getFileName().startsWith(filePrefix))
                return (SBRFile) file;

        return null;
    }

    /**
     * Add a resource to the chunked file.
     * An exception will be thrown if it is not possible to add.
     * @param resource The resource to add.
     */
    public void addResource(kcCResource resource) {
        if (resource == null)
            throw new NullPointerException("resource");
        if (resource instanceof kcCResourceTOC)
            throw new IllegalArgumentException("Table of Contents chunks cannot be manually added to chunked files.");
        if (resource.getParentFile() != this)
            throw new IllegalArgumentException("Cannot add resource " + resource + ", as it belongs to a different chunked file! (" + (resource.getParentFile() != null ? resource.getParentFile().getFilePath() : "null") + ")");

        int resourceHash = resource.getHash();
        if (resourceHash == 0 || resourceHash == -1)
            throw new IllegalArgumentException("Cannot add resource " + resource + ", as its hash is invalid.");

        kcCResource conflictingResource = getResourceByHash(resourceHash);
        if (conflictingResource == resource)
            throw new IllegalArgumentException("Cannot add resource " + resource + ", as it is already registered.");
        if (conflictingResource != null)
            throw new IllegalArgumentException("Cannot add resource " + resource + ", as another resource (" + conflictingResource + ") has a conflicting hash.");

        int searchResult = Collections.binarySearch(this.chunks, resource, RESOURCE_ORDERING);
        if (searchResult >= 0) {
            conflictingResource = this.chunks.get(searchResult);
            throw new IllegalArgumentException("[Shouldn't Happen] Cannot add resource " + resource + ", as another resource (" + conflictingResource + ") has a conflicting hash.");
        }

        int insertionIndex = -(searchResult + 1);
        this.chunks.add(insertionIndex, resource);
        resource.onAddedToChunkFile();
    }

    /**
     * Removes a resource registered to this chunked file.
     * @param resource the resource to remove
     */
    public void removeResource(kcCResource resource) {
        if (resource == null)
            throw new NullPointerException("resource");
        if (resource instanceof kcCResourceTOC)
            throw new IllegalArgumentException("Table of Contents chunks cannot be manually removed from chunked files.");
        if (resource.getParentFile() != this)
            throw new IllegalArgumentException("Cannot remove resource " + resource + ", as it belongs to a different chunked file! (" + (resource.getParentFile() != null ? resource.getParentFile().getFilePath() : "null") + ")");

        int resourceIndex = Collections.binarySearch(this.chunks, resource, RESOURCE_ORDERING);
        if (resourceIndex < 0)
            throw new IllegalArgumentException("Cannot remove resource " + resource + ", as it does not appear to be registered in the chunk file.");

        kcCResource removedResource = this.chunks.remove(resourceIndex);
        if (removedResource != resource)
            throw new IllegalArgumentException("[Shouldn't happen] The resource we removed (" + removedResource + ") was not the one we expected to remove!! (" + resourceIndex + ")");

        removedResource.onRemovedFromChunkFile();
    }

    /**
     * Import entities from a folder.
     * @param folder The folder to import entity data from
     */
    public void importEntities(File folder) {
        if (folder == null)
            throw new NullPointerException("folder");

        List<Config> importConfigs = new ArrayList<>();
        for (File file : FileUtils.listFiles(folder)) {
            if (!file.isFile() || !file.getName().endsWith(Config.DEFAULT_EXTENSION))
                continue;

            Config entityCfg = Config.loadConfigFromTextFile(file, false);
            importConfigs.add(entityCfg);

            // Entities can reference each other, so it is important to create new instances for each entity before loading individual entity data.
            String entityName = entityCfg.getSectionName();
            kcCResourceEntityInst entityInst = getResourceByHash(GreatQuestUtils.hash(entityName));
            if (entityInst == null) {
                entityInst = new kcCResourceEntityInst(this);
                entityInst.setName(entityName, true);
                entityInst.setInstance(new kcEntity3DInst(entityInst));
                addResource(entityInst);
            }
        }

        for (Config entityCfg : importConfigs) {
            String entityName = entityCfg.getSectionName();
            kcCResourceEntityInst entityInst = getResourceByHash(GreatQuestUtils.hash(entityName));
            if (entityInst == null) // Should not happen.
                throw new RuntimeException("Could not find entity '" + entityName + "'.");

            if (entityInst.getInstance() == null)
                entityInst.setInstance(new kcEntity3DInst(entityInst));

            entityInst.getInstance().fromConfigIncludeScripts(entityCfg);
        }

        getLogger().info("Imported " + importConfigs.size() + " entity instances.");
    }

    /**
     * Export entities to a folder.
     * @param folder The folder to export entity data to
     */
    public void exportEntities(File folder, kcScriptList scriptList, kcScriptDisplaySettings settings) {
        if (folder == null)
            throw new NullPointerException("folder");

        int entityCount = 0;
        for (kcCResource testChunk : this.chunks) {
            if (!(testChunk instanceof kcCResourceEntityInst))
                continue;

            kcCResourceEntityInst entity = (kcCResourceEntityInst) testChunk;
            if (entity.getInstance() == null) {
                getLogger().warning("Skipping '" + entity.getName() + "', as the entity instance was null.");
                continue;
            }

            FileUtils.makeDirectory(folder);

            Config newEntityCfg = new Config(entity.getName());
            entity.getInstance().toConfig(newEntityCfg, scriptList, settings);
            newEntityCfg.saveTextFile(new File(folder, entity.getName() + "." + Config.DEFAULT_EXTENSION));
            entityCount++;
        }

        getLogger().info("Exported " + entityCount + " entity instances.");
    }

    /**
     * Saves all the scripts to a file.
     * @param file     The file to save to.
     * @param settings The settings to print the scripts with.
     */
    public void saveScripts(File file, kcScriptDisplaySettings settings) {
        File scriptFolder = new File(file.getParentFile(), "scripts/");

        StringBuilder scriptBuilder = new StringBuilder();
        for (kcCResource testChunk : this.chunks) {
            if (testChunk instanceof kcScriptList) {
                kcScriptList scriptList = (kcScriptList) testChunk;
                scriptBuilder.append("// Script List: '").append(scriptList.getName()).append("'\n");
                scriptList.toString(this, scriptBuilder, settings);
                scriptBuilder.append('\n');

                if (scriptList.getScripts().isEmpty())
                    continue;

                FileUtils.makeDirectory(scriptFolder);
                for (int i = 0; i < scriptList.getScripts().size(); i++) {
                    kcScript script = scriptList.getScripts().get(i);
                    Config rootNode = script.toConfigNode(settings);
                    rootNode.saveTextFile(new File(scriptFolder, rootNode.getSectionName() + "." + kcScript.EXTENSION));
                }
            }
        }

        // Save scripts to folder.
        saveExport(file, scriptBuilder);
    }

    /**
     * Gets the script list in this chunked file, if there is one.
     */
    public kcScriptList getScriptList() {
        kcScriptList scriptList = getResourceByHash(kcScriptList.GLOBAL_SCRIPT_NAME_HASH);
        if (scriptList != null)
            return scriptList;

        for (kcCResource testChunk : this.chunks) {
            if (testChunk instanceof kcScriptList) {
                if (scriptList != null)
                    throw new RuntimeException("There are multiple script lists in the level! ('" + scriptList.getName() + "', '" + testChunk.getName() + "')");
                scriptList = (kcScriptList) testChunk;
            }
        }

        return scriptList;
    }

    /**
     * Saves text strings found in generic chunks to a text file.
     * @param file The file to save the info to.
     */
    public void saveGenericText(File file) {
        StringBuilder builder = new StringBuilder();
        for (kcCResource chunk : this.chunks) {
            if (!(chunk instanceof kcCResourceGeneric))
                continue;

            kcCResourceGeneric generic = (kcCResourceGeneric) chunk;
            if (generic.getResourceType() != kcCResourceGenericType.STRING_RESOURCE)
                continue;

            builder.append(generic.getHashAsHexString())
                    .append("/'").append(generic.getName()).append("': ")
                    .append(generic.getAsString()).append(Constants.NEWLINE);
        }

        saveExport(file, builder);
    }

    private void writeData(StringBuilder builder, kcCResource resource, IMultiLineInfoWriter data) {
        builder.append(data != null ? data.getClass().getSimpleName() : "Unknown Format")
                .append(' ').append(resource.getName()).append('[')
                .append(resource.getHashAsHexString()).append("]:").append(Constants.NEWLINE);

        if (data == null) {
            builder.append(" This data is in an unknown (potentially outdated) format.")
                    .append(Constants.NEWLINE).append(Constants.NEWLINE);
            builder.append(" Resource: '").append(resource.getName()).append("' in ").append(getDebugName()).append(Constants.NEWLINE);
            return;
        }

        data.writeMultiLineInfo(builder, " ");
    }

    /**
     * Import entities from a folder.
     * @param folder The folder to import entity data from
     */
    public void importEntityDescriptions(File folder) {
        if (folder == null)
            throw new NullPointerException("folder");

        int importCount = 0;
        for (File file : FileUtils.listFiles(folder)) {
            if (!file.isFile() || !file.getName().endsWith(Config.DEFAULT_EXTENSION))
                continue;

            Config entityDescCfg = Config.loadConfigFromTextFile(file, false);
            kcEntityDescType descType = entityDescCfg.getKeyValueNodeOrError(kcEntity3DDesc.CONFIG_KEY_DESC_TYPE).getAsEnumOrError(kcEntityDescType.class);

            String entityDescName = entityDescCfg.getSectionName();
            kcCResourceGeneric generic = getResourceByHash(GreatQuestUtils.hash(entityDescName));
            if (generic == null) {
                generic = new kcCResourceGeneric(this);
                generic.setName(entityDescName, true);
                addResource(generic);
            }

            kcEntity3DDesc entityDesc = generic.getAsEntityDescription();
            if (entityDesc == null)
                generic.setResourceData(entityDesc = descType.createNewInstance(generic));

            entityDesc.fromConfig(entityDescCfg);
            importCount++;
        }

        getLogger().info("Imported " + importCount + " entity descriptions.");
    }

    /**
     * Saves entity descriptions found in generic chunks to text files.
     * @param folder The folder to save the descriptions to.
     */
    public void exportEntityDescriptions(File folder) {
        int exportCount = 0;
        for (kcCResource chunk : this.chunks) {
            if (!(chunk instanceof kcCResourceGeneric))
                continue;

            kcCResourceGeneric generic = (kcCResourceGeneric) chunk;
            kcEntity3DDesc entityDesc = generic.getAsEntityDescription();
            if (entityDesc != null) {
                Config outputCfg = new Config(generic.getName());
                entityDesc.toConfig(outputCfg);
                FileUtils.makeDirectory(folder);
                outputCfg.saveTextFile(new File(folder, generic.getName() + "." + Config.DEFAULT_EXTENSION));
                exportCount++;
            }
        }

        getLogger().info("Exported " + exportCount + " entity descriptions.");
    }

    /**
     * Import collision proxies from a folder.
     * @param folder The folder to import proxy data from
     */
    public void importCollisionProxies(File folder) {
        if (folder == null)
            throw new NullPointerException("folder");

        int importCount = 0;
        for (File file : FileUtils.listFiles(folder)) {
            if (!file.isFile() || !file.getName().endsWith(Config.DEFAULT_EXTENSION))
                continue;

            Config proxyDescCfg = Config.loadConfigFromTextFile(file, false);
            kcProxyDescType descType = proxyDescCfg.getKeyValueNodeOrError(kcProxyDesc.CONFIG_KEY_DESC_TYPE).getAsEnumOrError(kcProxyDescType.class);

            String proxyDescName = proxyDescCfg.getSectionName();
            kcCResourceGeneric generic = getResourceByHash(GreatQuestUtils.hash(proxyDescName));
            if (generic == null) {
                generic = new kcCResourceGeneric(this);
                generic.setName(proxyDescName, true);
                addResource(generic);
            }

            kcProxyDesc proxyDesc = generic.getAsProxyDescription();
            if (proxyDesc == null)
                generic.setResourceData(proxyDesc = descType.createNewInstance(generic));

            proxyDesc.fromConfig(proxyDescCfg);
            importCount++;
        }

        getLogger().info("Imported " + importCount + " collision proxies.");
    }


    /**
     * Saves collision proxies found in generic chunks to text files.
     * @param folder The folder to save the descriptions to.
     */
    public void exportCollisionProxies(File folder) {
        int exportCount = 0;
        for (kcCResource chunk : this.chunks) {
            if (!(chunk instanceof kcCResourceGeneric))
                continue;

            kcCResourceGeneric generic = (kcCResourceGeneric) chunk;
            kcProxyDesc proxyDesc = generic.getAsProxyDescription();
            if (proxyDesc != null) {
                Config outputCfg = new Config(generic.getName());
                proxyDesc.toConfig(outputCfg);
                FileUtils.makeDirectory(folder);
                outputCfg.saveTextFile(new File(folder, generic.getName() + "." + Config.DEFAULT_EXTENSION));
                exportCount++;
            }
        }

        getLogger().info("Exported " + exportCount + " collision proxies.");
    }

    /**
     * Saves launcher information found in generic chunks to a text file.
     * @param file The file to save the info to.
     */
    public void saveGenericLauncherInfo(File file) {
        StringBuilder builder = new StringBuilder();
        for (kcCResource chunk : this.chunks) {
            if (!(chunk instanceof kcCResourceGeneric))
                continue;

            kcCResourceGeneric generic = (kcCResourceGeneric) chunk;
            if (generic.getResourceType() != kcCResourceGenericType.LAUNCHER_DESCRIPTION)
                continue;

            writeData(builder, chunk, generic.getAsLauncherParams());
            builder.append(Constants.NEWLINE);
        }

        saveExport(file, builder);
    }

    /**
     * Saves emitter information found in generic chunks to a text file.
     * @param file The file to save the info to.
     */
    public void saveGenericEmitterInfo(File file) {
        StringBuilder builder = new StringBuilder();
        for (kcCResource chunk : this.chunks) {
            if (!(chunk instanceof kcCResourceGeneric))
                continue;

            kcCResourceGeneric generic = (kcCResourceGeneric) chunk;
            if (generic.getResourceType() != kcCResourceGenericType.EMITTER_DESCRIPTION)
                continue;

            writeData(builder, chunk, generic.getAsEmitterDescription());
            builder.append(Constants.NEWLINE);
        }

        saveExport(file, builder);
    }

    /**
     * Saves animation sets found to a text file.
     * @param file The file to save the info to.
     */
    public void saveAnimationSets(File file) {
        StringBuilder builder = new StringBuilder();
        for (kcCResource chunk : this.chunks) {
            if (!(chunk instanceof kcCResourceAnimSet))
                continue;

            kcCResourceAnimSet animSet = (kcCResourceAnimSet) chunk;
            builder.append(chunk.getName()).append('[').append(chunk.getHashAsHexString())
                    .append("]: ").append(Constants.NEWLINE);
            animSet.getAnimSetDesc().writeMultiLineInfo(builder, " ");
        }

        saveExport(file, builder);
    }

    /**
     * Saves animation skeletons found to a text file.
     * @param file The file to save the info to.
     */
    public void saveAnimationSkeletons(File file) {
        StringBuilder builder = new StringBuilder();
        for (kcCResource chunk : this.chunks) {
            if (!(chunk instanceof kcCResourceSkeleton))
                continue;

            kcCResourceSkeleton skeleton = (kcCResourceSkeleton) chunk;
            skeleton.writeMultiLineInfo(builder, "");
            builder.append(Constants.NEWLINE);
        }

        saveExport(file, builder);
    }

    /**
     * Saves animation tracks found to a text file.
     * @param file The file to save the info to.
     */
    public void saveAnimationTracks(File file) {
        StringBuilder builder = new StringBuilder();
        for (kcCResource chunk : this.chunks) {
            if (!(chunk instanceof kcCResourceTrack))
                continue;

            kcCResourceTrack track = (kcCResourceTrack) chunk;
            track.writeMultiLineInfo(builder, "");
            builder.append(Constants.NEWLINE);
        }

        saveExport(file, builder);
    }

    /**
     * Saves resource paths found in generic chunks to a text file.
     * @param file The file to save the info to.
     */
    public void saveGenericResourcePaths(File file) {
        StringBuilder builder = new StringBuilder();
        for (kcCResource chunk : this.chunks) {
            if (!(chunk instanceof kcCResourceGeneric))
                continue;

            kcCResourceGeneric generic = (kcCResourceGeneric) chunk;
            if (generic.getResourceType() != kcCResourceGenericType.RESOURCE_PATH)
                continue;

            kcCResourcePath resourcePath = generic.getAsResourcePath();
            builder.append(chunk.getName()).append('[').append(chunk.getHashAsHexString())
                    .append("]: ").append(resourcePath.getFilePath()).append(" ")
                    .append(NumberUtils.to0PrefixedHexString(resourcePath.getFileNameHash())).append(Constants.NEWLINE);
        }

        saveExport(file, builder);
    }

    /**
     * Saves model descriptions found in generic chunks to a text file.
     * @param file The file to save the info to.
     */
    public void saveGenericModelDescriptions(File file) {
        StringBuilder builder = new StringBuilder();
        for (kcCResource chunk : this.chunks) {
            if (!(chunk instanceof kcCResourceGeneric))
                continue;

            kcCResourceGeneric generic = (kcCResourceGeneric) chunk;
            if (generic.getResourceType() != kcCResourceGenericType.MODEL_DESCRIPTION)
                continue;

            kcModelDesc modelDesc = generic.getAsModelDescription();
            builder.append(chunk.getName()).append('[').append(chunk.getHashAsHexString()).append("], ");
            modelDesc.writeInfo(builder);
            builder.append(Constants.NEWLINE);
        }

        saveExport(file, builder);
    }

    public void saveOctTreeSceneManager(File file) {
        kcCResOctTreeSceneMgr sceneMgr = getSceneManager();
        if (sceneMgr == null)
            return;

        StringBuilder builder = new StringBuilder();
        builder.append(sceneMgr.getName()).append('[').append(sceneMgr.getHashAsHexString()).append("], ");
        sceneMgr.writeMultiLineInfo(builder, " ");
        saveExport(file, builder);
    }

    /**
     * Saves model descriptions found in generic chunks to a text file.
     * @param file The file to save the info to.
     */
    public void saveNamedResourceHashes(File file) {
        StringBuilder builder = new StringBuilder();
        for (kcCResource chunk : this.chunks) {
            if (!(chunk instanceof kcCResourceNamedHash))
                continue;

            kcCResourceNamedHash namedHash = (kcCResourceNamedHash) chunk;
            builder.append(chunk.getName()).append('[').append(chunk.getHashAsHexString()).append("]:").append(Constants.NEWLINE);
            namedHash.writeMultiLineInfo(builder, " ");
            builder.append(Constants.NEWLINE);
        }

        saveExport(file, builder);
    }

    /**
     * Saves information about the chunked file contents to a text file.
     * @param textFile The file to save the information to.
     */
    public void saveInfo(File textFile) {
        // Export Info.
        StringBuilder builder = new StringBuilder();
        builder.append("Chunked File Dump").append(Constants.NEWLINE);
        if (hasFilePath())
            builder.append("Name: ").append(getFilePath()).append(Constants.NEWLINE);

        builder.append("File ID: #").append(getArchiveIndex()).append(Constants.NEWLINE);
        builder.append("Name Hash: ").append(getHashAsHexString()).append(Constants.NEWLINE);
        builder.append("Has Compression: ").append(isCompressed()).append(Constants.NEWLINE);

        if (this.chunks.size() > 0) {
            // Write environment info
            for (kcCResource chunk : this.chunks) {
                if (!(chunk instanceof kcEnvironment))
                    continue;

                builder.append(Constants.NEWLINE);
                ((kcEnvironment) chunk).writePrefixedMultiLineInfo(builder, "kcEnvironment", "", " ");
            }

            builder.append(Constants.NEWLINE);
            builder.append("Chunks (");
            builder.append(this.chunks.size());
            builder.append("):");
            builder.append(Constants.NEWLINE);
            for (kcCResource chunk : this.chunks) {
                builder.append(" - [");
                builder.append(chunk.getHashAsHexString());

                int nameHash = GreatQuestUtils.hash(chunk.getName());
                if (nameHash != chunk.getHash()) { // Should be equivalent to !chunk.isHashBasedOnName()
                    builder.append("|");
                    builder.append(NumberUtils.to0PrefixedHexString(nameHash));
                }

                if (chunk instanceof kcCResourceGeneric) {
                    kcCResourceGeneric genericResource = (kcCResourceGeneric) chunk;
                    builder.append("|");
                    builder.append(genericResource.getResourceType());
                    if (genericResource.getResourceData() != null) {
                        builder.append("|");
                        builder.append(Utils.getSimpleName(genericResource.getResourceData()));
                    }
                } else {
                    builder.append("|");
                    builder.append(StringUtils.stripAlphanumeric(chunk.getChunkIdentifier()));
                    builder.append("|");
                    builder.append(chunk.getClass().getSimpleName());
                }

                builder.append("]: '");
                builder.append(chunk.getName());
                builder.append("', ");
                builder.append(chunk.getRawData() != null ? chunk.getRawData().length : 0);
                builder.append(" bytes");
                if (!chunk.isHashBasedOnName() && chunk.getSelfHash().getOriginalString() != null)
                    builder.append(" (").append(chunk.getSelfHash().getOriginalString()).append(')');

                builder.append(Constants.NEWLINE);
            }
        }

        saveExport(textFile, builder);
    }

    private static void saveExport(File target, StringBuilder builder) {
        if (builder == null || builder.length() == 0)
            return;

        try {
            Files.write(target.toPath(), Arrays.asList(builder.toString().split(Constants.NEWLINE)));
        } catch (IOException ex) {
            throw new RuntimeException("Failed to save data to '" + target + "'.", ex);
        }
    }

    /**
     * Exports this file and all of its chunks to the directory.
     * This makes debugging easier.
     * @param directory The folder to export chunks to.
     */
    @SneakyThrows
    public void exportChunksToDirectory(File directory) {
        Map<String, Integer> countMap = new HashMap<>();
        for (kcCResource chunk : this.chunks) {
            String signature = StringUtils.stripAlphanumeric(chunk.getChunkIdentifier());
            int count = countMap.getOrDefault(signature, 0) + 1;
            countMap.put(signature, count);

            // Check there is data.
            String fileName = NumberUtils.padNumberString(count, 3);
            if (chunk.getRawData() == null) {
                getLogger().warning("Skipping chunk with null data: '" + signature + "-" + fileName + "'.");
                continue;
            }

            // Create sub-directory.
            File subDirectory = new File(directory, signature);
            FileUtils.makeDirectory(subDirectory);

            // Save file contents.
            File outputFile = new File(subDirectory, fileName);
            if (!outputFile.exists())
                Files.write(outputFile.toPath(), chunk.getRawData());
        }
    }

    /**
     * Write the asset name to the builder to a single line.
     * @param file The chunked file to search for assets from.
     * @param builder The builder to write to.
     * @param padding The line padding data.
     * @param prefix The prefix to write.
     * @param hashObj The hash to lookup.
     */
    public static StringBuilder writeAssetLine(GreatQuestChunkedFile file, StringBuilder builder, String padding, String prefix, GreatQuestHash<?> hashObj) {
        return writeAssetInfo(file, builder, padding, prefix, hashObj != null ? hashObj.getHashNumber() : 0, kcCResource::getName).append(Constants.NEWLINE);
    }

    /**
     * Write asset information to the builder. The information written is specified via the function.
     * If the asset isn't found, the hash is written instead.
     * @param file         The chunked file to search for assets from.
     * @param builder      The builder to write to.
     * @param padding      The line padding data.
     * @param prefix       The prefix to write.
     * @param resourceHash The hash value to lookup.
     * @param getter       The function to turn the resource into a string.
     * @param <TResource>  The resource type to lookup.
     */
    public static <TResource extends kcCResource> StringBuilder writeAssetInfo(GreatQuestChunkedFile file, StringBuilder builder, String padding, String prefix, int resourceHash, Function<TResource, String> getter) {
        builder.append(padding).append(prefix).append(": ");

        TResource resource = GreatQuestUtils.findResourceByHash(file, file != null ? file.getGameInstance() : null, resourceHash);
        if (resource != null) {
            builder.append(getter.apply(resource));
        } else if (resourceHash != 0 && resourceHash != -1) {
            builder.append(NumberUtils.to0PrefixedHexString(resourceHash));
        } else {
            builder.append("None");
        }

        return builder;
    }

    /**
     * Write the asset name to the UI.
     * @param file The chunked file to search for assets from.
     * @param label The label to write.
     * @param hashObj The hash to lookup.
     */
    public static Label writeAssetLine(GUIEditorGrid grid, GreatQuestChunkedFile file, String label, GreatQuestHash<?> hashObj) {
        return writeAssetInfo(grid, file, label, hashObj != null ? hashObj.getHashNumber() : 0, kcCResource::getName);
    }

    /**
     * Write asset information to the UI. The information written is specified via the function.
     * If the asset isn't found, the hash is written instead.
     * @param file         The chunked file to search for assets from.
     * @param label        The label to write.
     * @param resourceHash The hash value to lookup.
     * @param getter       The function to turn the resource into a string.
     * @param <TResource>  The resource type to lookup.
     */
    public static <TResource extends kcCResource> Label writeAssetInfo(GUIEditorGrid grid, GreatQuestChunkedFile file, String label, int resourceHash, Function<TResource, String> getter) {
        String resourceName = null;
        TResource resource = GreatQuestUtils.findResourceByHash(file, file != null ? file.getGameInstance() : null, resourceHash);
        if (resource != null)
            resourceName = getter.apply(resource);

        return grid.addLabel(label + ":", (resourceName != null ? resourceName : "Not Found") + " (" + NumberUtils.to0PrefixedHexString(resourceHash) + ")");
    }

    @Override
    public void setupRightClickMenuItems(ContextMenu contextMenu) {
        super.setupRightClickMenuItems(contextMenu);

        boolean hasWorld = getSceneManager() != null;

        if (hasWorld) {
            MenuItem exportEntitiesItem = new MenuItem("Export Entities");
            contextMenu.getItems().add(exportEntitiesItem);
            exportEntitiesItem.setOnAction(event -> {
                File outputFolder = FileUtils.askUserToSelectFolder(getGameInstance(), RESOURCE_EXPORT_PATH);
                if (outputFolder != null)
                    exportEntities(outputFolder, getScriptList(), createScriptDisplaySettings());
            });

            MenuItem importEntitiesItem = new MenuItem("Import Entities");
            contextMenu.getItems().add(importEntitiesItem);
            importEntitiesItem.setOnAction(event -> {
                File inputFolder = FileUtils.askUserToSelectFolder(getGameInstance(), RESOURCE_IMPORT_PATH);
                if (inputFolder != null)
                    importEntities(inputFolder);
            });
        }

        MenuItem exportEntityDescriptionsItem = new MenuItem("Export Entity Descriptions");
        contextMenu.getItems().add(exportEntityDescriptionsItem);
        exportEntityDescriptionsItem.setOnAction(event -> {
            File outputFolder = FileUtils.askUserToSelectFolder(getGameInstance(), RESOURCE_EXPORT_PATH);
            if (outputFolder != null)
                exportEntityDescriptions(outputFolder);
        });

        MenuItem importEntityDescriptionsItem = new MenuItem("Import Entity Descriptions");
        contextMenu.getItems().add(importEntityDescriptionsItem);
        importEntityDescriptionsItem.setOnAction(event -> {
            File inputFolder = FileUtils.askUserToSelectFolder(getGameInstance(), RESOURCE_IMPORT_PATH);
            if (inputFolder != null)
                importEntityDescriptions(inputFolder);
        });

        MenuItem exportProxyDescriptionsItem = new MenuItem("Export Collision Proxies");
        contextMenu.getItems().add(exportProxyDescriptionsItem);
        exportProxyDescriptionsItem.setOnAction(event -> {
            File outputFolder = FileUtils.askUserToSelectFolder(getGameInstance(), RESOURCE_EXPORT_PATH);
            if (outputFolder != null)
                exportCollisionProxies(outputFolder);
        });

        MenuItem importProxyDescriptionsItem = new MenuItem("Import Collision Proxies");
        contextMenu.getItems().add(importProxyDescriptionsItem);
        importProxyDescriptionsItem.setOnAction(event -> {
            File inputFolder = FileUtils.askUserToSelectFolder(getGameInstance(), RESOURCE_IMPORT_PATH);
            if (inputFolder != null)
                importCollisionProxies(inputFolder);
        });
    }

    /**
     * Creates a new resource.
     * @param readType the type of resource to create
     * @param rawBytes the raw byte data to create with
     * @param identifier the raw identifier string
     * @return newResource
     */
    public kcCResource createResource(KCResourceID readType, byte[] rawBytes, String identifier) {
        kcCResource newChunk;
        if (readType == KCResourceID.RAW && DataUtils.testSignature(rawBytes, kcEnvironment.ENVIRONMENT_NAME)) {
            newChunk = new kcEnvironment(this);
        } else if (readType == KCResourceID.RAW && DataUtils.testSignature(rawBytes, kcScriptList.GLOBAL_SCRIPT_NAME)) {
            newChunk = new kcScriptList(this);
        } else if (readType != null && readType.getMaker() != null) {
            newChunk = readType.getMaker().apply(this);
        } else {
            newChunk = new GreatQuestDummyFileChunk(this, identifier);
            getLogger().warning("Reading unsupported chunk with identifier '" + identifier + "'.");
        }

        return newChunk;
    }
}