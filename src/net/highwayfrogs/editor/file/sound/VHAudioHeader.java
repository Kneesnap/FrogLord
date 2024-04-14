package net.highwayfrogs.editor.file.sound;

import javafx.scene.image.Image;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.games.sony.SCGameFile.SCSharedGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.ui.file.VABController;

/**
 * Represents a .VH file, or rather an audio header file.
 * Created by Kneesnap on 9/9/2023.
 */
@Getter @Setter
public abstract class VHAudioHeader extends SCSharedGameFile {
    private VBAudioBody<?> vbFile;

    public static final Image ICON = loadIcon("sound");

    public VHAudioHeader(SCGameInstance instance) {
        super(instance);
    }

    @Override
    public Image getCollectionViewIcon() {
        return ICON;
    }

    @Override
    public VABController makeEditorUI() {
        if (getVbFile() == null) {
            System.out.println("The associated VB sound is null.");
            return null;
        }

        return getVbFile().makeEditorUI(); // Build the editor for the right file.
    }
}