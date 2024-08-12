package net.highwayfrogs.editor.games.renderware;

import javafx.scene.image.Image;
import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameData.SharedGameData;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.CollectionViewComponent.ICollectionViewEntry;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.DataSizeUnit;
import net.highwayfrogs.editor.utils.Utils;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Represents a Renderware stream chunk.
 * <a href="https://gtamods.com/wiki/RenderWare_binary_stream_file"/>
 * Created by Kneesnap on 6/9/2020.
 */
public abstract class RwStreamChunk extends SharedGameData implements ICollectionViewEntry, IPropertyListCreator {
    @Getter private final RwStreamFile streamFile;
    @Getter private final RwStreamChunk parentChunk;
    @Getter @NonNull private final IRwStreamSectionType sectionType;
    @Getter protected int version; // RwVersion
    @Getter private byte[] rawReadData;
    @Getter private final List<RwStreamChunk> childChunks = new ArrayList<>();
    @Getter private ChunkReadResult readResult;
    private transient SoftReference<Logger> logger;

    public RwStreamChunk(RwStreamFile streamFile, @NonNull IRwStreamSectionType sectionType, int version, RwStreamChunk parentChunk) {
        super(streamFile != null ? streamFile.getGameInstance() : null);
        this.streamFile = streamFile;
        this.sectionType = sectionType;
        this.version = version;
        this.parentChunk = parentChunk;
    }

    @Override
    public final void load(DataReader reader) {
        this.childChunks.clear();

        // chunk ID has already been read by this point, used to construct this object.
        int readSize = reader.readInt();
        this.version = reader.readInt();

        if (readSize > reader.getRemaining())
            throw new RuntimeException("Cannot read chunk " + Utils.getSimpleName(this) + " of size " + readSize + " when there are only " + reader.getRemaining() + " bytes left.");
        if (this.parentChunk != null && this.parentChunk.getVersion() != this.version) // Unsure if this can happen yet.
            getLogger().warning("The chunk's version was " + RwVersion.getDebugString(this.version) + ", but its parent was " + RwVersion.getDebugString(this.parentChunk.getVersion()) + ". (This may not indicate a problem.)");

        /*if (this.parentChunk != null && this.parentChunk.getReadSize() == this.readSize) {
            loadChunkData(reader);
            return;
        }*/ // TODO: Test against Rescue?

        this.rawReadData = reader.readBytes(readSize);
        DataReader chunkReader = new DataReader(new ArraySource(this.rawReadData));

        try {
            loadChunkData(chunkReader, readSize, this.version);
            if (chunkReader.hasMore()) { // Warn if we're leaving data unread.
                getLogger().warning("The chunk left " + chunkReader.getRemaining() + " bytes of data unread!");
                this.readResult = ChunkReadResult.DID_NOT_REACH_END;
            } else {
                this.readResult = ChunkReadResult.SUCCESSFUL;
            }
        } catch (Throwable th) {
            this.readResult = ChunkReadResult.EXCEPTION;
            Utils.handleError(getLogger(), th, false, "Failed to read RwStreamChunk data.");
        }
    }

    @Override
    public final void save(DataWriter writer) {
        writer.writeInt(this.sectionType.getTypeId());
        int chunkDataSizeAddress = writer.writeNullPointer();
        writer.writeInt(this.version);

        // Write chunk-specific stuff.
        int dataWriteStartIndex = writer.getIndex();
        try {
            saveChunkData(writer);
            writer.writeAddressAt(chunkDataSizeAddress, writer.getIndex() - dataWriteStartIndex);
        } catch (Throwable th) {
            Utils.handleError(getLogger(), th, false, "Failed to write RwStreamChunk data.");

            // Null out the data chunk.
            writer.setIndex(dataWriteStartIndex);
            writer.writeAddressAt(chunkDataSizeAddress, 0);
        }
    }

    /**
     * Reads data specific to this chunk type.
     * @param reader The reader to read data from.
     * @param dataLength the amount of bytes of data to read
     * @param version the version included with the chunk
     */
    protected abstract void loadChunkData(DataReader reader, int dataLength, int version);

