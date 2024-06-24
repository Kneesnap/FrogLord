package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.cave;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataMatrix;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

import java.util.function.Consumer;

/**
 * Represents the cave lighting entity data.
 * Implemented from 'cav_froggerlight' in ENTITIES.TXT and 'CAVES_FROG_LIGHT' in ent_cav.h
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class FroggerEntityDataCaveLight extends FroggerEntityDataMatrix {
    private int minRadius = 2;
    private int maxRadius = 4;
    private int dieSpeed = 10;

    public FroggerEntityDataCaveLight(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.minRadius = reader.readInt();
        this.maxRadius = reader.readInt();
        this.dieSpeed = reader.readInt();
        // TODO: These are only in old builds.
        reader.skipBytesRequireEmpty(Constants.INTEGER_SIZE); // "count" - Used to keep track of die time. Appears unused though.
        reader.skipBytesRequireEmpty(Constants.INTEGER_SIZE); // "setup" - Already setup?
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeInt(this.minRadius);
        writer.writeInt(this.maxRadius);
        writer.writeInt(this.dieSpeed);
        writer.writeNull(2 * Constants.INTEGER_SIZE);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor) {
        super.setupEditor(editor);
        editor.addUnsignedIntegerField("Min Radius (grid)", this.minRadius, (Consumer<Integer>) newMinRadius -> this.minRadius = newMinRadius);
        editor.addUnsignedIntegerField("Max Radius (grid)", this.maxRadius, (Consumer<Integer>) newMaxRadius -> this.maxRadius = newMaxRadius);
        editor.addFixedInt("Die Speed (grid/sec)", this.dieSpeed, newDieSpeed -> this.dieSpeed = newDieSpeed, 30);
    }
}