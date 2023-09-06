package net.highwayfrogs.editor.file.map.entity.script.volcano;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.script.EntityScriptData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.Utils;

/**
 * This holds data not present in the code, for the entity: vol_platform2.
 * Created by Kneesnap on 11/27/2018.
 */
@Getter
@Setter
public class ScriptPlatform2Data extends EntityScriptData {
    private boolean enabledByDefault;

    private static final int ENABLE_STATE = 0;
    private static final int DISABLE_STATE = 1;

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
    public void addData(GUIEditorGrid editor) {
        editor.addCheckBox("Enabled By Default", isEnabledByDefault(), this::setEnabledByDefault);
    }
}