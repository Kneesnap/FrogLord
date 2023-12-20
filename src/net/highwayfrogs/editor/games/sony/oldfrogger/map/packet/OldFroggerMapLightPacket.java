package net.highwayfrogs.editor.games.sony.oldfrogger.map.packet;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.map.light.APILightType;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.psx.CVector;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.oldfrogger.OldFroggerGameInstance;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds light sources for an old Frogger map.
 * Created by Kneesnap on 12/12/2023.
 */
@Getter
public class OldFroggerMapLightPacket extends OldFroggerMapPacket {
    public static final String IDENTIFIER = "LITE";
    private final List<OldFroggerMapLight> lights = new ArrayList<>();

    public OldFroggerMapLightPacket(OldFroggerMapFile parentFile) {
        super(parentFile, IDENTIFIER, PacketSizeType.NO_SIZE);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        this.lights.clear();
        int lightCount = reader.readInt();
        for (int i = 0; i < lightCount; i++) {
            OldFroggerMapLight newLight = new OldFroggerMapLight(getParentFile().getGameInstance());
            newLight.load(reader);
            this.lights.add(newLight);
        }
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        writer.writeInt(this.lights.size());
        for (int i = 0; i < this.lights.size(); i++)
            this.lights.get(i).save(writer);
    }

    @Override
    public int getKnownStartAddress() {
        OldFroggerMapGraphicalHeaderPacket graphicalPacket = getParentFile().getGraphicalHeaderPacket();
        return graphicalPacket != null ? graphicalPacket.getLightChunkAddress() : -1;
    }

    /**
     * Represents a map light in an old Frogger map.
     */
    @Getter
    public static class OldFroggerMapLight extends SCGameData<OldFroggerGameInstance> {
        private OldFroggerMapLightType type = OldFroggerMapLightType.DUMMY;
        private short priority; // can bin low priority lights (detail) top bit is ON/OFF
        private int parentId; // (depends on above)
        private APILightType apiType; // (point/parallel/etc.)
        private final CVector color = new CVector();
        private final SVector position = new SVector();
        private final SVector direction = new SVector();
        private int attrib1; // eg falloff if point, umbra angle if
        private int attrib2; // spot

        public OldFroggerMapLight(OldFroggerGameInstance instance) {
            super(instance);
        }

        @Override
        public void load(DataReader reader) {
            this.type = OldFroggerMapLightType.values()[reader.readUnsignedByteAsShort()];
            this.priority = reader.readUnsignedByteAsShort();
            this.parentId = reader.readUnsignedShortAsInt();
            this.apiType = APILightType.getType(reader.readUnsignedByteAsShort());
            reader.alignRequireEmpty(4);
            this.color.load(reader);
            this.position.loadWithPadding(reader);
            this.direction.loadWithPadding(reader);
            this.attrib1 = reader.readUnsignedShortAsInt();
            this.attrib2 = reader.readUnsignedShortAsInt();
            reader.skipBytesRequireEmpty(Constants.INTEGER_SIZE * 2);
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeUnsignedByte((short) (this.type != null ? this.type.ordinal() : 0));
            writer.writeUnsignedByte(this.priority);
            writer.writeUnsignedShort(this.parentId);
            writer.writeUnsignedByte((short) (this.apiType != null ? this.apiType.getFlag() : 0));
            writer.align(4);
            this.color.save(writer);
            this.position.saveWithPadding(writer);
            this.direction.saveWithPadding(writer);
            writer.writeUnsignedShort(this.attrib1);
            writer.writeUnsignedShort(this.attrib2);
            writer.writeNullPointer(); // Runtime pointer.
            writer.writeNullPointer(); // Runtime pointer.
        }
    }

    public enum OldFroggerMapLightType {
        DUMMY,
        STATIC,
        ENTITY
    }
}