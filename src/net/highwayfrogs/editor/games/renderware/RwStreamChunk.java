package net.highwayfrogs.editor.games.renderware;

import javafx.scene.image.Image;
import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameData.SharedGameData;
import net.highwayfrogs.editor.games.renderware.chunks.RwStringChunk;
import net.highwayfrogs.editor.games.renderware.chunks.RwStructChunk;
import net.highwayfrogs.editor.games.renderware.chunks.RwUnicodeStringChunk;
import net.highwayfrogs.editor.games.renderware.struct.RwStruct;
import net.highwayfrogs.editor.games.renderware.ui.IRwStreamSectionUIEntry;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.CollectionViewComponent.ICollectionViewEntry;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.DataSizeUnit;
import net.highwayfrogs.editor.utils.Utils;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Represents a Renderware stream chunk.
 * TODO: Rename to RwStreamSection.
 * <a href="https://gtamods.com/wiki/RenderWare_binary_stream_file"/>
 * Created by Kneesnap on 6/9/2020.
 */
public abstract class RwStreamChunk extends SharedGameData implements IRwStreamSectionUIEntry {
    @Getter private final RwStreamFile streamFile;
    @Getter private final RwStreamChunk parentChunk;
    @Getter @NonNull private final IRwStreamSectionType sectionType;
    @Getter protected int version; // RwVersion
    @Getter private byte[] rawReadData;
    @Getter private final List<RwStreamChunk> childChunks = new ArrayList<>();
    @Getter protected final List<IRwStreamSectionUIEntry> childUISections = new ArrayList<>();
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
        this.childUISections.clear();

        // chunk ID has already been read by this point, used to construct this object.
        int readSize = reader.readInt();
        this.version = reader.readInt();

        if (readSize > reader.getRemaining())
            throw new RuntimeException("Cannot read section " + Utils.getSimpleName(this) + " of size " + readSize + " when there are only " + reader.getRemaining() + " bytes left.");
        if (this.parentChunk != null && this.parentChunk.getVersion() != this.version) // This can happen, but doesn't necessarily seem to indicate a problem, unless it doesn't look like a valid version.
            getLogger().info("The section version is " + RwVersion.getDebugString(this.version) + ", but its parent was " + RwVersion.getDebugString(this.parentChunk.getVersion()) + ".");

        /*if (this.parentChunk != null && this.parentChunk.getReadSize() == this.readSize) {
            loadChunkData(reader);
            return;
        }*/ // TODO: Test against Rescue? Beyond?

        this.rawReadData = reader.readBytes(readSize);
        DataReader chunkReader = new DataReader(new ArraySource(this.rawReadData));

