package net.highwayfrogs.editor.games.sony.beastwars;

import javafx.scene.paint.Color;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.file.FroggerPaletteFile;
import net.highwayfrogs.editor.utils.ColorUtils;

/**
 * Beast Wars Palette Support.
 * Created by Kneesnap on 5/23/2020.
 */
public class PLTFile extends FroggerPaletteFile {
    public PLTFile(BeastWarsInstance instance) {
        super(instance);
    }

    @Override
    public BeastWarsInstance getGameInstance() {
        return (BeastWarsInstance) super.getGameInstance();
    }

    @Override
    public BeastWarsConfig getConfig() {
        return (BeastWarsConfig) super.getConfig();
    }

    @Override
    public void load(DataReader reader) {
        // The first four bytes are supposed to be the color count, but they are not behaviorally consistent.
        // This likely suggests exporting with an earlier version of their tools.
        // Most files include the colorCount readable as a u32.
        // Others write it as a u8 and have non-zeroed garbage data for the remaining 3 bytes.
        // The final two (MPLAYER8.PLT and MS3_M_IB.PLT) write the first u8 correctly, but are off by one. (Write 128 when 127 is the real color count).
        // As such, it makes the most sense to just skip the value, as we can determine the color count by how much data is present in the file.
        reader.skipInt();

        // Color format is RGBA888, where the alpha is always zero. Maybe it's BGRA8888, I can't easily tell.
        while (reader.getRemaining() >= Constants.INTEGER_SIZE)
            getColors().add(ColorUtils.fromRGB(reader.readInt() >>> 8, 1D));

        reader.skipBytesRequireEmpty(1);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(getColors().size());

        // Color format is RGBA888, where the alpha is always zero.
        for (Color color : getColors())
            writer.writeInt(ColorUtils.toRGB(color) << 8);

        writer.writeByte(Constants.NULL_BYTE);
    }
}