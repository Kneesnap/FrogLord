package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.forest;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataMatrix;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents the swaying branch entity data 'for_swayingbranch' in ENTITIES.TXT and 'FOREST_SWAYING_BRANCH' in ent_for.h
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class FroggerEntityDataSwayingBranch extends FroggerEntityDataMatrix {
    private int swayAngle = 90; // Set to 45 degrees by the game when 0.
    private int swayDuration = 90;
    private int onceOffDelay;

    public FroggerEntityDataSwayingBranch(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.swayAngle = reader.readUnsignedShortAsInt();
        this.swayDuration = reader.readUnsignedShortAsInt();
        if (!getConfig().isAtOrBeforeBuild11() && !getConfig().isWindowsBeta())
            this.onceOffDelay = reader.readUnsignedShortAsInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeUnsignedShort(this.swayAngle);
        writer.writeUnsignedShort(this.swayDuration);
        if (!getConfig().isAtOrBeforeBuild11() && !getConfig().isWindowsBeta())
            writer.writeUnsignedShort(this.onceOffDelay);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor) {
        super.setupEditor(editor);
        editor.addUnsignedFixedShort("Sway Angle (degrees)", this.swayAngle, newSwayAngle -> this.swayAngle = newSwayAngle, 1, 0, 360).setTooltip(FXUtils.createTooltip("Controls how far the branch will swing.\nIt will swing half of this distance in either direction from its initial position. Branches always swing clockwise first."));
        editor.addUnsignedFixedShort("Sway Duration (secs)", this.swayDuration, newSwayDuration -> this.swayDuration = newSwayDuration, getGameInstance().getFPS()).setTooltip(FXUtils.createTooltip("Controls how long the branch will take to swing from one side to the other."));
        if (!getConfig().isAtOrBeforeBuild11() && !getConfig().isWindowsBeta())
            editor.addUnsignedFixedShort("Delay (secs)", this.onceOffDelay, newOnceOffDelay -> this.onceOffDelay = newOnceOffDelay, 30).setTooltip(FXUtils.createTooltip("Controls how long to wait after the level loads to start swaying."));
    }
}