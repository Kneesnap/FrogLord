package net.highwayfrogs.editor.file.config.exe;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.WADFile;
import net.highwayfrogs.editor.file.config.exe.general.FormEntry;
import net.highwayfrogs.editor.file.config.exe.pc.PCThemeBook;
import net.highwayfrogs.editor.file.config.exe.psx.PSXThemeBook;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.MAPTheme;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Represents a platform-independent ThemeBook.
 * Created by Kneesnap on 1/27/2019.
 */
@Getter
public abstract class ThemeBook extends ExeStruct {
    @Setter private transient MAPTheme theme;
    private final List<FormEntry> formBook = new ArrayList<>();

    public ThemeBook(FroggerGameInstance instance) {
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
            FormEntry formEntry = new FormEntry(instance, getTheme(), localFormId++, globalFormId++);
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
        return execute(PCThemeBook::getFormLibraryPointer, PSXThemeBook::getFormLibraryPointer);
    }

    /**
     * Get the VLO of this book.
     * @param file The map file to get the vlo from.
     * @return vloArchive
     */
    public abstract VLOArchive getVLO(MAPFile file);

    /**
     * Get the wad of this book.
     * @param file The map file to get the vlo from.
     * @return vloArchive
     */
    public abstract WADFile getWAD(MAPFile file);

    /**
     * Tests if this is a valid theme.
     * @return isValid
     */
    public abstract boolean isValid();

    /**
     * Execute something depending on which MapBook type this is.
     * @param pcHandler  The PC handler.
     * @param psxHandler The psx handler.
     * @return result
     */
    public abstract <T> T execute(Function<PCThemeBook, T> pcHandler, Function<PSXThemeBook, T> psxHandler);

    /**
     * Execute something depending on which MapBook type this is.
     * @param pcHandler  The PC handler.
     * @param psxHandler The psx handler.
     */
    public void execute(Consumer<PCThemeBook> pcHandler, Consumer<PSXThemeBook> psxHandler) {
        execute(pc -> {
            pcHandler.accept(pc);
            return null;
        }, psx -> {
            psxHandler.accept(psx);
            return null;
        });
    }

    /**
     * Perform some logic on all vlos.
     * @param handler The logic to perform.
     */
    public void forEachVLO(Consumer<VLOArchive> handler) {
        execute(pc -> {
            if (pc.getHighVloId() != 0)
                handler.accept(getGameInstance().getGameFile(pc.getHighVloId()));
            if (pc.getLowVloId() != 0)
                handler.accept(getGameInstance().getGameFile(pc.getLowVloId()));
            if (pc.getHighMultiplayerVloId() != 0)
                handler.accept(getGameInstance().getGameFile(pc.getHighMultiplayerVloId()));
            if (pc.getLowMultiplayerVloId() != 0)
                handler.accept(getGameInstance().getGameFile(pc.getLowMultiplayerVloId()));
        }, psx -> {
            if (psx.getVloId() != 0)
                handler.accept(getGameInstance().getGameFile(psx.getVloId()));
            if (psx.getMultiplayerVloId() != 0)
                handler.accept(getGameInstance().getGameFile(psx.getMultiplayerVloId()));
        });
    }

    @Override
    public void save(DataWriter writer) {
        writer.jumpTemp((int) (getFormLibraryPointer() - getGameInstance().getRamOffset()));
        getFormBook().forEach(entry -> entry.save(writer));
        writer.jumpReturn();
    }
}