package net.highwayfrogs.editor.games.sony.frogger.data.theme;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.data.FroggerHardcodedResourceEntry;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapTheme;
import net.highwayfrogs.editor.games.sony.frogger.map.data.form.FroggerFormEntry;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloFile;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Represents the THEME_BOOK struct from Frogger.
 * This struct is implemented differently depending on the game version, so this is the abstract base class.
 * Created by Kneesnap on 1/27/2019.
 */
@Getter
public abstract class FroggerThemeBook extends FroggerHardcodedResourceEntry {
    @Setter private transient FroggerMapTheme theme;
    private final List<FroggerFormEntry> formBook = new ArrayList<>();

    public FroggerThemeBook(FroggerGameInstance instance) {
        super(instance);
    }

    /**
     * Load form library data from the game executable.
     * @param instance The frogger instance to load the form library for.
     * @param toRead   The number of form libraries to read.
     */
    public void loadFormLibrary(FroggerGameInstance instance, int toRead) {
        DataReader reader = instance.getExecutableReader();
        int globalFormId = instance.getFullFormBook().size();
        reader.jumpTemp((int) (getFormLibraryPointer() - instance.getRamOffset()));

        int localFormId = 0;
        for (int i = 0; i < toRead; i++) {
            FroggerFormEntry formEntry = new FroggerFormEntry(instance, getTheme(), localFormId++, globalFormId++);
            formEntry.load(reader);
            this.formBook.add(formEntry);
        }
        reader.jumpReturn();

        instance.getFullFormBook().addAll(this.formBook);
    }

    /**
     * Get the pointer to the form library.
     * @return formLibraryPointer
     */
    public long getFormLibraryPointer() {
        return execute(FroggerThemeBookPC::getFormLibraryPointer, FroggerThemeBookPSX::getFormLibraryPointer);
    }

    /**
     * Get the VLO of this book.
     * @param file The map file to get the vlo from.
     * @return vloArchive
     */
    public VloFile getVLO(FroggerMapFile file) {
        return getVLO(file != null && file.isMultiplayer(), file != null && file.isLowPolyMode());
    }

    /**
     * Get the VLO of this book.
     * @param isMultiplayer whether the VLO used for multiplayer mode is returned
     * @param isLowPolyMode whether the VLO used for low-poly mode is returned
     * @return vloArchive
     */
    public abstract VloFile getVLO(boolean isMultiplayer, boolean isLowPolyMode);

    /**
     * Get the wad of this book.
     * @param file The map file to get the vlo from.
     * @return vloArchive
     */
    public WADFile getWAD(FroggerMapFile file) {
        return getWAD(file != null && file.isMultiplayer(), file != null && file.isLowPolyMode());
    }

    /**
     * Get the wad of this book.
     * @param isMultiplayer whether the VLO used for multiplayer mode is returned
     * @param isLowPolyMode whether the VLO used for low-poly mode is returned
     * @return vloArchive
     */
    public abstract WADFile getWAD(boolean isMultiplayer, boolean isLowPolyMode);

    /**
     * Tests if this is a valid theme.
     * @return isValid
     */
    public abstract boolean isValid();

    /**
     * Execute something depending on which FroggerMapBook type this is.
     * @param pcHandler  The PC handler.
     * @param psxHandler The psx handler.
     * @return result
     */
    public abstract <T> T execute(Function<FroggerThemeBookPC, T> pcHandler, Function<FroggerThemeBookPSX, T> psxHandler);

    @Override
    public void save(DataWriter writer) {
        writer.jumpTemp((int) (getFormLibraryPointer() - getGameInstance().getRamOffset()));
        List<FroggerFormEntry> formEntries = getFormBook();
        for (int i = 0; i < formEntries.size(); i++)
            formEntries.get(i).save(writer);

        writer.jumpReturn();
    }

    /**
     * Create a new FroggerThemeBook from a frogger instance.
     * @param instance The instance to use to determine the theme book type to create.
     * @return mapBook
     */
    public static FroggerThemeBook makeNewThemeBook(FroggerGameInstance instance) {
        if (instance.getVersionConfig().isAtLeastRetailWindows()) {
            return new FroggerThemeBookPC(instance);
        } else {
            return new FroggerThemeBookPSX(instance);
        }
    }
}