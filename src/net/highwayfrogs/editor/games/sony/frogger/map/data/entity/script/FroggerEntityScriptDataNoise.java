package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.script;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Loads and saves sound radius data.
 * TODO: For this (and other ones with radiuses), I'd like to have a way to visualize this in the 3D world.
 * TODO: Actually, go over all of the entity data entries & script data entries to come up with a 3D visualization plan.
 * Created by Kneesnap on 11/27/2018.
 */
@Getter
public class FroggerEntityScriptDataNoise extends FroggerEntityScriptData {
    private int minRadius = 512;
    private int maxRadius = 1024;

    public FroggerEntityScriptDataNoise(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        this.minRadius = reader.readInt();
        this.maxRadius = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.minRadius);
        writer.writeInt(this.maxRadius);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor, FroggerUIMapEntityManager manager) {
        editor.addFixedInt("Min Radius (grid)", this.minRadius, newMinRadius -> this.minRadius = newMinRadius, 256)
                .setTooltip(FXUtils.createTooltip("The minimum distance away (in grid squares) to hear the sound from."));
        editor.addFixedInt("Max Radius (grid)", this.maxRadius, newMaxRadius -> this.maxRadius = newMaxRadius, 256)
                .setTooltip(FXUtils.createTooltip("The maximum distance away (in grid squares) to hear the sound from."));
    }
}