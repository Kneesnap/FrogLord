package net.highwayfrogs.editor.games.sony.shared.sound;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameFile.SCSharedGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.ui.file.SCVABUIController;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.utils.FXUtils;

/**
 * Represents a .VB file.
 * Created by Kneesnap on 5/13/2024.
 */
@Getter
public class SCSplitVBFile extends SCSharedGameFile {
    private final SCSplitSoundBankBody<?, ?> body;
    SCSplitSoundBank soundBank;

    public SCSplitVBFile(SCGameInstance instance, SCSplitSoundBankBody<?, ?> body) {
        super(instance);
        this.body = body;
    }

    @Override
    public void load(DataReader reader) {
        this.body.load(reader);
    }

    @Override
    public void save(DataWriter writer) {
        this.body.save(writer);
    }

    @Override
    public boolean warnIfEndNotReached() {
        return false;
    }

    @Override
    public Image getCollectionViewIcon() {
        return ImageResource.MUSIC_NOTE_32.getFxImage();
    }

    @Override
    public GameUIController<?> makeEditorUI() {
        if (this.soundBank != null)
            return loadEditor(getGameInstance(), "edit-file-vb", new SCVABUIController(getGameInstance()), this);

        FXUtils.makePopUp("Could not find the sound bank header file which pairs with this file.", AlertType.ERROR);
        return null;
    }

    /**
     * Creates the sound bank.
     */
    public void createSoundBank(SCSplitVHFile vhFile) {
        if (this.soundBank != null)
            throw new RuntimeException("A sound bank already exists for this file.");
        if (vhFile == null)
            throw new NullPointerException("vhFile");

        vhFile.vbFile = this;
        this.soundBank = vhFile.soundBank = new SCSplitSoundBank(getGameInstance(), vhFile.getHeader(), this.body);
    }
}