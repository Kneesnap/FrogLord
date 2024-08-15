package net.highwayfrogs.editor.games.renderware.struct.types;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.renderware.RwUtils;
import net.highwayfrogs.editor.games.renderware.RwVersion;
import net.highwayfrogs.editor.games.renderware.struct.RwStruct;
import net.highwayfrogs.editor.games.renderware.struct.RwStructType;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Implemented from 'babinworld.h'
 * Created by Kneesnap on 8/12/2024.
 */
@Getter
public class RpWorldChunkInfo extends RwStruct {
    private boolean rootIsWorldSector;
    private final RwV3d invWorldOrigin;
    private int numTriangles;
    private int numVertices;
    private int numPlaneSectors;
    private int numWorldSectors;
    private int colSectorSize;
    private int format; // Flags about the world.
    private RwBBox boundingBox; // Null in early versions.

    public RpWorldChunkInfo(GameInstance instance) {
        super(instance, RwStructType.WORLD);
        this.invWorldOrigin = new RwV3d(instance);
    }

    @Override
    public void load(DataReader reader, int version, int byteLength) {
        int readStartIndex = reader.getIndex();
        this.rootIsWorldSector = RwUtils.readRwBool(reader);
        this.invWorldOrigin.load(reader, version, byteLength - (reader.getIndex() - readStartIndex));
        this.numTriangles = reader.readInt();
        this.numVertices = reader.readInt();
        this.numPlaneSectors = reader.readInt();
        this.numWorldSectors = reader.readInt();
        this.colSectorSize = reader.readInt();
        this.format = reader.readInt();

        if (RwVersion.isAtLeast(version, RwVersion.VERSION_3403)) {
            this.boundingBox = new RwBBox(getGameInstance());
            this.boundingBox.load(reader, version, byteLength - (reader.getIndex() - readStartIndex));
        }

        if (this.colSectorSize > 0)
            getLogger().warning("The BSP has not been exported since RW3.03, and contains collision data that is not currently supported.");
    }

    @Override
    public void save(DataWriter writer, int version) {
        RwUtils.writeRwBool(writer, this.rootIsWorldSector);
        this.invWorldOrigin.save(writer, version);
        writer.writeInt(this.numTriangles);
        writer.writeInt(this.numVertices);
        writer.writeInt(this.numPlaneSectors);
        writer.writeInt(this.numWorldSectors);
        writer.writeInt(this.colSectorSize);
        writer.writeInt(this.format);

        if (RwVersion.isAtLeast(version, RwVersion.VERSION_3403)) {
            if (this.boundingBox == null)
                throw new NullPointerException("Expected a non-null boundingBox for saving.");

            this.boundingBox.save(writer, version);
        }
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("Root Is World Sector", this.rootIsWorldSector);
        propertyList.add("Inv World Origin", this.invWorldOrigin);
        propertyList.add("Triangles", this.numTriangles);
        propertyList.add("Vertices", this.numVertices);
        propertyList.add("Plane Sectors", this.numPlaneSectors);
        propertyList.add("Collision Sector Size", this.colSectorSize);
        propertyList.add("World Format Flags", Utils.toHexString(this.format));
        if (this.boundingBox != null)
            propertyList.add("Bounding Box", this.boundingBox);

        return propertyList;
    }
}