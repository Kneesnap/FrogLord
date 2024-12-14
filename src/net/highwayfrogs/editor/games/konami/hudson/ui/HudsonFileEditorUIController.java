package net.highwayfrogs.editor.games.konami.hudson.ui;

import lombok.Getter;
import net.highwayfrogs.editor.games.konami.hudson.HudsonGameFile;
import net.highwayfrogs.editor.games.konami.hudson.HudsonGameInstance;
import net.highwayfrogs.editor.games.shared.basic.ui.BasicFileEditorUIController;

/**
 * Represents editor UI for an HudsonSoft game file.
 * Created by Kneesnap on 8/8/2024.
 */
@Getter
public class HudsonFileEditorUIController<TGameFile extends HudsonGameFile> extends BasicFileEditorUIController<TGameFile, HudsonGameInstance> {
    public HudsonFileEditorUIController(HudsonGameInstance instance) {
        super(instance);
    }
}