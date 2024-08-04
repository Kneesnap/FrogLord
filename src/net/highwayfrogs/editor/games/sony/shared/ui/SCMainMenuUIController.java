package net.highwayfrogs.editor.games.sony.shared.ui;

import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.MWDFile;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings.ImageState;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.file.writer.FileReceiver;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.ui.file.VLOController;
import net.highwayfrogs.editor.gui.InputMenu;
import net.highwayfrogs.editor.gui.MainMenuController;
import net.highwayfrogs.editor.gui.components.CollectionEditorComponent;
import net.highwayfrogs.editor.gui.components.ProgressBarComponent;
import net.highwayfrogs.editor.gui.extra.hash.HashPlaygroundController;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

/**
 * Represents the editor main menu for a Millennium Interactive game.
 * Created by Kneesnap on 4/12/2024.
 */
public class SCMainMenuUIController<TGameInstance extends SCGameInstance> extends MainMenuController<TGameInstance, SCGameFile<?>> {
    public SCMainMenuUIController(TGameInstance instance) {
        super(instance);
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        super.onControllerLoad(rootNode);

        // Allow exporting MWI.
        addMenuItem(this.menuBarFile, "Export MWI", () -> {
            File selectedFile = Utils.promptFileSave(getGameInstance(), "Specify the file to export the MWI as...", "FROGPSX", "Millennium WAD Index", "MWI");
            if (selectedFile == null)
                return; // Cancel.

            Utils.deleteFile(selectedFile); // Don't merge files, create a new one.
            DataWriter writer = new DataWriter(new FileReceiver(selectedFile));
            getGameInstance().getArchiveIndex().save(writer);
            writer.closeReceiver();

            getLogger().info("Exported MWI.");
        });

        addMenuItem(this.menuBarFile, "Import File", this::importFile); // Ctrl + I
        addMenuItem(this.menuBarFile, "Export File", this::exportFile); // Ctrl + O
        addMenuItem(this.menuBarFile, "Export Original File", this::exportOriginalFile);
        addMenuItem(this.menuBarFile, "Export File (Alternate Format)", () -> getSelectedFileEntry().exportAlternateFormat(getFileEntry())); // Ctrl + E
        addMenuItem(this.menuBarFile, "Export All Textures", this::exportBulkTextures);

        addMenuItem(this.menuBarEdit, "Open Hash Playground", () -> HashPlaygroundController.openEditor(getGameInstance()));
        addMenuItem(this.menuBarEdit, "Find Texture By ID", this::promptSearchForTexture);
    }

