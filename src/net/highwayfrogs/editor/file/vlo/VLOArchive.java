package net.highwayfrogs.editor.file.vlo;

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import lombok.Cleanup;
import lombok.Getter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameFile;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.ArrayReceiver;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIMain;
import net.highwayfrogs.editor.gui.editor.VLOController;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * VLOArchive - Image archive format created by VorgPC/Vorg2.
 * ABGR 8888 = 24 + 8.
 * Created by Kneesnap on 8/17/2018.
 */
@Getter
public class VLOArchive extends GameFile {
    private List<GameImage> images = new ArrayList<>();
    private List<ClutEntry> clutEntries = new ArrayList<>();
    private boolean psxMode;

    private static final String PC_SIGNATURE = "2GRP";
    private static final String PSX_SIGNATURE = "2GRV";
    private static final int SIGNATURE_LENGTH = 4;

    private static final int IMAGE_INFO_BYTES = 24;
    private static final int HEADER_SIZE = SIGNATURE_LENGTH + (2 * Constants.INTEGER_SIZE);
    public static final int TYPE_ID = 1;
    public static final int WAD_TYPE = 0;
    public static final Image ICON = loadIcon("image");

    @Override
    public void load(DataReader reader) {
        String readSignature = reader.readString(SIGNATURE_LENGTH);
        if (readSignature.equals(PSX_SIGNATURE)) {
            this.psxMode = true;
        } else {
            Utils.verify(readSignature.equals(PC_SIGNATURE), "Invalid VLO signature: %s.", readSignature);
        }

        int fileCount = reader.readInt();
        int textureOffset = reader.readInt();

        // Load clut data.
        if (isPsxMode()) { // GRV file has clut data.
            int clutCount = reader.readInt();
            int clutOffset = reader.readInt();

            reader.jumpTemp(clutOffset);
            for (int i = 0; i < clutCount; i++) {
                ClutEntry clut = new ClutEntry();
                clut.load(reader);
                clutEntries.add(clut);
            }

            reader.jumpReturn();
        }

        // Load image data.
        reader.jumpTemp(textureOffset);
        for (int i = 0; i < fileCount; i++) {
            GameImage image = new GameImage(this);
            image.load(reader);
            this.images.add(image);
        }
        reader.jumpReturn();
    }

    @Override
    public void save(DataWriter writer) {
        int imageCount = getImages().size();
        writer.writeStringBytes(isPsxMode() ? PSX_SIGNATURE : PC_SIGNATURE);
        writer.writeInt(imageCount);
        writer.writeInt(writer.getIndex() + ((isPsxMode() ? 3 : 1) * Constants.INTEGER_SIZE)); // Offset to the VLO table info.

        int clutFirstSetupIndex = -1;
        if (isPsxMode()) {
            writer.writeInt(this.clutEntries.size());
            clutFirstSetupIndex = writer.getIndex();
            writer.writeNull(Constants.INTEGER_SIZE); // This will be written later.
        }


        AtomicInteger imageBytesOffset = new AtomicInteger((IMAGE_INFO_BYTES * imageCount) + HEADER_SIZE);
        if (isPsxMode()) // Add room for clut setup data.
            imageBytesOffset.addAndGet(getClutEntries().stream().mapToInt(ClutEntry::getByteSize).sum());

        for (GameImage image : getImages())
            image.save(writer, imageBytesOffset);

        // Save CLUT data. CLUT should be saved after images are saved, because changed images will generate a new CLUT.
        if (isPsxMode()) {
            writer.jumpTemp(clutFirstSetupIndex);
            writer.writeInt(imageBytesOffset.get());
            writer.jumpReturn();

            for (ClutEntry entry : clutEntries)
                entry.save(writer, imageBytesOffset);
        }
    }

    /**
     * Export all images in this VLO archive.
     * @param directory         The directory to save them in.
     * @param trimEdges         Should image edges be trimmed?
     * @param allowTransparency Should transparency be allowed?
     */
    public void exportAllImages(File directory, boolean trimEdges, boolean allowTransparency, boolean allowFlip) {
        try {
            for (int i = 0; i < getImages().size(); i++) {
                File output = new File(directory, i + ".png");
                ImageIO.write(getImages().get(i).toBufferedImage(trimEdges, allowTransparency, allowFlip), "png", output);
                System.out.println("Exported image #" + i + ".");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public Image getIcon() {
        return ICON;
    }

    @Override
    public Node makeEditor() {
        return loadEditor(new VLOController(), "vlo", this);
    }

    @Override
    public void setupEditor(AnchorPane editorPane, Node node) {
        super.setupEditor(editorPane, node);

        AnchorPane pane = (AnchorPane) node;
        editorPane.setMaxHeight(pane.getMinHeight()); // Restricts the height of this editor, since there's nothing beyond the editing area.
    }

    @Override // Allows searching for remaps.
    @SneakyThrows
    public void exportAlternateFormat(FileEntry entry) {
        FileChooser remapChooser = new FileChooser();
        remapChooser.setTitle("Select the remap file.");
        remapChooser.getExtensionFilters().add(new ExtensionFilter("Frogger Remap Tables", "*.txt"));
        remapChooser.setInitialDirectory(GUIMain.getWorkingDirectory());

        File remapFile = remapChooser.showOpenDialog(GUIMain.MAIN_STAGE);
        if (remapFile == null)
            return;

        FileChooser exeChooser = new FileChooser();
        exeChooser.setTitle("Select the Frogger executable to search.");
        exeChooser.getExtensionFilters().add(new ExtensionFilter("Frogger PC Executable", "*.exe"));
        exeChooser.getExtensionFilters().add(new ExtensionFilter("Frogger PSX Executable", "*.01", "*.02", "*.03", "*.04", "*.05", "*.06", "*.07", "*.08", "*.09"));
        exeChooser.setInitialDirectory(GUIMain.getWorkingDirectory());
        exeChooser.setInitialFileName("frogger.exe");

        File selectedExe = exeChooser.showOpenDialog(GUIMain.MAIN_STAGE);
        if (selectedExe == null)
            return;

        ArrayReceiver byteQuery = new ArrayReceiver();
        DataWriter byteWriter = new DataWriter(byteQuery);

        @Cleanup Scanner scanner = new Scanner(remapFile);

        while (scanner.hasNextInt()) {
            int entryId = scanner.nextInt();
            Utils.verify(entryId >= 0 && getImages().size() > entryId, "%d is not a valid entry.", entryId);
            GameImage image = getImages().get(entryId);
            byteWriter.writeShort(image.getTextureId());
        }

        byteWriter.closeReceiver();
        byte[] bytesToFind = byteQuery.toArray();
        byte[] exeBytes = Files.readAllBytes(selectedExe.toPath());

        // Inefficient, but w/e, this is a dev tool.
        int matches = 0;
        for (int i = 0; i < exeBytes.length; i++) {

            boolean match = true;
            for (int j = 0; j < bytesToFind.length; j++) {
                if (bytesToFind[j] != exeBytes[i + j]) {
                    match = false;
                    break;
                }
            }

            if (match) {
                matches++;
                System.out.println("[Remap Match]: " + Integer.toHexString(i).toUpperCase());
            }
        }

        System.out.println("Search complete, found " + matches + " possible remaps.");

    }
}
