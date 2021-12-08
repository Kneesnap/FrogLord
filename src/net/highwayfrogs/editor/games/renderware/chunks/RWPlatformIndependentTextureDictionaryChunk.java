package net.highwayfrogs.editor.games.renderware.chunks;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.renderware.RWSChunk;
import net.highwayfrogs.editor.games.renderware.RWSChunkManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Platform Independent Texture Dictionary
 * Created by Kneesnap on 6/9/2020.
 */
@Getter
public class RWPlatformIndependentTextureDictionaryChunk extends RWSChunk {
    private final List<RWPlatformIndependentTextureEntry> entries = new ArrayList<>();

    public static final int TYPE_ID = 0x23;

    public RWPlatformIndependentTextureDictionaryChunk(int renderwareVersion, RWSChunk parentChunk) {
        super(TYPE_ID, renderwareVersion, parentChunk);
    }

    @Override
    public void loadChunkData(DataReader reader) {
        short imageCount = reader.readShort(); // Might be number of textures, might be number of frames.
        short creatorVersion = reader.readShort(); // Supported: 0

        if (creatorVersion == (short) 0) {
            // _rtpitexdImage2TextureReadLegacy

            for (int i = 0; i < imageCount; i++) {
                RWPlatformIndependentTextureEntry entry = new RWPlatformIndependentTextureEntry(this);
                entry.load(reader);
                this.entries.add(entry);
            }

        } else {
            throw new UnsupportedOperationException("Texture Dictionary with creator version " + creatorVersion + " is unsupported!");
        }
    }

    @Override
    public void saveChunkData(DataWriter writer) {

    }

    @Getter
    public static class RWPlatformIndependentTextureEntry extends GameObject {
        private final RWPlatformIndependentTextureDictionaryChunk parentChunk;
        private String name;
        private String mask;
        private int flags;
        private final List<RWImageChunk> mipLevelImages = new ArrayList<>();

        public RWPlatformIndependentTextureEntry(RWPlatformIndependentTextureDictionaryChunk chunk) {
            this.parentChunk = chunk;
        }

        @Override
        public void load(DataReader reader) {
            this.name = reader.readTerminatedStringOfLength(32);
            this.mask = reader.readTerminatedStringOfLength(32);
            int mipLevelCount = reader.readInt();
            this.flags = reader.readInt();
            for (int i = 0; i < mipLevelCount; i++)
                this.mipLevelImages.add((RWImageChunk) RWSChunkManager.readChunk(reader, this.parentChunk));
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
