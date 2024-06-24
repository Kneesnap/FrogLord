package net.highwayfrogs.editor.games.sony.medievil.map.packet;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapCollprim;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the MediEvil map format collprim packet.
 * Created by RampantSpirit on 3/13/2024.
 */
@Getter
public class MediEvilMapCollprimsPacket extends MediEvilMapPacket implements IPropertyListCreator {
    public static final String IDENTIFIER = "PLOC"; // 'COLP'.
    private final List<MediEvilMapCollprim> collprims = new ArrayList<>();

    public MediEvilMapCollprimsPacket(MediEvilMapFile parentFile) {
        super(parentFile, IDENTIFIER);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        int collprimCount = reader.readUnsignedShortAsInt();
        reader.skipBytesRequireEmpty(Constants.SHORT_SIZE); // Padding
        int collprimListPtr = reader.readInt();
        if (collprimListPtr != reader.getIndex()) {
            getLogger().warning("The collprim list pointer was not at the expected position! (Was: " + Utils.toHexString(collprimListPtr) + ", Expected: " + Utils.toHexString(reader.getIndex()));
            reader.setIndex(collprimListPtr);
        }

        // Read collprims.
        this.collprims.clear();
        for (int i = 0; i < collprimCount; i++) {
            MediEvilMapCollprim collprim = new MediEvilMapCollprim(getParentFile());
            collprim.load(reader);
            this.collprims.add(collprim);
        }
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        writer.writeUnsignedShort(this.collprims.size());
        writer.writeUnsignedShort(0); // Padding
        int collprimListPtr = writer.writeNullPointer();

        // Write collprims.
        writer.writeAddressTo(collprimListPtr);
        for (int i = 0; i < this.collprims.size(); i++)
            this.collprims.get(i).save(writer);
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList.add("Collision Primitives", this.collprims.size());
        return propertyList;
    }
}