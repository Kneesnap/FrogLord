package net.highwayfrogs.editor.games.konami.greatquest.toc;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameData;
import net.highwayfrogs.editor.games.konami.greatquest.*;
import net.highwayfrogs.editor.games.konami.greatquest.loading.kcLoadContext;
import net.highwayfrogs.editor.utils.Utils;

import java.util.logging.Logger;

/**
 * Represents a resource in a TGQ file.
 * Created by Kneesnap on 8/25/2019.
 */
public abstract class kcCResource extends GameData<GreatQuestInstance> {
    @Getter private byte[] rawData;
    @Getter private final KCResourceID chunkType;
    @Getter @Setter private int hash; // The real hash comes from the TOC chunk.
    @Getter @Setter private String name;
    @Getter @Setter private GreatQuestChunkedFile parentFile;
    private Logger cachedLogger;

    private static final int NAME_SIZE = 32;

    public kcCResource(GreatQuestChunkedFile parentFile, KCResourceID chunkType) {
        super(parentFile != null ? parentFile.getGameInstance() : null);
        this.chunkType = chunkType;
        this.parentFile = parentFile;
    }

    @Override
    public Logger getLogger() {
        if (this.cachedLogger == null)
            this.cachedLogger = Logger.getLogger((this.chunkType != null ? Utils.stripAlphanumeric(this.chunkType.getSignature()) : "????") + "|" + this.name + (this.parentFile != null ? "@" + this.parentFile.getExportName() : ""));

        return this.cachedLogger;
    }

    /**
     * Calculates the hash from the name used for this resource.
     */
    public int getNameHash() {
        return this.name != null ? GreatQuestUtils.hash(this.name) : 0;
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

    @Override
    public void load(DataReader reader) {
        readRawData(reader);
        this.name = reader.readTerminatedStringOfLength(NAME_SIZE);
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
        writer.writeTerminatedStringOfLength(this.name, NAME_SIZE);
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
}