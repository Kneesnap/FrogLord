package net.highwayfrogs.editor.games.renderware;

import javafx.scene.image.Image;
import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.generic.data.GameData.SharedGameData;
import net.highwayfrogs.editor.games.renderware.IRwStreamChunkType.RwStreamChunkTypeDisplayImportance;
import net.highwayfrogs.editor.games.renderware.chunks.RwPlatformIndependentTextureDictionaryChunk;
import net.highwayfrogs.editor.games.renderware.chunks.RwPlatformIndependentTextureDictionaryChunk.IRwPlatformIndependentTexturePrefix;
import net.highwayfrogs.editor.games.renderware.chunks.RwWorldChunk;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.LazyInstanceLogger;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a RWS (Renderware Stream) file. Can have different extensions such as: rws, dff, bin, etc.
 *
 * Resources:
 *  - <a ref="http://wiki.xentax.com/index.php/RWS"/>
 *  - <a ref="https://www.grandtheftwiki.com/RenderWare"/>
 *  - <a ref="https://gtamods.com/wiki/RenderWare_binary_stream_file"/>
 *  - <a ref="https://gtamods.com/wiki/List_of_RW_section_IDs"/>
 *  - <a ref="https://github.com/aap/rwtools/blob/master/include/renderware.h"/>
 *  - <a ref="https://github.com/scandinavianaddons/rwtools/blob/master/src/renderware.cpp"/>
 *  - <a ref="https://rwsreader.sourceforge.net/"/>
 * Created by Kneesnap on 6/9/2020.
 */
public class RwStreamFile extends SharedGameData {
    @Getter @NonNull private final RwStreamChunkTypeRegistry chunkTypeRegistry;
    @Getter private final List<RwStreamChunk> chunks = new ArrayList<>();
    @Getter private final String locationName;
    private ILogger cachedLogger;

    public static final int HEADER_SIZE_IN_BYTES = 3 * Constants.INTEGER_SIZE;

    public RwStreamFile(GameInstance instance, RwStreamChunkTypeRegistry chunkTypeRegistry) {
        this(instance, chunkTypeRegistry, null);
    }

    public RwStreamFile(GameInstance instance, RwStreamChunkTypeRegistry chunkTypeRegistry, String locationName) {
        super(instance);
        this.chunkTypeRegistry = chunkTypeRegistry;
        this.locationName = locationName;
    }

    @Override
    public ILogger getLogger() {
        if (this.locationName == null)
            return super.getLogger();

        if (this.cachedLogger == null)
            this.cachedLogger = new LazyInstanceLogger(getGameInstance(), RwStreamFile::getLoggerInfo, this);

        return this.cachedLogger;
    }

    @Override
    public void load(DataReader reader) {
        this.chunks.clear();
        while (reader.hasMore())
            this.chunks.add(this.chunkTypeRegistry.readChunk(reader, this));
    }

    @Override
    public void save(DataWriter writer) {
        for (RwStreamChunk chunk : this.chunks)
            chunk.save(writer);
    }

    /**
     * The logger string.
     */
    public final String getLoggerInfo() {
        return Utils.getSimpleName(this) + "{" + getExtraLoggerInfo() + "}";
    }

    /**
     * Gets extra info to include in the logger string.
     */
    public String getExtraLoggerInfo() {
        return this.locationName;
    }

    /**
     * Export all textures in the file.
     * TODO: Toss this later once we have better support.
     * @param outputFolder the file to export textures to
     * @param fileNameCountMap the file-name count map to use.
     */
    public void exportTextures(File outputFolder, Map<String, AtomicInteger> fileNameCountMap) {
        for (RwStreamChunk chunk : this.chunks) {
            if (!(chunk instanceof RwPlatformIndependentTextureDictionaryChunk))
                continue;

            RwPlatformIndependentTextureDictionaryChunk textureDictionaryChunk = (RwPlatformIndependentTextureDictionaryChunk) chunk;
            for (IRwPlatformIndependentTexturePrefix entry : textureDictionaryChunk.getEntries()) {
                for (int i = 0; i < entry.getMipMapImages().size(); i++) {
                    String baseName = entry.makeFileName(i);
                    int num = fileNameCountMap.computeIfAbsent(baseName, key -> new AtomicInteger()).getAndIncrement();

                    FileUtils.makeDirectory(outputFolder);
                    File imageOutputFile = new File(outputFolder, String.format("%s_%02d.png", baseName, num));

                    try {
                        ImageIO.write(entry.getMipMapImages().get(i).getImage(), "png", imageOutputFile);
                    } catch (IOException ex) {
                        Utils.handleError(getLogger(), ex, false, "Failed to save '%s'.", imageOutputFile.getName());
                    }
                }
            }
        }
    }

