package net.highwayfrogs.editor.file;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.PLTFile;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.WADFile.WADEntry;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.MAPTheme;
import net.highwayfrogs.editor.file.map.SkyLand;
import net.highwayfrogs.editor.file.mof.MOFHolder;
import net.highwayfrogs.editor.file.packers.PP20Packer;
import net.highwayfrogs.editor.file.packers.PP20Unpacker;
import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.sound.AbstractVBFile;
import net.highwayfrogs.editor.file.sound.VHFile;
import net.highwayfrogs.editor.file.sound.prototype.PrototypeVBFile;
import net.highwayfrogs.editor.file.sound.psx.PSXVBFile;
import net.highwayfrogs.editor.file.sound.psx.PSXVHFile;
import net.highwayfrogs.editor.file.sound.retail.RetailPCVBFile;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings.ImageState;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.file.writer.ArrayReceiver;
import net.highwayfrogs.editor.file.writer.DataWriter;
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
public class MWDFile extends GameObject {
    private final MWIFile wadIndexTable;
    private final List<GameFile> files = new ArrayList<>();
    private final Map<GameFile, FileEntry> entryMap = new HashMap<>();
    private final Map<FileEntry, GameFile> entryFileMap = new HashMap<>();
    @Setter private BiConsumer<FileEntry, GameFile> saveCallback;

    private final transient Map<MAPTheme, VLOArchive> vloThemeCache = new HashMap<>();

    public static String CURRENT_FILE_NAME = null;
    private static final String MARKER = "DAWM";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEEE, d MMMM yyyy");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    public static final ImageFilterSettings VLO_ICON_SETTING = new ImageFilterSettings(ImageState.EXPORT);

    public MWDFile(MWIFile table) {
        this.wadIndexTable = table;
    }

    @Override
    public void load(DataReader reader) {
        reader.verifyString(MARKER);

        AbstractVBFile<?> lastVB = null; // VBs are indexed before VHs, but need to be loaded after VH. This allows us to do that.
        int index = 1;

        for (FileEntry entry : wadIndexTable.getEntries()) {
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

            GameFile file = loadFile(fileBytes, entry, lastVB);

            try {
                file.load(new DataReader(new ArraySource(fileBytes)));
            } catch (Exception ex) {
                System.out.println("Failed to load " + entry.getDisplayName() + " (" + entry.getResourceId() + ")");
                ex.printStackTrace();

                //throw new RuntimeException("Failed to load " + entry.getDisplayName() + ", " + entry.getResourceId(), ex);
            }

            this.files.add(file);
            lastVB = file instanceof AbstractVBFile ? (AbstractVBFile<?>) file : null;
            if (file instanceof VHFile)
                lastVB = ((VHFile) file).getVB();

            index++;
        }
    }

    /**
     * Create a replacement file. (Does not actually update MWD)
     * @param fileBytes The bytes to replace the file with.
     * @param oldFile   The file to replace.
     * @return replacementFile
     */
    @SuppressWarnings("unchecked")
    public <T extends GameFile> T replaceFile(byte[] fileBytes, FileEntry entry, GameFile oldFile) {
        T newFile;

        if (oldFile instanceof MOFHolder) {
            MOFHolder oldHolder = (MOFHolder) oldFile;
            newFile = (T) new MOFHolder(oldHolder.getTheme(), oldHolder.getCompleteMOF());
        } else {
            AbstractVBFile<?> lastVB = (oldFile instanceof AbstractVBFile) ? ((AbstractVBFile<?>) oldFile) : null;
            if (oldFile instanceof VHFile)
                lastVB = ((VHFile) oldFile).getVB();

            newFile = this.loadFile(fileBytes, entry, lastVB);
        }

        entryMap.put(newFile, entry);
        entryFileMap.put(entry, newFile);
        CURRENT_FILE_NAME = entry.getDisplayName();

        newFile.load(new DataReader(new ArraySource(fileBytes)));
        return newFile;
    }

