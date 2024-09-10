package net.highwayfrogs.editor.games.konami.greatquest;

import lombok.Getter;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;

/**
 * Represents a file in Frogger: The Great Quest.
 * Created by Kneesnap on 9/9/2024.
 */
@Getter
public abstract class GreatQuestLooseGameFile extends GreatQuestGameFile {
    private final File file;

    public GreatQuestLooseGameFile(GreatQuestInstance instance, File file) {
        super(instance);
        this.file = file;
    }

    @Override
    public String getFileName() {
        return this.file.getName();
    }

    @Override
    public String getFilePath() {
        return Utils.toLocalPath(getGameInstance().getMainGameFolder(), this.file, true);
    }
}
