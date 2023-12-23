package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.WADFile;
import net.highwayfrogs.editor.file.WADFile.WADEntry;
import net.highwayfrogs.editor.file.mof.MOFHolder;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.oldfrogger.OldFroggerGameInstance;
import net.highwayfrogs.editor.games.sony.oldfrogger.OldFroggerReactionType;
import net.highwayfrogs.editor.games.sony.oldfrogger.config.OldFroggerFormConfig;
import net.highwayfrogs.editor.games.sony.oldfrogger.config.OldFroggerFormConfig.OldFroggerFormConfigEntry;
import net.highwayfrogs.editor.games.sony.oldfrogger.config.OldFroggerLevelTableEntry;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEditorUtils;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerFormUIManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.system.AbstractIndexStringConverter;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Represents a form definition in an old Frogger map.
 * Forms are wrappers for MOFS in the world.
 * Their primary function is to easily define behaviour for models (entities) that are used in-game, and to have multiple reactions (with data) on different parts of a model.
 * This is achieved by drawing plateaus over a model (in Os’s application Formy), where a plateau is a simple box, and supplying a pre-defined reaction (and some reaction data possibly) for each plateau.
 * Finally, a default reaction and data is also provided, which becomes the default reaction when that model is collided with by no plateaus are detected.
 * Created by Kneesnap on 12/10/2023.
 */
@Getter
public class OldFroggerMapForm extends SCGameData<OldFroggerGameInstance> {
    private final OldFroggerMapFile map;
    private int formType; // form identifier.
    private int mofId; // Index of the mof that is associated with this form (index in the mof wad file), which mappy sorts out into the correct order.
    private final List<OldFroggerMapFormDataEntry> formDataEntries = new ArrayList<>();

    public OldFroggerMapForm(OldFroggerMapFile map, int index) {
        super(map.getGameInstance());
        this.map = map;
        this.formType = index;
    }

    @Override
    public void load(DataReader reader) {
        this.formType = reader.readUnsignedShortAsInt();
        this.mofId = reader.readUnsignedShortAsInt();

        // The number of form data entries associated with this form! When we have multiple actions per (animated) mof, we have multiple forms too, one per action.
        int formEntryCount = reader.readUnsignedShortAsInt();
        short padding = reader.readShort();
        if (padding != 0)
            throw new RuntimeException("Form Padding short was not zero! (Was: " + padding + ")");

        // Read form data entries.
        int endPointer = reader.getIndex();
        this.formDataEntries.clear();
        for (int i = 0; i < formEntryCount; i++) {
            int formEntryDataStartAddress = reader.readInt();
            OldFroggerMapFormDataEntry newEntry = new OldFroggerMapFormDataEntry(getGameInstance());

            reader.jumpTemp(formEntryDataStartAddress);
            newEntry.load(reader);
            endPointer = Math.max(endPointer, reader.getIndex());
            reader.jumpReturn();

            this.formDataEntries.add(newEntry);
        }

        // Ensure we finish at the expected position.
        reader.setIndex(endPointer);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.formType);
        writer.writeUnsignedShort(this.mofId);
        writer.writeUnsignedShort(this.formDataEntries.size());
        writer.writeUnsignedShort(0); // Padding
        int formEntryDataStartAddress = writer.getIndex();

        // Write empty pointers.
        for (int i = 0; i < this.formDataEntries.size(); i++)
            writer.writeNullPointer();

