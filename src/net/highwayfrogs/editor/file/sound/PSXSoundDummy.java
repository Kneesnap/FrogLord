package net.highwayfrogs.editor.file.sound;

import javafx.scene.Node;
import javafx.scene.image.Image;
import net.highwayfrogs.editor.file.DummyFile;
import net.highwayfrogs.editor.gui.editor.PS1SoundController;

/**
 * Represents a PSX audio file which is not supported yet.
 * Created by Kneesnap on 7/24/2019.
 */
public class PSXSoundDummy extends DummyFile {
    public PSXSoundDummy(int length) {
        super(length);
    }

    @Override
    public Image getIcon() {
        return VHFile.ICON;
    }

    @Override
    public Node makeEditor() {
        return loadEditor(new PS1SoundController(), "psx-sound", this);
    }
}
