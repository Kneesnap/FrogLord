package net.highwayfrogs.editor.file;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.config.TargetPlatform;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.MAPTheme;
import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.sound.VABHeaderFile;
import net.highwayfrogs.editor.file.sound.VBFile;
import net.highwayfrogs.editor.file.sound.VHFile;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings.ImageState;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.file.writer.ArrayReceiver;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIMain;
import net.highwayfrogs.editor.gui.SelectionMenu;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * MWAD File Format: Medieval WAD Archive.
 * This represents a loaded MWAD file.
 * Created by Kneesnap on 8/10/2018.
 */
@Getter
public class MWDFile extends GameObject {
    private MWIFile wadIndexTable;
    private List<GameFile> files = new ArrayList<>();
    private Map<GameFile, FileEntry> entryMap = new HashMap<>();
    @Setter private BiConsumer<FileEntry, GameFile> saveCallback;

    private transient Map<MAPTheme, VLOArchive> vloThemeCache = new HashMap<>();

    public static String CURRENT_FILE_NAME = null;
    private static final String MARKER = "DAWM";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEEE, d MMMM yyyy");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private static final ImageFilterSettings VLO_ICON_SETTING = new ImageFilterSettings(ImageState.EXPORT);

    public MWDFile(MWIFile table) {
        this.wadIndexTable = table;
    }

    @Override
    public void load(DataReader reader) {
        String marker = reader.readString(MARKER.length());
        Utils.verify(marker.equals(MARKER), "MWD Identifier %s was incorrectly read as %s!", MARKER, marker);

        VBFile lastVB = null; // VBs are indexed before VHs, but need to be loaded after VH. This allows us to do that.
        AtomicInteger soundId = new AtomicInteger(0);

        for (FileEntry entry : wadIndexTable.getEntries()) {
            if (entry.testFlag(FileEntry.FLAG_GROUP_ACCESS))
                continue; // This file is part of a WAD archive, and isn't a file entry in the MWD, so we can't load it here.

            reader.setIndex(entry.getArchiveOffset());

            // Read the file. Decompress if needed.
            byte[] fileBytes = reader.readBytes(entry.getArchiveSize());
            if (entry.isCompressed())
                fileBytes = PP20Unpacker.unpackData(fileBytes);

            CURRENT_FILE_NAME = entry.getDisplayName();
            GameFile file = loadFile(fileBytes, entry, lastVB);

            try {
                DataReader newReader = new DataReader(new ArraySource(fileBytes));
                if (file instanceof VHFile) {
                    ((VHFile) file).load(newReader, soundId);
                } else {
                    file.load(newReader);
                }
            } catch (Exception ex) {
                throw new RuntimeException("Failed to load " + entry.getDisplayName() + ", " + entry.getLoadedId(), ex);
            }

            entryMap.put(file, entry);
            files.add(file);
            lastVB = file instanceof VBFile ? (VBFile) file : null;
        }
    }

    /**
     * Create a replacement file. (Does not actually update MWD)
     * @param fileBytes The bytes to replace the file with.
     * @param oldFile   The file to replace.
     * @return replacementFile
     */
    public <T extends GameFile> T replaceFile(byte[] fileBytes, FileEntry entry, GameFile oldFile) {
        VBFile lastVB = (oldFile instanceof VHFile) ? ((VHFile) oldFile).getVB() : null;
        T newFile = this.loadFile(fileBytes, entry, lastVB);
        newFile.load(new DataReader(new ArraySource(fileBytes)));
        return newFile;
    }

    /**
     * Create a GameFile instance.
     * @param fileBytes The data to read
     * @param entry     The file entry being loaded.
     * @param lastVB    The lastVB value.
     * @return loadedFile
     */
    @SuppressWarnings("unchecked")
    public <T extends GameFile> T loadFile(byte[] fileBytes, FileEntry entry, VBFile lastVB) {
        // Turn the byte data into the appropriate game-file.
        GameFile file;

        if (entry.getTypeId() == VLOArchive.TYPE_ID || entry.getDisplayName().startsWith("LS_ALL")) { // For some reason, Level Select vlos are registered as maps. This loads them as their proper VLO.
            file = new VLOArchive();
        } else if (entry.getTypeId() == MAPFile.TYPE_ID) { // Disabled until fully supported.
            boolean isDemoJungle = (entry.getDisplayName().startsWith("JUN1") && GUIMain.EXE_CONFIG.isDemo() && GUIMain.EXE_CONFIG.getPlatform() == TargetPlatform.PSX);
            boolean isSkyLand = entry.getDisplayName().startsWith("SKY_LAND");
            boolean isIsland = entry.getDisplayName().startsWith("ISLAND.MAP");
            boolean isQB = entry.getDisplayName().startsWith("QB.MAP");

            if (isDemoJungle || isSkyLand || isIsland || isQB) { // These maps are entered as a map, even though it is not. It should be loaded as a DummyFile for now.
                file = new DummyFile(fileBytes.length);
            } else {
                file = new MAPFile(this);
            }
        } else if (entry.getTypeId() == WADFile.TYPE_ID) { // Disabled until fully supported.
            file = new WADFile(this);
        } else if (entry.getTypeId() == DemoFile.TYPE_ID) {
            file = new DemoFile();
        } else if (entry.getTypeId() == PALFile.TYPE_ID) {
            file = new PALFile();
        } else if (entry.getTypeId() == VHFile.TYPE_ID && !testSignature(fileBytes, 0, VABHeaderFile.SIGNATURE)) { // PSX support is disabled until it is complete.
            if (lastVB != null) {
                VHFile vhFile = new VHFile();
                vhFile.setVB(lastVB);
                file = vhFile;
            } else {
                file = new VBFile();
            }

        } else {
            file = new DummyFile(fileBytes.length);
        }

        return (T) file;
    }

