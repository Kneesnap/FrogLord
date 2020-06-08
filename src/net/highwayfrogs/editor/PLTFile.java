package net.highwayfrogs.editor;

import javafx.scene.paint.Color;
import net.highwayfrogs.editor.file.PALFile;
import net.highwayfrogs.editor.file.WADFile;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.MainController;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Beast Wars Palette Support.
 * Created by Kneesnap on 5/23/2020.
 */
public class PLTFile extends PALFile {
    public static final int FILE_TYPE = 9;

    @Override
    public void load(DataReader reader) {
        int count = reader.readInt(); // Actually, we should just get the lowest 8 bits as the count, it seems they're usually correct.
        while (reader.getRemaining() >= Constants.INTEGER_SIZE)
            getColors().add(Utils.fromRGB(reader.readInt(), 1D));
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(getColors().size());
        for (Color color : getColors())
            writer.writeInt(Utils.toRGB(color));
    }

    @Override
    public void handleWadEdit(WADFile parent) {
        MainController.MAIN_WINDOW.openEditor(MainController.MAIN_WINDOW.getCurrentFilesList(), this);
        //((PaletteController) MainController.getCurrentController()).setParentWad(parent);
    }
}
