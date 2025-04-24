package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.desert;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataMatrix;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents the 'DES_CRACK' entity data in ent_des.h
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class FroggerEntityDataCrack extends FroggerEntityDataMatrix {
    private int fallDelay = 30;
    private int hopsBeforeBreak;

    public FroggerEntityDataCrack(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.fallDelay = reader.readUnsignedShortAsInt();
        this.hopsBeforeBreak = reader.readUnsignedShortAsInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeUnsignedShort(this.fallDelay);
        writer.writeUnsignedShort(this.hopsBeforeBreak);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor) {
        super.setupEditor(editor);
        editor.addUnsignedFixedShort("Fall Delay (secs)", this.fallDelay, newFallDelay -> this.fallDelay = newFallDelay, 30);
        editor.addUnsignedShortField("Hops Before Break", this.hopsBeforeBreak, newHopsBeforeBreak -> this.hopsBeforeBreak = newHopsBeforeBreak);
    }
}