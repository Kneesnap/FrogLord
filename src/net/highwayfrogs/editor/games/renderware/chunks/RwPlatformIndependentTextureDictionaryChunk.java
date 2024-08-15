package net.highwayfrogs.editor.games.renderware.chunks;

import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.renderware.RwStreamChunk;
import net.highwayfrogs.editor.games.renderware.RwStreamChunkType;
import net.highwayfrogs.editor.games.renderware.RwStreamFile;
import net.highwayfrogs.editor.games.renderware.RwVersion;
import net.highwayfrogs.editor.games.renderware.chunks.RwImageChunk.RwImageViewUIController;
import net.highwayfrogs.editor.games.renderware.ui.IRwStreamChunkUIEntry;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.CollectionViewComponent.ICollectionViewEntry;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.IBinarySerializable;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Platform Independent Texture Dictionary
 * Created by Kneesnap on 6/9/2020.
 */
@Getter
public class RwPlatformIndependentTextureDictionaryChunk extends RwStreamChunk {
    private final List<IRwPlatformIndependentTexturePrefix> entries = new ArrayList<>();
    private int textureFormatVersion;

    public RwPlatformIndependentTextureDictionaryChunk(RwStreamFile streamFile, int renderwareVersion, RwStreamChunk parentChunk) {
        super(streamFile, RwStreamChunkType.PITEX_DICTIONARY, renderwareVersion, parentChunk);
        this.textureFormatVersion = RwVersion.isAtLeast(renderwareVersion, RwVersion.VERSION_3603) ? 1 : 0; // As seen in Frogger. This check can probably be changed.
    }

    @Override
    public void loadChunkData(DataReader reader, int dataLength, int version) {
        // rtpitexdChunkPrefix
        int numTextures = reader.readUnsignedShortAsInt(); // Might be number of textures, might be number of frames.
        this.textureFormatVersion = reader.readUnsignedShortAsInt(); // Supported: 0, 1.

        this.entries.clear();
        if (this.textureFormatVersion == 0 || this.textureFormatVersion == 1) {
            for (int i = 0; i < numTextures; i++) {
                IRwPlatformIndependentTexturePrefix entry = createNewTexturePrefix();
                entry.load(reader);
                this.entries.add(entry);
                this.childUISections.add(entry);
            }
        } else {
            throw new UnsupportedOperationException("Texture Dictionary with texture format version " + this.textureFormatVersion + " is unsupported!");
        }
    }

    /**
     * Creates a new texture prefix for the version which is currently active.
     */
    public IRwPlatformIndependentTexturePrefix createNewTexturePrefix() {
        switch (this.textureFormatVersion) {
            case 0:
                return new RwPlatformIndependentTexturePrefixLegacy(this);
            case 1:
                return new RwPlatformIndependentTexturePrefix(this);
            default:
                throw new UnsupportedOperationException("Texture Dictionary with texture format version " + this.textureFormatVersion + " is unsupported!");
        }
    }

    @Override
    public void saveChunkData(DataWriter writer) {
        writer.writeUnsignedShort(this.entries.size());
        writer.writeUnsignedShort(this.textureFormatVersion);

        if (this.textureFormatVersion == 0 || this.textureFormatVersion == 1) {
            for (int i = 0; i < this.entries.size(); i++) {
                IRwPlatformIndependentTexturePrefix texturePrefix = this.entries.get(i);
                texturePrefix.save(writer);
                this.childUISections.add(texturePrefix);
            }
        } else {
            throw new UnsupportedOperationException("Texture Dictionary with texture format version " + this.textureFormatVersion + " is unsupported!");
        }
    }

    @Override
    protected String getLoggerInfo() {
        return super.getLoggerInfo() + ",entries=" + this.entries.size();
    }

    public interface IRwPlatformIndependentTexturePrefix extends IBinarySerializable, IRwStreamChunkUIEntry {
        /**
         * Gets the mip map images at different levels of detail.
         */
        List<RwImageChunk> getMipMapImages();

        /**
         * Gets the texture name.
         */
        String getName();

        /**
         * Gets the texture mask name
         */
        String getMask();

        /**
         * Gets the texture flags.
         */
        int getFlags();

        /**
         * Creates a file name for the given image.
         * @param mipMapLevel the mip map level of detail id for the image
         * @return fileName
         */
        default String makeFileName(int mipMapLevel) {
            StringBuilder sb = new StringBuilder();

            String name = getName();
            String mask = getMask();
            if (name == null || name.isEmpty()) {
                sb.append("UNKNOWN_NAME");
            } else {
                sb.append(name);
            }

            if (mipMapLevel >= 0 && getMipMapImages().size() > 1)
                sb.append("-MIPMAP_").append(mipMapLevel);
            if (mask != null && mask.trim().length() > 0)
                sb.append(" (Mask ").append(mask).append(")");
            return sb.toString();
        }

        /**
         * Gets the string to show for the image when displayed in UI elements.
         * @param mipMapId the mipMapId to apply
         * @return displayString
         */
        default String getDisplayInfo(int mipMapId) {
            String name = getName();
            String mask = getMask();
            int flags = getFlags();
            return (name != null && name.length() > 0 ? "'" + name + "'" : "Unknown Name")
                    + (getMipMapImages().size() > 1 && mipMapId >= 0 ? ", mipMap=" + mipMapId : "")
                    + (mask != null && mask.trim().length() > 0 ? ", Mask: '" + mask + "'" : "")
                    + (flags != 0 ? ", Flags: " + Utils.toHexString(flags) : "");
        }

