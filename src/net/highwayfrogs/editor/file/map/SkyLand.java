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
    private List<SkyLandTile> skyData = new ArrayList<>();

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
            skyData.add(new SkyLandTile(reader.readShort()));

        // & 0x3FFF -> Get texture id in txl_sky_land. Bits 0 -> 13.
        // & 0xC000 -> Texture Rotation [0->4]. Bits 14 + 15.
        //TODO: Use disassembly to find these values in the exe. Then, configure
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.xLength);
        writer.writeUnsignedShort(this.yLength);
        for (SkyLandTile tile : getSkyData())
            writer.writeShort(tile.toShort());
    }

    /**
     * Gets the max sky land index.
     * @return maxIndex
     */
    public int getMaxIndex() {
        int max = -1;
        for (SkyLandTile tile : getSkyData())
            if (tile.getId() > max)
                max = tile.getId();
        return max;
    }

    @Getter
    public static final class SkyLandTile {
        private short id;
        private SkyLandRotation rotation;

        public SkyLandTile(short readShort) {
            this.id = (short) (readShort & 0x3FFF);
            this.rotation = SkyLandRotation.values()[((readShort & 0xC000) >> 14)];
        }

        public short toShort() {
            return (short) ((this.id & 0x3FFF) | (this.rotation.ordinal() << 14));
        }
    }

    public enum SkyLandRotation {
        NORMAL, MODE1, MODE2, MODE3
    }
}
