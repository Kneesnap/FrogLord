package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.script.volcano;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.script.FroggerEntityScriptData;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * This holds data represented as 'VOL_TRIGGERED_PLATFORM', for the entity: vol_platform2.
 * Created by Kneesnap on 11/27/2018.
 */
@Getter
public class FroggerEntityScriptDataPlatform2 extends FroggerEntityScriptData {
    private boolean enabledByDefault;

    private static final int ENABLE_STATE = 0;
    private static final int DISABLE_STATE = 1;

    public FroggerEntityScriptDataPlatform2(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        int readValue = reader.readInt();
        Utils.verify(readValue == ENABLE_STATE || readValue == DISABLE_STATE, "Unknown platform-state: %d.", readValue);
        this.enabledByDefault = (readValue == ENABLE_STATE);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(isEnabledByDefault() ? ENABLE_STATE : DISABLE_STATE);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor, FroggerUIMapEntityManager manager) {
        editor.addCheckBox("Enabled By Default?", this.enabledByDefault, newEnabledByDefault -> this.enabledByDefault = newEnabledByDefault)
                .setTooltip(FXUtils.createTooltip("This overrides the 'No Movement' flag, BUT ONLY when the entity is targetted by a color trigger entity."));
    }
}