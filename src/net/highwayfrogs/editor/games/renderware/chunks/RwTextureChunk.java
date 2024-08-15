package net.highwayfrogs.editor.games.renderware.chunks;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.renderware.RwStreamChunk;
import net.highwayfrogs.editor.games.renderware.RwStreamChunkType;
import net.highwayfrogs.editor.games.renderware.RwStreamFile;
import net.highwayfrogs.editor.games.renderware.struct.types.RwStreamTexture;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Implements the texture stream chunk as seen in rtpitexd.c/rtpitexdTextureStreamReadPre
 * Created by Kneesnap on 8/14/2024.
 */
@Getter
public class RwTextureChunk extends RwStreamChunk {
    private int texFiltAddr;
    private String name = "";
    private String mask = "";
    private RwExtensionChunk extensionData;

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
        this.extensionData = reader.hasMore() ? readChunk(reader, RwExtensionChunk.class) : null;
    }

    @Override
    protected void saveChunkData(DataWriter writer) {
        writeStruct(writer, new RwStreamTexture(getGameInstance(), this.texFiltAddr));
        writeChunk(writer, new RwStringChunk(getStreamFile(), getVersion(), this, this.name));
        writeChunk(writer, new RwStringChunk(getStreamFile(), getVersion(), this, this.mask));
        if (this.extensionData != null)
            writeChunk(writer, this.extensionData);
    }

    @Override
    public String getCollectionViewDisplayName() {
        return super.getCollectionViewDisplayName() + (this.name != null && this.name.trim().length() > 0 ? " [" + this.name + "]" : "");
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        if (this.name != null && this.name.trim().length() > 0)
            propertyList.add("Name", this.name);
        if (this.mask != null && this.mask.trim().length() > 0)
            propertyList.add("Mask", this.mask);
        propertyList.add("Flags", Utils.toHexString(this.texFiltAddr));
        return propertyList;
    }

    @Override
    protected String getLoggerInfo() {
        return super.getLoggerInfo()
                + (this.name != null && this.name.trim().length() > 0 ? ",name='" + this.name + "'" : "")
                + (this.mask != null && this.mask.trim().length() > 0 ? ",mask='" + this.mask + "'" : "")
                + ",flags=" + Utils.toHexString(this.texFiltAddr);
    }
}