package net.highwayfrogs.editor.games.konami.greatquest;

import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.utils.Utils;

/**
 * A TGQ dummy file.
 * Created by Kneesnap on 8/17/2019.
 */
@Getter
public class GreatQuestDummyArchiveFile extends GreatQuestArchiveFile {
    private byte[] data;
    private final int length;

    public GreatQuestDummyArchiveFile(GreatQuestInstance instance, int length) {
        super(instance);
        this.length = length;
    }

    @Override
    public void load(DataReader reader) {
        this.data = reader.readBytes(this.length);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeBytes(this.data);
    }

    @Override
    public String getExtension() {
        if (this.data.length >= 3) {
            String useString = new String(new byte[]{data[0], data[1], data[2]});
            if (Utils.isAlphanumeric(useString))
                return useString;
        }

        return super.getExtension();
    }

    @Override
    public String getDefaultFolderName() {
        return "Dummy";
    }

    @Override
    public Image getCollectionViewIcon() {
        return ImageResource.QUESTION_MARK_16.getFxImage();
    }

    @Override
    public GameUIController<?> makeEditorUI() {
        return null;
    }
}