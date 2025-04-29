package net.highwayfrogs.editor.games.renderware.chunks;

import lombok.Getter;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.renderware.RwStreamChunk;
import net.highwayfrogs.editor.games.renderware.RwStreamChunkType;
import net.highwayfrogs.editor.games.renderware.RwStreamFile;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.NumberUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents the table of contents chunk.
 * Created by Kneesnap on 8/10/2024.
 */
@Getter
public class RwTableOfContentsChunk extends RwStreamChunk {
    private final List<RwTableOfContentsChunkEntry> entries = new ArrayList<>();

    public RwTableOfContentsChunk(RwStreamFile streamFile, int version, RwStreamChunk parentChunk) {
        super(streamFile, RwStreamChunkType.TOC, version, parentChunk);
    }

    @Override
    protected void loadChunkData(DataReader reader, int dataLength, int version) {
        int numEntries = reader.readInt();

        this.entries.clear();
        for (int i = 0; i < numEntries; i++) {
            RwTableOfContentsChunkEntry newEntry = new RwTableOfContentsChunkEntry(getGameInstance());
            newEntry.load(reader);
            this.entries.add(newEntry);
        }
    }

    @Override
    protected void saveChunkData(DataWriter writer) {
        writer.writeInt(this.entries.size());
        for (int i = 0; i < this.entries.size(); i++)
            this.entries.get(i).save(writer);
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("Entry Count", this.entries.size());
        for (int i = 0; i < this.entries.size(); i++)
            propertyList.add("Entry #" + (i + 1), this.entries.get(i));

        return propertyList;
    }

    public static class RwTableOfContentsChunkEntry extends SharedGameData {
        private int chunkId; // Lookup in the chunk type registry.
        private int gameId; // TODO: What is this?
        private int offset; // Offset address of the chunk header from the start of the file.
        private byte[] guid; // TODO: Can we allow converting to the java UUID object? Maybe in a static RwUtils.java file. Would be nice to store this as a UUID for easy toString() access, and then just convert back when it's time to save.

        public RwTableOfContentsChunkEntry(GameInstance instance) {
            super(instance);
        }

        @Override
        public void load(DataReader reader) {
            this.chunkId = reader.readInt();
            this.gameId = reader.readInt();
            this.offset = reader.readInt();
            this.guid = reader.readBytes(16); // TODO: Hardcode bad.

            /* Convert to the correct platform endianess */
            /*(void) RwMemNative32( & tocEntry -> id, sizeof(RwCorePluginID));
            (void) RwMemNative32( & tocEntry -> offset, sizeof(RwUInt32));
            (void) RwMemNative32( & tocEntry -> guid.data1, sizeof(RwUInt32));
            (void) RwMemNative16( & tocEntry -> guid.data2, sizeof(RwUInt16));
            (void) RwMemNative16( & tocEntry -> guid.data3, sizeof(RwUInt16));*/ // TODO: not sure how we want to deal with endianness yet. Perhaps we try loading the Gamecube version (uses PowerPC / big endian) and compare it to PC.
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeInt(this.chunkId);
            writer.writeInt(this.gameId);
            writer.writeInt(this.offset);
            writer.writeBytes(this.guid);
        }

        @Override
        public String toString() {
            return "ChunkDef{offset=" + NumberUtils.toHexString(this.offset) + ",game=" + Integer.toHexString(this.gameId).toUpperCase()
                    + ",type=" + Integer.toHexString(this.chunkId).toUpperCase() + ",guid=" + Arrays.toString(this.guid) + "}";
        }
    }
}