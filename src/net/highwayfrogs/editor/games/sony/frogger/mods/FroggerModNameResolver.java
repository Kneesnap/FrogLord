package net.highwayfrogs.editor.games.sony.frogger.mods;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.data.FroggerLevelSelectWorldID;
import net.highwayfrogs.editor.games.sony.frogger.data.demo.FroggerDemoFile;
import net.highwayfrogs.editor.games.sony.frogger.data.map.FroggerMapBook;
import net.highwayfrogs.editor.games.sony.frogger.data.theme.FroggerThemeBook;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapLevelID;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapTheme;
import net.highwayfrogs.editor.games.sony.frogger.map.data.form.FroggerFormEntry;
import net.highwayfrogs.editor.games.sony.frogger.map.data.form.FroggerFormEntry.FroggerFormLibFlag;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;
import net.highwayfrogs.editor.games.sony.shared.mof2.MRModel;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile.WADEntry;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloFile;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloImage;
import net.highwayfrogs.editor.utils.FileUtils;

import java.util.*;

/**
 * Resolves the names a Frogger mod package uses (banks, images, levels, models, demos, remaps, themes, worlds) to game objects and back.
 * Lookups named find... return null when nothing matches; lookups named resolve... throw an IllegalArgumentException naming the manifest key.
 * Models are identified by the MWI display name of the slot's own entry (identity rule 4); slots that carry the placeholder id
 * ("NULL", left behind by SCUtils.copyWadEntry) are only identified by their bytes, through findModelSlotsByBytes and modelNameOf.
 * Created by jimbolina on 11/6/2026.
 */
@Getter
public class FroggerModNameResolver {
    private final FroggerGameInstance gameInstance;

    public static final String BANK_EXTENSION = ".VLO";
    public static final String MAP_EXTENSION = ".MAP";
    public static final String LOW_POLY_MAP_SUFFIX = "_WIN95";

    public FroggerModNameResolver(@NonNull FroggerGameInstance gameInstance) {
        this.gameInstance = gameInstance;
    }

    /**
     * Finds the texture bank (VLO) with the given MWI display name, written without the .VLO extension.
     * @param bankName the bank name, e.g. ORG_VRAM
     * @return the bank, or null when no VLO has that name
     */
    public VloFile findBank(String bankName) {
        if (bankName == null || bankName.isEmpty() || bankName.toUpperCase(Locale.ROOT).endsWith(BANK_EXTENSION))
            return null;

        MWIResourceEntry entry = this.gameInstance.getResourceEntryByName(bankName + BANK_EXTENSION);
        SCGameFile<?> file = entry != null ? this.gameInstance.getGameFile(entry) : null;
        return file instanceof VloFile ? (VloFile) file : null;
    }

    /**
     * Resolves a texture bank by name, reporting failure against the [Images] key of that bank.
     * @param bankName the bank name, e.g. ORG_VRAM
     * @return the bank
     */
    public VloFile resolveBank(String bankName) {
        return resolveBank(imagesKey(bankName, null), bankName);
    }

    /**
     * Resolves a texture bank by name.
     * @param key the manifest key being resolved, used in the error message
     * @param bankName the bank name, e.g. ORG_VRAM
     * @return the bank
     */
    public VloFile resolveBank(String key, String bankName) {
        VloFile bank = findBank(bankName);
        if (bank == null)
            throw fail(key, "no texture bank named '%s' (the MWI display name without %s)", bankName, BANK_EXTENSION);
        return bank;
    }

    /**
     * Gets the bank name of a VLO: its MWI display name without the extension.
     * @param bank the bank to name
     * @return bankName, or null when the VLO has no MWI entry
     */
    public String bankNameOf(VloFile bank) {
        MWIResourceEntry entry = bank != null ? bank.getIndexEntry() : null;
        return entry != null ? FileUtils.stripExtension(entry.getDisplayName()) : null;
    }

    /**
     * Finds an image by name inside one bank (custom name first, then the build's image list).
     * @param bank the bank to search
     * @param name the image name
     * @return the image, or null
     */
    public VloImage findImage(VloFile bank, String name) {
        return bank != null ? bank.getImageByName(name) : null;
    }

    /**
     * Finds an image by bank name and image name.
     * @param bankName the bank name, e.g. ORG_VRAM
     * @param name the image name
     * @return the image, or null
     */
    public VloImage findImage(String bankName, String name) {
        return findImage(findBank(bankName), name);
    }

    /**
     * Resolves an image by bank and name, reporting failure against [Images] [[bank]] [[[name]]].
     * @param bankName the bank name, e.g. ORG_VRAM
     * @param name the image name
     * @return the image
     */
    public VloImage resolveImage(String bankName, String name) {
        return resolveImage(imagesKey(bankName, name), bankName, name);
    }

