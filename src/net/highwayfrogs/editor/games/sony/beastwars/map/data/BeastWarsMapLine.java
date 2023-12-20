package net.highwayfrogs.editor.games.sony.beastwars.map.data;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.beastwars.BeastWarsInstance;
import net.highwayfrogs.editor.games.sony.beastwars.map.BeastWarsMapFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a LINE entry in a Beast Wars map.
 * Created by Kneesnap on 9/22/2023.
 */
@Setter
@Getter
public class BeastWarsMapLine extends SCGameData<BeastWarsInstance> {
    private final BeastWarsMapFile mapFile;
    private final List<SVector> positions = new ArrayList<>();
    private int unknown1; // TODO: What are these? a pointer to this spot is used interestingly, so these might contain important data?
    private int unknown2;
    private int unknown3;
    private int unknown4;

    public BeastWarsMapLine(BeastWarsMapFile mapFile) {
        super(mapFile.getGameInstance());
        this.mapFile = mapFile;
    }

    @Override
    public void load(DataReader reader) {
        int positionCount = reader.readUnsignedShortAsInt();
        this.unknown1 = reader.readUnsignedShortAsInt();
        this.unknown2 = reader.readUnsignedShortAsInt();
        this.unknown3 = reader.readUnsignedShortAsInt();
        this.unknown4 = reader.readUnsignedShortAsInt();

        // Read positions
        this.positions.clear();
        for (int i = 0; i < positionCount; i++)
            this.positions.add(SVector.readWithoutPadding(reader));
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.positions.size());
        writer.writeUnsignedShort(this.unknown1);
        writer.writeUnsignedShort(this.unknown2);
        writer.writeUnsignedShort(this.unknown3);
        writer.writeUnsignedShort(this.unknown4);

        // Write positions
        for (int i = 0; i < this.positions.size(); i++)
            this.positions.get(i).save(writer);
    }
}