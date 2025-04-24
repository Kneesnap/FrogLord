package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.cave;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataMatrix;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * This is a very old data structure, seen at/before the PSX Alpha (June) build.
 * Represents 'CAV_SPIDER' in ent_cav.h
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class FroggerEntityDataSpider extends FroggerEntityDataMatrix {
    private int speed;

    public FroggerEntityDataSpider(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.speed = reader.readUnsignedShortAsInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeUnsignedShort(this.speed);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor) {
        super.setupEditor(editor);
        editor.addUnsignedFixedShort("Speed", this.speed, newSpeed -> this.speed = newSpeed, 16);
    }
}