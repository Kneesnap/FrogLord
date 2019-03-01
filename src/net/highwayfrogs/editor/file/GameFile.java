package net.highwayfrogs.editor.file;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.gui.MainController;
import net.highwayfrogs.editor.gui.editor.EditorController;

import javax.imageio.ImageIO;
import java.io.IOException;

/**
 * Represents a game file.
 * Created by Kneesnap on 8/11/2018.
 */
public abstract class GameFile extends GameObject {

    /**
     * Get the icon which will appear for this file in the file list.
     * @return icon
     */
    public abstract Image getIcon();

    /**
     * Makes an editor for this file.
     * @return editor
     */
    public abstract Node makeEditor();

    /**
     * Called when this file is imported.
     * @param oldFile          The old file.
     * @param oldFileName      The old file's name.
     * @param importedFileName The name of the file just imported.
     */
    public void onImport(GameFile oldFile, String oldFileName, String importedFileName) {

    }

    /**
     * Handle when it's time to be edited.
     * @param parent The wad parent.
     */
    public void handleWadEdit(WADFile parent) {

    }

    /**
     * Gets this file's MWI FileEntry.
     * @return fileEntry
     */
    public FileEntry getFileEntry() {
        return getMWD().getEntryMap().get(this);
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
    public static <T extends GameFile> Node loadEditor(EditorController<T> controller, String template, T editFile) {
        try {
            FXMLLoader loader = Utils.getFXMLLoader(template);
            loader.setController(controller);
            Node node = loader.load();
            controller.loadFile(editFile);
            MainController.setCurrentController(controller);

            return node;
        } catch (IOException ex) {
            throw new RuntimeException("Failed to setup file editor.", ex);
        }
    }
}
