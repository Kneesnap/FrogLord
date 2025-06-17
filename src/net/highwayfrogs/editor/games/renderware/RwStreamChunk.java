package net.highwayfrogs.editor.games.renderware;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.generic.data.GameData.SharedGameData;
import net.highwayfrogs.editor.games.renderware.chunks.RwExtensionChunk;
import net.highwayfrogs.editor.games.renderware.chunks.RwStringChunk;
import net.highwayfrogs.editor.games.renderware.chunks.RwStructChunk;
import net.highwayfrogs.editor.games.renderware.chunks.RwUnicodeStringChunk;
import net.highwayfrogs.editor.games.renderware.struct.RwStruct;
import net.highwayfrogs.editor.games.renderware.ui.IRwStreamChunkUIEntry;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.*;
import net.highwayfrogs.editor.utils.data.reader.ArraySource;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.LazyInstanceLogger;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Renderware stream chunk.
 * These are officially called "chunks". Unofficially, the GTA community sometimes calls them "sections".
 * Both terms may be used here and refer to this idea. Sometimes called "chunk sections" too.
 * <a href="https://gtamods.com/wiki/RenderWare_binary_stream_file"/>
 * Created by Kneesnap on 6/9/2020.
 */
public abstract class RwStreamChunk extends SharedGameData implements IRwStreamChunkUIEntry {
    @Getter private final RwStreamFile streamFile;
    @Getter private final RwStreamChunk parentChunk;
    @Getter @NonNull private final IRwStreamChunkType chunkType;
    @Getter protected int version; // RwVersion
    @Getter private byte[] rawReadData;
    @Getter private final List<RwStreamChunk> childChunks = new ArrayList<>();
    @Getter protected final List<IRwStreamChunkUIEntry> childUISections = new ArrayList<>();
    @Getter private ChunkReadResult readResult = ChunkReadResult.READ_HAS_NOT_OCCURRED;
    private RwExtensionChunk extension;
    private transient WeakReference<ILogger> logger;

    public RwStreamChunk(RwStreamFile streamFile, @NonNull IRwStreamChunkType chunkType, int version, RwStreamChunk parentChunk) {
        super(streamFile != null ? streamFile.getGameInstance() : null);
        this.streamFile = streamFile;
        this.chunkType = chunkType;
        this.version = version;
        this.parentChunk = parentChunk;
    }

    @Override
    public void load(DataReader reader) {
        this.childChunks.clear();
        this.childUISections.clear();

        // chunk ID has already been read by this point, used to construct this object.
        int readSize = reader.readInt();
        int versionDataIndex = reader.getIndex();
        this.version = reader.readInt();

        if (!RwVersion.doesVersionAppearValid(this.version))
            getLogger().warning("The version %s was read from 0x%X, and does not appear valid!", RwVersion.getDebugString(this.version), versionDataIndex);

        if (readSize > reader.getRemaining())
            throw new RuntimeException("Cannot read chunk " + Utils.getSimpleName(this) + " of size " + readSize + " when there are only " + reader.getRemaining() + " bytes left.");
        if (this.parentChunk != null && this.parentChunk.getVersion() != this.version) // This can happen, but doesn't necessarily seem to indicate a problem, unless it doesn't look like a valid version.
            getLogger().info("The chunk version is %s, but its parent was %s.", RwVersion.getDebugString(this.version), RwVersion.getDebugString(this.parentChunk.getVersion()));

        /*if (this.parentChunk != null && this.parentChunk.getReadSize() == this.readSize) {
            loadChunkData(reader);
            return;
        }*/ // TODO: Test against Rescue? Beyond?

        this.rawReadData = reader.readBytes(readSize);
        DataReader chunkReader = new DataReader(new ArraySource(this.rawReadData));

        try {
            loadChunkData(chunkReader, readSize, this.version);
            if (chunkReader.hasMore()) { // Warn if we're leaving data unread.
                getLogger().warning("The chunk left %d bytes of data unread!", chunkReader.getRemaining());
                this.readResult = ChunkReadResult.DID_NOT_REACH_END;
            } else {
                this.readResult = ChunkReadResult.SUCCESSFUL;
            }
        } catch (Throwable th) {
            this.readResult = ChunkReadResult.EXCEPTION;
            Utils.handleError(getLogger(), th, false, "Failed to read RwStreamChunk data."
                    + " (Parent Index: " + NumberUtils.toHexString(reader.getIndex() + chunkReader.getIndex())
                    + ", Local Index: " + NumberUtils.toHexString(chunkReader.getIndex()) + ")");
        }
    }

