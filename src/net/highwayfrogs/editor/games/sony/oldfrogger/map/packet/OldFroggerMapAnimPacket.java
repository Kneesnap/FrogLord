package net.highwayfrogs.editor.games.sony.oldfrogger.map.packet;

import lombok.Getter;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.oldfrogger.OldFroggerGameInstance;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Includes map animation data from old Frogger maps.
 * This seems to be unused, and isn't really supported fully by FrogLord.
 * Created by Kneesnap on 12/12/2023.
 */
@Getter
public class OldFroggerMapAnimPacket extends OldFroggerMapPacket {
    public static final String IDENTIFIER = "ANIM";
    private final List<OldFroggerMapAnimUV> uvAnimations = new ArrayList<>();

    public OldFroggerMapAnimPacket(OldFroggerMapFile parentFile) {
        super(parentFile, IDENTIFIER, PacketSizeType.NO_SIZE);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        int uvAnimCount = reader.readInt();
        int uvAnimPtr = reader.readInt();
        int texAnimCount = reader.readInt();
        int texAnimPtr = reader.readInt();
        int extraDataCount = reader.readInt();
        int extraDataPtr = reader.readInt();

        if (texAnimCount > 0 || extraDataCount > 0) // This is untested, but FrogLord support here is primarily for reading data from game files, not creating new levels for Old Frogger.
            throw new RuntimeException("Texture animations are not supported by FrogLord as they do not appear to be used by Old Frogger, but they were seen in '" + getParentFile().getFileDisplayName() + "'.");

        // Load UV animations.
        this.uvAnimations.clear();
        reader.setIndex(uvAnimPtr);
        for (int i = 0; i < uvAnimCount; i++) {
            OldFroggerMapAnimUV newAnimation = new OldFroggerMapAnimUV(getParentFile().getGameInstance());
            newAnimation.load(reader);
            this.uvAnimations.add(newAnimation);
        }
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        writer.writeInt(this.uvAnimations.size());
        int uvAnimPtr = writer.writeNullPointer();
        writer.writeInt(0); // texture animation count
        int texAnimPtr = writer.writeNullPointer();
        writer.writeInt(0); // extra data count.
        int extraDataPtr = writer.writeNullPointer();

        // Write UV animations.
        writer.writeAddressTo(uvAnimPtr);
        for (int i = 0; i < this.uvAnimations.size(); i++)
            this.uvAnimations.get(i).save(writer);

        // Write texture animations.
        writer.writeAddressTo(texAnimPtr);

        // Write extra data.
        writer.writeAddressTo(extraDataPtr);
    }

    /**
     * Represents a map UV animation.
     * Right now this has only been seen in MULTIPLAYER1.MAP
     */
    @Getter
    public static class OldFroggerMapAnimUV extends SCGameData<OldFroggerGameInstance> {
        private int vertexPointer; // Replace this with vertex reference or with vertex ID.
        private int vertexCount; // The number of impacted vertices.
        private short deltaU;
        private short deltaV;
        private int target; // Not sure.
        private int count; // Not sure.

        public OldFroggerMapAnimUV(OldFroggerGameInstance instance) {
            super(instance);
        }

        @Override
        public void load(DataReader reader) {
            this.vertexPointer = reader.readInt();
            this.vertexCount = reader.readInt();
            this.deltaU = reader.readUnsignedByteAsShort();
            this.deltaV = reader.readUnsignedByteAsShort();
            this.target = reader.readUnsignedShortAsInt();
            this.count = reader.readUnsignedShortAsInt();
            reader.alignRequireEmpty(4);
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeInt(this.vertexPointer);
            writer.writeInt(this.vertexCount);
            writer.writeUnsignedByte(this.deltaU);
            writer.writeUnsignedByte(this.deltaV);
            writer.writeUnsignedShort(this.target);
            writer.writeUnsignedShort(this.count);
            writer.align(4);
        }
    }

    @Override
    public int getKnownStartAddress() {
        OldFroggerMapGraphicalHeaderPacket graphicalPacket = getParentFile().getGraphicalHeaderPacket();
        return graphicalPacket != null ? graphicalPacket.getAnimChunkAddress() : -1;
    }
}