package net.highwayfrogs.editor.games.sony.shared.ui;

import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import net.highwayfrogs.editor.games.sony.SCGameConfig;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.SCGameType;
import net.highwayfrogs.editor.games.sony.shared.ISCMWDHeaderGenerator;
import net.highwayfrogs.editor.games.sony.shared.mwd.MWDFile;
import net.highwayfrogs.editor.games.sony.shared.ui.file.VLOController;
import net.highwayfrogs.editor.games.sony.shared.utils.SCAnalysisUtils;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloFile;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloImage;
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
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.data.writer.FileReceiver;

import java.io.File;
import java.util.List;

/**
 * Represents the editor main menu for a Millennium Interactive game.
 * Created by Kneesnap on 4/12/2024.
 */
public class SCMainMenuUIController<TGameInstance extends SCGameInstance> extends MainMenuController<TGameInstance, SCGameFile<?>> {
    private static final SavedFilePath TEXTURE_FOLDER = new SavedFilePath("bulkTextureExportPath", "Choose the folder to save all textures to.");
    public static final BrowserFileType MWI_FILE_TYPE = new BrowserFileType("Millennium WAD Index", "MWI");
    private static final SavedFilePath MWI_FILE = new SavedFilePath("mwiFilePath", "Specify the file to save the MWI as...", MWI_FILE_TYPE);
    private static final SavedFilePath MWD_HEADER_FILE = new SavedFilePath("mwdHeaderFilePath", "Specify the file to save the MWD header as...", FileUtils.EXPORT_C_HEADER_FILE_TYPE);
    private static final SavedFilePath VLO_HEADER_FILE = new SavedFilePath("vloHeaderFilePath", "Specify the file to save the VLO header as...", FileUtils.EXPORT_C_HEADER_FILE_TYPE);

    private static final SavedFilePath SAVE_MWD_FILE_PATH = new SavedFilePath("mwd-save-path", "Please select the file to save the MWD file as...", SCGameType.MWD_FILE_TYPE);
    private static final SavedFilePath SAVE_EXE_FILE_PATH = new SavedFilePath("exe-save-path", "Please select the file to save the executable as...", SCGameType.EXECUTABLE_FILE_TYPE);

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

        addMenuItem(this.menuBarFile, "Export All Textures", this::exportBulkTextures);
        if (getGameInstance().getVloTree() != null) {
            addMenuItem(this.menuBarEdit, "Rebuild Vlos", () -> {
                ProgressBarComponent.openProgressBarWindow(getGameInstance(), "Texture Placement", progressBar -> {
                    long startTime = System.currentTimeMillis();
                    getGameInstance().getVloTree().rebuildRecursive(progressBar);
                    long endTime = System.currentTimeMillis();
                    getLogger().info("Rebuild took %d ms.", endTime - startTime);
                });
            });
        }

