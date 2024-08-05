package net.highwayfrogs.editor.games.konami.ancientshadow;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.games.generic.GameData;
import net.highwayfrogs.editor.games.konami.ancientshadow.ui.AncientShadowFileEditorUIController;
import net.highwayfrogs.editor.games.konami.hudson.IHudsonFileDefinition;
import net.highwayfrogs.editor.gui.DefaultFileUIController;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.components.CollectionViewComponent.ICollectionViewEntry;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.DataSizeUnit;
import net.highwayfrogs.editor.utils.Utils;

import java.net.URL;
import java.util.logging.Logger;

/**
 * Represents a game file found in Frogger Ancient Shadow
 * Created by Kneesnap on 8/4/2024.
 */
public abstract class AncientShadowGameFile extends GameData<AncientShadowInstance> implements ICollectionViewEntry, IPropertyListCreator {
    @Getter private IHudsonFileDefinition fileDefinition;
    @Getter @Setter private byte[] rawData;
    @Getter @Setter private boolean compressionEnabled;
    private Logger cachedLogger;

    public AncientShadowGameFile(IHudsonFileDefinition fileDefinition) {
        super((AncientShadowInstance) fileDefinition.getGameInstance());
        this.fileDefinition = fileDefinition;
    }

    @Override
    public Logger getLogger() {
        if (this.cachedLogger == null)
            this.cachedLogger = Logger.getLogger(getLoggerString());

        return this.cachedLogger;
    }

    /**
     * Gets the logger string used to build the logger object.
     */
    public String getLoggerString() {
        return getFullDisplayName();
    }

    /**
     * Gets the displayed name of the file.
     * @return fileDisplayName
     */
    public String getDisplayName() {
        if (this.fileDefinition == null)
            throw new RuntimeException("Cannot getFileDisplayName() as the fileDefinition is null!");

        return this.fileDefinition.getFileName();
    }

    /**
     * Gets the full name of the file.
     * @return fullFileDisplayName
     */
    public String getFullDisplayName() {
        if (this.fileDefinition == null)
            throw new RuntimeException("Cannot getFullFileDisplayName() as the fileDefinition is null!");

        return this.fileDefinition.getFullFileName();
    }

    @Override
    public ICollectionViewEntry getCollectionViewParentEntry() {
        return null;
    }

    @Override
    public String getCollectionViewDisplayName() {
        return getFileDefinition().getFullFileName() + " [Compressed: " + this.compressionEnabled + "]";
    }

    @Override
    public String getCollectionViewDisplayStyle() {
        return null;
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList.add("Compression Enabled", this.compressionEnabled);
        if (this.rawData != null)
            propertyList.add("Loaded File Size", DataSizeUnit.formatSize(this.rawData.length));

        return propertyList;
    }

    /**
     * Creates an editor UI for the file.
     */
    public GameUIController<?> makeEditorUI() {
        return DefaultFileUIController.loadEditor(getGameInstance(), new DefaultFileUIController<>(getGameInstance(), Utils.getSimpleName(this), getCollectionViewIcon()), this);
    }

    /**
     * Loads a GameFile editor.
     * @param gameInstance the game instance to create the editor for
     * @param controller the controller to control the GUI
     * @param template the gui layout template
     * @param fileToEdit the file to edit
     * @return editor
     */
    public static <TGameFile extends AncientShadowGameFile, TUIController extends AncientShadowFileEditorUIController<TGameFile>> TUIController loadEditor(AncientShadowInstance gameInstance, String template, TUIController controller, TGameFile fileToEdit) {
        try {
            URL templateUrl = Utils.getFXMLTemplateURL(gameInstance, template);
            GameUIController.loadController(gameInstance, templateUrl, controller);
            controller.setTargetFile(fileToEdit);
        } catch (Throwable th) {
            Utils.handleError(fileToEdit.getLogger(), th, true, "Failed to create editor UI.");
        }

        return controller;
    }
}