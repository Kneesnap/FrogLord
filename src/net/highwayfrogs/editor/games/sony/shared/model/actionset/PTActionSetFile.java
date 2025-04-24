package net.highwayfrogs.editor.games.sony.shared.model.actionset;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameFile.SCSharedGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.model.PTModelFileUIController;
import net.highwayfrogs.editor.games.sony.shared.model.staticmesh.PTStaticFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an action (animation?) set.
 * Created by Kneesnap on 5/15/2024.
 */
@Getter
public class PTActionSetFile extends SCSharedGameFile {
    private int flags; // Always zero/empty.
    private final List<PTActionSet> actionSets = new ArrayList<>();

    public static final int IDENTIFIER = 0x33305341;
    public static final String IDENTIFIER_STRING = Utils.toIdentifierString(IDENTIFIER); // 'AS03'

    public PTActionSetFile(SCGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        reader.verifyString(IDENTIFIER_STRING);
        this.flags = reader.readInt();
        warnAboutInvalidBitFlags(this.flags, 0);
        int actionSetCount = reader.readUnsignedShortAsInt();
        reader.skipBytesRequireEmpty(Constants.SHORT_SIZE); // Padding.
        int actionSetListPointer = reader.readInt();

        // Verify action set position.
        requireReaderIndex(reader, actionSetListPointer, "Expected PTActionSet");

        // 1) Read action sets.
        this.actionSets.clear();
        for (int i = 0; i < actionSetCount; i++) {
            PTActionSet newActionSet = new PTActionSet(this);
            this.actionSets.add(newActionSet);
            newActionSet.load(reader);
        }

        // 2) Read action lists.
        for (int i = 0; i < this.actionSets.size(); i++)
            this.actionSets.get(i).readActionList(reader);

        // 3) Read action index lists.
        for (int i = 0; i < this.actionSets.size(); i++)
            this.actionSets.get(i).readActionKeyFrameLists(reader);

        // 4) Read action transform index lists.
        for (int i = 0; i < this.actionSets.size(); i++)
            this.actionSets.get(i).readActionTransformIndexLists(reader);

        // 5) Read rotation data.
        for (int i = 0; i < this.actionSets.size(); i++) {
            PTActionSet actionSet = this.actionSets.get(i);
            actionSet.readRotations(reader);
            actionSet.readRotationsWithTranslations(reader);
        }
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeStringBytes(IDENTIFIER_STRING);
        writer.writeInt(this.flags);
        writer.writeUnsignedShort(this.actionSets.size());
        writer.writeUnsignedShort((short) 0); // Padding.
        int actionSetListPointer = writer.writeNullPointer();

        // 1) Write action sets.
        writer.writeAddressTo(actionSetListPointer);
        for (int i = 0; i < this.actionSets.size(); i++)
            this.actionSets.get(i).save(writer);

        // 2) Write action sets.
        for (int i = 0; i < this.actionSets.size(); i++)
            this.actionSets.get(i).writeActionList(writer);

        // 3) Write action index lists.
        for (int i = 0; i < this.actionSets.size(); i++)
            this.actionSets.get(i).writeActionKeyFrameLists(writer);

        // 4) Write action transform index lists.
        for (int i = 0; i < this.actionSets.size(); i++)
            this.actionSets.get(i).writeActionTransformIndexLists(writer);

        // 5) Write rotation data.
        for (int i = 0; i < this.actionSets.size(); i++) {
            PTActionSet actionSet = this.actionSets.get(i);
            actionSet.writeRotations(writer);
            actionSet.writeRotationsWithTranslations(writer);
        }
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("Flags", NumberUtils.toHexString(this.flags));
        propertyList.add("Action Sets", this.actionSets.size());

        int actionCount = 0;
        int rotationCount = 0;
        int rotationWithTranslationCount = 0;
        for (int i = 0; i < this.actionSets.size(); i++) {
            PTActionSet actionSet = this.actionSets.get(i);
            actionCount += actionSet.getActions().size();
            rotationCount += actionSet.getRotations().size();
            rotationWithTranslationCount += actionSet.getRotationWithTranslations().size();
        }

        propertyList.add("Actions", actionCount);
        propertyList.add("Rotations", rotationCount);
        propertyList.add("Rotations (w/Translations)", rotationWithTranslationCount);
        return propertyList;
    }

    @Override
    public Image getCollectionViewIcon() {
        return ImageResource.ICON_MULTIMEDIA_32.getFxImage();
    }

    @Override
    public void handleWadEdit(WADFile parent) {
        PTStaticFile staticFile = getGameInstance().getMainArchive().getFileByName(FileUtils.stripExtension(getFileDisplayName()) + ".STAT");
        if (staticFile != null) {
            staticFile.handleWadEdit(parent);
        } else {
            FXUtils.makePopUp("Couldn't find static mesh for file '" + getFileDisplayName() + "'!", AlertType.ERROR);
        }
    }

    @Override
    public GameUIController<?> makeEditorUI() {
        return loadEditor(getGameInstance(), new PTModelFileUIController<>(getGameInstance(), "Animation/Action Set", getCollectionViewIcon()), this);
    }
}