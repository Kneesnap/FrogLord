package net.highwayfrogs.editor.file;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.WADFile.WADEntry;
import net.highwayfrogs.editor.file.map.MAPTheme;
import net.highwayfrogs.editor.file.mof.MOFHolder;
import net.highwayfrogs.editor.file.packers.PP20Packer;
import net.highwayfrogs.editor.file.packers.PP20Unpacker;
import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings.ImageState;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.file.writer.ArrayReceiver;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.gui.SelectionMenu;
import net.highwayfrogs.editor.utils.FroggerVersionComparison;
import net.highwayfrogs.editor.utils.Utils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * MWAD File Format: MediEvil WAD Archive.
 * This represents a loaded MWAD file.
 * Created by Kneesnap on 8/10/2018.
 */
@Getter
public class MWDFile extends SCSharedGameData {
    private final List<SCGameFile<?>> files = new ArrayList<>();
    @Setter private BiConsumer<FileEntry, SCGameFile<?>> saveCallback;

    private final transient Map<MAPTheme, VLOArchive> vloThemeCache = new HashMap<>();

    public static String CURRENT_FILE_NAME = null;
    private static final String MARKER = "DAWM";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEEE, d MMMM yyyy");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    public static final ImageFilterSettings VLO_ICON_SETTING = new ImageFilterSettings(ImageState.EXPORT);

    public MWDFile(SCGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        reader.verifyString(MARKER);

        for (FileEntry entry : getGameInstance().getArchiveIndex().getEntries()) {
            if (entry.testFlag(FileEntry.FLAG_GROUP_ACCESS))
                continue; // This file is part of a WAD archive, and isn't a file entry in the MWD, so we can't load it here.

            reader.setIndex(entry.getArchiveOffset());

            // Read the file. Decompress if it is PP20 compression.
            byte[] fileBytes = reader.readBytes(entry.getArchiveSize());
            if (entry.isCompressed()) {
                if (PP20Unpacker.isCompressed(fileBytes)) {
                    fileBytes = PP20Unpacker.unpackData(fileBytes);
                } else {
                    System.out.println("ERROR: File is compressed, but not using PowerPacker (PP20) compression.");
                }
            }

            // Calculate the SHA1 hash.
            if (FroggerVersionComparison.isEnabled() && entry.getSha1Hash() == null)
                entry.setSha1Hash(Utils.calculateSHA1Hash(fileBytes));

            SCGameFile<?> file = loadFile(fileBytes, entry);

            try {
                file.load(new DataReader(new ArraySource(fileBytes)));
            } catch (Exception ex) {
                Utils.handleError(getLogger(), ex, false, "Failed to load %s (%d)", entry.getDisplayName(), entry.getResourceId());
            }

            this.files.add(file);
        }
    }

    /**
     * Create a replacement file. (Does not actually update MWD)
     * @param fileBytes The bytes to replace the file with.
     * @param oldFile   The file to replace.
     * @return replacementFile
     */
    @SuppressWarnings("unchecked")
    public <T extends SCGameFile<?>> T replaceFile(byte[] fileBytes, FileEntry entry, SCGameFile<?> oldFile, boolean isInsideWadFile) {
        T newFile;

        if (oldFile instanceof MOFHolder) {
            MOFHolder oldHolder = (MOFHolder) oldFile;
            newFile = (T) new MOFHolder(getGameInstance(), oldHolder.getTheme(), oldHolder.getCompleteMOF());
        } else {
            newFile = this.loadFile(fileBytes, entry);
        }

        if (oldFile != null) {
            getGameInstance().getFileObjectsByFileEntries().remove(entry, oldFile);
            getGameInstance().getFileEntriesByFileObjects().remove(oldFile, entry);
        }

        getGameInstance().getFileObjectsByFileEntries().put(entry, newFile);
        getGameInstance().getFileEntriesByFileObjects().put(newFile, entry);
        CURRENT_FILE_NAME = entry.getDisplayName();

        // Replace file.
        if (!isInsideWadFile) {
            int fileIndex = this.files.indexOf(oldFile);
            if (fileIndex >= 0)
                this.files.set(fileIndex, newFile);
        }

        // Load new file data.
        try {
            newFile.load(new DataReader(new ArraySource(fileBytes)));
        } catch (Exception ex) {
            Utils.handleError(getLogger(), ex, true, "Failed to load %s (%d)", entry.getDisplayName(), entry.getResourceId());
        }

        return newFile;
    }

