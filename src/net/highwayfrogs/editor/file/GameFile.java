package net.highwayfrogs.editor.file;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Utils;
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
        return SwingFXUtils.toFXImage(ImageIO.read(Utils.getResource("icons/" + iconName + ".png")), null);
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
            FXMLLoader loader = new FXMLLoader(Utils.getResource("javafx/" + template + ".fxml"));
            loader.setController(controller);
            Node node = loader.load();
            controller.loadFile(editFile);

            return node;
        } catch (IOException ex) {
            throw new RuntimeException("Failed to setup file editor.", ex);
        }
    }
}