        addMenuItem(this.menuBarEdit, "Open Hash Playground", () -> HashPlaygroundController.openEditor(getGameInstance()));
        addMenuItem(this.menuBarEdit, "Find Texture By ID", this::promptSearchForTexture);
        addMenuItem(this.menuBarEdit, "Find Unused Textures", () -> SCAnalysisUtils.findUnusedTextures(getGameInstance()));
        if (getGameInstance() instanceof ISCMWDHeaderGenerator) {
            addMenuItem(this.menuBarEdit, "Generate MWD Header File (.H)", () -> {
                String defaultFileName = getGameInstance().getGameType().getMwdHeaderFileName();
                if (defaultFileName == null)
                    defaultFileName = "export.h";

                File targetFile = FileUtils.askUserToSaveFile(getGameInstance(), MWD_HEADER_FILE, defaultFileName, false);
                if (targetFile != null)
                    ((ISCMWDHeaderGenerator) getGameInstance()).generateMwdCHeader(targetFile);
            });

            addMenuItem(this.menuBarEdit, "Generate VLO Source Files (.C/.H)", () -> {
                String defaultFileName = getGameInstance().getGameType().getVloHeaderFileName();
                if (defaultFileName == null)
                    defaultFileName = "vlo.h";

                File targetFile = FileUtils.askUserToSaveFile(getGameInstance(), VLO_HEADER_FILE, defaultFileName, false);
                if (targetFile != null)
                    ((ISCMWDHeaderGenerator) getGameInstance()).generateVloSourceFiles(getGameInstance(), targetFile);
            });
        }
    }

    @Override
    protected void saveMainGameData() {
        if (getConfig().isMwdLooseFiles()) {
            // We can support this at any time I think.
            boolean saveAnyways = FXUtils.makePopUpYesNo("Saving files outside of the MWD is not supported yet.\n"
                    + "So, this will create a MWD file instead. Would you like to continue?");
            if (!saveAnyways)
                return;
        }

        File baseFolder = getGameInstance().getMainGameFolder();
        if (!baseFolder.canWrite()) {
            FXUtils.showPopup(AlertType.ERROR, "Can't write to the file.", "Do you have permission to save in this folder?");
            return;
        }

        // The canWrite check does not work on the files, only on the directory.

        File outputMwdFile = FileUtils.askUserToSaveFile(getGameInstance(), SAVE_MWD_FILE_PATH, "modded_" + FileUtils.stripExtension(getGameInstance().getMwdFile().getName()) + ".MWD");
        if (outputMwdFile == null)
            return;

        // This is for the user's own good-- I've seen it too many times.
        // A user either doesn't understand the consequences or doesn't think it's a big deal, until it becomes a problem.
        if (outputMwdFile.equals(getGameInstance().getMwdFile())) {
            FXUtils.showPopup(AlertType.ERROR, "Safety check failed.", "Overwriting loaded game files is not permitted.");
            return;
        }

        File outputExeFile = FileUtils.askUserToSaveFile(getGameInstance(), SAVE_EXE_FILE_PATH, "modded_" + getGameInstance().getExeFile().getName());
        if (outputExeFile == null)
            return;

        // This is for the user's own good-- I've seen it too many times.
        // A user either doesn't understand the consequences or doesn't think it's a big deal, until it becomes a problem.
        if (outputExeFile.equals(getGameInstance().getExeFile())) {
            FXUtils.showPopup(AlertType.ERROR, "Safety check failed.", "Overwriting loaded game files is not permitted.");
            return;
        }

        // Prevent the user from separating the files unless they really intend to.
        if (!outputExeFile.getParentFile().equals(outputMwdFile.getParentFile()))
            if (!FXUtils.makePopUpYesNo("Are you sure?", "Are you sure you would like to save " + outputMwdFile.getName() + " and " + outputExeFile.getName() + " to different folders?\nUnless you know what you're doing, respond 'No'."))
                return;

        File outputMwiFile = new File(outputExeFile.getParentFile(), FileUtils.stripExtension(outputMwdFile.getName()) + ".MWI");

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
            getGameInstance().getArchiveIndex().writeDataToFile(getLogger(), outputMwiFile, true);
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

        List<VloFile> allVlos = getArchive().getAllFiles(VloFile.class);
        for (VloFile saveVLO : allVlos) {
            File vloFolder = new File(targetFolder, FileUtils.stripExtension(saveVLO.getFileDisplayName()));
            FileUtils.makeDirectory(vloFolder);
            saveVLO.exportAllImages(vloFolder, VloFile.IMAGE_EXPORT_SETTINGS);
        }
    }

    private void promptSearchForTexture() {
        InputMenu.promptInput(getGameInstance(), "Please enter the texture id/name to lookup.", str -> {
            int textureId;
            Short resolvedTextureIdByName = getGameInstance().getTextureIdByOriginalName(str);
            if (resolvedTextureIdByName != null && resolvedTextureIdByName >= 0) {
                textureId = resolvedTextureIdByName;
            } else {
                if (!NumberUtils.isInteger(str)) {
                    FXUtils.showPopup(AlertType.WARNING, "Invalid Texture ID", "'" + str + "' is not a valid number or texture name.");
                    return;
                }

                textureId = Integer.parseInt(str);
            }

            List<VloImage> images = getArchive().getImagesByTextureId(textureId);
            if (images.isEmpty()) {
                FXUtils.showPopup(AlertType.WARNING, "Couldn't find image.", "Could not find an image with the id " + textureId + ".");
                return;
            }

            for (VloImage image : images)
                getLogger().info("Found %d as texture #%d in %s.", textureId, image.getLocalImageID(), FileUtils.stripExtension(image.getParent().getFileDisplayName()));

            VloImage image = images.get(0);
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
                if (fileController.getFile() != file)
                    fileController.setTargetFile(file);
                return;
            }
        }

        showEditor(file != null ? file.makeEditorUI() : null);
    }
}