    @Override
    public final void save(DataWriter writer) {
        this.childChunks.clear(); // We've got new chunks to display.
        this.childUISections.clear();

        writer.writeInt(this.chunkType.getTypeId());
        int chunkDataSizeAddress = writer.writeNullPointer();
        writer.writeInt(this.version);

        // Write chunk-specific stuff.
        int dataWriteStartIndex = writer.getIndex();
        try {
            saveChunkData(writer);
            writer.writeIntAtPos(chunkDataSizeAddress, writer.getIndex() - dataWriteStartIndex);
        } catch (Throwable th) {
            Utils.handleError(getLogger(), th, false, "Failed to write RwStreamChunk data.");

            // Null out the data chunk.
            writer.setIndex(dataWriteStartIndex);
            writer.writeIntAtPos(chunkDataSizeAddress, 0);
        }
    }

    @Override
    public void setupRightClickMenuItems(ContextMenu contextMenu) {
        MenuItem menuItem = new MenuItem("Export Raw Chunk Data");
        contextMenu.getItems().add(menuItem);
        menuItem.setOnAction(event -> {
            File outputFile = FXUtils.promptFileSave(getGameInstance(), "Specify the file to save the chunk data as...", "raw-chunk-data", "Raw RenderWare Stream", "rawrws");
            if (outputFile != null)
                FileUtils.writeBytesToFile(getLogger(), outputFile, getRawReadData(), true);
        });
    }

    /**
     * Returns a UI controller specific to this chunk, if one exists.
     */
    public GameUIController<?> makeEditorUI() {
        return null; // By default, there is no UI.
    }

