package net.highwayfrogs.editor.games.sony.shared.ui;

import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings.ImageState;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.file.writer.FileReceiver;
import net.highwayfrogs.editor.games.sony.SCGameConfig;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.mwd.MWDFile;
import net.highwayfrogs.editor.games.sony.shared.ui.file.VLOController;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.InputMenu;
import net.highwayfrogs.editor.gui.MainMenuController;
import net.highwayfrogs.editor.gui.components.CollectionEditorComponent;
import net.highwayfrogs.editor.gui.components.ProgressBarComponent;
import net.highwayfrogs.editor.gui.extra.hash.HashPlaygroundController;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.FileUtils.BrowserFileType;
import net.highwayfrogs.editor.utils.FileUtils.SavedFilePath;
import net.highwayfrogs.editor.utils.NumberUtils;

import java.io.File;
import java.util.List;

/**
 * Represents the editor main menu for a Millennium Interactive game.
 * Created by Kneesnap on 4/12/2024.
 */
public class SCMainMenuUIController<TGameInstance extends SCGameInstance> extends MainMenuController<TGameInstance, SCGameFile<?>> {
    private static final SavedFilePath TEXTURE_FOLDER = new SavedFilePath("bulkTextureExportPath", "Choose the folder to save all textures to.");
    private static final SavedFilePath MWI_FILE = new SavedFilePath("mwiFilePath", "Specify the file to save the MWI as...", new BrowserFileType("Millennium WAD Index", "MWI"));

    public SCMainMenuUIController(TGameInstance instance) {
        super(instance);
    }

    @Override
    public SCGameConfig getConfig() {
        return (SCGameConfig) super.getConfig();
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        super.onControllerLoad(rootNode);

        // Allow exporting MWI.
        addMenuItem(this.menuBarFile, "Export MWI", () -> {
            File selectedFile = FileUtils.askUserToSaveFile(getGameInstance(), MWI_FILE, "FROGPSX.MWI");
            if (selectedFile == null)
                return; // Cancel.

            FileUtils.deleteFile(selectedFile); // Don't merge files, create a new one.
            DataWriter writer = new DataWriter(new FileReceiver(selectedFile));
            getGameInstance().getArchiveIndex().save(writer);
            writer.closeReceiver();

            getLogger().info("Exported MWI.");
        });

        addMenuItem(this.menuBarFile, "Export File (Alternate Format)", () -> getSelectedFileEntry().exportAlternateFormat()); // Ctrl + E
        addMenuItem(this.menuBarFile, "Export All Textures", this::exportBulkTextures);

        addMenuItem(this.menuBarEdit, "Open Hash Playground", () -> HashPlaygroundController.openEditor(getGameInstance()));
        addMenuItem(this.menuBarEdit, "Find Texture By ID", this::promptSearchForTexture);
    }

    @Override
    protected void saveMainGameData() {
        if (getGameInstance().getGameType().isShowSaveWarning()) {
            boolean saveAnyways = FXUtils.makePopUpYesNo("Saving " + getGameInstance().getGameType().getDisplayName() + " is not supported yet.\n"
                    + "It will most likely crash the game if used. Would you like to continue?");
            if (!saveAnyways)
                return;
        }

        if (getConfig().isMwdLooseFiles()) {
            // We can support this at any time I think.
            boolean saveAnyways = FXUtils.makePopUpYesNo("Saving files outside of the MWD is not supported yet.\n"
                    + "So, this will create a MWD file instead. Would you like to continue?");
            if (!saveAnyways)
                return;
        }

        File baseFolder = getGameInstance().getMainGameFolder();
        if (!baseFolder.canWrite()) {
            FXUtils.makePopUp("Can't write to the file." + Constants.NEWLINE + "Do you have permission to save in this folder?", AlertType.ERROR);
            return;
        }

        // The canWrite check does not work on the files, only on the directory.
        File outputMwdFile = new File(baseFolder, FileUtils.stripExtension(getGameInstance().getMwdFile().getName()) + "-MODIFIED.MWD");
        File outputMwiFile = new File(baseFolder, FileUtils.stripExtension(getGameInstance().getMwdFile().getName()) + "-MODIFIED.MWI");
        File outputExeFile = new File(baseFolder, FileUtils.stripExtension(getGameInstance().getExeFile().getName()) + "-modified.exe");

        ProgressBarComponent.openProgressBarWindow(getGameInstance(), "Saving Files", progressBar -> {
            // Save the MWD file.
            DataWriter mwdWriter = new DataWriter(new FileReceiver(outputMwdFile));

            try {
                getGameInstance().getMainArchive().save(mwdWriter, progressBar);
            } catch (Throwable th) {
                throw new RuntimeException("Failed to save the MWD file: '" + outputMwdFile.getName() + "'.", th);
            } finally {
                mwdWriter.closeReceiver();
            }

            // Save the executable too.
            progressBar.update(0, 1, "Saving the modified executable...");
            try {
                getGameInstance().saveExecutable(outputExeFile, true);
                progressBar.addCompletedProgress(1);
            } catch (Throwable th) {
                throw new RuntimeException("Failed to save the patched game executable '" + outputExeFile.getName() + "'.", th);
            }

            // Wait until after the MWD has been saved to save the MWI.
            FileUtils.deleteFile(outputMwiFile); // Don't merge files, create a new one.
            DataWriter writer = new DataWriter(new FileReceiver(outputMwiFile));
            getGameInstance().getArchiveIndex().save(writer);
            writer.closeReceiver();
        });
    }

