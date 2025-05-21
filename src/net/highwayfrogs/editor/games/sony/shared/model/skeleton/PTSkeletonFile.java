package net.highwayfrogs.editor.games.sony.shared.model.skeleton;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.games.sony.SCGameFile.SCSharedGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.model.PTModelFileUIController;
import net.highwayfrogs.editor.games.sony.shared.model.staticmesh.PTStaticFile;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a model's animation skeleton.
 * Created by Kneesnap on 5/15/2024.
 */
@Getter
public class PTSkeletonFile extends SCSharedGameFile implements IPropertyListCreator {
    private short flags; // Seems to always be empty.
    private final List<PTSkeletonBone> bones = new ArrayList<>();

    public static final int IDENTIFIER = 0x33304B53;
    public static final String IDENTIFIER_STRING = Utils.toIdentifierString(IDENTIFIER); // 'SK03'

    public PTSkeletonFile(SCGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        reader.verifyString(IDENTIFIER_STRING);
        this.flags = reader.readShort();
        warnAboutInvalidBitFlags(this.flags, 0);
        int boneCount = reader.readUnsignedShortAsInt();
        int boneDataStartAddress = reader.readInt();

        // Read bones.
        this.bones.clear();
        requireReaderIndex(reader, boneDataStartAddress, "Expected PTSkeletonBone");
        for (int i = 0; i < boneCount; i++) {
            PTSkeletonBone newBone = new PTSkeletonBone(this);
            this.bones.add(newBone);
            newBone.load(reader);
        }
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeStringBytes(IDENTIFIER_STRING);
        writer.writeShort(this.flags);
        writer.writeUnsignedShort(this.bones.size());
        int boneDataStartAddress = writer.writeNullPointer();

        // Write bones.
        writer.writeAddressTo(boneDataStartAddress);
        for (int i = 0; i < this.bones.size(); i++)
            this.bones.get(i).save(writer);
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("Flags", NumberUtils.toHexString(this.flags));
        propertyList.add("Bones", this.bones.size());
        return propertyList;
    }

    @Override
    public Image getCollectionViewIcon() {
        return ImageResource.SKELETON_JOINTS_32.getFxImage();
    }

    @Override
    public void performDefaultUIAction() {
        PTStaticFile staticFile = getGameInstance().getMainArchive().getFileByName(FileUtils.stripExtension(getFileDisplayName()) + ".STAT");
        if (staticFile != null) {
            staticFile.performDefaultUIAction();
        } else {
            FXUtils.makePopUp("Couldn't find static mesh for file '" + getFileDisplayName() + "'!", AlertType.ERROR);
        }
    }

    @Override
    public GameUIController<?> makeEditorUI() {
        return loadEditor(getGameInstance(), new PTModelFileUIController<>(getGameInstance(), "Model Skeleton Rig", getCollectionViewIcon()), this);
    }
}