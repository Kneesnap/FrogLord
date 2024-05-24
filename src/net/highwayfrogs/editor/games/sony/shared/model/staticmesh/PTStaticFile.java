package net.highwayfrogs.editor.games.sony.shared.model.staticmesh;

import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.WADFile;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameFile.SCSharedGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.model.PTModel;
import net.highwayfrogs.editor.games.sony.shared.model.PTModelFileUIController;
import net.highwayfrogs.editor.games.sony.shared.model.actionset.PTActionSetFile;
import net.highwayfrogs.editor.games.sony.shared.model.meshview.PTModelMesh;
import net.highwayfrogs.editor.games.sony.shared.model.meshview.PTModelMeshController;
import net.highwayfrogs.editor.games.sony.shared.model.skeleton.PTSkeletonFile;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * Name of this is implied by all the MR_ prefixes getting replaced with PT_ in the Moon Warrior binary.
 * Created by Kneesnap on 5/15/2024.
 */
@Getter
public class PTStaticFile extends SCSharedGameFile {
    private short flags;
    private final List<PTStaticPart> parts = new ArrayList<>();
    private PTModelCollprim collprim;

    public static final int IDENTIFIER = 0x33305453;
    public static final String IDENTIFIER_STRING = Utils.toMagicString(IDENTIFIER); // 'ST03'

    // Flag 0 is set when the file is loaded by the game. Probably some kind of "is resolved" flag.
    // Flag 1 is unknown, but does not seem to be set.
    public static final int FLAG_HAS_SKELETON = Constants.BIT_FLAG_2;
    public static final int FLAG_HAS_ANIMATION = Constants.BIT_FLAG_3;
    public static final int FLAG_ENABLE_MIME = Constants.BIT_FLAG_4;
    public static final int FLAG_ENABLE_SKIN_WEIGHTING = Constants.BIT_FLAG_5;
    public static final int FLAG_ENABLE_BAKED_LIGHTING = Constants.BIT_FLAG_6;
    public static final int FLAG_MASK = 0b1111100;

    public PTStaticFile(SCGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        reader.verifyString(IDENTIFIER_STRING);
        this.flags = reader.readShort();
        warnAboutInvalidBitFlags(this.flags, FLAG_MASK);
        int partCount = reader.readUnsignedShortAsInt();
        int partDataStartAddress = reader.readInt();
        int collprimAddress = reader.readInt();

        // Read collprim.
        if (collprimAddress > 0) {
            // Verify reader position.
            reader.requireIndex(getLogger(), collprimAddress, "Expected main collprim data");

            this.collprim = new PTModelCollprim(getGameInstance());
            this.collprim.load(reader);
            this.collprim.readMatrix(reader);
            this.collprim.applyRadiusToLength();
        } else {
            // No collprim.
            this.collprim = null;
        }

        // Verify part position.
        reader.requireIndex(getLogger(), partDataStartAddress, "Expected PTStaticPart data");

        // Read part data.
        this.parts.clear();
        for (int i = 0; i < partCount; i++) {
            PTStaticPart newPart = new PTStaticPart(this);
            this.parts.add(newPart);
            newPart.load(reader);
        }

        // Read partCels.
        for (int i = 0; i < this.parts.size(); i++)
            this.parts.get(i).readPartCels(reader);

        // Read partCel primitive blocks.
        for (int i = 0; i < this.parts.size(); i++)
            this.parts.get(i).readPartCelPrimitiveBlocks(reader);

        // Read partCel collprims.
        for (int i = 0; i < this.parts.size(); i++)
            this.parts.get(i).readPartCelCollprims(reader);

        // Read partCel collprim matrices.
        for (int i = 0; i < this.parts.size(); i++)
            this.parts.get(i).readPartCelCollprimMatrices(reader);

        // Read partCel mime vectors.
        for (int i = 0; i < this.parts.size(); i++)
            this.parts.get(i).readPartCelMimeVectors(reader);

        // Read partCel vectors.
        for (int i = 0; i < this.parts.size(); i++)
            this.parts.get(i).readPartCelVectors(reader);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeStringBytes(IDENTIFIER_STRING);
        writer.writeShort(this.flags);
        writer.writeUnsignedInt(this.parts.size());
        int partDataStartAddress = writer.writeNullPointer();
        int collprimAddress = writer.writeNullPointer();

        // Write collprim.
        if (this.collprim != null) {
            writer.writeAddressTo(collprimAddress);
            this.collprim.save(writer);
            this.collprim.writeMatrix(writer);
        }

        // Write part data.
        writer.writeAddressTo(partDataStartAddress);
        for (int i = 0; i < this.parts.size(); i++)
            this.parts.get(i).save(writer);

        // Write partCels.
        for (int i = 0; i < this.parts.size(); i++)
            this.parts.get(i).writePartCels(writer);

        // Write partCel primitive blocks.
        for (int i = 0; i < this.parts.size(); i++)
            this.parts.get(i).writePartCelPrimitiveBlocks(writer);

        // Write partCel collprims.
        for (int i = 0; i < this.parts.size(); i++)
            this.parts.get(i).writePartCelCollprims(writer);

        // Write partCel collprim matrices.
        for (int i = 0; i < this.parts.size(); i++)
            this.parts.get(i).writePartCelCollprimMatrices(writer);

        // Write partCel mime vectors.
        for (int i = 0; i < this.parts.size(); i++)
            this.parts.get(i).writePartCelMimeVectors(writer);

        // Write partCel vectors.
        for (int i = 0; i < this.parts.size(); i++)
            this.parts.get(i).writePartCelVectors(writer);
    }

    @Override
    public Image getCollectionViewIcon() {
        return ImageResource.GEOMETRIC_SHAPES_32.getFxImage();
    }

    @Override
    public GameUIController<?> makeEditorUI() {
        return loadEditor(getGameInstance(), new PTModelFileUIController<>(getGameInstance(), "3D Model Mesh", getCollectionViewIcon()), this);
    }

    @Override
    public void handleWadEdit(WADFile parent) {
        PTSkeletonFile skeletonFile = getGameInstance().getMainArchive().getFileByName(Utils.stripExtension(getFileDisplayName()) + ".SKEL");
        PTActionSetFile animationFile = getGameInstance().getMainArchive().getFileByName(Utils.stripExtension(getFileDisplayName()) + ".ANIM");
        PTModel model = new PTModel(this, skeletonFile, animationFile);
        model.updateStaticModel();
        model.updateAnimation();
        model.updateMimeAndSkin();
        MeshViewController.setupMeshViewer(getGameInstance(), new PTModelMeshController(), new PTModelMesh(model));
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("Flags", Utils.toHexString(this.flags));
        propertyList.add("Parts", this.parts.size());
        return propertyList;
    }
}