    private static boolean testSignature(byte[] data, int startIndex, String test) {
        byte[] testBytes = test.getBytes();
        byte[] signatureBytes = new byte[testBytes.length];
        System.arraycopy(data, startIndex, signatureBytes, 0, signatureBytes.length);
        return Arrays.equals(testBytes, signatureBytes);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeBytes(MARKER.getBytes());
        writer.writeInt(0);

        Date date = Date.from(Calendar.getInstance().toInstant());
        writer.writeTerminatorString("\nCreation Date: " + DATE_FORMAT.format(date)
                + "\nCreation Time: " + TIME_FORMAT.format(date)
                + "\nThis MWD was changed using FrogLord.\n");

        int sectorOffset = 0;
        long mwdStart = System.currentTimeMillis();
        for (GameFile file : files) {
            FileEntry entry = entryMap.get(file);

            do { // Find the next unused sector, to write the next entry.
                entry.setSectorOffset(++sectorOffset);
            } while (writer.getIndex() > entry.getArchiveOffset());
            writer.jumpTo(entry.getArchiveOffset());

            long start = System.currentTimeMillis();
            CURRENT_FILE_NAME = entry.getDisplayName();
            System.out.print("Saving " + entry.getDisplayName() + " to MWD. (" + (files.indexOf(file) + 1) + "/" + files.size() + ") ");
            if (getSaveCallback() != null)
                getSaveCallback().accept(entry, file);

            ArrayReceiver receiver = new ArrayReceiver();
            file.save(new DataWriter(receiver));

            byte[] transfer = receiver.toArray();
            entry.setUnpackedSize(transfer.length);
            if (entry.isCompressed())
                transfer = PP20Packer.packData(transfer);

            entry.setPackedSize(transfer.length);

            writer.writeBytes(transfer);
            System.out.println("Time: " + ((System.currentTimeMillis() - start) / 1000) + "s.");
        }
        System.out.println("MWD Built. Total Time: " + ((System.currentTimeMillis() - mwdStart) / 1000) + "s.");

        // Fill the rest of the file with null bytes.
        GameFile lastFile = files.get(files.size() - 1);
        writer.writeNull(Constants.CD_SECTOR_SIZE - (entryMap.get(lastFile).getArchiveSize() % Constants.CD_SECTOR_SIZE));
    }

    /**
     * Get the VLO for a given map theme.
     * @param theme     The theme to get it for. Can be null, will prompt user then.
     * @param handler   The handler for when the VLO is determined.
     * @param allowNull Are null VLOs allowed?
     */
    public void promptVLOSelection(MAPTheme theme, Consumer<VLOArchive> handler, boolean allowNull) {
        List<VLOArchive> allVLOs = getFiles().stream()
                .filter(VLOArchive.class::isInstance)
                .map(VLOArchive.class::cast)
                .collect(Collectors.toList());

        if (allowNull)
            allVLOs.add(0, null);

        if (theme != null) {
            List<VLOArchive> movedVLOs = allVLOs.stream()
                    .filter(vlo -> getEntryMap().get(vlo).getDisplayName().startsWith(theme.getInternalName()))
                    .collect(Collectors.toList());
            allVLOs.removeAll(movedVLOs);
            allVLOs.addAll(0, movedVLOs);
        }

        VLOArchive cachedVLO = getVloThemeCache().get(theme); // Move cached vlo to the top.
        if (cachedVLO != null && allVLOs.remove(cachedVLO))
            allVLOs.add(0, cachedVLO);

        SelectionMenu.promptSelection("Select " + (theme != null ? theme.name() + "'s" : "a") + " VLO.", vlo -> {
                    if (vlo != null && theme != null)
                        getVloThemeCache().put(theme, vlo);
                    handler.accept(vlo);
                }, allVLOs,
                vlo -> vlo != null ? getEntryMap().get(vlo).getDisplayName() : "No Textures",
                vlo -> vlo != null ? new ImageView(SwingFXUtils.toFXImage(Utils.resizeImage(vlo.getImages().get(0).toBufferedImage(VLO_ICON_SETTING), 25, 25), null)) : null);
    }
}
