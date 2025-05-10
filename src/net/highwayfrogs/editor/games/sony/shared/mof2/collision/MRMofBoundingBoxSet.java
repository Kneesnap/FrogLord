package net.highwayfrogs.editor.games.sony.shared.mof2.collision;

import lombok.Getter;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.games.sony.SCGameInstance;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a set of bounding boxes.
 * Created by Kneesnap on 2/24/2025.
 */
@Getter
public class MRMofBoundingBoxSet extends SCSharedGameData {
    private short padding;
    private final List<MRMofBoundingBoxGroup> boundingBoxGroups = new ArrayList<>();

    public MRMofBoundingBoxSet(SCGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        short groupCount = reader.readShort();
        this.padding = reader.readShort();
        int groupPointer = reader.readInt();

        // Load groups.
        this.boundingBoxGroups.clear();
        requireReaderIndex(reader, groupPointer, "Expected BoundingBoxSet bounding boxes to begin");
        for (int i = 0; i < groupCount; i++) {
            MRMofBoundingBoxGroup newGroup = new MRMofBoundingBoxGroup(this);
            this.boundingBoxGroups.add(newGroup);
            newGroup.load(reader);
        }

        // Load group bounding boxes.
        for (int i = 0; i < this.boundingBoxGroups.size(); i++)
            this.boundingBoxGroups.get(i).readBoundingBoxes(reader);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.boundingBoxGroups.size());
        writer.writeShort(this.padding);
        int groupPointer = writer.writeNullPointer();

        // Write groups.
        writer.writeAddressTo(groupPointer);
        for (int i = 0; i < this.boundingBoxGroups.size(); i++)
            this.boundingBoxGroups.get(i).save(writer);

        // Write group bounding boxes.
        for (int i = 0; i < this.boundingBoxGroups.size(); i++)
            this.boundingBoxGroups.get(i).writeBoundingBoxes(writer);
    }
}
