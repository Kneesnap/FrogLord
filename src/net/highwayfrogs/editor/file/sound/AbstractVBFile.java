package net.highwayfrogs.editor.file.sound;

import javafx.scene.Node;
import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.file.WADFile;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.games.sony.SCGameFile.SCSharedGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.gui.MainController;
import net.highwayfrogs.editor.gui.editor.VABController;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a general VBFile.
 * Created by Kneesnap on 2/13/2019.
 */
public abstract class AbstractVBFile<T extends SCSharedGameFile> extends SCSharedGameFile {
    @Getter private final List<GameSound> audioEntries = new ArrayList<>();
    protected transient DataReader cachedReader;
    @Getter private transient T header;

    public AbstractVBFile(SCGameInstance instance) {
        super(instance);
    }

    /**
     * Load the VB file, with the mandatory VH file.
     * @param file The VHFile to load information from.
     */
    public void load(T file) {
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
        return loadEditor(new VABController(getGameInstance()), "vb", this);
    }

    @Override
    public void handleWadEdit(WADFile parent) {
        MainController.MAIN_WINDOW.openEditor(MainController.MAIN_WINDOW.getCurrentFilesList(), this);
    }
}