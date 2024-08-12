package net.highwayfrogs.editor.games.sony.shared.sound;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameFile.SCSharedGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.ui.file.SCVABUIController;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents a .VH file.
 * Created by Kneesnap on 5/13/2024.
 */
@Getter
public class SCSplitVHFile extends SCSharedGameFile {
    private final SCSplitSoundBankHeader<?, ?> header;
    SCSplitSoundBank soundBank;
    SCSplitVBFile vbFile;

    public SCSplitVHFile(SCGameInstance instance, SCSplitSoundBankHeader<?, ?> header) {
        super(instance);
        this.header = header;
    }

    @Override
    public void load(DataReader reader) {
        this.header.load(reader);
    }

    @Override
    public void save(DataWriter writer) {
        this.header.save(writer);
    }

    @Override
    public Image getCollectionViewIcon() {
        return ImageResource.MUSIC_NOTE_32.getFxImage();
    }

    @Override
    public GameUIController<?> makeEditorUI() {
        if (this.soundBank != null && this.vbFile != null)
            return loadEditor(getGameInstance(), "edit-file-vb", new SCVABUIController(getGameInstance()), this.vbFile);

        Utils.makePopUp("Could not find the sound bank body file which pairs with this file.", AlertType.ERROR);
        return null;
    }

    /**
     * Creates the sound bank.
     */
    public void createSoundBank(SCSplitVBFile vbFile) {
        if (this.soundBank != null)
            throw new RuntimeException("A sound bank already exists for this file.");
        if (vbFile == null)
            throw new NullPointerException("vbFile");

        this.vbFile = vbFile;
        this.soundBank = vbFile.soundBank = new SCSplitSoundBank(getGameInstance(), this.header, vbFile.getBody());
    }
}