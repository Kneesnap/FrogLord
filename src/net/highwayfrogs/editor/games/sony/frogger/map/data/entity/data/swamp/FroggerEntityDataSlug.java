package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.swamp;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataPathInfo;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

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
        editor.addEnumSelector("Motion Type", this.motionType, FroggerEntityDataSlugMotionType.values(), false, newMotionType -> this.motionType = newMotionType)
                .setTooltip(FXUtils.createTooltip("Controls if the slug movement type is ground-based or pipe-based. (Compare Bang Bang Barrel vs Slime Sliding)"));
    }

    public enum FroggerEntityDataSlugMotionType {
        NORMAL, CURVY
    }
}