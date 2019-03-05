package net.highwayfrogs.editor.file.config.exe;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.config.FroggerEXEInfo;
import net.highwayfrogs.editor.file.config.exe.general.FormEntry;
import net.highwayfrogs.editor.file.config.exe.pc.PCThemeBook;
import net.highwayfrogs.editor.file.config.exe.psx.PSXThemeBook;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.MAPTheme;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.file.writer.DataWriter;

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
    private List<FormEntry> formBook = new ArrayList<>();

    public void loadFormLibrary(FroggerEXEInfo config, int toRead) {
        DataReader reader = config.getReader();
        int globalFormId = config.getFullFormBook().size();
        reader.jumpTemp((int) (getFormLibraryPointer() - config.getRamPointerOffset()));

        int localFormId = 0;
        for (int i = 0; i < toRead; i++) {
            FormEntry formEntry = new FormEntry(config, getTheme(), localFormId++, globalFormId++);
            formEntry.load(reader);
            this.formBook.add(formEntry);
        }
        reader.jumpReturn();

        config.getFullFormBook().addAll(this.formBook);
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
                handler.accept(getConfig().getGameFile(pc.getHighVloId()));
            if (pc.getLowVloId() != 0)
                handler.accept(getConfig().getGameFile(pc.getLowVloId()));
            if (pc.getHighMultiplayerVloId() != 0)
                handler.accept(getConfig().getGameFile(pc.getHighMultiplayerVloId()));
            if (pc.getLowMultiplayerVloId() != 0)
                handler.accept(getConfig().getGameFile(pc.getLowMultiplayerVloId()));
        }, psx -> {
            if (psx.getVloId() != 0)
                handler.accept(getConfig().getGameFile(psx.getVloId()));
            if (psx.getMultiplayerVloId() != 0)
                handler.accept(getConfig().getGameFile(psx.getMultiplayerVloId()));
        });
    }

    @Override
    public void save(DataWriter writer) {
        writer.jumpTemp((int) (getFormLibraryPointer() - getConfig().getRamPointerOffset()));
        getFormBook().forEach(entry -> entry.save(writer));
        writer.jumpReturn();
    }
}
