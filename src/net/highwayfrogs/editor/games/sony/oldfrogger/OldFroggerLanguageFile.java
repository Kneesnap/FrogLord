package net.highwayfrogs.editor.games.sony.oldfrogger;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.shared.ui.SCFileEditorPropertyListUIController;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.InputMenu;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a file with language strings.
 * Created by Kneesnap on 7/22/2024.
 */
public class OldFroggerLanguageFile extends SCGameFile<OldFroggerGameInstance> {
    private final List<String> entries = new ArrayList<>();
    private final List<String> immutableEntries = Collections.unmodifiableList(this.entries);

    private static final int FIXED_STRING_LENGTH = 50;
    public OldFroggerLanguageFile(OldFroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        this.entries.clear();
        while (reader.getRemaining() >= FIXED_STRING_LENGTH)
            this.entries.add(reader.readNullTerminatedFixedSizeString(FIXED_STRING_LENGTH));
    }

    @Override
    public void save(DataWriter writer) {
        for (int i = 0; i < this.entries.size(); i++)
            writer.writeNullTerminatedFixedSizeString(this.entries.get(i), FIXED_STRING_LENGTH);
    }

    @Override
    public Image getCollectionViewIcon() {
        return ImageResource.GHIDRA_ICON_PAPER_WITH_TEXT_32.getFxImage();
    }

    @Override
    public GameUIController<?> makeEditorUI() {
        return loadEditor(getGameInstance(), "edit-file-property-list-base", new SCFileEditorPropertyListUIController<>(getGameInstance(), "Language File"), this);
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("Language String Entries", this.entries.size());
        for (int i = 0; i < this.entries.size(); i++) {
            final int index = i;
            propertyList.add("Language String Entry " + i, this.entries.get(i), () -> {
                String newValue = InputMenu.promptInput(getGameInstance(), "Please enter a new value.", this.entries.get(index));
                if (newValue == null)
                    return null;
                if (newValue.length() >= FIXED_STRING_LENGTH) {
                    Utils.makePopUp("The string provided is too long.", AlertType.ERROR);
                    return null;
                }

                setEntry(index, newValue);
                return newValue;
            });
        }

        return propertyList;
    }

    /**
     * Gets the entries available in the language file.
     */
    public List<String> getEntries() {
        return this.immutableEntries;
    }

    /**
     * Sets the string for a particular entry.
     * @param entryIndex the index of the entry to apply the string to
     * @param newEntry the string value to apply to the given entry index
     */
    public void setEntry(int entryIndex, String newEntry) {
        if (entryIndex < 0 || entryIndex > this.entries.size())
            throw new IndexOutOfBoundsException("Invalid entryIndex " + entryIndex + " should be within [0, " + this.entries.size() + "]");

        if (entryIndex == this.entries.size()) {
            if (newEntry == null)
                throw new NullPointerException("newEntry");
            if (newEntry.length() >= FIXED_STRING_LENGTH) // It cannot be 50 characters since it needs room for the null character.
                throw new IllegalArgumentException("The provided entry is too large!");

            this.entries.add(newEntry);
            return;
        }

        if (newEntry != null) {
            if (newEntry.length() >= FIXED_STRING_LENGTH) // It cannot be 50 characters since it needs room for the null character.
                throw new IllegalArgumentException("The provided entry is too large!");

            this.entries.set(entryIndex, newEntry);
        } else {
            this.entries.remove(entryIndex);
        }
    }
}
