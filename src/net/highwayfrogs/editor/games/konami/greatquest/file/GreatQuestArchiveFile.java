package net.highwayfrogs.editor.games.konami.greatquest.file;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash.kcHashedResource;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.IFileExport;
import net.highwayfrogs.editor.gui.components.CollectionViewComponent.ICollectionViewEntry;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.DataSizeUnit;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.data.reader.ArraySource;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.LazyInstanceLogger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A base TGQ file.
 * Created by Kneesnap on 8/17/2019.
 */
public abstract class GreatQuestArchiveFile extends GreatQuestGameFile implements ICollectionViewEntry, IPropertyListCreator, kcHashedResource {
    @Getter private final GreatQuestHash<GreatQuestArchiveFile> selfHash;
    @Getter private byte[] rawData;
    @Getter private String fileName;
    @Getter private String filePath;
    @Getter private boolean collision; // This is true iff there are multiple files that share the hash.
    @Getter @Setter private boolean compressed;
    private ILogger cachedLogger;

    public GreatQuestArchiveFile(GreatQuestInstance instance) {
        super(instance);
        this.selfHash = new GreatQuestHash<>(this);
    }

    @Override
    public String getResourceName() {
        return this.filePath; // If the file path is null, then so be it.
    }

    @Override
    public ILogger getLogger() {
        if (this.cachedLogger == null)
            this.cachedLogger = new LazyInstanceLogger(getGameInstance(), GreatQuestArchiveFile::getExportName, this);

        return this.cachedLogger;
    }

    @Override
    public void setupRightClickMenuItems(ContextMenu contextMenu) {
        if (this.rawData != null) {
            MenuItem saveOriginalFileData = new MenuItem("Export Original File Data");
            contextMenu.getItems().add(saveOriginalFileData);
            saveOriginalFileData.setOnAction(event -> {
                File outputFile = FXUtils.promptFileSave(getGameInstance(), "Please select the file to save '" + getFileName() + "' as...", getFileName(), "All Files", "*");
                if (outputFile != null)
                    exportOriginalFileData(outputFile);
            });
        }

        super.setupRightClickMenuItems(contextMenu);
    }

    /**
     * Exports original file data to the given file.
     * @param targetFile the file to save the original data to
     */
    public void exportOriginalFileData(File targetFile) {
        if (targetFile == null)
            throw new NullPointerException("targetFile");

        try {
            Files.write(targetFile.toPath(), this.rawData);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to export original file data for '" + getFileName() + "' to '" + targetFile.getName() + "'.", ex);
        }
    }

    /**
     * Gets the main archive.
     */
    private GreatQuestAssetBinFile getMainArchive() {
        return getGameInstance().getMainArchive();
    }

    @Override
    public String getCollectionViewDisplayName() {
        return (this.fileName != null ? this.fileName : getExportName()) + " [Hash: " + NumberUtils.toHexString(getHash()) + (this.filePath != null ? ", Full Path: " + this.filePath : "") + "]";
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);

        propertyList.add("Name Hash", NumberUtils.to0PrefixedHexString(getHash()));
        propertyList.add("Name Collision", this.collision);
        propertyList.add("Compression Enabled", this.compressed);
        if (this.rawData != null)
            propertyList.add("Loaded File Size", DataSizeUnit.formatSize(this.rawData.length));

        return propertyList;
    }

    /**
     * Initialize the information about this file.
     * @param realName   This file's raw name. Can be null.
     * @param compressed Whether this file is compressed.
     */
    public void init(String realName, boolean compressed, int hash, byte[] rawBytes, boolean collision) {
        setFilePath(realName);
        this.compressed = compressed;
        this.selfHash.setHash(hash);
        this.rawData = rawBytes;
        this.collision = collision;
    }

    /**
     * Loads the file from the provided bytes.
     * @param fileBytes the bytes to load the file data from
     */
    public void loadFileFromBytes(byte[] fileBytes) {
        // Read file.
        try {
            DataReader fileReader = new DataReader(new ArraySource(fileBytes));
            this.load(fileReader);
        } catch (Exception ex) {
            throw new RuntimeException("There was a problem reading " + getClass().getSimpleName() + " [" + getDebugName() + "]", ex);
        }
    }

    /**
     * Sets the raw file path of this file. This will also update the file name.
     * @param filePath The raw file name. (Full path)
     */
    public void setFilePath(String filePath) {
        // Cut off everything before the "\\Game" folder.
        // Only files which have collision hashes have something before that (eg: "\\Netapp1\PD\.....").
        // I've chosen to start counting game files at the "root folder of game data" instead of "root folder of network drive".
        if (filePath != null) {
            int startPos = GreatQuestUtils.indexOfMultiple(filePath, GreatQuestUtils.GAME_PATH_INDEX_PATTERNS);
            if (startPos > 0)
                filePath = filePath.substring(startPos);
        }

        // If we already have a path, we shouldn't be replacing it, and we should warn if the path differs.
        if (this.filePath != null && !this.filePath.isEmpty() && !this.filePath.equalsIgnoreCase(filePath)) {
            getLogger().warning("Attempted to replace file name '" + this.filePath + "' with '" + filePath + "'. Not sure how to handle.");
            return;
        }

        // Apply data.
        this.filePath = filePath;
        this.fileName = filePath;
        if (filePath != null && filePath.contains("\\")) // Remove path.
            this.fileName = filePath.substring(filePath.lastIndexOf("\\") + 1);
    }

    /**
     * Gets the archive into the main archive.
     * @return archiveIndex
     */
    public int getArchiveIndex() {
        return getMainArchive() != null ? getMainArchive().getFiles().indexOf(this) : -1;
    }

    /**
     * Tests if this file has a file path assigned.
     * @return hasFilePath
     */
    public boolean hasFilePath() {
        return this.filePath != null;
    }

    /**
     * Gets the export name.
     * @return exportName
     */
    public String getExportName() {
        if (hasFilePath() && FileUtils.isValidFileName(getFileName()))
            return getFileName();

        int index = getArchiveIndex();
        if (index == -1)
            index = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);

        return index + "." + getExtension();
    }

    /**
     * Gets the file name shown in debug contexts. Tries to show as much detail as possible.
     */
    public String getDebugName() {
        if (this.filePath != null)
            return this.filePath;
        if (this.fileName != null)
            return this.fileName;
        return getExportName();
    }

    /**
     * Gets the extension for this file.
     */
    public String getExtension() {
        return "dat";
    }

    /**
     * Gets the folder the file gets exported to if the file path is unknown.
     */
    public abstract String getDefaultFolderName();

    /**
     * Gets the folder the file gets exported for any custom export.
     */
    public String getExportFolderName() {
        return "Usable";
    }

    /**
     * Exports the file to a folder.
     * @param baseFolder The base folder that game assets are saved to.
     */
    public void export(File baseFolder) {
        File targetFile = GreatQuestUtils.getExportFile(baseFolder, this);
        File targetFolder = targetFile.getParentFile();
        if (this.rawData != null && (!targetFile.exists() || targetFile.length() != this.rawData.length))
            exportOriginalFileData(targetFile);

        if (this instanceof IFileExport) {
            File exportFolder = new File(targetFolder, getExportFolderName() + "/");
            FileUtils.makeDirectory(exportFolder);

            try {
                ((IFileExport) this).exportToFolder(exportFolder);
            } catch (IOException ex) {
                throw new RuntimeException("Failed to export file '" + getDebugName() + "' to usable format.", ex);
            }
        }
    }
}