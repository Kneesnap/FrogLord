package net.highwayfrogs.editor.games.konami.hudson.file;

import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.hudson.HudsonGameFile;
import net.highwayfrogs.editor.games.konami.hudson.IHudsonFileDefinition;
import net.highwayfrogs.editor.games.renderware.RwStreamChunk;
import net.highwayfrogs.editor.games.renderware.RwStreamFile;
import net.highwayfrogs.editor.games.renderware.chunks.RwPlatformIndependentTextureDictionaryChunk;
import net.highwayfrogs.editor.games.renderware.chunks.RwPlatformIndependentTextureDictionaryChunk.RwPlatformIndependentTextureEntry;
import net.highwayfrogs.editor.games.renderware.ui.RenderWareStreamEditorUIController;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.Utils;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a hudson game file which is a RenderWare stream file.
 * Created by Kneesnap on 8/8/2024.
 */
@Getter
public class HudsonRwStreamFile extends HudsonGameFile {
    private final RwStreamFile rwStreamFile;

    public HudsonRwStreamFile(IHudsonFileDefinition fileDefinition) {
        super(fileDefinition);
        this.rwStreamFile = new RwStreamFile(getGameInstance(), getGameInstance().getRwStreamChunkTypeRegistry(), fileDefinition.getFileName());
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

    /**
     * Export all textures in the file.
     * @param outputFolder the file to export textures to
     * @param fileNameCountMap the file-name count map to use.
     */
    public void exportTextures(File outputFolder, Map<String, AtomicInteger> fileNameCountMap) {
        for (RwStreamChunk chunk : this.rwStreamFile.getChunks()) {
            if (!(chunk instanceof RwPlatformIndependentTextureDictionaryChunk))
                continue;

            RwPlatformIndependentTextureDictionaryChunk textureDictionaryChunk = (RwPlatformIndependentTextureDictionaryChunk) chunk;
            for (RwPlatformIndependentTextureEntry entry : textureDictionaryChunk.getEntries()) {
                for (int i = 0; i < entry.getMipLevelImages().size(); i++) {
                    String baseName = entry.makeFileName(i);
                    int num = fileNameCountMap.computeIfAbsent(baseName, key -> new AtomicInteger()).getAndIncrement();

                    Utils.makeDirectory(outputFolder);
                    File imageOutputFile = new File(outputFolder, String.format("%s_%02d.png", baseName, num));

                    try {
                        ImageIO.write(entry.getMipLevelImages().get(i).getImage(), "png", imageOutputFile);
                    } catch (IOException ex) {
                        Utils.handleError(getLogger(), ex, false, "Failed to save '%s'.", imageOutputFile.getName());
                    }
                }
            }
        }
    }
}