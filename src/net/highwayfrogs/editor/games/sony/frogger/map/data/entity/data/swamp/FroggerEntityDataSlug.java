package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.swamp;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataPathInfo;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Implements the 'SWAMP_SLUG' entity data definition in ent_swp.h
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class FroggerEntityDataSlug extends FroggerEntityDataPathInfo {
    private FroggerEntityDataSlugMotionType motionType = FroggerEntityDataSlugMotionType.NORMAL;

    public FroggerEntityDataSlug(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.motionType = FroggerEntityDataSlugMotionType.values()[reader.readInt()];
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeInt(this.motionType != null ? this.motionType.ordinal() : FroggerEntityDataSlugMotionType.NORMAL.ordinal());
    }

    @Override
    public void setupEditor(GUIEditorGrid editor) {
        super.setupEditor(editor);
        editor.addEnumSelector("Motion Type", this.motionType, FroggerEntityDataSlugMotionType.values(), false, newMotionType -> this.motionType = newMotionType);
    }

    public enum FroggerEntityDataSlugMotionType {
        NORMAL, CURVY
    }
}