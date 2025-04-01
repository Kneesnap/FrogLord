package net.highwayfrogs.editor.file.map.entity.data;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.path.PathInfo;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;

/**
 * Base entity data which holds path data.
 * Created by Kneesnap on 1/20/2019.
 */
@Getter
@Setter
public class PathData extends EntityData {
    private PathInfo pathInfo = new PathInfo();

    public PathData(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        this.pathInfo.load(reader);
    }

    @Override
    public void save(DataWriter writer) {
        this.pathInfo.save(writer);
    }
}