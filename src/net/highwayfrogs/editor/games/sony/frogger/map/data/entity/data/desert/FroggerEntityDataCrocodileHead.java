package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.desert;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataMatrix;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents the 'DES_CROC_HEAD' entity data definition in ent_des.h
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class FroggerEntityDataCrocodileHead extends FroggerEntityDataMatrix {
    private int riseHeight = 256;
    private int riseSpeed = 64;
    private int snapDelay = 20;
    private int pauseDelay = 20;
    private boolean shouldSnap;
    private int submergedDelay = 10;

    public FroggerEntityDataCrocodileHead(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.riseHeight = reader.readUnsignedShortAsInt();
        this.riseSpeed = reader.readUnsignedShortAsInt();
        this.snapDelay = reader.readUnsignedShortAsInt();
        this.pauseDelay = reader.readUnsignedShortAsInt();
        int snapOrNot = reader.readUnsignedShortAsInt();
        this.submergedDelay = reader.readUnsignedShortAsInt();

        // Parse snap or not.
        this.shouldSnap = (snapOrNot == 1);
        if (snapOrNot != 0 && snapOrNot != 1)
            getLogger().warning("ch_snap_or_not_to_snap was expected to be either 0 or 1, but was actually " + snapOrNot + ".");
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeUnsignedShort(this.riseHeight);
        writer.writeUnsignedShort(this.riseSpeed);
        writer.writeUnsignedShort(this.snapDelay);
        writer.writeUnsignedShort(this.pauseDelay);
        writer.writeUnsignedShort(this.shouldSnap ? 1 : 0);
        writer.writeUnsignedShort(this.submergedDelay);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor) {
        super.setupEditor(editor);
        editor.addUnsignedFixedShort("Rise Height (grid)", this.riseHeight, newRiseHeight -> this.riseHeight = newRiseHeight, 256);
        editor.addUnsignedFixedShort("Rise Speed (per frame)", this.riseSpeed, newRiseSpeed -> this.riseSpeed = newRiseSpeed, 256);
        editor.addUnsignedFixedShort("Snap Delay (secs)", this.snapDelay, newSnapDelay -> this.snapDelay = newSnapDelay, 30);
        editor.addCheckBox("Should Snap", this.shouldSnap, newSnapOrNot -> this.shouldSnap = newSnapOrNot);
        editor.addUnsignedFixedShort("Pause Delay (secs)", this.pauseDelay, newPauseDelay -> this.pauseDelay = newPauseDelay, 30);
        editor.addUnsignedFixedShort("Submerged Delay (secs)", this.submergedDelay, newSubmergedDelay -> this.submergedDelay = newSubmergedDelay, 30);
    }
}