    /**
     * Gets the best chunk icon to represent the RwStreamFile.
     */
    public Image getBestChunkIcon() {
        // Look through the chunks to find the best chunk to get the icon from.
        Image bestIcon = ImageResource.GHIDRA_ICON_MULTIMEDIA_16.getFxImage();
        RwStreamChunkTypeDisplayImportance bestImportance = null;
        for (int i = 0; i < this.chunks.size(); i++) {
            RwStreamChunk streamChunk = this.chunks.get(i);
            RwStreamChunkTypeDisplayImportance importance = streamChunk.getChunkType().getDisplayImportance();
            if (importance == null || (bestImportance != null && bestImportance.ordinal() >= importance.ordinal()))
                continue;

            bestIcon = streamChunk.getCollectionViewIcon();
            bestImportance = importance;
        }

        return bestIcon;
    }

    /**
     * Returns true if the provided bytes appear to be a valid RWS stream header.
     * @param reader the reader to test
     */
    public static boolean isRwStreamHeader(DataReader reader) {
        if (reader == null || reader.getRemaining() < HEADER_SIZE_IN_BYTES)
            return false;

        reader.jumpTemp(reader.getIndex() + Constants.INTEGER_SIZE); // int typeId; // Skipped
        int chunkReadSize = reader.readInt();
        int version = reader.readInt();
        reader.jumpReturn();
        return chunkReadSize >= 0 && RwVersion.doesVersionAppearValid(version);
    }

    /**
     * Returns true if the provided bytes appear to be a valid RWS stream header.
     * @param rawBytes the bytes to test
     */
    public static boolean isRwStreamHeader(byte[] rawBytes, int index) {
        if (rawBytes == null || rawBytes.length < HEADER_SIZE_IN_BYTES)
            return false;

        // int typeId; // Skipped
        int chunkReadSize = DataUtils.readIntFromBytes(rawBytes, index + Constants.INTEGER_SIZE);
        int version = DataUtils.readIntFromBytes(rawBytes, index + (2 * Constants.INTEGER_SIZE));
        return chunkReadSize >= 0 && RwVersion.doesVersionAppearValid(version);
    }

    /**
     * Returns true if the provided bytes appear to be a valid RWS file.
     * @param rawBytes the bytes to test
     */
    public static boolean isRwStreamFile(byte[] rawBytes) {
        if (rawBytes == null || rawBytes.length < HEADER_SIZE_IN_BYTES)
            return false;

        int readIndex = 0;
        while (rawBytes.length >= readIndex + HEADER_SIZE_IN_BYTES) {
            // int typeId; // Skipped
            int chunkReadSize = DataUtils.readIntFromBytes(rawBytes, readIndex + Constants.INTEGER_SIZE);
            int version = DataUtils.readIntFromBytes(rawBytes, readIndex + (2 * Constants.INTEGER_SIZE));
            readIndex += HEADER_SIZE_IN_BYTES;

            // Byte amount is less than zero or the version index appears invalid.
            if (chunkReadSize < 0 || !RwVersion.doesVersionAppearValid(version))
                return false;

            readIndex += chunkReadSize;
        }

        return readIndex == rawBytes.length;
    }

    /**
     * Handles this file being double-clicked in a UI.
     */
    public void handleDoubleClick() {
        for (int i = 0; i < this.chunks.size(); i++) {
            RwStreamChunk chunk = this.chunks.get(i);
            if (chunk instanceof RwWorldChunk) {
                ((RwWorldChunk) chunk).openMeshView();
                return;
            }
        }
    }
}