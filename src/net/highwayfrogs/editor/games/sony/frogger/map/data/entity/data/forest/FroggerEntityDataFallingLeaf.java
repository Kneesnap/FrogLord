package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.forest;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataMatrix;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Implementation of the 'FOREST_FALLING_LEAF' entity data definition in ent_for.h as well as 'for_fallingleaves' in ENTITIES.TXT
 * This entity seems to be unused, it is unclear if it was in any map post-recode.
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
@Setter
public class FroggerEntityDataFallingLeaf extends FroggerEntityDataMatrix {
    private int fallSpeed = 2184;
    private int swayDuration = 90; // 2048 / this -> fl_speed.
    private int swayAngle = 90; // The game sets this to 45 if this is zero.

    public FroggerEntityDataFallingLeaf(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.fallSpeed = reader.readUnsignedShortAsInt();
        this.swayDuration = reader.readUnsignedShortAsInt();
        this.swayAngle = reader.readUnsignedShortAsInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeUnsignedShort(this.fallSpeed);
        writer.writeUnsignedShort(this.swayDuration);
        writer.writeUnsignedShort(this.swayAngle);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor) {
        super.setupEditor(editor);
        editor.addUnsignedFixedShort("Fall Speed", this.fallSpeed, newFallSpeed -> this.fallSpeed = newFallSpeed, 2184.5);
        editor.addUnsignedFixedShort("Sway (percent/frame)", this.swayDuration, newSwayDuration -> this.swayDuration = newSwayDuration, 1024, 0, 1024);
        editor.addUnsignedFixedShort("Sway Angle (degrees)", this.swayAngle, newSwayAngle -> this.swayAngle = newSwayAngle, 1, 0, 360);
    }
}