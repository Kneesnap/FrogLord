package net.highwayfrogs.editor.games.konami.hudson.file;

import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.hudson.HudsonGameFile;
import net.highwayfrogs.editor.games.renderware.RwStreamFile;
import net.highwayfrogs.editor.games.renderware.ui.RenderWareStreamEditorUIController;
import net.highwayfrogs.editor.games.shared.basic.file.definition.IGameFileDefinition;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;

/**
 * Represents a hudson game file which is a RenderWare stream file.
 * Created by Kneesnap on 8/8/2024.
 */
@Getter
public class HudsonRwStreamFile extends HudsonGameFile {
    private final RwStreamFile rwStreamFile;

    public HudsonRwStreamFile(IGameFileDefinition fileDefinition) {
        super(fileDefinition);
        this.rwStreamFile = new RwStreamFile(getGameInstance(), getGameInstance().getRwStreamChunkTypeRegistry(), fileDefinition.getFullFilePath());
    }

    @Override
    public void load(DataReader reader) {
        this.rwStreamFile.load(reader);
    }

    @Override
    public void save(DataWriter writer) {
        this.rwStreamFile.save(writer);
    }

    @Override
    public Image getCollectionViewIcon() {
        return ImageResource.GHIDRA_ICON_MULTIMEDIA_16.getFxImage();
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("Chunks", this.rwStreamFile.getChunks().size());

        for (int i = 0; i < this.rwStreamFile.getChunks().size(); i++)
            propertyList.add("Chunk " + i, this.rwStreamFile.getChunks().get(i).getChunkDescriptor());

        return propertyList;
    }

    @Override
    public GameUIController<?> makeEditorUI() {
        return RenderWareStreamEditorUIController.loadController(getGameInstance(), this.rwStreamFile);
    }
}