package net.highwayfrogs.editor.games.renderware.struct.types;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.renderware.struct.RwStruct;
import net.highwayfrogs.editor.games.renderware.struct.RwStructType;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.NumberUtils;

/**
 * Implements the 'rwStreamTexture' struct defined in babintex.c
 * Created by Kneesnap on 8/14/2024.
 */
@Getter
public class RwStreamTexture extends RwStruct {
    private int filterAndAddress;

    public RwStreamTexture(GameInstance instance) {
        super(instance, RwStructType.STREAM_TEXTURE);
    }

    public RwStreamTexture(GameInstance instance, int filterAndAddress) {
        super(instance, RwStructType.STREAM_TEXTURE);
        this.filterAndAddress = filterAndAddress;
    }

    @Override
    public void load(DataReader reader, int version, int byteLength) {
        this.filterAndAddress = reader.readInt();
    }

    @Override
    public void save(DataWriter writer, int version) {
        writer.writeInt(this.filterAndAddress);
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("Filter & Address Flags", NumberUtils.toHexString(this.filterAndAddress));
        return propertyList;
    }
}