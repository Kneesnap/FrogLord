package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.general;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerNullDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.MatrixEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents data belonging to the GEN_WATERENTITY which enables swimming.
 * Created by Kneesnap on 12/19/2023.
 */
@Getter
public class WaterEntityData extends MatrixEntityData<OldFroggerNullDifficultyData> {
    private int zone;

    public WaterEntityData(OldFroggerMapEntity entity) {
        super(entity, null);
    }

    @Override
    protected void loadMainEntityData(DataReader reader) {
        super.loadMainEntityData(reader);
        this.zone = reader.readUnsignedShortAsInt();
        reader.alignRequireEmpty(4);
        reader.skipBytesRequireEmpty(Constants.INTEGER_SIZE); // Padding
    }

    @Override
    protected void saveMainEntityData(DataWriter writer) {
        super.saveMainEntityData(writer);
        writer.writeUnsignedShort(this.zone);
        writer.writeInt(0); // Padding
    }

    @Override
    public void setupMainEntityDataEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
        super.setupMainEntityDataEditor(manager, editor);
        editor.addUnsignedFixedShort("Zone ID", this.zone, newValue -> this.zone = newValue, 1);
    }
}