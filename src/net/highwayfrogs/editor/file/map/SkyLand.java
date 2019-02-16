package net.highwayfrogs.editor.file.map;

import javafx.scene.Node;
import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.file.DummyFile;
import net.highwayfrogs.editor.file.GameFile;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads the sky land file.
 * Created by Kneesnap on 2/15/2019.
 */
@Getter
public class SkyLand extends GameFile {
    private int xLength;
    private int yLength;
    private List<Short> skyData = new ArrayList<>();

    @Override
    public Image getIcon() {
        return DummyFile.ICON;
    }

    @Override
    public Node makeEditor() {
        return null;
    }

    @Override
    public void load(DataReader reader) {
        this.xLength = reader.readUnsignedShortAsInt();
        this.yLength = reader.readUnsignedShortAsInt();
        for (int i = 0; i < (xLength * yLength); i++)
            skyData.add(reader.readShort());
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.xLength);
        writer.writeUnsignedShort(this.yLength);
        skyData.forEach(writer::writeShort);
    }

    /**
     * Gets the max sky land index.
     * @return maxIndex
     */
    public int getMaxIndex() {
        int max = -1;

        for (short value : skyData) {
            int testVal = (value & 0x3FFF);
            if (testVal > max)
                max = testVal;
        }

        return max;
    }
}