    /**
     * Create a GameFile instance.
     * @param fileBytes The data to read
     * @param entry     The file entry being loaded.
     * @return loadedFile
     */
    @SuppressWarnings("unchecked")
    public <T extends SCGameFile<?>> T loadFile(byte[] fileBytes, FileEntry entry) {
        // Turn the byte data into the appropriate game-file.
        SCGameFile<?> file = getGameInstance().createFile(entry, fileBytes);
        if (file == null)
            file = new DummyFile(getGameInstance(), fileBytes.length);

        getGameInstance().getFileObjectsByFileEntries().put(entry, file);
        getGameInstance().getFileEntriesByFileObjects().put(file, entry);
        CURRENT_FILE_NAME = entry.getDisplayName();
        return (T) file;
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeBytes(MARKER.getBytes());
        writer.writeInt(0);

        Date date = Date.from(Calendar.getInstance().toInstant());
        writer.writeTerminatorString("\nCreation Date: " + DATE_FORMAT.format(date)
                + "\nCreation Time: " + TIME_FORMAT.format(date)
                + "\nThis MWD was built using FrogLord.\n");

        int sectorOffset = 0;
        long mwdStart = System.currentTimeMillis();
        for (SCGameFile<?> file : this.files) {
            FileEntry entry = file.getIndexEntry();

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
        SCGameFile<?> lastFile = this.files.get(this.files.size() - 1);
        writer.writeNull(Constants.CD_SECTOR_SIZE - (lastFile.getIndexEntry().getArchiveSize() % Constants.CD_SECTOR_SIZE));
    }

    /**
     * Grabs the first VLO we can find.
     */
    public VLOArchive findFirstVLO() {
        List<VLOArchive> allVLOs = getFiles().stream()
                .filter(VLOArchive.class::isInstance)
                .map(VLOArchive.class::cast)
                .collect(Collectors.toList());

        return allVLOs.size() > 0 ? allVLOs.get(0) : null;
    }

    /**
     * Get the VLO for a given map theme.
     * @param theme     The theme to get it for. Can be null, will prompt user then.
     * @param handler   The handler for when the VLO is determined.
     * @param allowNull Are null VLOs allowed?
     */
    public void promptVLOSelection(MAPTheme theme, Consumer<VLOArchive> handler, boolean allowNull) {
        List<VLOArchive> allVLOs = getAllFiles(VLOArchive.class);

        if (allowNull)
            allVLOs.add(0, null);

        if (theme != null) {
            List<VLOArchive> movedVLOs = allVLOs.stream()
                    .filter(vlo -> {
                        FileEntry entry = vlo != null ? vlo.getIndexEntry() : null;
                        return entry != null && entry.getDisplayName().startsWith(theme.getInternalName());
                    })
                    .collect(Collectors.toList());
            allVLOs.removeAll(movedVLOs);
            allVLOs.addAll(0, movedVLOs);
        }

        VLOArchive cachedVLO = this.vloThemeCache.get(theme); // Move cached vlo to the top.
        if (cachedVLO != null && allVLOs.remove(cachedVLO))
            allVLOs.add(0, cachedVLO);

        SelectionMenu.promptSelection(getGameInstance(), "Select " + (theme != null ? theme.name() + "'s" : "a") + " VLO.", vlo -> {
                    if (vlo != null && theme != null)
                        this.vloThemeCache.put(theme, vlo);
                    handler.accept(vlo);
                }, allVLOs,
                vlo -> vlo != null ? vlo.getFileDisplayName() : "No Textures",
                vlo -> vlo.getImages().get(0).toFXImage(VLO_ICON_SETTING));
    }

    /**
     * Get each file of a given class type, including those found in wads.
     * @param fileClass The type to iterate over.
     */
    public <T extends SCGameFile<?>> List<T> getAllFiles(Class<T> fileClass) {
        List<T> results = new ArrayList<>();

        for (SCGameFile<?> file : getFiles()) {
            if (fileClass.isInstance(file))
                results.add(fileClass.cast(file));

            if (file instanceof WADFile) {
                WADFile wadFile = (WADFile) file;
                for (WADEntry entry : wadFile.getFiles()) {
                    SCGameFile<?> testFile = entry.getFile();
                    if (fileClass.isInstance(testFile))
                        results.add(fileClass.cast(testFile));
                }
            }
        }

        return results;
    }

    /**
     * Iterate over each file of a given type.
     * @param fileClass The type to iterate over.
     * @param handler   The behavior to apply.
     */
    public <T extends SCGameFile<?>> void forEachFile(Class<T> fileClass, Consumer<T> handler) {
        for (SCGameFile<?> file : getFiles()) {
            if (fileClass.isInstance(file))
                handler.accept(fileClass.cast(file));

            if (file instanceof WADFile) {
                WADFile wadFile = (WADFile) file;
                for (WADEntry entry : wadFile.getFiles()) {
                    SCGameFile<?> testFile = entry.getFile();
                    if (fileClass.isInstance(testFile))
                        handler.accept(fileClass.cast(testFile));
                }
            }
        }
    }

    /**
     * Iterate over each file of a given type.
     * @param fileClass The type to iterate over.
     * @param handler   The behavior to apply.
     */
    public <T extends SCGameFile<?>, R> R resolveForEachFile(Class<T> fileClass, Function<T, R> handler) {
        for (SCGameFile<?> file : getFiles()) {
            if (fileClass.isInstance(file)) {
                R result = handler.apply(fileClass.cast(file));
                if (result != null)
                    return result; // If there's a result.
            }

            if (file instanceof WADFile) {
                WADFile wadFile = (WADFile) file;
                for (WADEntry wadEntry : wadFile.getFiles()) {
                    if (!fileClass.isInstance(wadEntry.getFile()))
                        continue;
                    R result = handler.apply(fileClass.cast(wadEntry.getFile()));
                    if (result != null)
                        return result;
                }
            }
        }

        return null; // Nothing found.
    }

    /**
     * Gets an image by the given texture ID.
     * @param textureId The texture ID to get.
     * @return gameImage
     */
    public GameImage getImageByTextureId(int textureId) {
        if (textureId < 0)
            textureId = 0; // This is a hack to allow for loading maps without remaps on build 20. In new FrogLord, this should be null / return blank texture.

        for (VLOArchive vlo : getAllFiles(VLOArchive.class))
            for (GameImage testImage : vlo.getImages())
                if (testImage.getTextureId() == textureId)
                    return testImage;

        return null;
    }

    /**
     * Gets an image by the given texture ID.
     * @param textureId The texture ID to get.
     * @return gameImage
     */
    public List<GameImage> getImagesByTextureId(int textureId) {
        List<GameImage> results = new ArrayList<>();

        for (VLOArchive vlo : getAllFiles(VLOArchive.class))
            for (GameImage testImage : vlo.getImages())
                if (testImage.getTextureId() == textureId)
                    results.add(testImage);

        return results;
    }
}