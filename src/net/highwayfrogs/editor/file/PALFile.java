package net.highwayfrogs.editor.file;

import javafx.scene.Node;
import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Pallete file. PC-Only file. Appears to not actually do anything though. Loading corrupted palettes doesn't seem to break anything, and they are not used on the PSX version. I have tested in both low poly mode and high poly mode.
 * Appears this might be "Windows 3.0 logical palette" or "LOGPALETTE"
 * Created by Kneesnap on 8/14/2018.
 */
@Getter
public class PALFile extends GameFile {
    private List<Color> colors = new ArrayList<>();

    public static final int TYPE_ID = 7;
    public static final Image ICON = loadIcon("palette");
    private static final String RIFF_SIGNATURE = "RIFF";
    private static final String PAL_SIGNATURE = "PAL ";
    private static final String DATA_HEADER = "data";
    private static final short PAL_VERSION = 0x300;
    private static final byte FLAG = 0x00;
    private static final int COLOR_SIZE = 4;

    @Override
    public void load(DataReader reader) {
        Utils.verify(reader.readString(RIFF_SIGNATURE.length()).equals(RIFF_SIGNATURE), "Invalid RIFF signature!");
        reader.readInt(); // File size. Does not include marker.
        Utils.verify(reader.readString(PAL_SIGNATURE.length()).equals(PAL_SIGNATURE), "Invalid PAL signature!");
        Utils.verify(reader.readString(DATA_HEADER.length()).equals(DATA_HEADER), "Invalid data chunk signature.");
        reader.readInt(); // Data chunk size. Excludes the header.
        Utils.verify(reader.readShort() == PAL_VERSION, "Unknown PAL version.");
        short colorCount = reader.readShort();

        for (int i = 0; i < colorCount; i++) {
            int red = reader.readUnsignedByte();
            int green = reader.readUnsignedByte();
            int blue = reader.readUnsignedByte();
            int flag = reader.readUnsignedByte();
            Utils.verify(flag == FLAG, "Unknown flag value.");
            getColors().add(new Color(red, green, blue));
        }
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeStringBytes(RIFF_SIGNATURE);

        int dataChunkSize = getColors().size() * COLOR_SIZE + (2 * Constants.INTEGER_SIZE);
        int fullSize = dataChunkSize + DATA_HEADER.length() + PAL_SIGNATURE.length() + Constants.INTEGER_SIZE + RIFF_SIGNATURE.length();
        writer.writeInt(fullSize);

        writer.writeStringBytes(PAL_SIGNATURE);
        writer.writeStringBytes(DATA_HEADER);
        writer.writeInt(dataChunkSize);
        writer.writeShort(PAL_VERSION);
        writer.writeShort((short) getColors().size());

        for (Color color : colors) {
            writer.writeByte((byte) color.getRed());
            writer.writeByte((byte) color.getGreen());
            writer.writeByte((byte) color.getBlue());
            writer.writeByte(FLAG);
        }

        writer.writeNull(4); // Unsure what this serves, but it's there.
    }

    @Override
    public Image getIcon() {
        return ICON;
    }

    @Override
    public Node makeEditor() {
        return null;
    }
}
