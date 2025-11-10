package net.highwayfrogs.editor.games.konami.hudson;

import javafx.fxml.FXMLLoader;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.games.konami.hudson.ui.HudsonFileEditorUIController;
import net.highwayfrogs.editor.games.shared.basic.file.BasicGameFile;
import net.highwayfrogs.editor.games.shared.basic.file.definition.IGameFileDefinition;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents a game file found in a game using the same engine as Frogger's Adventures: The Rescue and Frogger Ancient Shadow.
 * Created by Kneesnap on 8/8/2024.
 */
@Getter @Setter
public abstract class HudsonGameFile extends BasicGameFile<HudsonGameInstance> {
    private boolean compressionEnabled;

    public HudsonGameFile(IGameFileDefinition fileDefinition) {
        super(fileDefinition);
    }

    @Override
    public String getCollectionViewDisplayName() {
        String displayName = super.getCollectionViewDisplayName();
        if (this.compressionEnabled)
            displayName += " (Compressed)";

        return displayName;
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        propertyList.add("Compression Enabled", this.compressionEnabled);
        super.addToPropertyList(propertyList);
    }

    /**
     * Loads a GameFile editor.
     * @param gameInstance the game instance to create the editor for
     * @param controller the controller to control the GUI
     * @param template the gui layout template
     * @param fileToEdit the file to edit
     * @return editor
     */
    public static <TGameFile extends HudsonGameFile, TUIController extends HudsonFileEditorUIController<TGameFile>> TUIController loadEditor(HudsonGameInstance gameInstance, String template, TUIController controller, TGameFile fileToEdit) {
        try {
            FXMLLoader templateLoader = FXUtils.getFXMLTemplateLoader(gameInstance, template);
            GameUIController.loadController(gameInstance, templateLoader, controller);
            controller.setTargetFile(fileToEdit);
        } catch (Throwable th) {
            Utils.handleError(fileToEdit.getLogger(), th, true, "Failed to create editor UI.");
        }

        return controller;
    }
}