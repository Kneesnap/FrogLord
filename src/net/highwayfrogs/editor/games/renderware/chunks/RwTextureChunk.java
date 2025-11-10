package net.highwayfrogs.editor.games.renderware.chunks;

import lombok.Getter;
import net.highwayfrogs.editor.games.renderware.RwStreamChunk;
import net.highwayfrogs.editor.games.renderware.RwStreamChunkType;
import net.highwayfrogs.editor.games.renderware.RwStreamFile;
import net.highwayfrogs.editor.games.renderware.chunks.RwPlatformIndependentTextureDictionaryChunk.IRwPlatformIndependentTexturePrefix;
import net.highwayfrogs.editor.games.renderware.struct.types.RwStreamTexture;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Implements the texture stream chunk as seen in rtpitexd.c/rtpitexdTextureStreamReadPre
 * Created by Kneesnap on 8/14/2024.
 */
@Getter
public class RwTextureChunk extends RwStreamChunk {
    private int texFiltAddr;
    private String name = "";
    private String mask = "";
    private transient RwImageChunk image;

    public RwTextureChunk(RwStreamFile streamFile, int version, RwStreamChunk parentChunk) {
        super(streamFile, RwStreamChunkType.TEXTURE, version, parentChunk);
    }

    @Override
    protected void loadChunkData(DataReader reader, int dataLength, int version) {
        this.texFiltAddr = readStruct(reader, RwStreamTexture.class).getFilterAndAddress();
        /*int addressingU = (texFiltAddr.getFilterAndAddress() >> 8) & 0x0F;
        int addressingV = (texFiltAddr.getFilterAndAddress() >> 12) & 0x0F;
        int flags = (texFiltAddr.getFilterAndAddress() >> 16) & 0xFF;*/

        this.name = readString(reader);
        this.mask = readString(reader);
        readOptionalExtensionData(reader);
    }

    @Override
    protected void saveChunkData(DataWriter writer) {
        writeStruct(writer, new RwStreamTexture(getGameInstance(), this.texFiltAddr));
        writeChunk(writer, new RwStringChunk(getStreamFile(), getVersion(), this, this.name));
        writeChunk(writer, new RwStringChunk(getStreamFile(), getVersion(), this, this.mask));
        writeOptionalExtensionData(writer);
    }

    @Override
    public String getCollectionViewDisplayName() {
        return super.getCollectionViewDisplayName() + (this.name != null && this.name.trim().length() > 0 ? " [" + this.name + "]" : "");
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        super.addToPropertyList(propertyList);
        if (this.name != null && this.name.trim().length() > 0)
            propertyList.add("Name", this.name);
        if (this.mask != null && this.mask.trim().length() > 0)
            propertyList.add("Mask", this.mask);
        propertyList.add("Flags", NumberUtils.toHexString(this.texFiltAddr));
    }

    @Override
    protected String getLoggerInfo() {
        return super.getLoggerInfo()
                + (this.name != null && this.name.trim().length() > 0 ? ",name='" + this.name + "'" : "")
                + (this.mask != null && this.mask.trim().length() > 0 ? ",mask='" + this.mask + "'" : "")
                + ",flags=" + NumberUtils.toHexString(this.texFiltAddr);
    }

    /**
     * Resolves the image which this texture chunk uses.
     */
    public RwImageChunk getImage() {
        if (this.image == null) {
            if (this.name == null)
                return null;

            for (RwStreamChunk streamChunk : getStreamFile().getChunks()) {
                if (!(streamChunk instanceof RwPlatformIndependentTextureDictionaryChunk))
                    continue;

                RwPlatformIndependentTextureDictionaryChunk pitexChunk = (RwPlatformIndependentTextureDictionaryChunk) streamChunk;
                for (int i = 0; i < pitexChunk.getEntries().size(); i++) {
                    IRwPlatformIndependentTexturePrefix entry = pitexChunk.getEntries().get(i);
                    if (this.name.equalsIgnoreCase(entry.getName())) {
                        RwImageChunk image = entry.getLargestImage();
                        if (image != null)
                            return this.image = image;
                    }
                }
            }
        }

        return this.image;
    }
}