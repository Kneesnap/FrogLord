package net.highwayfrogs.editor.file;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Utils;

import javax.imageio.ImageIO;

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
     * Load an icon by name.
     * @param iconName The icon to load.
     * @return loadedIcon
     */
    @SneakyThrows
    public static Image loadIcon(String iconName) {
        return SwingFXUtils.toFXImage(ImageIO.read(Utils.getResource("icons/" + iconName + ".png")), null);
    }
}
