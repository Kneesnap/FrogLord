package net.highwayfrogs.editor.games.konami.greatquest;

import javafx.fxml.FXMLLoader;
import net.highwayfrogs.editor.games.generic.GameData;
import net.highwayfrogs.editor.games.konami.greatquest.loading.kcLoadContext;
import net.highwayfrogs.editor.games.konami.greatquest.ui.GreatQuestFileEditorUIController;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.components.CollectionViewComponent.ICollectionViewEntry;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.Utils;

import java.util.logging.Logger;

/**
 * Represents a game file in Frogger: The Great Quest.
 * Created by Kneesnap on 9/9/2024.
 */
public abstract class GreatQuestGameFile extends GameData<GreatQuestInstance> implements ICollectionViewEntry, IPropertyListCreator {
    private Logger cachedLogger;

    public GreatQuestGameFile(GreatQuestInstance instance) {
        super(instance);
    }

    @Override
    public Logger getLogger() {
        if (this.cachedLogger == null)
            this.cachedLogger = Logger.getLogger(getClass().getSimpleName() + "{" + getFileName() + "}");

        return this.cachedLogger;
    }

    /**
     * Gets the file name for this game file.
     */
    public abstract String getFileName();

    /**
     * Gets the file path for this file.
     */
    public abstract String getFilePath();

    @Override
    public String getCollectionViewDisplayName() {
        return getFileName();
    }

    @Override
    public String getCollectionViewDisplayStyle() {
        return null;
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        String fileName = getFileName();
        if (fileName != null)
            propertyList.add("File Name", fileName);

        String filePath = getFilePath();
        if (filePath != null)
            propertyList.add("File Path", filePath);

        return propertyList;
    }

    /**
     * The first method called after all files have been loaded.
     */
    public void afterLoad1(kcLoadContext context) {
        // Do nothing.
    }

    /**
     * The second method called after all files have been loaded.
     */
    public void afterLoad2(kcLoadContext context) {
        // Do nothing.
    }

    /**
     * Creates an editor UI for the file.
     */
    public abstract GameUIController<?> makeEditorUI();

    /**
     * Called when the file is double-clicked in the file list.
     */
    public void handleDoubleClick() {
        // Do nothing by default.
    }

    /**
     * Tests if this file has a file path assigned.
     * @return hasFilePath
     */
    public boolean hasFilePath() {
        return getFilePath() != null;
    }

    /**
     * Loads a GameFile editor.
     * @param gameInstance the game instance to create the editor for
     * @param controller the controller to control the GUI
     * @param template the gui layout template
     * @param fileToEdit the file to edit
     * @return editor
     */
    public static <TGameFile extends GreatQuestGameFile, TUIController extends GreatQuestFileEditorUIController<TGameFile>> TUIController loadEditor(GreatQuestInstance gameInstance, String template, TUIController controller, TGameFile fileToEdit) {
        try {
            FXMLLoader templateLoader = Utils.getFXMLTemplateLoader(gameInstance, template);
            GameUIController.loadController(gameInstance, templateLoader, controller);
            controller.setTargetFile(fileToEdit);
        } catch (Throwable th) {
            Utils.handleError(fileToEdit.getLogger(), th, true, "Failed to create editor UI.");
        }

        return controller;
    }
}
