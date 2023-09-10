package net.highwayfrogs.editor.file.sound;

import javafx.scene.Node;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.WADFile;
import net.highwayfrogs.editor.games.sony.SCGameFile.SCSharedGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.gui.MainController;

/**
 * Represents a .VH file, or rather an audio header file.
 * Created by Kneesnap on 9/9/2023.
 */
@Getter
public abstract class VHAudioHeader extends SCSharedGameFile {
    @Setter private VBAudioBody<?> vbFile;

    public static final Image ICON = loadIcon("sound");

    public VHAudioHeader(SCGameInstance instance) {
        super(instance);
    }

    @Override
    public Image getIcon() {
        return ICON;
    }

    @Override
    public Node makeEditor() {
        if (getVbFile() == null) {
            System.out.println("The associated VB sound is null.");
            return null;
        }

        return getVbFile().makeEditor(); // Build the editor for the right file.
    }

    @Override
    public void handleWadEdit(WADFile parent) {
        MainController.MAIN_WINDOW.openEditor(MainController.MAIN_WINDOW.getCurrentFilesList(), this);
    }
}