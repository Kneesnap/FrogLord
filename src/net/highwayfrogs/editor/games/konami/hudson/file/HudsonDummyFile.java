package net.highwayfrogs.editor.games.konami.hudson.file;

import javafx.scene.image.Image;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.hudson.HudsonGameFile;
import net.highwayfrogs.editor.games.shared.basic.file.definition.IGameFileDefinition;
import net.highwayfrogs.editor.gui.ImageResource;

/**
 * Represents a hudson file which is treated as raw bytes.
 * Created by Kneesnap on 8/8/2024.
 */
public class HudsonDummyFile extends HudsonGameFile {
    public HudsonDummyFile(IGameFileDefinition fileDefinition) {
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
        return ImageResource.QUESTION_MARK_16.getFxImage();
    }
}