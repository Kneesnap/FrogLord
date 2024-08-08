package net.highwayfrogs.editor.games.renderware;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameData.SharedGameData;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.List;

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
@Getter
public class RwStreamFile extends SharedGameData {
    @NonNull private final RwStreamChunkTypeRegistry chunkTypeRegistry;
    private final List<RwStreamChunk> chunks = new ArrayList<>();
    @Setter private String locationName;

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
     * Returns true if the provided bytes appear to be a valid RWS file.
     * @param rawBytes the bytes to test
     */
    public static boolean isRwStreamFile(byte[] rawBytes) {
        if (rawBytes == null || rawBytes.length < HEADER_SIZE_IN_BYTES)
            return false;

        int readIndex = 0;
        while (rawBytes.length >= readIndex + HEADER_SIZE_IN_BYTES) {
            // int typeId; // Skipped
            int chunkReadSize = Utils.readIntFromBytes(rawBytes, readIndex + Constants.INTEGER_SIZE);
            int version = Utils.readIntFromBytes(rawBytes, readIndex + (2 * Constants.INTEGER_SIZE));
            readIndex += HEADER_SIZE_IN_BYTES;

            // Byte amount is less than zero or the version index appears invalid.
            if (chunkReadSize < 0 || !RwVersion.doesVersionAppearValid(version))
                return false;

            readIndex += chunkReadSize;
        }

        return readIndex == rawBytes.length;
    }
}