    /**
     * Resolves an image by bank and name.
     * @param key the manifest key being resolved, used in the error message
     * @param bankName the bank name, e.g. ORG_VRAM
     * @param name the image name
     * @return the image
     */
    public VloImage resolveImage(String key, String bankName, String name) {
        VloImage image = findImage(resolveBank(key, bankName), name);
        if (image == null)
            throw fail(key, "no image named '%s' in bank %s", name, bankName);
        return image;
    }

    /**
     * Finds the texture id of an image name: the build's image list first, then a FrogLord custom name in any bank.
     * @param name the image name
     * @return textureId, or null when the name is unknown
     */
    public Short findTextureId(String name) {
        if (name == null || name.isEmpty())
            return null;

        Short textureId = this.gameInstance.getTextureIdByOriginalName(name);
        if (textureId != null)
            return textureId;

        for (VloFile bank : this.gameInstance.getMainArchive().getAllFiles(VloFile.class)) {
            VloImage image = bank.getImageByName(name);
            if (image != null)
                return image.getTextureId();
        }

        return null;
    }

    /**
     * Resolves the texture id of an image name.
     * @param key the manifest key being resolved, used in the error message
     * @param name the image name
     * @return textureId
     */
    public short resolveTextureId(String key, String name) {
        Short textureId = findTextureId(name);
        if (textureId == null)
            throw fail(key, "no image named '%s' in the image list of %s or as a custom name in any bank", name, getBuildName());
        return textureId;
    }

    /**
     * Gets the name of an image: its FrogLord custom name, else its image-list name.
     * @param image the image to name
     * @return imageName, or null when the image is nameless
     */
    public String imageNameOf(VloImage image) {
        return image != null ? image.getName() : null;
    }

    /**
     * Gets the name of a texture id: its image-list name, else the custom name of an image holding that id in any bank.
     * @param textureId the texture id to name
     * @return imageName, or null when the id is nameless
     */
    public String imageNameOfTextureId(int textureId) {
        if (textureId < 0 || textureId > Short.MAX_VALUE)
            return null;

        String name = this.gameInstance.getVersionConfig().getImageList().getImageNameFromID((short) textureId);
        if (name != null && !name.trim().isEmpty())
            return name;

        List<VloImage> images = this.gameInstance.getMainArchive().getImagesByTextureId(textureId);
        if (images != null)
            for (VloImage image : images)
                if (image.getName() != null)
                    return image.getName();

        return null;
    }

    /**
     * Finds the RAM pointer (bmp_pointers entry) the level select uses to show an image.
     * Throws when the name exists but the texture is not referenced by name, because the level select cannot point at it.
     * @param imageName the image name
     * @return the pointer, or null when the name is unknown
     */
    public Long findPicturePointer(String imageName) {
        return findPicturePointer(levelSelectKey(imageName), imageName);
    }

    /**
     * Finds the RAM pointer (bmp_pointers entry) the level select uses to show an image.
     * @param key the manifest key being resolved, used in the error message
     * @param imageName the image name
     * @return the pointer, or null when the name is unknown
     */
    public Long findPicturePointer(String key, String imageName) {
        Short textureId = findTextureId(imageName);
        if (textureId == null)
            return null;

        List<Long> pointers = this.gameInstance.getBmpTexturePointers();
        Long pointer = textureId >= 0 && textureId < pointers.size() ? pointers.get(textureId) : null;
        if (pointer == null || pointer == 0 || !this.gameInstance.isTextureReferencedByName(textureId))
            throw fail(key, "image '%s' (texture id %d) has no bmp_pointers entry (FLAG_REFERENCED_BY_NAME), so the level select cannot show it; pick an image that has one", imageName, textureId);

        return pointer;
    }

    /**
     * Resolves the level-select RAM pointer of an image name.
     * @param key the manifest key being resolved, used in the error message
     * @param imageName the image name
     * @return pointer
     */
    public long resolvePicturePointer(String key, String imageName) {
        Long pointer = findPicturePointer(key, imageName);
        if (pointer == null)
            throw fail(key, "no image named '%s' in the image list of %s or as a custom name in any bank", imageName, getBuildName());
        return pointer;
    }

    /**
     * Gets the image name a level-select RAM pointer shows.
     * @param pointer the RAM pointer
     * @return imageName, or null when the pointer is not a bmp_pointers entry or its texture is nameless
     */
    public String imageNameForPointer(long pointer) {
        if (this.gameInstance.getBmpTexturePointers().isEmpty())
            return null;

        int textureId = this.gameInstance.getTextureIdFromPointer(pointer);
        if (textureId < 0)
            return null;

        String name = imageNameOf(this.gameInstance.getImageFromPointer(pointer));
        return name != null ? name : imageNameOfTextureId(textureId);
    }

    /**
     * Finds a level by its file code (CAV1, ORG1, ...). Only exact codes match.
     * @param code the level code
     * @return level, or null
     */
    public FroggerMapLevelID findLevel(String code) {
        if (code == null)
            return null;

        for (FroggerMapLevelID level : FroggerMapLevelID.values())
            if (level.getInternalName().equals(code))
                return level;

        return null;
    }

