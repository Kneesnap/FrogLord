package net.highwayfrogs.editor.file.mof.flipbook;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents "MR_PART_FLIPBOOK_ACTION".
 * Created by Kneesnap on 1/8/2019.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MOFFlipbookAction extends GameObject {
    private int partcelCount;
    private int partcelIndex;

    @Override
    public void load(DataReader reader) {
        this.partcelCount = reader.readUnsignedShortAsInt();
        this.partcelIndex = reader.readUnsignedShortAsInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.partcelCount);
        writer.writeUnsignedShort(this.partcelIndex);
    }

    /**
     * Gets the number of frames this animation will last for.
     * @return frameCount
     */
    public int getFrameCount() {
        return this.partcelCount;
    }
}
