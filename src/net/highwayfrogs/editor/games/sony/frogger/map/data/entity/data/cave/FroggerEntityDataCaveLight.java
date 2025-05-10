package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.cave;

import javafx.scene.control.TextField;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataMatrix;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.FroggerMapFilePacketGrid;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

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
        reader.skipBytesRequireEmpty(Constants.INTEGER_SIZE); // "count" - Used to keep track of die time. Appears unused though.
        if (hasExtraPaddingValue())
            reader.skipBytesRequireEmpty(Constants.INTEGER_SIZE); // "setup" - Already setup?
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeInt(this.minRadius);
        writer.writeInt(this.maxRadius);
        writer.writeInt(this.dieSpeed);
        writer.writeNull(Constants.INTEGER_SIZE);
        if (hasExtraPaddingValue())
            writer.writeNull(Constants.INTEGER_SIZE);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor) {
        super.setupEditor(editor);
        TextField timeTakenField = editor.addTextField("Light Duration (sec)", String.format("%.4f", calculateTimeTakenForLightToDie()));
        timeTakenField.setTooltip(FXUtils.createTooltip("This value shows how long the light will take to drop from the maximum level to the minimum level, in seconds."));
        timeTakenField.setDisable(true);

        // Add radius.
        editor.addUnsignedIntegerField("Min Radius (grid sq)", this.minRadius,
                value -> value <= this.maxRadius && value <= FroggerMapFilePacketGrid.MAX_GRID_SQUARE_COUNT_X,
                (Consumer<Integer>) newMinRadius -> {
                    this.minRadius = newMinRadius;
                    timeTakenField.setText(String.format("%.4f", calculateTimeTakenForLightToDie()));
                }).setTooltip(FXUtils.createTooltip("The minimum light radius for the level, measured in grid squares from the player's position.\nMay not be larger than the maximum radius."));
        editor.addUnsignedIntegerField("Max Radius (grid sq)", this.maxRadius,
                value -> value >= this.minRadius && value <= FroggerMapFilePacketGrid.MAX_GRID_SQUARE_COUNT_X,
                (Consumer<Integer>) newMaxRadius -> {
                    this.maxRadius = newMaxRadius;
                    timeTakenField.setText(String.format("%.4f", calculateTimeTakenForLightToDie()));
                }).setTooltip(FXUtils.createTooltip("The maximum light radius for the level, measured in grid squares from the player's position.\nMay not be smaller than the minimum radius."));

        // The 30 is hardcoded in ent_cav.c, and is not tied to the framerate.
        editor.addFixedInt("Die Speed", this.dieSpeed, newDieSpeed -> {
            this.dieSpeed = newDieSpeed;
            timeTakenField.setText(String.format("%.4f", calculateTimeTakenForLightToDie()));
        }, 30).setTooltip(FXUtils.createTooltip("How fast the visible light radius decreases while playing.\nThe higher this number, the faster the light vanishes."));
    }

    /**
     * Calculates the amount of time (in seconds) it will take for the light to die.
     */
    public float calculateTimeTakenForLightToDie() {
        int minRadiusSq = (this.minRadius << 8) * (this.minRadius << 8);
        int maxRadiusSq = (this.maxRadius << 8) * (this.maxRadius << 8);
        if (minRadiusSq > maxRadiusSq)
            return 0;

        int totalAreaToCover = maxRadiusSq - minRadiusSq;
        int dieSpeed = (this.dieSpeed << 8) / 30; // Hardcoded 30 in ent_cav.c, it's not framerate bound.
        dieSpeed = (dieSpeed * dieSpeed) >> 8;
        return (float) ((double) totalAreaToCover / (getGameInstance().getFPS() * dieSpeed));
    }

    private boolean hasExtraPaddingValue() {
        // This was probably false in May '97 builds.
        if ("CAVM.MAP".equals(getMapFile().getFileDisplayName())) {
            return getConfig().getBuild() != 2;
        } else if ("CAV1.MAP".equals(getMapFile().getFileDisplayName())) {
            return !getConfig().isAtOrBeforeBuild1();
        } else {
            return true;
        }
    }
}