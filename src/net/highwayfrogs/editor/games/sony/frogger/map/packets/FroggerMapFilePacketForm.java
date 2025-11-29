package net.highwayfrogs.editor.games.sony.frogger.map.packets;

import javafx.scene.control.Alert.AlertType;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.FroggerMapEntity;
import net.highwayfrogs.editor.games.sony.frogger.map.data.form.FroggerFormGrid;
import net.highwayfrogs.editor.games.sony.frogger.map.data.form.FroggerOldMapForm;
import net.highwayfrogs.editor.games.sony.frogger.map.data.form.IFroggerFormEntry;
import net.highwayfrogs.editor.games.sony.shared.SCChunkedFile;
import net.highwayfrogs.editor.games.sony.shared.SCChunkedFile.SCFilePacket;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents form entity configuration data.
 * Created by Kneesnap on 5/25/2024.
 */
public class FroggerMapFilePacketForm extends FroggerMapFilePacket {
    public static final String IDENTIFIER = "FORM";
    private final List<FroggerFormGrid> forms = new ArrayList<>();
    private final List<FroggerFormGrid> immutableForms = Collections.unmodifiableList(this.forms);
    @Getter private final List<FroggerOldMapForm> oldForms = new ArrayList<>();

    public FroggerMapFilePacketForm(FroggerMapFile parentFile) {
        super(parentFile, IDENTIFIER);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        clear();
        int formCount = reader.readUnsignedShortAsInt();
        reader.skipBytesRequireEmpty(Constants.SHORT_SIZE); // Padding.
        int formPointerList = reader.getIndex();
        reader.setIndex(formPointerList + (formCount * Constants.POINTER_SIZE));
        boolean isOldFormFormat = getMapConfig().isOldFormFormat();
        for (int i = 0; i < formCount; i++) {
            // Read from the pointer list.
            reader.jumpTemp(formPointerList);
            int nextFormStartAddress = reader.readInt();
            formPointerList = reader.getIndex();
            reader.jumpReturn();

            // Read form.
            reader.requireIndex(getLogger(), nextFormStartAddress, "Expected FroggerFormGrid");
            if (isOldFormFormat) {
                FroggerOldMapForm oldForm = new FroggerOldMapForm(getParentFile());
                this.oldForms.add(oldForm);
                oldForm.load(reader);
            } else {
                FroggerFormGrid form = new FroggerFormGrid(getParentFile());
                this.forms.add(form);
                form.load(reader);
            }
        }
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        writer.writeUnsignedShort(this.forms.size()); // formCount
        writer.writeNull(Constants.SHORT_SIZE); // Padding.

        // Write slots for pointers to the form data.
        int formPointerListAddress = writer.getIndex();
        for (int i = 0; i < this.forms.size(); i++)
            writer.writeNullPointer();

        // Write the forms.
        boolean isOldFormat = getMapConfig().isOldFormFormat();
        for (int i = 0; i < (isOldFormat ? this.oldForms : this.forms).size(); i++) {
            // Write the pointer to the form we're about to save.
            int nextFormStartAddress = writer.getIndex();
            writer.jumpTemp(formPointerListAddress);
            writer.writeInt(nextFormStartAddress);
            formPointerListAddress = writer.getIndex();
            writer.jumpReturn();

            // Write form data.
            (isOldFormat ? this.oldForms : this.forms).get(i).save(writer);
        }
    }

    @Override
    public void clear() {
        this.forms.clear();
        this.oldForms.clear();
    }

    @Override
    public void copyAndConvertData(SCFilePacket<? extends SCChunkedFile<FroggerGameInstance>, FroggerGameInstance> newChunk) {
        if (!(newChunk instanceof FroggerMapFilePacketForm))
            throw new ClassCastException("The provided chunk was of type " + Utils.getSimpleName(newChunk) + " when " + FroggerMapFilePacketForm.class.getSimpleName() + " was expected.");

        FroggerMapFilePacketForm newFormChunk = (FroggerMapFilePacketForm) newChunk;
        for (int i = 0; i < this.forms.size(); i++) {
            FroggerFormGrid oldFormGrid = this.forms.get(i);
            newFormChunk.addFormGrid(oldFormGrid.clone(newFormChunk.getParentFile()));
        }
    }

    @Override
    public int getKnownStartAddress() {
        return getParentFile().getHeaderPacket().getFormPacketAddress();
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        propertyList.add("Form Count", this.oldForms.size() + this.forms.size());
    }

    /**
     * Gets the form grids tracked by the packet.
     */
    public List<FroggerFormGrid> getForms() {
        return this.immutableForms;
    }

    /**
     * When a map file is first read, it contains form grid data for each entity.
     * However, it has been determined that this data is not actually per-level, but per-form.
     * Specifically in the original, each entry in the "form book" has its own unique form grid, but when a level is exported to .MAP from .iv, the form grid was taken from the "form book", and stored in the per-level data.
     * This function reverses that process, replacing local form grid data (when feasible) with the shared form grid.
     */
    public void convertLocalFormToGlobalForm(int formGridIndex, IFroggerFormEntry formEntry) {
        if (formGridIndex < 0 || formGridIndex >= this.forms.size())
            throw new IllegalArgumentException("Invalid formGridIndex: " + formGridIndex);

        FroggerFormGrid formGrid = this.forms.get(formGridIndex);
        if (formGrid.getFormEntry() == null) {
            FroggerFormGrid replacementEntry = formGrid.initFormEntry(formEntry);
            if (replacementEntry != null && replacementEntry != formGrid)
                this.forms.set(formGridIndex, replacementEntry);
        } else if (formGrid.getFormEntry() != formEntry) {
            // This is not expected / should not happen.
            formGrid.getLogger().warning("Form has more than one formEntry linked to it! (%s and %s)", formGrid.getFormEntry().getFormTypeName(), formEntry.getFormTypeName());
        }
    }

    /**
     * Adds a form grid to the end of the list.
     * @param formGrid the form grid to add
     * @return the index which the formGrid was added to the list at
     */
    public int addFormGrid(FroggerFormGrid formGrid) {
        if (formGrid == null)
            return -1;

        for (int i = 0; i < this.forms.size(); i++)
            if (this.forms.get(i) == formGrid) // NOTE: Don't use equals() here, since we do want to allow multiple empty form grids.
                return i;

        int formIndex = this.forms.size();
        this.forms.add(formGrid);
        return formIndex;
    }

    /**
     * Attempts to remove the form grid from the packet.
     * @param formGrid the form grid to remove
     * @return if the removal was successful
     */
    public boolean removeFormGrid(FroggerFormGrid formGrid) {
        int formGridIndex = this.forms.indexOf(formGrid);
        if (formGridIndex < 0)
            return false; // Wasn't in the list.

        // Ensure no entities are using it.
        int usageCount = 0;
        List<FroggerMapEntity> entities = getParentFile().getEntityPacket().getEntities();
        for (int i = 0; i < entities.size(); i++)
            if (entities.get(i).getFormGridId() == formGridIndex)
                usageCount++;

        if (usageCount > 0) {
            FXUtils.makePopUp("Cannot remove form because it is actively used by " + usageCount + (usageCount != 1 ? " entities." : " entity."), AlertType.WARNING);
            return false;
        }

        // Remove the form, and update all entities to not break their IDs after the removal.
        this.forms.remove(formGridIndex);
        for (int i = 0; i < entities.size(); i++)
            entities.get(i).onFormGridRemoved(formGridIndex);

        return true;
    }
}