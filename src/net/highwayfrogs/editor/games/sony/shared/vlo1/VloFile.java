package net.highwayfrogs.editor.games.sony.shared.vlo1;

import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.sony.SCGameFile.SCSharedGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.ui.file.VLOController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a .VLO file written using Vorg. (199? -> 1996)
 * Games used in: Defcon 5 (Probably more)
 * Reverse engineered from Defcon 5 (PSX, 80014514).
 * Created by Kneesnap on 12/13/2025.
 */
@Getter
public class VloFile extends SCSharedGameFile {
    private final List<VloImage> images = new ArrayList<>();

    public static final String SIGNATURE = "GROV"; // VORG backwards.
    private static final int SIGNATURE_LENGTH = SIGNATURE.length();

    public VloFile(SCGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        reader.verifyString(SIGNATURE);

        int imageCount = reader.readInt();

        this.images.clear();
        for (int i = 0; i < imageCount; i++) {
            VloImage newImage = new VloImage(this);
            this.images.add(newImage);
            newImage.load(reader);
        }

        // Read image data.
        for (int i = 0; i < this.images.size(); i++) {
            reader.alignRequireEmpty(Constants.INTEGER_SIZE);
            this.images.get(i).readImageData(reader);
        }
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeStringBytes(SIGNATURE);
        writer.writeInt(this.images.size());

        // Write images.
        for (int i = 0; i < this.images.size(); i++)
            this.images.get(i).save(writer);

        // Write image data.
        for (int i = 0; i < this.images.size(); i++) {
            writer.align(Constants.INTEGER_SIZE);
            this.images.get(i).writeImageData(writer);
        }
    }

    @Override
    public Image getCollectionViewIcon() {
        return ImageResource.PHOTO_ALBUM_32.getFxImage();
    }

    @Override
    public VLOController makeEditorUI() {
        return null; // loadEditor(getGameInstance(), "edit-file-vlo", new VLOController(getGameInstance()), this);
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        super.addToPropertyList(propertyList);
        propertyList.add("Images", getImages().size());
    }
}