    @Override
    protected void saveMainGameData() {
        File baseFolder = getGameInstance().getMainGameFolder();
        if (!baseFolder.canWrite()) {
            Utils.makePopUp("Can't write to the file." + Constants.NEWLINE + "Do you have permission to save in this folder?", AlertType.ERROR);
            return;
        }

        // The canWrite check does not work on the files, only on the directory.
        File outputMwdFile = new File(baseFolder, Utils.stripExtension(getGameInstance().getMwdFile().getName()) + "-MODIFIED.MWD");
        File outputExeFile = new File(baseFolder, Utils.stripExtension(getGameInstance().getExeFile().getName()) + "-modified.exe");

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
        });
    }

    @Override
    protected CollectionEditorComponent<TGameInstance, SCGameFile<?>> createFileListEditor() {
        return new SCGameFileListEditor<>(getGameInstance());
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

    /**
     * Get the FileEntry associated with the selected file.
     * @return fileEntry
     */
    public FileEntry getFileEntry() {
        SCGameFile<?> currentFile = getSelectedFileEntry();
        return currentFile != null ? currentFile.getIndexEntry() : null;
    }

    /**
     * Import a file to replace the current file.
     */
    @SneakyThrows
    public void importFile() {
        File selectedFile = Utils.promptFileOpen(getGameInstance(), "Select the file to import...", "All Files", "*");
        if (selectedFile == null)
            return; // Cancelled.

        FileEntry mwiEntry = getFileEntry(); // Gets the file entry before we reroute files, to avoid resolving the old file's MWI.
        byte[] fileBytes = Files.readAllBytes(selectedFile.toPath());
        SCGameFile<?> oldFile = getSelectedFileEntry();
        SCGameFile<?> newFile = getArchive().replaceFile(fileBytes, mwiEntry, oldFile, false);
        newFile.onImport(oldFile, mwiEntry.getDisplayName(), selectedFile.getName());
        showEditor(newFile.makeEditorUI()); // Open the editor for the new file.
        if (getFileListComponent() != null) // Update the file list.
            getFileListComponent().getCollectionViewComponent().refreshDisplay();

        getLogger().info("Imported " + selectedFile.getName() + " as " + mwiEntry.getDisplayName() + ".");
    }

    /**
     * Export the current file in its original form.
     */
    @SneakyThrows
    public void exportOriginalFile() {
        SCGameFile<?> currentFile = getSelectedFileEntry();
        if (currentFile == null || currentFile.getRawFileData() == null) {
            Utils.makePopUp("Cannot export file.", AlertType.ERROR);
            return;
        }

        FileEntry entry = getFileEntry();
        File selectedFile = Utils.promptFileSave(getGameInstance(), "Specify the file to export this data as...", entry.getDisplayName(), "All Files", "*");
        if (selectedFile == null)
            return; // Cancel.

        Files.write(selectedFile.toPath(), currentFile.getRawFileData());
        getLogger().info("Exported " + selectedFile.getName() + ".");
    }

    /**
     * Export the current file.
     */
    @SneakyThrows
    public void exportFile() {
        SCGameFile<?> currentFile = getSelectedFileEntry();
        FileEntry entry = getFileEntry();

        File selectedFile = Utils.promptFileSave(getGameInstance(), "Specify the file to export this data as...", entry.getDisplayName(), "All Files", "*");
        if (selectedFile == null)
            return; // Cancel.

        Utils.deleteFile(selectedFile); // Don't merge files, create a new one.
        DataWriter writer = new DataWriter(new FileReceiver(selectedFile));
        currentFile.save(writer);
        writer.closeReceiver();

        getLogger().info("Exported " + selectedFile.getName() + ".");
    }

    private void exportBulkTextures() {
        File targetFolder = Utils.promptChooseDirectory(getGameInstance(), "Choose the directory to save all textures to.", false);

        ImageFilterSettings exportSettings = new ImageFilterSettings(ImageState.EXPORT).setTrimEdges(false).setAllowTransparency(true);
        List<VLOArchive> allVlos = getArchive().getAllFiles(VLOArchive.class);
        for (VLOArchive saveVLO : allVlos) {
            File vloFolder = new File(targetFolder, Utils.stripExtension(saveVLO.getFileDisplayName()));
            Utils.makeDirectory(vloFolder);
            saveVLO.exportAllImages(vloFolder, exportSettings);
        }
    }

    private void promptSearchForTexture() {
        InputMenu.promptInput(getGameInstance(), "Please enter the texture id to lookup.", str -> {
            if (!Utils.isInteger(str)) {
                Utils.makePopUp("'" + str + "' is not a valid number.", AlertType.WARNING);
                return;
            }

            int texId = Integer.parseInt(str);
            List<GameImage> images = getArchive().getImagesByTextureId(texId);
            if (images.isEmpty()) {
                Utils.makePopUp("Could not find an image with the id " + texId + ".", AlertType.WARNING);
                return;
            }

            for (GameImage image : images)
                getLogger().info("Found " + texId + " as texture #" + image.getLocalImageID() + " in " + Utils.stripExtension(image.getParent().getFileDisplayName()) + ".");

            GameImage image = images.get(0);
            VLOController controller = image.getParent().makeEditorUI();
            showEditor(controller);
            controller.selectImage(image, true);
        });
    }
}