package net.highwayfrogs.editor.games.renderware.chunks;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.renderware.RwStreamChunk;
import net.highwayfrogs.editor.games.renderware.RwStreamFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Platform Independent Texture Dictionary
 * Created by Kneesnap on 6/9/2020.
 */
@Getter
public class RwPlatformIndependentTextureDictionaryChunk extends RwStreamChunk {
    private final List<RwPlatformIndependentTextureEntry> entries = new ArrayList<>();

    public static final int TYPE_ID = 0x23;

    public RwPlatformIndependentTextureDictionaryChunk(RwStreamFile streamFile, int renderwareVersion, RwStreamChunk parentChunk) {
        super(streamFile, TYPE_ID, renderwareVersion, parentChunk);
    }

    @Override
    public void loadChunkData(DataReader reader, int dataLength, int version) {
        short imageCount = reader.readShort(); // Might be number of textures, might be number of frames.
        short creatorVersion = reader.readShort(); // Supported: 0

        if (creatorVersion == (short) 0) {
            // _rtpitexdImage2TextureReadLegacy / RtPITexDictionaryStreamRead (rpitexd.o)

            for (int i = 0; i < imageCount; i++) {
                RwPlatformIndependentTextureEntry entry = new RwPlatformIndependentTextureEntry(this);
                entry.load(reader);
                this.entries.add(entry);
            }
        } else if (creatorVersion == (short) 1){
            getLogger().warning("Unknown texture format! Skipping!!!");
            reader.skipBytes(Math.min(dataLength, reader.getRemaining()));
            // _rtpitexdImage2TextureRead (rpitexd.o)
            // TODO: IMPLEMENT.
        } else {
            throw new UnsupportedOperationException("Texture Dictionary with creator version " + creatorVersion + " is unsupported!");
        }
    }
    @Override
    protected String getLoggerInfo() {
        return super.getLoggerInfo() + ",entries=" + this.entries.size();
    }


    @Override
    public void saveChunkData(DataWriter writer) {
        // TODO: Implement.
    }

    @Getter
    public static class RwPlatformIndependentTextureEntry extends GameObject {
        private final RwPlatformIndependentTextureDictionaryChunk parentChunk;
        private String name;
        private String mask;
        private int flags;
        private final List<RwImageChunk> mipLevelImages = new ArrayList<>();

        public RwPlatformIndependentTextureEntry(RwPlatformIndependentTextureDictionaryChunk chunk) {
            this.parentChunk = chunk;
        }

        @Override
        public void load(DataReader reader) {
            this.name = reader.readTerminatedStringOfLength(32);
            this.mask = reader.readTerminatedStringOfLength(32);
            int mipLevelCount = reader.readInt();
            this.flags = reader.readInt();
            for (int i = 0; i < mipLevelCount; i++)
                this.mipLevelImages.add(this.parentChunk.readChunk(reader, RwImageChunk.class));
        }

        @Override
        public void save(DataWriter writer) {
            // TODO
        }

        /**
         * Makes a file name to save a particular image as.
         * @param mipLevelId The mip level id.
         * @return fileName
         */
        public String makeFileName(int mipLevelId) {
            StringBuilder sb = new StringBuilder();

            if (this.name == null || this.name.isEmpty()) {
                sb.append("INVALID_NAME");
            } else {
                sb.append(this.name);
            }

            if (mipLevelId >= 0 && this.mipLevelImages.size() > 1)
                sb.append("-").append(mipLevelId);
            if (this.mask != null && this.mask.trim().length() > 0)
                sb.append(" (").append(this.mask).append(")");
            return sb.toString();
        }
    }
}