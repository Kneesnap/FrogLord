package net.highwayfrogs.editor.games.konami.greatquest.chunks;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntity3DInst;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntityInst;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.FileUtils.SavedFilePath;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.io.File;

/**
 * Recreation of the 'kcCResourceEntityInst' class from the PS2 version.
 * Created by Kneesnap on 8/25/2019.
 */
@Getter
@Setter
public class kcCResourceEntityInst extends kcCResource {
    private kcEntityInst instance;
    private byte[] dummyBytes;

    private static final String ENTITY_FILE_PATH_KEY = "entityCfgFilePath";
    private static final SavedFilePath ENTITY_EXPORT_PATH = new SavedFilePath(ENTITY_FILE_PATH_KEY, "Select the directory to export the entity to", Config.DEFAULT_FILE_TYPE);
    private static final SavedFilePath ENTITY_IMPORT_PATH = new SavedFilePath(ENTITY_FILE_PATH_KEY, "Select the directory to import entity data from", Config.DEFAULT_FILE_TYPE);

    public kcCResourceEntityInst(GreatQuestChunkedFile parentFile) {
        super(parentFile, KCResourceID.ENTITYINST);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);

        reader.jumpTemp(reader.getIndex());
        int sizeInBytes = reader.readInt(); // Number of bytes used for entity data.
        reader.jumpReturn();

        int calculatedSize = reader.getRemaining(); // We've returned to before the size integer was read.
        if (sizeInBytes != calculatedSize)
            throw new RuntimeException("The expected amount of entity data (" + sizeInBytes + " bytes) different from the actual amount (" + calculatedSize + " bytes).");

        this.instance = null;
        this.dummyBytes = null;
        if (sizeInBytes == kcEntity3DInst.SIZE_IN_BYTES) {
            this.instance = new kcEntity3DInst(this);
            this.instance.load(reader);
        } else if (sizeInBytes == kcEntityInst.SIZE_IN_BYTES) {
            // This appears supported, although I am unsure in what circumstance this would ever be useful, and the game doesn't seem to have any entities like this.
            getLogger().warning("A non-3D entity was found! This is unusual, and may not work correctly.");
            this.instance = new kcEntityInst(this);
            this.instance.load(reader);
        } else {
            // TODO: Let's reverse engineer this.
            getLogger().severe("Couldn't identify the entity type for '%s' from the byte size of %d.", getName(), sizeInBytes);
            this.dummyBytes = reader.readBytes(sizeInBytes);
        }
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        if (this.instance != null) {
            this.instance.save(writer);
        } else if (this.dummyBytes != null) {
            writer.writeBytes(this.dummyBytes);
        }
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        // TODO: ADD ENTITY DATA INTO PROPERTY LIST!
        return propertyList;
    }

    @Override
    public void setupRightClickMenuItems(ContextMenu contextMenu) {
        super.setupRightClickMenuItems(contextMenu);

        MenuItem exportEntityItem = new MenuItem("Export Entity");
        contextMenu.getItems().add(exportEntityItem);
        exportEntityItem.setOnAction(event -> {
            File outputFile = FileUtils.askUserToSaveFile(getGameInstance(), ENTITY_EXPORT_PATH, getName() + "." + Config.DEFAULT_EXTENSION, true);
            if (outputFile != null) {
                this.instance.toConfig().saveTextFile(outputFile);
                getLogger().info("Saved '%s' as '%s'.", getName(), outputFile.getName());
            }
        });
        exportEntityItem.setDisable(this.instance == null);

        MenuItem importEntityItem = new MenuItem("Import Entity");
        contextMenu.getItems().add(importEntityItem);
        importEntityItem.setOnAction(event -> {
            File inputFile = FileUtils.askUserToOpenFile(getGameInstance(), ENTITY_IMPORT_PATH);
            if (inputFile == null)
                return;

            Config entityCfg = Config.loadConfigFromTextFile(inputFile, false);
            if (this.instance == null)
                this.instance = new kcEntity3DInst(this);

            this.instance.fromConfig(entityCfg);
            getLogger().info("Loaded '%s from '%s'.", getName(), inputFile.getName());
        });
    }
}