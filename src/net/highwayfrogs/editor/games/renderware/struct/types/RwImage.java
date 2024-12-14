package net.highwayfrogs.editor.games.renderware.struct.types;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.renderware.struct.RwStruct;
import net.highwayfrogs.editor.games.renderware.struct.RwStructType;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;

/**
 * Implements the __rwImage struct defined in baimage.c
 * Created by Kneesnap on 8/12/2024.
 */
@Getter
public class RwImage extends RwStruct {
    private int width;
    private int height;
    private int depth; // bitDepth
    private int stride; // Number of bytes to go from the first pixel in a row to the address of the pixel in the next row. (https://medium.com/@oleg.shipitko/what-does-stride-mean-in-image-processing-bba158a72bcd)

    public RwImage(GameInstance instance) {
        super(instance, RwStructType.IMAGE);
    }

    public RwImage(GameInstance instance, int width, int height, int depth, int stride) {
        super(instance, RwStructType.IMAGE);
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.stride = stride;
    }

    @Override
    public void load(DataReader reader, int version, int byteLength) {
        this.width = reader.readInt();
        this.height = reader.readInt();
        this.depth = reader.readInt();
        this.stride = reader.readInt();
    }

    @Override
    public void save(DataWriter writer, int version) {
        writer.writeInt(this.width);
        writer.writeInt(this.height);
        writer.writeInt(this.depth);
        writer.writeInt(this.stride);
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("Image Dimensions", this.width + " x " + this.height);
        propertyList.add("Depth", this.depth);
        propertyList.add("Stride", this.stride);
        return propertyList;
    }

    @Override
    public String toString() {
        return "RwImage{dimensions=" + this.width + "x" + this.height + ",depth=" + this.depth + ",stride=" + this.stride + "}";
    }
}