    /**
     * Saves data specific to this chunk type.
     * @param writer The writer to write data to.
     */
    protected abstract void saveChunkData(DataWriter writer);

    /**
     * Reads an RwStreamChunk from the reader, with this chunk as the target chunk's parent.
     * If an unexpected chunk is loaded instead, a ClassCastException will be thrown.
     * @param reader the reader to read the chunk data from
     * @param chunkClass the chunk class to read
     * @return newChunk
     * @param <TStreamChunk> the type of chunk expected.
     */
    protected <TStreamChunk extends RwStreamChunk> TStreamChunk readChunk(DataReader reader, Class<TStreamChunk> chunkClass) {
        if (chunkClass == null)
            throw new RuntimeException("chunkClass");
        if (this.streamFile == null)
            throw new IllegalStateException("Cannot read a stream chunk because the current chunk's stream file is null, meaning we cannot resolve which chunk registry to use.");

        RwStreamChunk newChunk = this.streamFile.getChunkTypeRegistry().readChunk(reader, this);
        if (!chunkClass.isInstance(newChunk))
            throw new ClassCastException("Expected to read a(n) " + chunkClass.getSimpleName() + ", but instead got " + Utils.getSimpleName(newChunk) + "/" + newChunk + ".");

        this.childChunks.add(newChunk);
        return chunkClass.cast(newChunk);
    }

    /**
     * Gets information used for the logger.
     */
    protected String getLoggerInfo() {
        return (this.rawReadData != null ? "size=" + this.rawReadData.length + "," : "") + "ver=" + RwVersion.convertVersionToString(this.version);
    }

    @Override
    public Logger getLogger() {
        Logger cachedLogger = this.logger != null ? this.logger.get() : null;
        if (cachedLogger != null)
            return cachedLogger;

        cachedLogger = Logger.getLogger(toString());
        this.logger = new SoftReference<>(cachedLogger);
        return cachedLogger;
    }

    /**
     * Gets a string describing the chunk.
     */
    public String getChunkDescriptor() {
        return getClass().getSimpleName() + "{" + getLoggerInfo() + "}";
    }

    @Override
    public String toString() {
        String locationName = this.streamFile != null ? this.streamFile.getLocationName() : null;
        return (locationName != null ? locationName + "@" : "") + getChunkDescriptor();
    }

    @Override
    public ICollectionViewEntry getCollectionViewParentEntry() {
        return this.parentChunk;
    }

    @Override
    public String getCollectionViewDisplayName() {
        return this.sectionType.getDisplayName();
    }

    @Override
    public Image getCollectionViewIcon() {
        switch (this.readResult) {
            case EXCEPTION:
                return ImageResource.GHIDRA_ICON_STOP_SIGN_X_16.getFxImage();
            case DID_NOT_REACH_END:
                return ImageResource.GHIDRA_ICON_WARNING_TRIANGLE_YELLOW_16.getFxImage();
            case SUCCESSFUL:
            default:
                return this.sectionType.getIcon().getFxImage();
        }
    }

    @Override
    public String getCollectionViewDisplayStyle() {
        switch (this.readResult) {
            case EXCEPTION:
                return "-fx-text-fill: red;";
            case DID_NOT_REACH_END:
                return "-fx-text-fill: green;";
            case SUCCESSFUL:
            default:
                return null;
        }
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList.add("Type ID", Utils.toHexString(this.sectionType.getTypeId()) + " (" + Utils.getSimpleName(this) + ")");
        propertyList.add("RenderWare Version", RwVersion.getDebugString(this.version));
        if (this.rawReadData != null)
            propertyList.add("Size (In Bytes)", this.rawReadData.length + " (" + DataSizeUnit.formatSize(this.rawReadData.length) + ")");

        return propertyList;
    }

    public enum ChunkReadResult {
        SUCCESSFUL,
        DID_NOT_REACH_END,
        EXCEPTION
    }
}