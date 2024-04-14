package net.highwayfrogs.editor.games.konami.greatquest.ui;

import javafx.scene.Node;
import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestArchiveFile;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestConfig;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.gui.GameUIController;

/**
 * Represents editor UI for a GreatQuestArchiveFile.
 * Created by Kneesnap on 4/14/2024.
 */
@Getter
public class GreatQuestFileEditorUIController<TGameFile extends GreatQuestArchiveFile> extends GameUIController<GreatQuestInstance> {
    private TGameFile file;

    public GreatQuestFileEditorUIController(GreatQuestInstance instance) {
        super(instance);
    }

    @Override
    public GreatQuestConfig getConfig() {
        return (GreatQuestConfig) super.getConfig();
    }

    /**
     * Setup this window, by loading a GameFile to edit.
     * @param file The file to load and edit.
     */
    public void setTargetFile(TGameFile file) {
        TGameFile oldFile = this.file;
        if (oldFile != file)
            this.file = file;
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        // Do nothing.
    }
}