        /**
         * Adds properties to the property list.
         * @param propertyList the PropertyList to populate
         */
        default PropertyList addToPropertyList(PropertyList propertyList) {
            String name = getName();
            if (name != null && name.length() > 0)
                propertyList.add("Name", name);

            String mask = getMask();
            if (mask != null && mask.length() > 0)
                propertyList.add("Mask", mask);

            propertyList.add("Mip-Map Images", getMipMapImages().size());
            propertyList.add("Flags", Utils.toHexString(getFlags()));
            return propertyList;
        }
    }

    @Getter
    public static abstract class RwPlatformIndependentTexturePrefixBase extends SharedGameData implements IRwPlatformIndependentTexturePrefix {
        protected final RwPlatformIndependentTextureDictionaryChunk parentChunk;
        protected final List<RwImageChunk> mipMapImages = new ArrayList<>();

        public RwPlatformIndependentTexturePrefixBase(RwPlatformIndependentTextureDictionaryChunk chunk) {
            super(chunk.getGameInstance());
            this.parentChunk = chunk;
        }

        @Override
        public List<? extends IRwStreamChunkUIEntry> getChildUISections() {
            return this.mipMapImages;
        }

        @Override
        public RwStreamFile getStreamFile() {
            return this.parentChunk.getStreamFile();
        }

        @Override
        public GameUIController<?> makeEditorUI() {
            RwImageChunk largestImage = null;
            int largestArea = Integer.MIN_VALUE;
            for (int i = 0; i < this.mipMapImages.size(); i++) {
                RwImageChunk image = this.mipMapImages.get(i);
                int tempArea = image.getImage().getWidth() * image.getImage().getHeight();
                if (tempArea > largestArea) {
                    largestArea = tempArea;
                    largestImage = image;
                }
            }

            return largestImage != null ? new RwImageViewUIController(largestImage) : null;
        }

        @Override
        public ICollectionViewEntry getCollectionViewParentEntry() {
            return this.parentChunk;
        }

        @Override
        public String getCollectionViewDisplayName() {
            return "rtpiTexturePrefix [" + getName() + "]";
        }

        @Override
        public String getCollectionViewDisplayStyle() {
            return null;
        }

        @Override
        public Image getCollectionViewIcon() {
            return ImageResource.PHOTO_ALBUM_15.getFxImage();
        }

        @Override
        public abstract void load(DataReader reader);

        @Override
        public abstract void save(DataWriter writer);

        @Override
        public abstract String getName();

        @Override
        public abstract String getMask();

        @Override
        public abstract int getFlags();
    }

    @Getter
    public static class RwPlatformIndependentTexturePrefixLegacy extends RwPlatformIndependentTexturePrefixBase {
        private String name = "";
        private String mask = "";
        private int flags;

        private static final int TEXTURE_BASE_NAME_LENGTH = 32;

        public RwPlatformIndependentTexturePrefixLegacy(RwPlatformIndependentTextureDictionaryChunk chunk) {
            super(chunk);
        }

        @Override
        public void load(DataReader reader) {
            this.name = reader.readTerminatedStringOfLength(TEXTURE_BASE_NAME_LENGTH);
            this.mask = reader.readTerminatedStringOfLength(TEXTURE_BASE_NAME_LENGTH);
            int mipLevelCount = reader.readInt();
            this.flags = reader.readInt();

            this.mipMapImages.clear();
            for (int i = 0; i < mipLevelCount; i++)
                this.mipMapImages.add(this.parentChunk.readChunk(reader, new RwImageChunk(this.parentChunk.getStreamFile(), this.parentChunk.getVersion(), this.parentChunk, this, i), false));
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeTerminatedStringOfLength(this.name, TEXTURE_BASE_NAME_LENGTH);
            writer.writeTerminatedStringOfLength(this.mask, TEXTURE_BASE_NAME_LENGTH);
            writer.writeInt(this.mipMapImages.size());
            writer.writeInt(this.flags);
            for (int i = 0; i < this.mipMapImages.size(); i++)
                this.parentChunk.writeChunk(writer, this.mipMapImages.get(i));
        }
    }

    @Getter
    public static class RwPlatformIndependentTexturePrefix extends RwPlatformIndependentTexturePrefixBase {
        private final RwTextureChunk texture;

        public RwPlatformIndependentTexturePrefix(RwPlatformIndependentTextureDictionaryChunk chunk) {
            super(chunk);
            this.texture = new RwTextureChunk(chunk.getStreamFile(), chunk.getVersion(), chunk);
        }

        @Override
        public void load(DataReader reader) {
            this.mipMapImages.clear();
            int mipLevelCount = reader.readInt();
            for (int i = 0; i < mipLevelCount; i++)
                this.mipMapImages.add(this.parentChunk.readChunk(reader, new RwImageChunk(this.parentChunk.getStreamFile(), this.parentChunk.getVersion(), this.parentChunk, this, i), false));
            this.parentChunk.readChunk(reader, this.texture);
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeInt(this.mipMapImages.size());
            for (int i = 0; i < this.mipMapImages.size(); i++)
                this.parentChunk.writeChunk(writer, this.mipMapImages.get(i));
            this.parentChunk.writeChunk(writer, this.texture);
        }

        @Override
        public String getName() {
            return this.texture.getName();
        }

        @Override
        public String getMask() {
            return this.texture.getMask();
        }

        @Override
        public int getFlags() {
            return this.texture.getTexFiltAddr();
        }
    }
}