    @Override
    protected CollectionEditorComponent<TGameInstance, SCGameFile<?>> createFileListEditor() {
        return new CollectionEditorComponent<>(getGameInstance(),  new SCGameFileGroupedListViewComponent<>(getGameInstance()), false);
    }

    protected static MenuItem addMenuItem(Menu menuBar, String title, Runnable action) {
        MenuItem menuItem = new MenuItem(title);
        menuItem.setOnAction(event -> action.run());
        menuBar.getItems().add(menuItem);
        return menuItem;
    }

    /**
     * Gets the main file archive.
     */
    public MWDFile getArchive() {
        return getGameInstance() != null ? getGameInstance().getMainArchive() : null;
    }

    private void exportBulkTextures() {
        File targetFolder = FileUtils.askUserToSelectFolder(getGameInstance(), TEXTURE_FOLDER);
        if (targetFolder == null)
            return;

        ImageFilterSettings exportSettings = new ImageFilterSettings(ImageState.EXPORT).setTrimEdges(false).setAllowTransparency(true);
        List<VLOArchive> allVlos = getArchive().getAllFiles(VLOArchive.class);
        for (VLOArchive saveVLO : allVlos) {
            File vloFolder = new File(targetFolder, FileUtils.stripExtension(saveVLO.getFileDisplayName()));
            FileUtils.makeDirectory(vloFolder);
            saveVLO.exportAllImages(vloFolder, exportSettings);
        }
    }

    private void promptSearchForTexture() {
        InputMenu.promptInput(getGameInstance(), "Please enter the texture id to lookup.", str -> {
            if (!NumberUtils.isInteger(str)) {
                FXUtils.makePopUp("'" + str + "' is not a valid number.", AlertType.WARNING);
                return;
            }

            int texId = Integer.parseInt(str);
            List<GameImage> images = getArchive().getImagesByTextureId(texId);
            if (images.isEmpty()) {
                FXUtils.makePopUp("Could not find an image with the id " + texId + ".", AlertType.WARNING);
                return;
            }

            for (GameImage image : images)
                getLogger().info("Found " + texId + " as texture #" + image.getLocalImageID() + " in " + FileUtils.stripExtension(image.getParent().getFileDisplayName()) + ".");

            GameImage image = images.get(0);
            VLOController controller = image.getParent().makeEditorUI();
            showEditor(controller);
            controller.selectImage(image, true);
        });
    }

    /**
     * Open an editor for a given file.
     * @param file the file to display UI for
     */
    public void showEditor(SCGameFile<?> file) {
        GameUIController<?> controller = getCurrentEditor();
        if (controller instanceof SCFileEditorUIController) {
            @SuppressWarnings("unchecked")
            SCFileEditorUIController<?, ? super SCGameFile<?>> fileController = (SCFileEditorUIController<?, ? super SCGameFile<?>>) controller;
            if ((fileController.getFileClass() != null && fileController.getFileClass().isInstance(file))) {
                fileController.setTargetFile(file);
                return;
            }
        }

        showEditor(file != null ? file.makeEditorUI() : null);
    }
}