    /**
     * Resolves a level by its file code, reporting failure against [Maps] [[code]].
     * @param code the level code
     * @return level
     */
    public FroggerMapLevelID resolveLevel(String code) {
        return resolveLevel(mapsKey(code), code);
    }

    /**
     * Resolves a level by its file code.
     * @param key the manifest key being resolved, used in the error message
     * @param code the level code
     * @return level
     */
    public FroggerMapLevelID resolveLevel(String key, String code) {
        FroggerMapLevelID level = findLevel(code);
        if (level == null)
            throw fail(key, "no level with the code '%s' (FroggerMapLevelID file codes such as CAV1)", code);
        return level;
    }

    /**
     * Gets the file code of a level.
     * @param level the level
     * @return code, or null
     */
    public String levelCodeOf(FroggerMapLevelID level) {
        return level != null ? level.getInternalName() : null;
    }

    /**
     * Gets the level code of a map file, derived from its MWI display name.
     * @param map the map file
     * @return code, or null when the map is not a known level
     */
    public String levelCodeOf(FroggerMapFile map) {
        return map != null ? levelCodeOf(map.getMapLevelID()) : null;
    }

    /**
     * Finds the map file of a level by its MWI name, CODE.MAP.
     * @param level the level
     * @return map, or null
     */
    public FroggerMapFile findMapFile(FroggerMapLevelID level) {
        return level != null ? findMapFileByName(level.getInternalName() + MAP_EXTENSION) : null;
    }

    /**
     * Resolves the map file of a level, reporting failure against [Maps] [[code]].
     * @param level the level
     * @return map
     */
    public FroggerMapFile resolveMapFile(FroggerMapLevelID level) {
        return resolveMapFile(mapsKey(levelCodeOf(level)), level);
    }

    /**
     * Resolves the map file of a level.
     * @param key the manifest key being resolved, used in the error message
     * @param level the level
     * @return map
     */
    public FroggerMapFile resolveMapFile(String key, FroggerMapLevelID level) {
        FroggerMapFile map = findMapFile(level);
        if (map == null)
            throw fail(key, "no map file named '%s%s' in %s", levelCodeOf(level), MAP_EXTENSION, getBuildName());
        return map;
    }

    /**
     * Finds the PC low-poly twin of a level's map, CODE_WIN95.MAP. Builds other than PC have none.
     * @param level the level
     * @return lowPolyMap, or null
     */
    public FroggerMapFile findLowPolyMapFile(FroggerMapLevelID level) {
        if (level == null || !this.gameInstance.isPC())
            return null;

        return findMapFileByName(level.getInternalName() + LOW_POLY_MAP_SUFFIX + MAP_EXTENSION);
    }

    /**
     * Resolves the PC low-poly twin of a level's map.
     * @param key the manifest key being resolved, used in the error message
     * @param level the level
     * @return lowPolyMap
     */
    public FroggerMapFile resolveLowPolyMapFile(String key, FroggerMapLevelID level) {
        FroggerMapFile map = findLowPolyMapFile(level);
        if (map == null)
            throw fail(key, "no low-poly map file named '%s%s%s' in %s", levelCodeOf(level), LOW_POLY_MAP_SUFFIX, MAP_EXTENSION, getBuildName());
        return map;
    }

    /**
     * Gets the map book of a level.
     * @param level the level
     * @return mapBook, or null
     */
    public FroggerMapBook getMapBook(FroggerMapLevelID level) {
        return level != null ? this.gameInstance.getMapBook(level) : null;
    }

