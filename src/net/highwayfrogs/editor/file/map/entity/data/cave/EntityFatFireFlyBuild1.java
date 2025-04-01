package net.highwayfrogs.editor.file.map.entity.data.cave;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;

/**
 * An earlier version of the fat fire fly bug. Seems to be from before the camera movement from eating the bug.
 * Created by Kneesnap on 1/10/2023.
 */
@Getter
@Setter
public class EntityFatFireFlyBuild1 extends MatrixData {
    private SVector target = new SVector(); // Appears to be the position which gets shown when eating the bug.

    public EntityFatFireFlyBuild1(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.target = SVector.readWithPadding(reader);
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        this.target.saveWithPadding(writer);
    }
}