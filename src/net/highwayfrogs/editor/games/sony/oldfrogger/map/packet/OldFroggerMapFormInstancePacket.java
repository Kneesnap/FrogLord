package net.highwayfrogs.editor.games.sony.oldfrogger.map.packet;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapForm;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the form instance table in an old Frogger map file.
 * Created by Kneesnap on 12/10/2023.
 */
@Getter
public class OldFroggerMapFormInstancePacket extends OldFroggerMapPacket {
    public static final String IDENTIFIER = "FORM";

    private int formTableSize = DEFAULT_FORM_TABLE_SIZE;
    private final List<OldFroggerMapForm> forms = new ArrayList<>();

    public static final int DEFAULT_FORM_TABLE_SIZE = 253;

    public OldFroggerMapFormInstancePacket(OldFroggerMapFile parentFile) {
        super(parentFile, IDENTIFIER);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        this.formTableSize = reader.readUnsignedShortAsInt();
        int activeFormCount = reader.readUnsignedShortAsInt();

        int lastFormTypeId = Integer.MIN_VALUE;
        int endPosition = reader.getIndex();
        this.forms.clear();
        for (int i = 0; i < this.formTableSize; i++) {
            int formOffset = reader.readInt();
            if (formOffset == -1)
                continue;

            OldFroggerMapForm newForm = new OldFroggerMapForm(getParentFile(), i);

            // Load form.
            reader.jumpTemp(formOffset);
            newForm.load(reader);
            endPosition = Math.max(endPosition, reader.getIndex());
            reader.jumpReturn();

            // Register form.
            this.forms.add(newForm);

            // Check forms are sorted.
            if (lastFormTypeId >= newForm.getFormType())
                getLogger().warning("Form %s was defined after Form %d.", newForm.getFormType(), lastFormTypeId);
            lastFormTypeId = newForm.getFormType();
        }

        if (this.forms.size() != activeFormCount)
            getLogger().warning("Expected to read %d form(s), but %d were found.", activeFormCount, this.forms.size());

        // Set the reader position to the end of the form data.
        reader.setIndex(endPosition);
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        writer.writeUnsignedShort(this.formTableSize);
        writer.writeUnsignedShort(this.forms.size());

        int formPointerArrayStart = writer.getIndex();
        for (int i = 0; i < this.formTableSize; i++)
            writer.writeInt(-1);

        for (int i = 0; i < this.forms.size(); i++) {
            OldFroggerMapForm form = this.forms.get(i);

            // Update form table.
            writer.writeAddressTo(formPointerArrayStart + (form.getFormType() * Constants.INTEGER_SIZE));

            // Save form.
            form.save(writer);
        }
    }

    @Override
    public void clear() {
        this.forms.clear();
    }

    /**
     * Finds a form by its ID.
     * @param formId The form ID to lookup.
     * @return form, if one exists.
     */
    public OldFroggerMapForm getFormById(int formId) {
        for (int i = 0; i < this.forms.size(); i++) {
            OldFroggerMapForm form = this.forms.get(i);
            if (form.getFormType() == formId)
                return form;
        }

        return null;
    }

    /**
     * Change the form table size.
     * @param newTableSize The new table size to use.
     */
    public void setFormTableSize(int newTableSize) {
        if (newTableSize < 0 || newTableSize >= 65536)
            throw new IllegalArgumentException("setFormTableSize was provided an invalid form table size: " + newTableSize);

        if (this.forms.size() > 0 && this.forms.get(this.forms.size() - 1).getFormType() >= newTableSize)
            throw new IllegalArgumentException("setFormTableSize was provided a form table size (" + newTableSize + ") which couldn't hold the largest currently active form type (" + this.forms.get(this.forms.size() - 1).getFormType() + ").");

        this.formTableSize = newTableSize;
    }
}