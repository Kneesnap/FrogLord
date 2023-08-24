package net.highwayfrogs.editor.games.tgq.entity;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.tgq.IInfoWriter.IMultiLineInfoWriter;
import net.highwayfrogs.editor.games.tgq.TGQChunkedFile;
import net.highwayfrogs.editor.games.tgq.kcClassID;
import net.highwayfrogs.editor.games.tgq.toc.kcCResource;
import net.highwayfrogs.editor.utils.Utils;

import java.util.function.Function;

/**
 * Implements the 'kcBaseDesc' struct.
 * This is an object template. It contains data to be loaded into an instance object.
 * The data contained depends on the classID which is specified.
 * The game calls this "Desc", which is believed to be short for description.
 * Created by Kneesnap on 8/23/2023.
 */
@Getter
public abstract class kcBaseDesc extends GameObject implements IMultiLineInfoWriter {
    @Setter private TGQChunkedFile parentFile;

    /**
     * Gets the ID of the class which this template contains data to fill.
     */
    protected abstract int getTargetClassID();

    /**
     * Test if alternative class IDs are allowed.
     */
    public boolean allowAlternativeClassID() {
        return false;
    }

    @Override
    public void load(DataReader reader) {
        int classID = reader.readInt();
        reader.skipInt(); // Size.
        if (classID != getTargetClassID()) {
            boolean shouldError = true;

            // Allow using alternative class ID?
            if (allowAlternativeClassID()) {
                kcClassID id = kcClassID.getClassById(classID);
                if (id != null && id.getAlternateClassId() == classID)
                    shouldError = false;
            }

            if (shouldError)
                throw new RuntimeException("Read an unexpected target class ID: " + Utils.to0PrefixedHexString(classID) + " for " + getClass().getSimpleName() + " in " + (this.parentFile != null ? this.parentFile.getDebugName() : ""));
        }
    }

    @Override
    public final void save(DataWriter writer) {
        int writtenDataStartIndex = writer.getIndex();
        writer.writeInt(getTargetClassID());
        int writtenDataSizeAddress = writer.writeNullPointer();

        // Write data.
        saveData(writer);

        // Write correct size.
        writer.writeAddressAt(writtenDataSizeAddress, writer.getIndex() - writtenDataStartIndex);
    }

    /**
     * Writes template/description data to the writer.
     * @param writer The writer to write data to.
     */
    protected abstract void saveData(DataWriter writer);

    /**
     * Write the asset name to the builder to a single line.
     * @param builder      The builder to write to.
     * @param padding      The line padding data.
     * @param prefix       The prefix to write.
     * @param resourceHash The hash value to lookup.
     */
    protected StringBuilder writeAssetLine(StringBuilder builder, String padding, String prefix, int resourceHash) {
        return writeAssetInfo(builder, padding, prefix, resourceHash, kcCResource::getName).append(Constants.NEWLINE);
    }

    /**
     * Write asset information to the builder. The information written is specified via the function.
     * If the asset isn't found, the hash is written instead.
     * @param builder      The builder to write to.
     * @param padding      The line padding data.
     * @param prefix       The prefix to write.
     * @param resourceHash The hash value to lookup.
     * @param getter       The function to turn the resource into a string.
     * @param <TResource>  The resource type to lookup.
     */
    protected <TResource extends kcCResource> StringBuilder writeAssetInfo(StringBuilder builder, String padding, String prefix, int resourceHash, Function<TResource, String> getter) {
        builder.append(padding).append(prefix).append(": ");

        TResource resource = this.parentFile != null ? this.parentFile.getResourceByHash(resourceHash) : null;
        if (resource != null) {
            builder.append(getter.apply(resource));
        } else if (resourceHash != 0 && resourceHash != -1) {
            builder.append(Utils.to0PrefixedHexString(resourceHash));
        } else {
            builder.append("None");
        }

        return builder;
    }
}