    /**
     * Handles when the stream chunk is double-clicked in the view UI.
     * By default, this does nothing.
     */
    public void handleDoubleClick() {
        // Do nothing by default.
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

    private <TStreamChunk extends RwStreamChunk> TStreamChunk registerChildChunk(TStreamChunk chunk, boolean showInUI) {
        this.childChunks.add(chunk);
        if (showInUI)
            this.childUISections.add(chunk);

        return chunk;
    }

    /**
     * Reads an RwStreamChunk from the reader, with this chunk as the target chunk's parent.
     * If an unexpected chunk is loaded instead, a ClassCastException will be thrown.
     * @param reader the reader to read the chunk data from
     * @param chunkClass the chunk class to read
     * @return newChunk
     * @param <TStreamChunk> the type of chunk expected.
     */
    protected <TStreamChunk extends RwStreamChunk> TStreamChunk readChunk(DataReader reader, Class<TStreamChunk> chunkClass) {
        return readChunk(reader, chunkClass, true);
    }

    /**
     * Reads an RwStreamChunk from the reader, with this chunk as the target chunk's parent.
     * If an unexpected chunk is loaded instead, a ClassCastException will be thrown.
     * @param reader the reader to read the chunk data from
     * @param chunkClass the chunk class to read
     * @param showInUI Whether it should be shown in the UI. This is rarely false.
     * @return newChunk
     * @param <TStreamChunk> the type of chunk expected.
     */
    protected <TStreamChunk extends RwStreamChunk> TStreamChunk readChunk(DataReader reader, Class<TStreamChunk> chunkClass, boolean showInUI) {
        if (chunkClass == null)
            throw new RuntimeException("chunkClass");
        if (this.streamFile == null)
            throw new IllegalStateException("Cannot read a stream chunk because the current chunk's stream file is null, meaning we cannot resolve which chunk registry to use.");

        RwStreamChunk newChunk = this.streamFile.getChunkTypeRegistry().readChunk(reader, this);
        if (!chunkClass.isInstance(newChunk))
            throw new ClassCastException("Expected to read a(n) " + chunkClass.getSimpleName() + ", but instead got " + Utils.getSimpleName(newChunk) + "/" + newChunk + ".");

        registerChildChunk(newChunk, showInUI);
        return chunkClass.cast(newChunk);
    }

    /**
     * Reads any RwStreamChunk from the reader, with this chunk as the target chunk's parent.
     * @param reader the reader to read the chunk data from
     * @return newChunk
     */
    protected RwStreamChunk readChunk(DataReader reader) {
        return readChunk(reader, true);
    }

    /**
     * Reads any RwStreamChunk from the reader, with this chunk as the target chunk's parent.
     * @param reader the reader to read the chunk data from
     * @param showInUI Whether it should be shown in the UI. This is rarely false.
     * @return newChunk
     */
    protected RwStreamChunk readChunk(DataReader reader, boolean showInUI) {
        if (this.streamFile == null)
            throw new IllegalStateException("Cannot read a stream chunk because the current chunk's stream file is null, meaning we cannot resolve which chunk registry to use.");

        RwStreamChunk newChunk = this.streamFile.getChunkTypeRegistry().readChunk(reader, this);
        registerChildChunk(newChunk, showInUI);
        return newChunk;
    }

    /**
     * Reads an RwStreamChunk from the reader.
     * If an unexpected chunk is loaded instead, an IllegalArgumentException will be thrown.
     * @param reader the reader to read the chunk data from
     * @param chunk the chunk object to read
     * @return newChunk
     * @param <TStreamChunk> the type of chunk expected.
     */
    protected <TStreamChunk extends RwStreamChunk> TStreamChunk readChunk(DataReader reader, TStreamChunk chunk) {
        return readChunk(reader, chunk, true);
    }

    /**
     * Reads an RwStreamChunk from the reader.
     * If an unexpected chunk is loaded instead, an IllegalArgumentException will be thrown.
     * @param reader the reader to read the chunk data from
     * @param chunk the chunk object to read
     * @param showInUI Whether it should be shown in the UI. This is rarely false.
     * @return newChunk
     * @param <TStreamChunk> the type of chunk expected.
     */
    protected <TStreamChunk extends RwStreamChunk> TStreamChunk readChunk(DataReader reader, TStreamChunk chunk, boolean showInUI) {
        if (chunk == null)
            throw new RuntimeException("chunk");
        if (this.streamFile == null)
            throw new IllegalStateException("Cannot read a stream chunk because the current chunk's stream file is null, meaning we cannot resolve which chunk registry to use.");

        this.streamFile.getChunkTypeRegistry().readChunkObject(reader, chunk);
        return registerChildChunk(chunk, showInUI);
    }

    /**
     * Reads an RwStructChunk from the reader nested within this chunk.
     * If an unexpected chunk is loaded instead, a ClassCastException will be thrown.
     * @param reader the reader to read the chunk data from
     * @param structClass the struct class to read
     * @return newChunk
     * @param <TStruct> the type of struct expected.
     */
    protected <TStruct extends RwStruct> RwStructChunk<TStruct> readStructChunk(DataReader reader, Class<TStruct> structClass) {
        return readStructChunk(reader, structClass, true);
    }

    /**
     * Reads an RwStructChunk from the reader nested within this chunk.
     * If an unexpected chunk is loaded instead, a ClassCastException will be thrown.
     * @param reader the reader to read the chunk data from
     * @param structClass the struct class to read
     * @param showInUI Whether it should be shown in the UI. This is rarely false.
     * @return newChunk
     * @param <TStruct> the type of struct expected.
     */
    protected <TStruct extends RwStruct> RwStructChunk<TStruct> readStructChunk(DataReader reader, Class<TStruct> structClass, boolean showInUI) {
        if (structClass == null)
            throw new RuntimeException("structClass");
        if (this.streamFile == null)
            throw new IllegalStateException("Cannot read a stream chunk because the current chunk's stream file is null, meaning we cannot resolve which chunk registry to use.");

        int typeId = reader.readInt();
        if (typeId != RwStreamChunkType.STRUCT.getTypeId())
            throw new IllegalArgumentException("Expected the stream type ID to be " + NumberUtils.toHexString(RwStreamChunkType.STRUCT.getTypeId()) + " for a struct, but got " + NumberUtils.toHexString(typeId) + " instead!");

        reader.jumpTemp(reader.getIndex() + Constants.INTEGER_SIZE);
        int version = reader.readInt();
        reader.jumpReturn();

        RwStructChunk<TStruct> structChunk = new RwStructChunk<>(this.streamFile, version, this, structClass);
        structChunk.load(reader);
        return registerChildChunk(structChunk, showInUI);
    }

    /**
     * Reads an RwStructChunk from the reader, and returns the resulting struct.
     * If an unexpected chunk is loaded instead, a ClassCastException will be thrown.
     * @param reader the reader to read the chunk data from
     * @param structClass the struct class to read
     * @return newStruct
     * @param <TStruct> the type of struct expected.
     */
    protected <TStruct extends RwStruct> TStruct readStruct(DataReader reader, Class<TStruct> structClass) {
        return readStruct(reader, structClass, true);
    }

    /**
     * Reads an RwStructChunk from the reader, and returns the resulting struct.
     * If an unexpected chunk is loaded instead, a ClassCastException will be thrown.
     * @param reader the reader to read the chunk data from
     * @param structClass the struct class to read
     * @param showInUI Whether it should be shown in the UI. This is rarely false.
     * @return newStruct
     * @param <TStruct> the type of struct expected.
     */
    protected <TStruct extends RwStruct> TStruct readStruct(DataReader reader, Class<TStruct> structClass, boolean showInUI) {
        return readStructChunk(reader, structClass, showInUI).getValue();
    }

    /**
     * Reads an RwStructChunk from the reader, reading the supplied struct instance.
     * If an unexpected chunk is loaded instead, a ClassCastException will be thrown.
     * @param reader the reader to read the chunk data from
     * @param structValue the struct object to read
     * @return structValue
     * @param <TStruct> the type of struct expected.
     */
    protected <TStruct extends RwStruct> TStruct readStruct(DataReader reader, TStruct structValue) {
        return readStruct(reader, structValue, true);
    }

    /**
     * Reads an RwStructChunk from the reader, reading the supplied struct instance.
     * If an unexpected chunk is loaded instead, a ClassCastException will be thrown.
     * @param reader the reader to read the chunk data from
     * @param structValue the struct object to read
     * @param showInUI Whether it should be shown in the UI. This is rarely false.
     * @return structValue
     * @param <TStruct> the type of struct expected.
     */
    protected <TStruct extends RwStruct> TStruct readStruct(DataReader reader, TStruct structValue, boolean showInUI) {
        RwStructChunk<TStruct> structChunk = new RwStructChunk<>(this.streamFile, getVersion(), this, structValue);
        return readChunk(reader, structChunk, showInUI).getValue();
    }

    /**
     * Reads an RwStringChunk from the reader, and returns the resulting string.
     * If an unexpected chunk is loaded instead, a ClassCastException will be thrown.
     * This follows the behavior of _rwStringStreamFindAndRead in rwstring.c, meaning non-string chunks will be skipped.
     * @param reader the reader to read the chunk data from
     * @return stringValue
     */
    protected String readString(DataReader reader) {
        return readString(reader, true);
    }

    /**
     * Reads an RwStringChunk from the reader, and returns the resulting string.
     * If an unexpected chunk is loaded instead, a ClassCastException will be thrown.
     * This follows the behavior of _rwStringStreamFindAndRead in rwstring.c, meaning non-string chunks will be skipped.
     * @param reader the reader to read the chunk data from
     * @param showInUI Whether it should be shown in the UI. This is rarely false.
     * @return stringValue
     */
    protected String readString(DataReader reader, boolean showInUI) {
        while (reader.hasMore()) {
            RwStreamChunk nextChunk = this.streamFile.getChunkTypeRegistry().readChunk(reader, this);
            if (nextChunk instanceof RwStringChunk) {
                registerChildChunk(nextChunk, showInUI);
                return ((RwStringChunk) nextChunk).getValue();
            } else if (nextChunk instanceof RwUnicodeStringChunk) {
                registerChildChunk(nextChunk, showInUI);
                return ((RwUnicodeStringChunk) nextChunk).getValue();
            } else {
                registerChildChunk(nextChunk, true);
            }
        }

        throw new IllegalStateException("Did not find any string chunks before reaching end of data.");
    }

    /**
     * Reads optional extension data from the reader
     * @param reader the reader to read the data from
     */
    protected void readOptionalExtensionData(DataReader reader) {
        this.extension = reader.hasMore() ? readChunk(reader, RwExtensionChunk.class) : null;
    }

    /**
     * Writes an RwStreamChunk to the writer.
     * @param writer the writer to write the chunk data to
     * @param streamChunk the stream chunk to write
     */
    protected void writeChunk(DataWriter writer, RwStreamChunk streamChunk) {
        writeChunk(writer, streamChunk, true);
    }


    /**
     * Writes an RwStreamChunk to the writer.
     * @param writer the writer to write the chunk data to
     * @param streamChunk the stream chunk to write
     * @param showInUI Whether it should be shown in the UI. This is rarely false.
     */
    protected void writeChunk(DataWriter writer, RwStreamChunk streamChunk, boolean showInUI) {
        if (streamChunk == null)
            throw new RuntimeException("streamChunk");

        streamChunk.save(writer);
        registerChildChunk(streamChunk, showInUI);
    }

    /**
     * Writes an RwStructChunk to the writer.
     * @param writer the writer to write the chunk data to
     * @param value the struct to write
     */
    protected <TStruct extends RwStruct> void writeStruct(DataWriter writer, TStruct value) {
        writeStruct(writer, value, true);
    }

    /**
     * Writes an RwStructChunk to the writer.
     * @param writer the writer to write the chunk data to
     * @param value the struct to write
     * @param showInUI Whether it should be shown in the UI. This is rarely false.
     */
    protected <TStruct extends RwStruct> void writeStruct(DataWriter writer, TStruct value, boolean showInUI) {
        if (value == null)
            throw new RuntimeException("value");

        writeChunk(writer, new RwStructChunk<>(this.streamFile, this.version, this, value), showInUI);
    }

    /**
     * Writes extension data to the writer, if there is an extension data chunk
     * @param writer the writer to write the data to
     */
    protected void writeOptionalExtensionData(DataWriter writer) {
        if (this.extension != null)
            writeChunk(writer, this.extension);
    }

    /**
     * Gets information used for the logger.
     */
    protected String getLoggerInfo() {
        return (this.rawReadData != null ? "size=" + this.rawReadData.length + "," : "") + "ver=" + RwVersion.convertVersionToString(this.version);
    }

    @Override
    public ILogger getLogger() {
        ILogger cachedLogger = this.logger != null ? this.logger.get() : null;
        if (cachedLogger != null)
            return cachedLogger;

        cachedLogger = new LazyInstanceLogger(getGameInstance(), RwStreamChunk::getLoggerInfo, this);
        this.logger = new WeakReference<>(cachedLogger);
        return cachedLogger;
    }

    /**
     * Gets a string describing the chunk.
     */
    public String getChunkDescriptor() {
        return getClass().getSimpleName() + "{" + getLoggerInfo() + "}";
    }

    @Override
    public final String toString() {
        String locationName = this.streamFile != null ? this.streamFile.getLocationName() : null;
        return (locationName != null ? locationName + "@" : "") + getChunkDescriptor();
    }

    @Override
    public String getCollectionViewDisplayName() {
        return this.chunkType.getDisplayName();
    }

    @Override
    public Image getCollectionViewIcon() {
        switch (this.readResult) {
            case EXCEPTION:
                return ImageResource.GHIDRA_ICON_STOP_SIGN_X_16.getFxImage();
            case DID_NOT_REACH_END:
                return ImageResource.GHIDRA_ICON_WARNING_TRIANGLE_YELLOW_16.getFxImage();
            case READ_HAS_NOT_OCCURRED:
            case SUCCESSFUL:
            default:
                ImageResource icon = this.chunkType.getIcon();
                if (icon == null) {
                    getLogger().warning("chunkType (%s) did not return a(n) ImageResource icon!", this.chunkType);
                    icon = ImageResource.GHIDRA_ICON_QUESTION_MARK_16;
                }

                return icon.getFxImage();
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
        propertyList.add("Type ID", NumberUtils.toHexString(this.chunkType.getTypeId()) + " (" + Utils.getSimpleName(this) + ")");
        propertyList.add("RenderWare Version", RwVersion.getDebugString(this.version));
        if (this.rawReadData != null)
            propertyList.add("Size (In Bytes)", this.rawReadData.length + " (" + DataSizeUnit.formatSize(this.rawReadData.length) + ")");

        return propertyList;
    }

    public enum ChunkReadResult {
        READ_HAS_NOT_OCCURRED, // Object initialized, but never read.
        SUCCESSFUL,
        DID_NOT_REACH_END,
        EXCEPTION
    }
}