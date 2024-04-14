package net.highwayfrogs.editor.file.sound;

import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.games.sony.SCGameFile.SCSharedGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.ui.file.VABController;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a .VB file which contains raw audio data.
 * Created by Kneesnap on 2/13/2019.
 */
public abstract class VBAudioBody<THeader extends VHAudioHeader> extends SCSharedGameFile {
    @Getter private final List<GameSound> audioEntries = new ArrayList<>();
    private DataReader cachedReader;
    @Getter private transient THeader header;

    public VBAudioBody(SCGameInstance instance) {
        super(instance);
    }

    @Override
    public Image getCollectionViewIcon() {
        return VHFile.ICON;
    }

    @Override
    public VABController makeEditorUI() {
        return loadEditor(getGameInstance(), "edit-file-vb", new VABController(getGameInstance()), this);
    }

    @Override
    public void load(DataReader reader) {
        if (this.cachedReader != null)
            throw new RuntimeException("There is already a DataReader cached for future file reading!");

        if (this.header != null) {
            this.load(reader, this.header);
        } else {
            this.cachedReader = reader;
        }
    }

    /**
     * Sets the header file corresponding to this audio body.
     * @param header The header in question.
     */
    @SuppressWarnings("unchecked")
    public void setHeader(VHAudioHeader header) {
        this.header = (THeader) header;
        if (header != null) {
            header.setVbFile(this);

            if (this.cachedReader != null) {
                this.load(this.cachedReader, this.header);
                this.cachedReader = null;
            }
        }
    }

    /**
     * Loads data with the corresponding audio header.
     * @param reader      The reader to read data from.
     * @param audioHeader The audio header to apply.
     */
    protected abstract void load(DataReader reader, THeader audioHeader);
}