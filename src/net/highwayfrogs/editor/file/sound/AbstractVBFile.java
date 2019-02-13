package net.highwayfrogs.editor.file.sound;

import javafx.scene.Node;
import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameFile;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.gui.editor.VABController;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a general VBFile.
 * Created by Kneesnap on 2/13/2019.
 */
public abstract class AbstractVBFile extends GameFile {
    @Getter private List<GameSound> audioEntries = new ArrayList<>();
    protected transient DataReader cachedReader;
    @Getter private transient VHFile header;

    /**
     * Load the VB file, with the mandatory VH file.
     * @param file The VHFile to load information from.
     */
    public void load(VHFile file) {
        Utils.verify(this.cachedReader != null, "Tried to load VB without a reader.");
        this.header = file;
        load(this.cachedReader);
        this.cachedReader = null;
    }

    @Override
    public Image getIcon() {
        return VHFile.ICON;
    }

    @Override
    public Node makeEditor() {
        return loadEditor(new VABController(), "vb", this);
    }
}
