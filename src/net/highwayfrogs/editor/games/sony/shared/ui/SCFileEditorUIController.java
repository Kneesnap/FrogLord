package net.highwayfrogs.editor.games.sony.shared.ui;

import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.games.sony.SCGameConfig;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.games.sony.shared.ui.file.WADController;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents editor UI for a SCGameFile.
 * Created by Kneesnap on 4/13/2024.
 */
@Getter
public abstract class SCFileEditorUIController<TGameInstance extends SCGameInstance, TGameFile extends SCGameFile<?>> extends GameUIController<TGameInstance> {
    private TGameFile file;
    private Class<? extends TGameFile> fileClass;
    @Setter private WADFile parentWadFile;

    public SCFileEditorUIController(TGameInstance instance) {
        super(instance);
    }

    @Override
    public SCGameConfig getConfig() {
        return (SCGameConfig) super.getConfig();
    }

    /**
     * Setup this window, by loading a GameFile to edit.
     * @param file The file to load and edit.
     */
    @SuppressWarnings("unchecked")
    public void setTargetFile(TGameFile file) {
        TGameFile oldFile = this.file;
        if (oldFile != file) {
            if (file != null && (this.fileClass == null || file.getClass().isAssignableFrom(this.fileClass)))
                this.fileClass = (Class<? extends TGameFile>) file.getClass();

            this.file = file;
            setParentWadFile(null);
        }
    }

    /**
     * Attempts to return to the parent wad file.
     */
    public void tryReturnToParentWadFile() {
        WADFile wadFile = getParentWadFile();
        if (wadFile == null) {
            Utils.makePopUp("There's no file to return to.", AlertType.WARNING);
            return;
        }

        WADController wadController = wadFile.makeEditorUI();
        getGameInstance().getMainMenuController().showEditor(wadController);
        wadController.selectFile(getFile()); // Highlight this file again.
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        // Do nothing.
    }
}