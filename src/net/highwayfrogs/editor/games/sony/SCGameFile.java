package net.highwayfrogs.editor.games.sony;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.WADFile;
import net.highwayfrogs.editor.gui.MainController;
import net.highwayfrogs.editor.gui.editor.EditorController;
import net.highwayfrogs.editor.system.Tuple2;
import net.highwayfrogs.editor.utils.Utils;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a file (data corresponding to MWI entry or contents of a filesystem entity) for a particul
 * @param <TGameInstance> The type of game instance this file can be used in.
 *                        Created by Kneesnap on 9/8/2023.
 */
public abstract class SCGameFile<TGameInstance extends SCGameInstance> extends SCGameData<TGameInstance> {
    public SCGameFile(TGameInstance instance) {
        super(instance);
    }

    /**
     * Get the icon which should appear for this file in the file list.
     */
    public abstract Image getIcon();

    /**
     * Makes a JavaFX UI Node which will be put into the preview pane for this file. Commonly used for editing.
     * @return editor
     */
    public abstract Node makeEditor();

    /**
     * WAD files are capable of containing any file.
     * This method is called when this file has the "Edit" button pressed from inside a WAD file.
     * Examples of how this can be used include: Opening a new UI area (Such as for viewing a 3D model) or replacing the WAD UI with the normal UI of the file created by makeEditor().
     * @param parent The wad file this is edited from.
     */
    public void handleWadEdit(WADFile parent) {

    }

    /**
     * Called when this file is imported.
     * @param oldFile          The old file.
     * @param oldFileName      The old file's name.
     * @param importedFileName The name of the file just imported.
     */
    public void onImport(SCGameFile<?> oldFile, String oldFileName, String importedFileName) {

    }

    /**
     * Gets a list of properties to show in contexts where a file has information shown.
     * @return wadProperties, can be null.
     */
    public List<Tuple2<String, String>> createPropertyList() {
        List<Tuple2<String, String>> list = new ArrayList<>();
        FileEntry fileEntry = getIndexEntry();
        list.add(new Tuple2<>("File Type ID", String.valueOf(fileEntry.getTypeId())));
        if (fileEntry.hasFilePath()) // Show path from MWI, not faked one.
            list.add(new Tuple2<>("File Path", fileEntry.getFilePath()));

        return list;
    }

    /**
     * Gets the file entry of this file from the MWI.
     * This is designed to always work, even during the load process.
     * @return fileEntry
     */
    public FileEntry getIndexEntry() {
        TGameInstance instance = getGameInstance();
        if (instance == null)
            throw new RuntimeException("The game instance is null.");

        FileEntry entry = instance.getFileEntriesByFileObjects().get(this);
        if (entry == null) // Shouldn't occur.
            throw new RuntimeException("The SCGameFile was not registered in the MWI entry mapping.");

        return entry;
    }

    /**
     * Gets the display name of this file.
     */
    public String getFileDisplayName() {
        return getIndexEntry().getDisplayName();
    }

    /**
     * Export this file in a non-Frogger format.
     */
    public void exportAlternateFormat(FileEntry entry) {
        System.out.println(entry.getDisplayName() + " (" + getClass().getSimpleName() + ") does not have an alternate file-type it can export as.");
    }

    /**
     * Setup the editor.
     * @param editorPane The parent pane holding the editor.
     * @param node       The node created by makeEditor.
     */
    public void setupEditor(AnchorPane editorPane, Node node) {
        editorPane.getChildren().add(node);
    }

    /**
     * Load an icon by name.
     * @param iconName The icon to load.
     * @return loadedIcon
     */
    @SneakyThrows
    public static Image loadIcon(String iconName) {
        return Utils.toFXImage(ImageIO.read(Utils.getResource("icons/" + iconName + ".png")), false);
    }

    /**
     * Sets up a GameFile editor as a JavaFX Node.
     * @param controller The controller to control the GUI.
     * @param template   The gui layout template.
     * @param editFile   The file to edit.
     * @return guiNode
     */
    public static <T extends SCGameFile<?>> Node loadEditor(EditorController<T, ?, ?> controller, String template, T editFile) {
        try {
            FXMLLoader loader = Utils.getFXMLLoader(template);
            loader.setController(controller);
            Node node = loader.load();
            controller.loadFile(editFile);
            MainController.setCurrentController(controller);

            return node;
        } catch (IOException ex) {
            throw new RuntimeException("Failed to setup file editor for '" + template + "'.", ex);
        }
    }

    /**
     * Represents an SCGameFile which can be used by any SCGameInstance.
     */
    public static abstract class SCSharedGameFile extends SCGameFile<SCGameInstance> {
        public SCSharedGameFile(SCGameInstance instance) {
            super(instance);
        }
    }
}