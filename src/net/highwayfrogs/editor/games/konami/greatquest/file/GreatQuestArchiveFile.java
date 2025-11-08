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
    @Getter private final GreatQuestArchiveFileType fileType;
    @Getter private final GreatQuestHash<GreatQuestArchiveFile> selfHash;
    @Getter private byte[] rawData;
    @Getter private String fileName;
    @Getter private String filePath;
    @Getter @Setter private boolean compressed;
    private ILogger cachedLogger;

    public GreatQuestArchiveFile(GreatQuestInstance instance, GreatQuestArchiveFileType fileType) {
        super(instance);
        this.fileType = fileType;
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
    public GreatQuestAssetBinFile getMainArchive() {
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
        propertyList.add("Name Collision", hasCollision());
        propertyList.add("Compression Enabled", this.compressed);
        if (this.rawData != null)
            propertyList.add("Loaded File Size", DataSizeUnit.formatSize(this.rawData.length));

        return propertyList;
    }

    /**
     * Initialize the information about this file.
     * @param filePath This file's raw name. Can be null.
     * @param rawBytes The raw unprocessed bytes containing the file data.
     */
    public void init(String filePath, byte[] rawBytes) {
        init(filePath, this.fileType.isCompressedByDefault(getGameInstance()), GreatQuestUtils.hashFilePath(filePath), rawBytes);
    }

    /**
     * Initialize the information about this file.
     * @param filePath   This file's raw name. Can be null.
     * @param compressed Whether this file is compressed.
     * @param hash       The hash of the file path.
     * @param rawBytes   The raw unprocessed bytes containing the file data.
     */
    public void init(String filePath, boolean compressed, int hash, byte[] rawBytes) {
        if (isRegistered())
            throw new IllegalStateException("Cannot re-init file '" + this.filePath + "'/" + getSelfHash().getHashNumberAsString() + " to '" + filePath + "'/" + NumberUtils.to0PrefixedHexString(hash) + " while the file is registered.");

        setFilePath(filePath);
        this.compressed = compressed;
        this.selfHash.setHash(hash);
        this.rawData = rawBytes;
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
        if (this.filePath != null && !this.filePath.isEmpty() && !this.filePath.equalsIgnoreCase(filePath) && isRegistered())
            throw new IllegalStateException("Cannot change file-path of '" + this.filePath + "' to '" + filePath + "' while the file is registered.");

        // Apply data.
        this.filePath = filePath;
        this.fileName = getFileNameFromPath(filePath);
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
     * Returns true iff this file's hash collides with another file's hash.
     */
    public boolean hasCollision() {
        return getMainArchive().hasCollision(this);
    }

    /**
     * Returns true iff the file is currently registered in the .bin file.
     */
    public boolean isRegistered() {
        return getMainArchive().isRegistered(this);
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

    /**
     * Gets the file name from the file path.
     * @param filePath the file path to condense into a file name
     * @return fileName
     */
    public static String getFileNameFromPath(String filePath) {
        if (filePath == null)
            return null;

        int backSlashIndex = filePath.lastIndexOf('\\');
        return backSlashIndex >= 0 ? filePath.substring(backSlashIndex + 1) : filePath;
    }
}