package net.highwayfrogs.editor.games.renderware.game.file;

import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.renderware.RwStreamFile;
import net.highwayfrogs.editor.games.renderware.ui.RenderWareStreamEditorUIController;
import net.highwayfrogs.editor.games.shared.basic.file.definition.IGameFileDefinition;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;

import java.io.File;
import java.util.HashMap;

/**
 * Represents an RwStreamFile for a generic RenderWare game.
 * Created by Kneesnap on 8/18/2024.
 */
@Getter
public class RwGenericStreamFile extends RwGenericFile {
    private final RwStreamFile rwStreamFile;

    public RwGenericStreamFile(IGameFileDefinition fileDefinition) {
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
        return this.rwStreamFile.getBestChunkIcon();
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

    @Override
    public void handleDoubleClick() {
        this.rwStreamFile.handleDoubleClick();
    }

    @Override
    public void export(File exportFolder) {
        File imagesExportDir = new File(exportFolder, "Images [" + getFileDefinition().getFile().getParentFile().getName() + "_" + getDisplayName() + "]");
        this.rwStreamFile.exportTextures(imagesExportDir, new HashMap<>());
    }
}