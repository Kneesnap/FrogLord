package net.highwayfrogs.editor.games.konami.greatquest.ui;

import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestConfig;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestGameFile;
import net.highwayfrogs.editor.gui.DefaultFileUIController;
import net.highwayfrogs.editor.gui.ImageResource;

/**
 * Represents editor UI for a GreatQuestArchiveFile.
 * Created by Kneesnap on 4/14/2024.
 */
public class GreatQuestFileEditorUIController<TGameFile extends GreatQuestGameFile> extends DefaultFileUIController<GreatQuestInstance, TGameFile> {

    public GreatQuestFileEditorUIController(GreatQuestInstance instance, String fileText, ImageResource imageResource) {
        super(instance, fileText, imageResource.getFxImage());
    }

    @Override
    public GreatQuestConfig getConfig() {
        return (GreatQuestConfig) super.getConfig();
    }
}