package net.highwayfrogs.editor.file.map.light;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.FroggerMapLight;
import net.highwayfrogs.editor.games.sony.shared.misc.MRLightType;

/**
 * Holds lighting data, or the "LIGHT" struct in mapdisp.H
 * Created by Kneesnap on 8/24/2018.
 */
@Getter
@Setter
public class Light extends GameObject {
    private short priority; // Always 131 for PARALLEL, 4 or 128 = UNKNOWN (4 is seen in SWP2.MAP), 130 = POINT, 255 = AMBIENT
    private int parentId;
    private MRLightType apiType = MRLightType.AMBIENT;
    private int color; // BbGgRr
    private SVector position = new SVector(); // This is probably the place in the world the light is placed in the editor.
    private SVector direction = new SVector(); // When this is AMBIENT, I think this is arbitrary. When this is parallel, it seems to be a 12bit normalized direction vector. When this is point it is unused.
    private int attribute0; // "falloff if point, umbra angle if spot."
    private int attribute1;

    public Light() {
    }

    public Light(MRLightType apiLightType) {
        this.apiType = apiLightType;
    }

    @Override
    public void load(DataReader reader) {
        short lightType = reader.readUnsignedByteAsShort();
        if (lightType != 1)
            throw new RuntimeException("Light type was not STATIC, instead it was " + lightType + ".");

        this.priority = reader.readUnsignedByteAsShort();
        this.parentId = reader.readUnsignedShortAsInt();
        this.apiType = MRLightType.getType(reader.readUnsignedByteAsShort());
        reader.skipBytes(3); // Padding (Always zero)
        this.color = reader.readInt();
        this.position = SVector.readWithPadding(reader);
        this.direction = SVector.readWithPadding(reader);
        this.attribute0 = reader.readUnsignedShortAsInt();
        this.attribute1 = reader.readUnsignedShortAsInt();
        reader.skipPointer(); // Frame pointer. Only has a value at run-time.
        reader.skipPointer(); // Object pointer. Only has a value at run-time.
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedByte((short) 1); // Light type. Always STATIC.
        writer.writeUnsignedByte(this.priority);
        writer.writeUnsignedShort(this.parentId);
        writer.writeUnsignedByte((short) this.apiType.getBitFlagMask());
        writer.writeNull(3);
        writer.writeInt(this.color);
        this.position.saveWithPadding(writer);
        this.direction.saveWithPadding(writer);
        writer.writeUnsignedShort(this.attribute0);
        writer.writeUnsignedShort(this.attribute1);
        writer.writeNullPointer();
        writer.writeNullPointer();
    }

    /**
     * Converts the light to the new map format.
     */
    public FroggerMapLight convertToNewFormat(FroggerMapFile mapFile) {
        return MAPFile.copyToNewViaBytes(this, new FroggerMapLight(mapFile));
    }
}