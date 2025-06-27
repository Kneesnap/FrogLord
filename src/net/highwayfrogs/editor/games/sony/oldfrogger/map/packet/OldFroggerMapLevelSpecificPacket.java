package net.highwayfrogs.editor.games.sony.oldfrogger.map.packet;

import lombok.Getter;
import net.highwayfrogs.editor.file.standard.IVector;
import net.highwayfrogs.editor.games.sony.oldfrogger.OldFroggerReactionType;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEditorUtils;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.Arrays;

/**
 * Contains level-specific data.
 * Created by Kneesnap on 12/8/2023.
 */
@Getter
public class OldFroggerMapLevelSpecificPacket extends OldFroggerMapPacket {
    public static final String IDENTIFIER = "GAME";
    public static final int LEVEL_TIMER_COUNT = 10;
    public static final int GENERIC_LEVEL_DATA_INTEGER_COUNT = 243;
    private final IVector froggerStartPosition = new IVector();
    private OldFroggerReactionType defaultReactionType = OldFroggerReactionType.Nothing; // Default reaction type for level (Used if no rectangles are hit)
    private final int[] defaultReactionData = new int[3]; // Default reaction data.
    private final int[] levelTimers = new int[LEVEL_TIMER_COUNT]; // Based on difficulty settings.
    private short preCalculatedLowestLandscapeHeight;
    private final int[] genericLevelData = new int[GENERIC_LEVEL_DATA_INTEGER_COUNT];

    public OldFroggerMapLevelSpecificPacket(OldFroggerMapFile parentFile) {
        super(parentFile, IDENTIFIER);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        this.froggerStartPosition.load(reader);
        this.defaultReactionType = OldFroggerReactionType.values()[reader.readUnsignedShortAsInt()];
        for (int i = 0; i < this.defaultReactionData.length; i++)
            this.defaultReactionData[i] = reader.readUnsignedShortAsInt();
        for (int i = 0; i < this.levelTimers.length; i++)
            this.levelTimers[i] = reader.readUnsignedShortAsInt();
        this.preCalculatedLowestLandscapeHeight = reader.readShort();
        reader.skipShort(); // Padding
        for (int i = 0; i < this.genericLevelData.length; i++)
            this.genericLevelData[i] = reader.readInt();
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        this.froggerStartPosition.save(writer);
        writer.writeUnsignedShort(this.defaultReactionType != null ? this.defaultReactionType.ordinal() : 0);
        for (int i = 0; i < this.defaultReactionData.length; i++)
            writer.writeUnsignedShort(this.defaultReactionData[i]);
        for (int i = 0; i < this.levelTimers.length; i++)
            writer.writeUnsignedShort(this.levelTimers[i]);
        writer.writeShort(this.preCalculatedLowestLandscapeHeight);
        writer.writeShort((short) 0); // Padding
        for (int i = 0; i < this.genericLevelData.length; i++)
            writer.writeInt(this.genericLevelData[i]);
    }

    @Override
    public void clear() {
        this.froggerStartPosition.clear();
        this.defaultReactionType = OldFroggerReactionType.Nothing;
        Arrays.fill(this.defaultReactionData, 0);
        Arrays.fill(this.levelTimers, 0);
        this.preCalculatedLowestLandscapeHeight = 0;
        Arrays.fill(this.genericLevelData, 0);
    }

    /**
     * Setup the editor for the data in this packet.
     * @param controller The controller to create the editor for.
     * @param editor     The editor to create the ui under.
     */
    public void setupEditor(MeshViewController<?> controller, GUIEditorGrid editor) {
        editor.addFloatVector("Frogger Start Position", this.froggerStartPosition, null, controller);
        editor.addFixedShort("Lowest Landscape Height", this.preCalculatedLowestLandscapeHeight, newValue -> this.preCalculatedLowestLandscapeHeight = newValue, 1, false);
        OldFroggerEditorUtils.setupReactionEditor(editor, this.defaultReactionType, this.defaultReactionData, newValue -> this.defaultReactionType = newValue);
        for (int i = 0; i < this.levelTimers.length; i++) {
            final int index = i;
            editor.addUnsignedFixedShort("Timer (Difficulty " + (i + 1) + ")", this.levelTimers[i], newValue -> this.levelTimers[index] = newValue, 1);
        }

        // Generic Level Data
        editor.addSeparator();
        editor.addBoldLabel("Generic Level Data:");
        for (int i = 0; i < this.genericLevelData.length; i++) {
            final int index = i;
            editor.addSignedIntegerField("Value " + (i + 1), this.genericLevelData[i], newValue -> this.genericLevelData[index] = newValue);
        }
    }
}