package net.highwayfrogs.editor.games.sony.shared.mof2.collision;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the struct 'MR_ANIM_BBOXES', which is a group of bounding boxes.
 * Created by Kneesnap on 2/24/2025.
 */
public class MRMofBoundingBoxGroup extends SCSharedGameData {
    @Getter @NonNull private final MRMofBoundingBoxSet parentSet;
    @Getter private final List<MRMofBoundingBox> boundingBoxes = new ArrayList<>();
    @Getter private short padding;

    private transient int tempBoundingBoxCount = -1;
    private transient int tempBoundingBoxPointer = -1;

    public MRMofBoundingBoxGroup(MRMofBoundingBoxSet parentSet) {
        super(parentSet.getGameInstance());
        this.parentSet = parentSet;
    }

    @Override
    public void load(DataReader reader) {
        this.tempBoundingBoxCount = reader.readShort();
        this.padding = reader.readShort();
        this.tempBoundingBoxPointer = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.boundingBoxes.size());
        writer.writeShort(this.padding);
        this.tempBoundingBoxPointer = writer.writeNullPointer();
    }

    /**
     * Reads the bounding boxes from the data reader.
     * @param reader the reader to read the bounding boxes from
     */
    void readBoundingBoxes(DataReader reader) {
        if (this.tempBoundingBoxCount < 0 || this.tempBoundingBoxPointer < 0)
            throw new RuntimeException("Cannot read bounding boxes, load(DataReader) does not appear to have been called.");

        int boundingBoxCount = this.tempBoundingBoxCount;
        int boundingBoxPointer = this.tempBoundingBoxPointer;
        this.tempBoundingBoxPointer = -1;
        this.tempBoundingBoxCount = -1;

        requireReaderIndex(reader, boundingBoxPointer, "Expected bounding box group entries");
        this.boundingBoxes.clear();
        for (int i = 0; i < boundingBoxCount; i++) {
            MRMofBoundingBox newBoundingBox = new MRMofBoundingBox();
            this.boundingBoxes.add(newBoundingBox);
            newBoundingBox.load(reader);
        }
    }

    /**
     * Writes the bounding boxes to the data writer.
     * @param writer the writer to write the bounding boxes to
     */
    void writeBoundingBoxes(DataWriter writer) {
        if (this.tempBoundingBoxPointer < 0)
            throw new RuntimeException("Cannot write bounding boxes, save(DataWriter) does not appear to have been called.");
        if (this.tempBoundingBoxCount >= 0)
            throw new RuntimeException("Cannot write bounding boxes, the state appears to be corrupted.");

        writer.writeAddressTo(this.tempBoundingBoxPointer);
        this.tempBoundingBoxPointer = -1;
        for (int i = 0; i < this.boundingBoxes.size(); i++)
            this.boundingBoxes.get(i).save(writer);
    }
}