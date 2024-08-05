package net.highwayfrogs.editor.games.konami.ancientshadow.file;

import javafx.scene.image.Image;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.ancientshadow.AncientShadowGameFile;
import net.highwayfrogs.editor.games.konami.hudson.IHudsonFileDefinition;
import net.highwayfrogs.editor.gui.ImageResource;

/**
 * Represents a file which is treated as raw bytes.
 * Created by Kneesnap on 8/4/2024.
 */
public class AncientShadowDummyFile extends AncientShadowGameFile {
    public AncientShadowDummyFile(IHudsonFileDefinition fileDefinition) {
        super(fileDefinition);
    }

    @Override
    public void load(DataReader reader) {
        reader.skipBytes(getRawData().length);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeBytes(getRawData());
    }

    @Override
    public Image getCollectionViewIcon() {
        return ImageResource.QUESTION_MARK_15.getFxImage();
    }
}