    /**
     * Create a GameFile instance.
     * @param fileBytes The data to read
     * @param entry     The file entry being loaded.
     * @param lastVB    The last VB file loaded/seen.
     * @return loadedFile
     */
    @SuppressWarnings("unchecked")
    public <T extends GameFile> T loadFile(byte[] fileBytes, FileEntry entry, AbstractVBFile<?> lastVB) {
        // Turn the byte data into the appropriate game-file.
        GameFile file;

        // .DAT is 0. (Remember, 0 is "standard", or .MAP, I believe it's game specific.)
        // VB VH is 0.
        // .BPP is also zero.

        // WADs seem to be -1.
        // VLOs seem to be 1, sometimes zero.
        // XMR seems to be 3.
        // MAP is four4
        // TEX (REMAP???) is 5
        // TIM seems to be 8.
        // PLT (Palette) is 9.

        //TODO:
        // - Check out TEX, MAP.
        // - Automatically determine textures, see if we can hook up things like a way to link things to vlos.
        // - Automatically export assets.

        if (entry.getSpoofedTypeId() == VLOArchive.TYPE_ID) {
            file = new VLOArchive();
        } else if (getConfig().isFrogger() && entry.getTypeId() == MAPFile.TYPE_ID) {
            if (entry.getDisplayName().startsWith(Constants.SKY_LAND_PREFIX)) { // These maps are entered as a map, even though it is not. It should be loaded as a DummyFile for now.
                file = new SkyLand();
            } else {
                file = new MAPFile();
            }
        } else if (entry.getSpoofedTypeId() == WADFile.TYPE_ID) {
            file = new WADFile();
        } else if (entry.getSpoofedTypeId() == DemoFile.TYPE_ID && getConfig().isFrogger()) {
            file = new DemoFile();
        } else if (entry.getTypeId() == PALFile.TYPE_ID && getConfig().isFrogger()) {
            file = new PALFile();
        } else if (entry.getSpoofedTypeId() == PLTFile.FILE_TYPE && getConfig().isBeastWars()) {
            file = new PLTFile();
        } else if (entry.getTypeId() == VHFile.TYPE_ID) { // PSX support is disabled until it is complete.
            if (getConfig().isPSX()) {
                if (lastVB != null) {
                    file = new PSXVHFile();
                    ((PSXVHFile) file).setVB((PSXVBFile) lastVB);
                } else {
                    file = new PSXVBFile();
                }
            } else if (lastVB != null) {
                VHFile vhFile = new VHFile();
                vhFile.setVB(lastVB);
                file = vhFile;
            } else if (getConfig().isAtLeastRetailWindows()) {
                file = new RetailPCVBFile();
            } else {
                file = new PrototypeVBFile();
            }
        } else {
            file = new DummyFile(fileBytes.length);
        }

        entryMap.put(file, entry);
        entryFileMap.put(entry, file);
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
        List<VLOArchive> allVLOs = getFiles().stream()
                .filter(VLOArchive.class::isInstance)
                .map(VLOArchive.class::cast)
                .collect(Collectors.toList());

        if (allowNull)
            allVLOs.add(0, null);

        if (theme != null) {
            List<VLOArchive> movedVLOs = allVLOs.stream()
                    .filter(vlo -> {
                        FileEntry entry = getEntryMap().get(vlo);
                        return entry != null && entry.getDisplayName().startsWith(theme.getInternalName());
                    })
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
                vlo -> vlo.getImages().get(0).toFXImage(VLO_ICON_SETTING));
    }

    /**
     * Gets this MWD's SkyLand file.
     * @return skyLand
     */
    public SkyLand getSkyLand() {
        for (GameFile file : getFiles())
            if (file instanceof SkyLand)
                return (SkyLand) file;
        throw new RuntimeException("Sky Land is not present.");
    }

    /**
     * Get each file of a given class type, including those found in wads.
     * @param fileClass The type to iterate over.
     */
    public <T extends GameFile> List<T> getAllFiles(Class<T> fileClass) {
        List<T> results = new ArrayList<>();

        for (GameFile file : getFiles()) {
            if (fileClass.isInstance(file))
                results.add(fileClass.cast(file));

            if (file instanceof WADFile) {
                WADFile wadFile = (WADFile) file;
                for (WADEntry entry : wadFile.getFiles()) {
                    GameFile testFile = entry.getFile();
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
    public <T extends GameFile> void forEachFile(Class<T> fileClass, Consumer<T> handler) {
        for (GameFile file : getFiles()) {
            if (fileClass.isInstance(file))
                handler.accept(fileClass.cast(file));

            if (file instanceof WADFile) {
                WADFile wadFile = (WADFile) file;
                for (WADEntry entry : wadFile.getFiles()) {
                    GameFile testFile = entry.getFile();
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
    public <T extends GameFile, R> R resolveForEachFile(Class<T> fileClass, Function<T, R> handler) {
        for (GameFile file : getFiles()) {
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

    /**
     * Gets the FPS this game will run at.
     * @return fps
     */
    public int getFPS() {
        return getConfig().isPSX() ? 30 : 25;
    }
}