package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.jungle;

import javafx.scene.control.TextField;
import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataMatrix;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Implements the 'JUN_ROPE_BRIDGE' entity data definition in ent_jun.h
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class FroggerEntityDataRopeBridge extends FroggerEntityDataMatrix {
    private int fallDelay = 90;
    private int hopsBeforeBreak = 1;

    public FroggerEntityDataRopeBridge(FroggerMapFile mapFile) {
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
        TextField textField = editor.addUnsignedFixedShort("Unused Fall Delay (secs)", this.fallDelay, newFallDelay -> this.fallDelay = newFallDelay, getGameInstance().getFPS());
        textField.setTooltip(FXUtils.createTooltip("How long to wait before beginning to fall, in seconds.\nThis value seems to be unused."));
        textField.setDisable(true); // Unused value.

        editor.addUnsignedShortField("Hops Before Break", this.hopsBeforeBreak, newHopsBeforeBreak -> this.hopsBeforeBreak = newHopsBeforeBreak)
                .setTooltip(FXUtils.createTooltip("How many times does the player need to jump on the bridge before it breaks?\n0 is treated as an instant break just like 1."));
    }
}