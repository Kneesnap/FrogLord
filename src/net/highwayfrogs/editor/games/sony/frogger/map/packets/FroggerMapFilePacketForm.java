package net.highwayfrogs.editor.games.sony.frogger.map.packets;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.form.FroggerMapForm;
import net.highwayfrogs.editor.games.sony.frogger.map.data.form.FroggerOldMapForm;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents form entity configuration data.
 * Created by Kneesnap on 5/25/2024.
 */
@Getter
public class FroggerMapFilePacketForm extends FroggerMapFilePacket {
    public static final String IDENTIFIER = "FORM";
    private final List<FroggerMapForm> forms = new ArrayList<>();
    private final List<FroggerOldMapForm> oldForms = new ArrayList<>();

    public FroggerMapFilePacketForm(FroggerMapFile parentFile) {
        super(parentFile, IDENTIFIER);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        this.forms.clear();
        this.oldForms.clear();
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
            reader.requireIndex(getLogger(), nextFormStartAddress, "Expected FroggerMapForm");
            if (isOldFormFormat) {
                FroggerOldMapForm oldForm = new FroggerOldMapForm(getParentFile());
                this.oldForms.add(oldForm);
                oldForm.load(reader);
            } else {
                FroggerMapForm form = new FroggerMapForm(getParentFile());
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
    public int getKnownStartAddress() {
        return getParentFile().getHeaderPacket().getFormPacketAddress();
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList.add("Form Count", this.oldForms.size() + this.forms.size());
        return null;
    }
}