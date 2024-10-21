package net.highwayfrogs.editor.games.konami.greatquest.chunks;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash.kcHashedResource;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestArchiveFile;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestAssetBinFile;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.loading.kcLoadContext;
import net.highwayfrogs.editor.utils.Utils;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * Represents a resource in a TGQ file.
 * Created by Kneesnap on 8/25/2019.
 */
public abstract class kcCResource extends GameData<GreatQuestInstance> implements kcHashedResource {
    @Getter private byte[] rawData;
    @Getter private final KCResourceID chunkType;
    @Getter private final GreatQuestHash<kcCResource> selfHash; // The real hash comes from the TOC chunk.
    @Getter private final ObjectProperty<String> nameProperty = new SimpleObjectProperty<>(); // Usually this is what the hash is based on, but not always.
    @Getter protected boolean hashBasedOnName;
    @Getter @Setter private GreatQuestChunkedFile parentFile;
    private Logger cachedLogger;

    private static final int NAME_SIZE = 32;
    public static final String DEFAULT_RESOURCE_NAME = "unnamed";

    public kcCResource(GreatQuestChunkedFile parentFile, KCResourceID chunkType) {
        super(parentFile != null ? parentFile.getGameInstance() : null);
        this.selfHash = new GreatQuestHash<>(this); // kcCBaseResource::Init, kcCResource::Init
        this.chunkType = chunkType;
        this.parentFile = parentFile;
        setName(DEFAULT_RESOURCE_NAME, false); // By default, resources are 'unnamed'. See kcCResource::Init()
    }

    @Override
    public Logger getLogger() {
        if (this.cachedLogger == null)
            this.cachedLogger = Logger.getLogger((this.chunkType != null ? Utils.stripAlphanumeric(this.chunkType.getSignature()) : "????") + "|" + getName() + (this.parentFile != null ? "@" + this.parentFile.getExportName() : "") + getExtraLoggerInfo());

        return this.cachedLogger;
    }

    /**
     * Gets the name of the resource.
     */
    public String getName() {
        return this.nameProperty.get();
    }

    /**
     * Gets any extra logger info to include.
     */
    protected String getExtraLoggerInfo() {
       return "";
    }

    @Override
    public String getResourceName() {
        return getName();
    }

    @Override
    public int calculateHash() {
        if (this.hashBasedOnName) {
            return GreatQuestUtils.hashFilePath(this.nameProperty.get());
        } else if (this.selfHash != null) {
            // If the hash isn't based on the name, then we just include the existing hash.
            return this.selfHash.getHashNumber();
        } else {
            // When initializing, we can default to zero.
            return 0;
        }
    }

    /**
     * Sets the name of the resource, updating the hash if the old name is already in sync with the hash.
     * @param newName the new name to apply to the resource.
     */
    public void setName(String newName) {
        setName(newName, this.hashBasedOnName);
    }

    /**
     * Sets the name of the resource.
     * @param newName the new name to apply to the resource.
     * @param updateHash Whether the hash should be updated.
     */
    public void setName(String newName, boolean updateHash) {
        if (newName == null)
            throw new NullPointerException("newName");

        String oldName = this.nameProperty.getName();
        boolean didNameChange = oldName == null || !oldName.equalsIgnoreCase(newName);
        if (!Objects.equals(oldName, newName)) {
            this.nameProperty.set(newName);
            this.cachedLogger = null; // The logger is no longer valid!
        }

        if (updateHash) {
            this.hashBasedOnName = true;
            this.selfHash.setHash(newName); // Replace whatever the old hash was with a new hash based on the name.
        } else if (this.hashBasedOnName && didNameChange) {
            this.hashBasedOnName = false;
        }
    }

    /**
     * Reads raw data.
     * @param reader The reader to read raw data from.
     */
    protected void readRawData(DataReader reader) {
        reader.jumpTemp(reader.getIndex());
        this.rawData = reader.readBytes(reader.getRemaining());
        reader.jumpReturn();
    }

    /**
     * Gets the file by its name.
     * @param filePath The file path to load.
     * @return fileByName
     */
    public GreatQuestArchiveFile getFileByName(String filePath) {
        GreatQuestAssetBinFile mainArchive = getMainArchive();
        return mainArchive != null ? mainArchive.getFileByName(getParentFile(), filePath) : null;
    }

    /**
     * Gets the file by its name, returning null if the file was not found.
     * @param filePath The file path to load.
     * @return fileByName
     */
    public GreatQuestArchiveFile getOptionalFileByName(String filePath) {
        GreatQuestAssetBinFile mainArchive = getMainArchive();
        return mainArchive != null ? mainArchive.getOptionalFileByName(filePath) : null;
    }

    /**
     * Loads the resource contents from a raw byte array.
     * @param rawBytes the raw byte array to read the data from
     */
    public void loadFromRawBytes(byte[] rawBytes) {
        if (rawBytes == null)
            throw new NullPointerException("rawBytes");

        DataReader chunkReader = new DataReader(new ArraySource(rawBytes));
        try {
            this.load(chunkReader);

            // Warn if not all data is read.
            if (chunkReader.hasMore())
                getLogger().warning("GreatQuest Chunk " + Utils.stripAlphanumeric(getChunkMagic()) + "/'" + getName() + "' in '" + getParentFile().getDebugName() + "' had " + chunkReader.getRemaining() + " remaining unread bytes.");
        } catch (Throwable th) {
            Utils.handleError(getLogger(), th, false, "Failed to read %s chunk from '%s'.", getChunkType(), getParentFile().getDebugName());
        }
    }

    @Override
    public void load(DataReader reader) {
        readRawData(reader);

        String newName = reader.readNullTerminatedFixedSizeString(NAME_SIZE, Constants.NULL_BYTE);
        this.nameProperty.set(newName);
        int newNameHash = GreatQuestUtils.hash(newName);
        this.hashBasedOnName = (newNameHash == this.selfHash.getHashNumber());
        if (this.hashBasedOnName)
            this.selfHash.setOriginalString(newName);
    }

    /**
     * First method called after all files have loaded.
     */
    public void afterLoad1(kcLoadContext context) {
        // Do nothing.
    }

    /**
     * Second method called after all files have been loaded.
     */
    public void afterLoad2(kcLoadContext context) {
        // Do nothing.
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeNullTerminatedFixedSizeString(this.nameProperty.get(), NAME_SIZE, Constants.NULL_BYTE);
    }

    /**
     * Gets the signature this chunk uses
     * @return signature
     */
    public String getChunkMagic() {
        if (getChunkType().getSignature() == null)
            throw new UnsupportedOperationException("getSignature() was called on " + getChunkType() + ", which needs to be overwritten instead.");
        return getChunkType().getSignature();
    }

    /**
     * Gets the main archive this file resides in.
     */
    public GreatQuestAssetBinFile getMainArchive() {
        GreatQuestInstance instance = getGameInstance();
        if (instance != null)
            return instance.getMainArchive();
        if (this.parentFile != null && this.parentFile.getGameInstance() != null)
            return this.parentFile.getGameInstance().getMainArchive();

        return null;
    }

    /**
     * Return true if the resource is named any one of the given names.
     * @param names the names to test
     * @return true iff any of the names match (case-insensitive)
     */
    public boolean doesNameMatch(String... names) {
        String resourceName = getName();
        if (resourceName == null)
            return false;

        for (int i = 0; i < names.length; i++)
            if (resourceName.equalsIgnoreCase(names[i]))
                return true;

        return false;
    }
}