    /**
     * Finds the level WAD a map loads its models from, through its map book.
     * @param map the map file
     * @return levelWad, or null
     */
    public WADFile findLevelWad(FroggerMapFile map) {
        if (map == null)
            return null;

        FroggerMapBook mapBook = getMapBook(map.getMapLevelID());
        if (mapBook == null)
            return null;

        try {
            return mapBook.getLevelWad(map);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /**
     * Resolves the level WAD a map loads its models from.
     * @param key the manifest key being resolved, used in the error message
     * @param map the map file
     * @return levelWad
     */
    public WADFile resolveLevelWad(String key, @NonNull FroggerMapFile map) {
        WADFile wad = findLevelWad(map);
        if (wad == null)
            throw fail(key, "the map book of '%s' names no level WAD", map.getFileDisplayName());
        return wad;
    }

    /**
     * Gets the MWI display name of a WAD.
     * @param wad the WAD
     * @return wadName, or null
     */
    public String wadNameOf(WADFile wad) {
        MWIResourceEntry entry = wad != null ? wad.getIndexEntry() : null;
        return entry != null ? entry.getDisplayName() : null;
    }

    /**
     * Finds the WAD with the given MWI display name.
     * @param name the WAD name, e.g. THEME_ORG1.WAD
     * @return wad, or null
     */
    public WADFile findWad(String name) {
        MWIResourceEntry entry = name != null ? this.gameInstance.getResourceEntryByName(name) : null;
        SCGameFile<?> file = entry != null ? this.gameInstance.getGameFile(entry) : null;
        return file instanceof WADFile ? (WADFile) file : null;
    }

    /**
     * Finds the MWI entry of a model by its display name. The entry must belong to a model.
     * @param name the model name, e.g. ORG_SNAKE.XMR
     * @return entry, or null
     */
    public MWIResourceEntry findModelEntry(String name) {
        if (name == null || name.isEmpty() || Constants.DUMMY_FILE_NAME.equalsIgnoreCase(name))
            return null;

        MWIResourceEntry entry = this.gameInstance.getResourceEntryByName(name);
        return entry != null && this.gameInstance.getGameFile(entry) instanceof MRModel ? entry : null;
    }

    /**
     * Resolves the MWI entry of a model, reporting failure against [Models] [[name]].
     * @param name the model name
     * @return entry
     */
    public MWIResourceEntry resolveModelEntry(String name) {
        return resolveModelEntry(modelsKey(name), name);
    }

    /**
     * Resolves the MWI entry of a model.
     * @param key the manifest key being resolved, used in the error message
     * @param name the model name
     * @return entry
     */
    public MWIResourceEntry resolveModelEntry(String key, String name) {
        MWIResourceEntry entry = findModelEntry(name);
        if (entry == null)
            throw fail(key, "no model named '%s' in the MWI of %s (a package cannot add names)", name, getBuildName());
        return entry;
    }

    /**
     * Tests whether a WAD child is a dummy (no file, or an MRModel holding only DUMY).
     * @param entry the WAD child
     * @return dummy
     */
    public boolean isDummy(WADEntry entry) {
        return entry == null || entry.isDummy();
    }

    /**
     * Tests whether a WAD child carries the placeholder resource id (MWI entry NULL) instead of its own entry.
     * @param entry the WAD child
     * @return placeholderId
     */
    public boolean hasPlaceholderId(WADEntry entry) {
        MWIResourceEntry fileEntry = entry != null ? entry.getFileEntry() : null;
        return fileEntry == null || Constants.DUMMY_FILE_NAME.equalsIgnoreCase(fileEntry.getDisplayName());
    }

    /**
     * Gets the model name a WAD child holds: the MWI display name of its own entry, or for a non-dummy child under the
     * placeholder id, the name of a named slot holding identical bytes, searched first in WADs sharing the child's slot layout,
     * then in WADs of the same low-poly role, then anywhere (distinct names can share bytes, e.g. ORG_SNAKE.XMR and ORG_SNAKE_WIN95.XMR).
     * @param entry the WAD child
     * @return modelName, or null for dummies, non-models and unmatched placeholder copies
     */
    public String modelNameOf(WADEntry entry) {
        if (isDummy(entry) || !(entry.getFile() instanceof MRModel))
            return null;

        if (!hasPlaceholderId(entry))
            return entry.getFileEntry().getDisplayName();

        byte[] bytes = getModelBytes(entry.getFile());
        WADFile ownWad = entry.getWadFile();
        boolean lowPoly = isLowPolyWad(ownWad);
        List<WADFile> searchOrder = new ArrayList<>(listSameLayoutWads(ownWad));
        for (WADFile wad : getAllWads())
            if (!searchOrder.contains(wad) && isLowPolyWad(wad) == lowPoly)
                searchOrder.add(wad);
        for (WADFile wad : getAllWads())
            if (!searchOrder.contains(wad))
                searchOrder.add(wad);

        for (WADFile wad : searchOrder)
            for (WADEntry other : wad.getFiles())
                if (other != entry && !isDummy(other) && other.getFile() instanceof MRModel && !hasPlaceholderId(other)
                        && Arrays.equals(bytes, getModelBytes(other.getFile())))
                    return other.getFileEntry().getDisplayName();

        return null;
    }

    /**
     * Describes one slot of a WAD.
     * @param wad the WAD
     * @param index the slot index
     * @return slot, or null when the index is out of range
     */
    public FroggerModWadSlot getSlot(WADFile wad, int index) {
        if (wad == null || index < 0 || index >= wad.getFiles().size())
            return null;

        WADEntry entry = wad.getFiles().get(index);
        return new FroggerModWadSlot(wad, index, entry, modelNameOf(entry), hasPlaceholderId(entry));
    }

    /**
     * Describes every slot of a WAD in slot order.
     * @param wad the WAD
     * @return slots
     */
    public List<FroggerModWadSlot> listSlots(@NonNull WADFile wad) {
        List<FroggerModWadSlot> slots = new ArrayList<>();
        for (int i = 0; i < wad.getFiles().size(); i++)
            slots.add(getSlot(wad, i));
        return slots;
    }

    /**
     * Lists the non-dummy model names a WAD holds, in slot order (placeholder copies named by their bytes; unmatched ones omitted).
     * @param wad the WAD
     * @return modelNames
     */
    public List<String> listModelNames(@NonNull WADFile wad) {
        List<String> names = new ArrayList<>();
        for (WADEntry entry : wad.getFiles()) {
            String name = modelNameOf(entry);
            if (name != null)
                names.add(name);
        }

        return names;
    }

    /**
     * Finds every non-dummy WAD slot, in any WAD, whose own MWI entry has the given display name.
     * @param name the model name
     * @return slots, empty when none
     */
    public List<FroggerModWadSlot> findModelSlots(String name) {
        List<FroggerModWadSlot> slots = new ArrayList<>();
        if (name == null || name.isEmpty())
            return slots;

        for (WADFile wad : getAllWads()) {
            List<WADEntry> entries = wad.getFiles();
            for (int i = 0; i < entries.size(); i++) {
                WADEntry entry = entries.get(i);
                if (isDummy(entry) || hasPlaceholderId(entry) || !(entry.getFile() instanceof MRModel))
                    continue;

                if (name.equalsIgnoreCase(entry.getFileEntry().getDisplayName()))
                    slots.add(new FroggerModWadSlot(wad, i, entry, entry.getFileEntry().getDisplayName(), false));
            }
        }

        return slots;
    }

    /**
     * Finds every non-dummy WAD slot holding the same bytes as the named model, including copies under the placeholder id.
     * The reference bytes are those of the first slot found by name; slots under other names with identical bytes are reported under
     * their own names. Bytes are the file's raw data as loaded or imported, else its serialised form.
     * @param name the model name
     * @return slots, empty when the name is held nowhere
     */
    public List<FroggerModWadSlot> findModelSlotsByBytes(String name) {
        List<FroggerModWadSlot> slots = new ArrayList<>();
        List<FroggerModWadSlot> named = findModelSlots(name);
        if (named.isEmpty())
            return slots;

        byte[] reference = getModelBytes(named.get(0).getEntry().getFile());
        for (WADFile wad : getAllWads()) {
            List<WADEntry> entries = wad.getFiles();
            for (int i = 0; i < entries.size(); i++) {
                WADEntry entry = entries.get(i);
                if (isDummy(entry) || !(entry.getFile() instanceof MRModel))
                    continue;

                if (Arrays.equals(reference, getModelBytes(entry.getFile()))) {
                    boolean placeholder = hasPlaceholderId(entry);
                    slots.add(new FroggerModWadSlot(wad, i, entry, placeholder ? named.get(0).getModelName() : entry.getFileEntry().getDisplayName(), placeholder));
                }
            }
        }

        return slots;
    }

    /**
     * Finds the theme a WAD belongs to, through the map books and theme books that reference it.
     * @param wad the WAD
     * @return theme, or null when no book references it
     */
    public FroggerMapTheme findThemeOfWad(WADFile wad) {
        if (wad == null || wad.getIndexEntry() == null)
            return null;

        for (FroggerMapLevelID level : FroggerMapLevelID.values()) {
            FroggerMapBook mapBook = getMapBook(level);
            if (mapBook != null && mapBook.isEntry(wad))
                return level.getTheme();
        }

        for (FroggerMapTheme theme : FroggerMapTheme.values()) {
            FroggerThemeBook themeBook = this.gameInstance.getThemeBook(theme);
            if (themeBook != null && themeBook.isEntry(wad))
                return theme;
        }

        return null;
    }

    /**
     * Lists every WAD belonging to a theme (level WADs of the theme's levels and the theme book's WADs), in archive order.
     * @param theme the theme
     * @return wads
     */
    public List<WADFile> listThemeWads(FroggerMapTheme theme) {
        List<WADFile> wads = new ArrayList<>();
        if (theme == null)
            return wads;

        for (WADFile wad : getAllWads())
            if (findThemeOfWad(wad) == theme)
                wads.add(wad);

        return wads;
    }

    /**
     * Tests whether a WAD is a PC low-poly WAD: the low WAD of a map book or theme book and not also its high WAD.
     * @param wad the WAD
     * @return lowPoly
     */
    public boolean isLowPolyWad(WADFile wad) {
        if (wad == null || wad.getIndexEntry() == null)
            return false;

        final int resourceId = wad.getIndexEntry().getResourceId();
        for (FroggerMapLevelID level : FroggerMapLevelID.values()) {
            FroggerMapBook mapBook = getMapBook(level);
            if (mapBook != null && mapBook.execute(pc -> pc.getLowWadId() == resourceId && pc.getHighWadId() != resourceId, psx -> false))
                return true;
        }

        for (FroggerMapTheme theme : FroggerMapTheme.values()) {
            FroggerThemeBook themeBook = this.gameInstance.getThemeBook(theme);
            if (themeBook != null && themeBook.execute(pc -> (pc.getLowWadId() == resourceId && pc.getHighWadId() != resourceId)
                    || (pc.getLowMultiplayerWadId() == resourceId && pc.getHighMultiplayerWadId() != resourceId), psx -> false))
                return true;
        }

        return false;
    }

    /**
     * Lists the WADs sharing a WAD's slot layout: same theme, same low-poly role and the same slot count (the SCUtils.copyWadEntry precondition).
     * The given WAD is included.
     * @param wad the WAD
     * @return layoutWads, empty when the WAD's theme is unknown
     */
    public List<WADFile> listSameLayoutWads(@NonNull WADFile wad) {
        List<WADFile> wads = new ArrayList<>();
        FroggerMapTheme theme = findThemeOfWad(wad);
        if (theme == null)
            return wads;

        boolean lowPoly = isLowPolyWad(wad);
        for (WADFile other : listThemeWads(theme))
            if (other.getFiles().size() == wad.getFiles().size() && isLowPolyWad(other) == lowPoly)
                wads.add(other);

        return wads;
    }

    /**
     * Finds the slot indices the theme's form library points at (getWadIndex) whose slot holds the named model in a WAD sharing
     * the given WAD's slot layout.
     * @param targetWad the WAD whose slot layout is meant
     * @param name the model name
     * @return wadIndices, empty when no form of the theme resolves to the model
     */
    public Set<Integer> findFormWadIndices(@NonNull WADFile targetWad, String name) {
        Set<Integer> indices = new TreeSet<>();
        FroggerMapTheme theme = findThemeOfWad(targetWad);
        FroggerThemeBook themeBook = theme != null ? this.gameInstance.getThemeBook(theme) : null;
        if (themeBook == null || theme == FroggerMapTheme.GENERAL || name == null)
            return indices;

        List<WADFile> layoutWads = listSameLayoutWads(targetWad);
        for (FroggerFormEntry form : themeBook.getFormBook()) {
            if (form.testFlag(FroggerFormLibFlag.NO_MODEL) || form.getId() >= FroggerFormEntry.PROJECT_MAX_THEME_MOFS)
                continue;

            int wadIndex = form.getWadIndex();
            for (WADFile wad : layoutWads) {
                if (wadIndex < 0 || wadIndex >= wad.getFiles().size())
                    continue;

                WADEntry entry = wad.getFiles().get(wadIndex);
                if (!isDummy(entry) && !hasPlaceholderId(entry) && entry.getFile() instanceof MRModel
                        && entry.getFileEntry().getDisplayName().equalsIgnoreCase(name)) {
                    indices.add(wadIndex);
                    break;
                }
            }
        }

        return indices;
    }

    /**
     * Finds the slot index a model must occupy in a level WAD: the slot it occupies in another WAD sharing the target's slot layout
     * (identity rule 4), cross-checked against the theme's form library.
     * @param targetLevelWad the level WAD that should hold the model
     * @param name the model name
     * @return slotIndex, or null when it cannot be derived
     */
    public Integer findSlotForModel(WADFile targetLevelWad, String name) {
        FroggerModWadSlot source = deriveSourceSlot(null, targetLevelWad, name, false);
        return source != null ? source.getIndex() : null;
    }

    /**
     * Resolves the slot index a model must occupy in a level WAD, reporting failure against [Models] [[name]].
     * @param targetLevelWad the level WAD that should hold the model
     * @param name the model name
     * @return slotIndex
     */
    public int resolveSlotForModel(WADFile targetLevelWad, String name) {
        return resolveSlotForModel(modelsKey(name), targetLevelWad, name);
    }

    /**
     * Resolves the slot index a model must occupy in a level WAD.
     * @param key the manifest key being resolved, used in the error message
     * @param targetLevelWad the level WAD that should hold the model
     * @param name the model name
     * @return slotIndex
     */
    public int resolveSlotForModel(String key, WADFile targetLevelWad, String name) {
        return deriveSourceSlot(key, targetLevelWad, name, true).getIndex();
    }

    /**
     * Finds the slot, in a WAD sharing the target's slot layout, that holds the named model and decides its slot index.
     * Its bytes are the base build's bytes for that model in this layout; the target WAD itself is preferred when it already holds the model.
     * @param targetLevelWad the level WAD that should hold the model
     * @param name the model name
     * @return sourceSlot, or null when the slot cannot be derived
     */
    public FroggerModWadSlot findSourceSlotForModel(WADFile targetLevelWad, String name) {
        return deriveSourceSlot(null, targetLevelWad, name, false);
    }

    /**
     * Resolves the slot, in a WAD sharing the target's slot layout, that holds the named model and decides its slot index.
     * @param key the manifest key being resolved, used in the error message
     * @param targetLevelWad the level WAD that should hold the model
     * @param name the model name
     * @return sourceSlot
     */
    public FroggerModWadSlot resolveSourceSlotForModel(String key, WADFile targetLevelWad, String name) {
        return deriveSourceSlot(key, targetLevelWad, name, true);
    }

    private FroggerModWadSlot deriveSourceSlot(String key, WADFile target, String name, boolean throwErrors) {
        if (target == null)
            return failOrNull(throwErrors, key, "no target WAD given for model '%s'", name);
        if (findModelEntry(name) == null)
            return failOrNull(throwErrors, key, "no model named '%s' in the MWI of %s (a package cannot add names)", name, getBuildName());

        String targetName = wadNameOf(target);
        FroggerMapTheme theme = findThemeOfWad(target);
        if (theme == null)
            return failOrNull(throwErrors, key, "%s is not referenced by any map book or theme book, so its theme is unknown", targetName);

        List<WADFile> layoutWads = listSameLayoutWads(target);
        String layout = theme.name() + (isLowPolyWad(target) ? " low-poly" : "") + " WADs with " + target.getFiles().size() + " slots";
        Set<Integer> indices = new TreeSet<>();
        Set<String> otherLayouts = new TreeSet<>();
        FroggerModWadSlot source = null;
        for (FroggerModWadSlot slot : findModelSlots(name)) {
            if (layoutWads.contains(slot.getWad())) {
                indices.add(slot.getIndex());
                if (source == null || slot.getWad() == target)
                    source = slot;
            } else {
                otherLayouts.add(slot.getWadName() + " slot " + slot.getIndex() + " (" + slot.getWad().getFiles().size() + " slots)");
            }
        }

        if (source == null) {
            if (!otherLayouts.isEmpty())
                return failOrNull(throwErrors, key, "model '%s' is held only by WADs with another slot layout than %s (%s): %s",
                        name, targetName, layout, String.join(", ", otherLayouts));
            return failOrNull(throwErrors, key, "no %s of %s holds model '%s', so its slot in %s cannot be derived", layout, getBuildName(), name, targetName);
        }

        if (indices.size() > 1)
            return failOrNull(throwErrors, key, "model '%s' occupies different slots %s in the %s, so its slot in %s is ambiguous", name, indices, layout, targetName);

        int index = source.getIndex();
        Set<Integer> formIndices = findFormWadIndices(target, name);
        if (!formIndices.isEmpty() && !formIndices.contains(index))
            return failOrNull(throwErrors, key, "model '%s' sits in slot %d of the %s, but the %s form library points at slots %s for it",
                    name, index, layout, theme.name(), formIndices);

        WADEntry targetEntry = target.getFiles().get(index);
        if (targetEntry.getFile() != null && !(targetEntry.getFile() instanceof MRModel))
            return failOrNull(throwErrors, key, "slot %d of %s holds '%s', which is not a model", index, targetName, targetEntry.getFileEntry().getDisplayName());

        return source;
    }

    /**
     * Finds a demo file by its MWI display name.
     * @param name the demo name, e.g. ORG1DEMO.DAT
     * @return demo, or null
     */
    public FroggerDemoFile findDemoFile(String name) {
        MWIResourceEntry entry = name != null ? this.gameInstance.getResourceEntryByName(name) : null;
        SCGameFile<?> file = entry != null ? this.gameInstance.getGameFile(entry) : null;
        return file instanceof FroggerDemoFile ? (FroggerDemoFile) file : null;
    }

    /**
     * Resolves a demo file, reporting failure against [Demos] files.
     * @param name the demo name
     * @return demo
     */
    public FroggerDemoFile resolveDemoFile(String name) {
        return resolveDemoFile("[Demos] files=" + name, name);
    }

    /**
     * Resolves a demo file.
     * @param key the manifest key being resolved, used in the error message
     * @param name the demo name
     * @return demo
     */
    public FroggerDemoFile resolveDemoFile(String key, String name) {
        FroggerDemoFile demo = findDemoFile(name);
        if (demo == null)
            throw fail(key, "no demo file named '%s' in %s", name, getBuildName());
        return demo;
    }

    /**
     * Resolves a remap file's image names to texture ids for a map, reporting failure against [Maps] [[code]] remap.
     * @param map the map whose remap the names replace
     * @param names one image name per remap slot, in slot order
     * @return textureIds
     */
    public short[] resolveRemapNames(@NonNull FroggerMapFile map, @NonNull List<String> names) {
        return resolveRemapNames(mapsKey(levelCodeOf(map)) + " remap", map, names);
    }

    /**
     * Resolves a remap file's image names to texture ids for a map. The name count must equal the map remap's slot count.
     * @param key the manifest key being resolved, used in the error message
     * @param map the map whose remap the names replace
     * @param names one image name per remap slot, in slot order
     * @return textureIds
     */
    public short[] resolveRemapNames(String key, @NonNull FroggerMapFile map, @NonNull List<String> names) {
        TextureRemapArray remap = map.getTextureRemap();
        if (remap == null)
            throw fail(key, "map '%s' has no texture remap", map.getFileDisplayName());

        int slotCount = remap.getTextureIds().size();
        if (names.size() != slotCount)
            throw fail(key, "lists %d names, but the remap of '%s' has %d slots (remaps cannot grow or shrink)", names.size(), map.getFileDisplayName(), slotCount);

        short[] textureIds = new short[slotCount];
        for (int i = 0; i < slotCount; i++)
            textureIds[i] = resolveTextureId(key + " line " + (i + 1), names.get(i));

        return textureIds;
    }

    /**
     * Gets a map remap's image names in slot order.
     * @param map the map
     * @return names
     */
    public List<String> remapNamesOf(@NonNull FroggerMapFile map) {
        String key = mapsKey(levelCodeOf(map)) + " remap";
        TextureRemapArray remap = map.getTextureRemap();
        if (remap == null)
            throw fail(key, "map '%s' has no texture remap", map.getFileDisplayName());

        List<Short> textureIds = remap.getTextureIds();
        List<String> names = new ArrayList<>(textureIds.size());
        for (int i = 0; i < textureIds.size(); i++) {
            Short textureId = textureIds.get(i);
            String name = textureId != null ? imageNameOfTextureId(textureId) : null;
            if (name == null)
                throw fail(key + " line " + (i + 1), "texture id %s in slot %d of '%s' has no name; name it with FrogLord's custom-name feature", textureId, i, map.getFileDisplayName());
            names.add(name);
        }

        return names;
    }

    /**
     * Finds a theme by its FroggerMapTheme enum name.
     * @param name the theme name, e.g. DESERT
     * @return theme, or null
     */
    public FroggerMapTheme findTheme(String name) {
        if (name == null)
            return null;

        for (FroggerMapTheme theme : FroggerMapTheme.values())
            if (theme.name().equals(name))
                return theme;

        return null;
    }

    /**
     * Resolves a theme by its enum name, reporting failure against [Themes] [[name]].
     * @param name the theme name
     * @return theme
     */
    public FroggerMapTheme resolveTheme(String name) {
        return resolveTheme("[Themes] [[" + name + "]]", name);
    }

    /**
     * Resolves a theme by its enum name.
     * @param key the manifest key being resolved, used in the error message
     * @param name the theme name
     * @return theme
     */
    public FroggerMapTheme resolveTheme(String key, String name) {
        FroggerMapTheme theme = findTheme(name);
        if (theme == null)
            throw fail(key, "no theme named '%s' (one of %s)", name, Arrays.toString(FroggerMapTheme.values()));
        return theme;
    }

    /**
     * Finds a level-select world by its FroggerLevelSelectWorldID enum name.
     * @param name the world name, e.g. ORIGINAL
     * @return world, or null
     */
    public FroggerLevelSelectWorldID findWorld(String name) {
        if (name == null)
            return null;

        for (FroggerLevelSelectWorldID world : FroggerLevelSelectWorldID.values())
            if (world.name().equals(name))
                return world;

        return null;
    }

    /**
     * Resolves a level-select world by its enum name.
     * @param key the manifest key being resolved, used in the error message
     * @param name the world name
     * @return world
     */
    public FroggerLevelSelectWorldID resolveWorld(String key, String name) {
        FroggerLevelSelectWorldID world = findWorld(name);
        if (world == null)
            throw fail(key, "no level-select world named '%s' (one of %s)", name, Arrays.toString(FroggerLevelSelectWorldID.values()));
        return world;
    }

    private FroggerMapFile findMapFileByName(String name) {
        MWIResourceEntry entry = this.gameInstance.getResourceEntryByName(name);
        SCGameFile<?> file = entry != null ? this.gameInstance.getGameFile(entry) : null;
        return file instanceof FroggerMapFile ? (FroggerMapFile) file : null;
    }

    private List<WADFile> getAllWads() {
        return this.gameInstance.getMainArchive().getAllFiles(WADFile.class);
    }

    private static byte[] getModelBytes(SCGameFile<?> file) {
        byte[] rawData = file.getRawFileData();
        return rawData != null ? rawData : file.writeDataToByteArray();
    }

    private String getBuildName() {
        return this.gameInstance.getVersionConfig().getInternalName();
    }

    private static String imagesKey(String bankName, String imageName) {
        return "[Images] [[" + bankName + "]]" + (imageName != null ? " [[[" + imageName + "]]]" : "");
    }

    private static String mapsKey(String code) {
        return "[Maps] [[" + code + "]]";
    }

    private static String modelsKey(String name) {
        return "[Models] [[" + name + "]]";
    }

    private static String levelSelectKey(String imageName) {
        return "[LevelSelect] " + imageName;
    }

    private static <T> T failOrNull(boolean throwErrors, String key, String message, Object... arguments) {
        if (throwErrors)
            throw fail(key, message, arguments);
        return null;
    }

    private static IllegalArgumentException fail(String key, String message, Object... arguments) {
        return new IllegalArgumentException(key + ": " + String.format(message, arguments));
    }
}