        // Write form data entries.
        for (int i = 0; i < this.formDataEntries.size(); i++) {
            writer.writeAddressTo(formEntryDataStartAddress + (i * Constants.INTEGER_SIZE));
            this.formDataEntries.get(i).save(writer);
        }
    }

    /**
     * Gets the configured name of this form, or null.
     */
    public String getName() {
        OldFroggerFormConfig formConfig = getMap().getFormConfig();
        OldFroggerFormConfigEntry formConfigEntry = formConfig != null ? formConfig.getFormByType(this.formType) : null;
        return formConfigEntry != null ? formConfigEntry.getDisplayName() : null;
    }

    /**
     * Updates the form type.
     * @param newFormType The new form type to apply.
     */
    public void setFormType(int newFormType) {
        if (newFormType == this.formType)
            return;

        int formTableSize = this.map.getFormInstancePacket().getFormTableSize();
        if (newFormType < 0 || newFormType >= formTableSize)
            throw new IllegalArgumentException("The form type " + newFormType + " is not a supported form type ID. (Allowed: [0, " + formTableSize + ")");

        // Ensure this gets moved to the correct position in the map form list.
        List<OldFroggerMapForm> formList = this.map.getFormInstancePacket().getForms();
        int oldIndex = Collections.binarySearch(formList, this, Comparator.comparingInt(OldFroggerMapForm::getFormType));
        this.formType = newFormType;

        // The form is not currently registered in the form list.
        if (oldIndex < 0)
            return;

        boolean needsMoving = (oldIndex > 0 && newFormType <= formList.get(oldIndex - 1).getFormType())
                || (oldIndex >= formList.size() - 1 && newFormType >= formList.get(oldIndex + 1).getFormType());

        int oldFormType = this.formType;
        this.formType = newFormType;
        if (needsMoving) {
            formList.remove(oldIndex);
            int newIndex = Collections.binarySearch(formList, this, Comparator.comparingInt(OldFroggerMapForm::getFormType));
            if (newIndex >= 0) {
                // We found one that already has this form type.
                this.formType = oldFormType;
                formList.add(oldIndex, this);
                throw new RuntimeException("Another form already claims form type ID " + newFormType + ".");
            } else {
                // Put at new sorted index.
                formList.add(-(newIndex + 1), this);
            }
        }
    }

    /**
     * Setup editor UI for this form.
     * @param manager The manager to setup the editor UI for.
     * @param editor  The editor to setup the editor UI using.
     */
    public void setupEditor(OldFroggerFormUIManager manager, GUIEditorGrid editor) {
        String formName = getName();
        editor.addLabel("Form Name", formName != null ? formName : "Unknown");
        editor.addIntegerField("Form ID", this.formType, newFormType -> {
            setFormType(newFormType);
            manager.updateEditor();
        }, newFormType -> newFormType >= 0 && newFormType < getMap().getFormInstancePacket().getFormTableSize());

        // Display MOF Selector
        OldFroggerLevelTableEntry levelTableEntry = this.map.getLevelTableEntry();
        WADFile wadFile = levelTableEntry != null ? levelTableEntry.getWadFile() : null;
        WADEntry currentWadEntry = wadFile != null && wadFile.getFiles().size() > this.mofId && this.mofId >= 0
                ? wadFile.getFiles().get(this.mofId) : null;

        if (wadFile != null) {
            List<WADEntry> wadEntries = new ArrayList<>(wadFile.getFiles());
            wadEntries.removeIf(entry -> !entry.getFileEntry().hasExtension("XMR"));
            editor.addSelectionBox("Model", currentWadEntry, wadEntries, newWadEntry -> {
                this.mofId = wadFile.getFiles().indexOf(newWadEntry);
                // TODO: Update Entity Viewer
            }).setConverter(new AbstractIndexStringConverter<>(wadEntries, (index, entry) -> Utils.stripExtension(entry.getDisplayName())));
        } else {
            editor.addUnsignedFixedShort("WAD File Index", this.mofId, newValue -> this.mofId = newValue, 1);
        }

        // Show form data entries.
        for (int i = 0; i < this.formDataEntries.size(); i++) {
            final int formDataIndex = i;
            editor.addSeparator();
            editor.addBoldLabelButton("Form Entry #" + (i + 1), "Delete", () -> {
                this.formDataEntries.remove(formDataIndex);
                manager.updateEditor();
            });
            this.formDataEntries.get(i).setupEditor(manager, editor);
        }

        // Add a button to create a new form entry.
        editor.addSeparator();
        editor.addButton("Create Form Entry", () -> {
            this.formDataEntries.add(new OldFroggerMapFormDataEntry(getGameInstance()));
            manager.updateEditor();
        });
    }

    /**
     * Gets the mof file associated with the form, if it can be found.
     */
    public WADEntry getMofFileEntry() {
        if (this.map == null)
            return null;

        OldFroggerLevelTableEntry levelTableEntry = this.map.getLevelTableEntry();
        if (levelTableEntry == null) {
            System.out.println("Couldn't get level table entry for " + this.map.getFileDisplayName() + ", which prevents getting the mof file for a form.");
            return null;
        }

        WADFile wadFile = levelTableEntry.getWadFile();
        if (wadFile == null) {
            System.out.println("Couldn't get WAD from the level table entry for " + this.map.getFileDisplayName() + ", which prevents getting the mof file for a form.");
            return null;
        }

        if (this.mofId < 0 || this.mofId >= wadFile.getFiles().size()) {
            System.out.println("Couldn't get file " + this.mofId + " from the WAD file '" + wadFile.getFileDisplayName() + "', which prevents getting the mof file for a form.");
            return null;
        }

        return wadFile.getFiles().get(this.mofId);
    }

    /**
     * Gets the mof file associated with the form, if it can be found.
     */
    public MOFHolder getMofFile() {
        WADEntry wadEntry = getMofFileEntry();
        if (wadEntry == null)
            return null;

        if (!(wadEntry.getFile() instanceof MOFHolder)) {
            System.out.println("The form specified file '" + wadEntry.getDisplayName() + "' as its MOF, but this seems to not actually be a MOF.");
            return null;
        }

        return (MOFHolder) wadEntry.getFile();
    }

    /**
     * Represents a form data entry in old Frogger.
     */
    @Getter
    public static class OldFroggerMapFormDataEntry extends SCGameData<OldFroggerGameInstance> {
        private OldFroggerReactionType reaction = OldFroggerReactionType.Nothing;
        private final int[] reactionData = new int[3];
        private short numberOfHeights;
        // TODO: Ideas, Reaction Type, some method of identifying what entity this is (probably hopefully?)
        // TODO: Potentially other enums seen in ENTITIES.TXT

        public OldFroggerMapFormDataEntry(OldFroggerGameInstance instance) {
            super(instance);
        }

        @Override
        public void load(DataReader reader) {
            this.reaction = OldFroggerReactionType.values()[reader.readUnsignedShortAsInt()];
            for (int i = 0; i < this.reactionData.length; i++)
                this.reactionData[i] = reader.readUnsignedShortAsInt();

            this.numberOfHeights = reader.readUnsignedByteAsShort(); // TODO: READ HEIGHTS, NOT JUST THEIR COUNT. IS THIS EVER NON-ZERO? MULTIPLAYER1.MAP
            reader.alignRequireEmpty(4);
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeUnsignedShort(this.reaction != null ? this.reaction.ordinal() : 0);
            for (int i = 0; i < this.reactionData.length; i++)
                writer.writeUnsignedShort(this.reactionData[i]);

            writer.writeUnsignedByte(this.numberOfHeights);
            writer.align(4);
        }

        /**
         * Setup editor UI for this form data entry.
         * @param manager The manager to set up the editor UI for.
         * @param editor  The editor to set up the editor UI using.
         */
        public void setupEditor(OldFroggerFormUIManager manager, GUIEditorGrid editor) {
            OldFroggerEditorUtils.setupReactionEditor(editor, this.reaction, this.reactionData, newValue -> this.reaction = newValue);

            // TODO: HRMM.
            editor.addUnsignedFixedShort("Number of Heights", this.numberOfHeights, newValue -> this.numberOfHeights = (short) (int) newValue, 1); //  TODO: Replace this once we support heights.
        }
    }
}