        try {
            loadChunkData(chunkReader, readSize, this.version);
            if (chunkReader.hasMore()) { // Warn if we're leaving data unread.
                getLogger().warning("The section left " + chunkReader.getRemaining() + " bytes of data unread!");
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
        this.childChunks.clear(); // We've got new chunks to display.
        this.childUISections.clear();

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
     * Returns a UI controller specific to this section, if one exists.
     */
    public GameUIController<?> makeEditorUI() {
        return null; // By default, there is no UI.
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

    private <TStreamChunk extends RwStreamChunk> TStreamChunk registerChildSection(TStreamChunk chunk, boolean showInUI) {
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

        registerChildSection(newChunk, showInUI);
        return chunkClass.cast(newChunk);
    }

    /**
     * Reads an RwStreamChunk from the reader.
     * If an unexpected chunk is loaded instead, an IllegalArgumentException will be thrown.
     * @param reader the reader to read the chunk data from
     * @param section the section object to read
     * @return newChunk
     * @param <TStreamChunk> the type of chunk expected.
     */
    protected <TStreamChunk extends RwStreamChunk> TStreamChunk readChunk(DataReader reader, TStreamChunk section) {
        return readChunk(reader, section, true);
    }

    /**
     * Reads an RwStreamChunk from the reader.
     * If an unexpected chunk is loaded instead, an IllegalArgumentException will be thrown.
     * @param reader the reader to read the chunk data from
     * @param section the section object to read
     * @param showInUI Whether it should be shown in the UI. This is rarely false.
     * @return newChunk
     * @param <TStreamChunk> the type of chunk expected.
     */
    protected <TStreamChunk extends RwStreamChunk> TStreamChunk readChunk(DataReader reader, TStreamChunk section, boolean showInUI) {
        if (section == null)
            throw new RuntimeException("section");
        if (this.streamFile == null)
            throw new IllegalStateException("Cannot read a stream chunk because the current chunk's stream file is null, meaning we cannot resolve which chunk registry to use.");

        this.streamFile.getChunkTypeRegistry().readSectionObject(reader, section);
        return registerChildSection(section, showInUI);
    }

    /**
     * Reads an RwStructChunk from the reader nested within this chunk.
     * If an unexpected chunk is loaded instead, a ClassCastException will be thrown.
     * @param reader the reader to read the chunk data from
     * @param structClass the struct class to read
     * @return newChunk
     * @param <TStruct> the type of struct expected.
     */
    protected <TStruct extends RwStruct> RwStructChunk<TStruct> readStructSection(DataReader reader, Class<TStruct> structClass) {
        return readStructSection(reader, structClass, true);
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
    protected <TStruct extends RwStruct> RwStructChunk<TStruct> readStructSection(DataReader reader, Class<TStruct> structClass, boolean showInUI) {
        if (structClass == null)
            throw new RuntimeException("structClass");
        if (this.streamFile == null)
            throw new IllegalStateException("Cannot read a stream chunk because the current chunk's stream file is null, meaning we cannot resolve which chunk registry to use.");

        int typeId = reader.readInt();
        if (typeId != RwStreamSectionType.STRUCT.getTypeId())
            throw new IllegalArgumentException("Expected the stream type ID to be " + Utils.toHexString(RwStreamSectionType.STRUCT.getTypeId()) + " for a struct, but got " + Utils.toHexString(typeId) + " instead!");

        reader.jumpTemp(reader.getIndex() + Constants.INTEGER_SIZE);
        int version = reader.readInt();
        reader.jumpReturn();

        RwStructChunk<TStruct> structSection = new RwStructChunk<>(this.streamFile, version, this, structClass);
        structSection.load(reader);
        return registerChildSection(structSection, showInUI);
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
        return readStructSection(reader, structClass, showInUI).getValue();
    }

    /**
     * Reads an RwStringChunk from the reader, and returns the resulting string.
     * If an unexpected chunk is loaded instead, a ClassCastException will be thrown.
     * This follows the behavior of _rwStringStreamFindAndRead in rwstring.c, meaning non-string sections will be skipped.
     * @param reader the reader to read the chunk data from
     * @return stringValue
     */
    protected String readString(DataReader reader) {
        return readString(reader, true);
    }

    /**
     * Reads an RwStringChunk from the reader, and returns the resulting string.
     * If an unexpected chunk is loaded instead, a ClassCastException will be thrown.
     * This follows the behavior of _rwStringStreamFindAndRead in rwstring.c, meaning non-string sections will be skipped.
     * @param reader the reader to read the chunk data from
     * @param showInUI Whether it should be shown in the UI. This is rarely false.
     * @return stringValue
     */
    protected String readString(DataReader reader, boolean showInUI) {
        while (reader.hasMore()) {
            RwStreamChunk nextChunk = this.streamFile.getChunkTypeRegistry().readChunk(reader, this);
            if (nextChunk instanceof RwStringChunk) {
                registerChildSection(nextChunk, showInUI);
                return ((RwStringChunk) nextChunk).getValue();
            } else if (nextChunk instanceof RwUnicodeStringChunk) {
                registerChildSection(nextChunk, showInUI);
                return ((RwUnicodeStringChunk) nextChunk).getValue();
            } else {
                registerChildSection(nextChunk, true);
            }
        }

        throw new IllegalStateException("Did not find any string sections before reaching end of data.");
    }

    /**
     * Writes an RwStreamChunk to the writer.
     * @param writer the writer to write the section data to
     * @param streamSection the stream section to write
     */
    protected void writeSection(DataWriter writer, RwStreamChunk streamSection) {
        writeSection(writer, streamSection, true);
    }


    /**
     * Writes an RwStreamChunk to the writer.
     * @param writer the writer to write the section data to
     * @param streamSection the stream section to write
     * @param showInUI Whether it should be shown in the UI. This is rarely false.
     */
    protected void writeSection(DataWriter writer, RwStreamChunk streamSection, boolean showInUI) {
        if (streamSection == null)
            throw new RuntimeException("streamSection");

        streamSection.save(writer);
        registerChildSection(streamSection, showInUI);
    }

    /**
     * Writes an RwStructChunk to the writer.
     * @param writer the writer to write the section data to
     * @param value the struct to write
     */
    protected <TStruct extends RwStruct> void writeStruct(DataWriter writer, TStruct value) {
        writeStruct(writer, value, true);
    }

    /**
     * Writes an RwStructChunk to the writer.
     * @param writer the writer to write the section data to
     * @param value the struct to write
     * @param showInUI Whether it should be shown in the UI. This is rarely false.
     */
    protected <TStruct extends RwStruct> void writeStruct(DataWriter writer, TStruct value, boolean showInUI) {
        if (value == null)
            throw new RuntimeException("value");

        writeSection(writer, new RwStructChunk<>(this.streamFile, this.version, this, value), showInUI);
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
    public final String toString() {
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