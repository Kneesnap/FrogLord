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
        int count = reader.readInt(); // Actually, we should just get the lowest 8 bits as the count, it seems they're usually correct.
        while (reader.getRemaining() >= Constants.INTEGER_SIZE)
            getColors().add(ColorUtils.fromRGB(reader.readInt(), 1D));
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(getColors().size());
        for (Color color : getColors())
            writer.writeInt(ColorUtils.toRGB(color));
    }
}