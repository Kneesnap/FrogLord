package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.data.GameData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter.IMultiLineInfoWriter;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResource;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.kcClassID;
import net.highwayfrogs.editor.utils.NumberUtils;

import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Implements the 'kcBaseDesc' struct.
 * This is an object template. It contains data to be loaded into an instance object.
 * The data contained depends on the classID which is specified.
 * The game calls this "Desc", which is believed to be short for description.
 * Created by Kneesnap on 8/23/2023.
 */
@Getter
public abstract class kcBaseDesc extends GameData<GreatQuestInstance> implements IMultiLineInfoWriter {
    private final kcCResource resource;

    public kcBaseDesc(kcCResource resource) {
        this(resource != null ? resource.getGameInstance() : null, resource);
    }

    private kcBaseDesc(GreatQuestInstance instance, kcCResource resource) {
        super(instance);
        this.resource = resource;
    }

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
    public Logger getLogger() {
        if (this.resource != null)
            return this.resource.getLogger();

        return super.getLogger();
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

            if (shouldError) {
                GreatQuestChunkedFile parentFile = getParentFile();
                throw new RuntimeException("Read an unexpected target class ID: " + NumberUtils.to0PrefixedHexString(classID) + " for " + getClass().getSimpleName() + " in " + (parentFile != null ? parentFile.getDebugName() : ""));
            }
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
        writer.writeIntAtPos(writtenDataSizeAddress, writer.getIndex() - writtenDataStartIndex);
    }

    /**
     * Writes template/description data to the writer.
     * @param writer The writer to write data to.
     */
    protected abstract void saveData(DataWriter writer);

    /**
     * Return true if the parent resource is named any one of the given names.
     * @param names the names to test
     * @return parent resources
     */
    public boolean isParentResourceNamed(String... names) {
        return this.resource != null && this.resource.doesNameMatch(names);
    }

    /**
     * Gets the parent chunk file containing the resource which contains this data.
     */
    public GreatQuestChunkedFile getParentFile() {
        return this.resource != null ? this.resource.getParentFile() : null;
    }

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
     * Write the asset name to the builder to a single line.
     * @param builder The builder to write to.
     * @param padding The line padding data.
     * @param prefix The prefix to write.
     * @param hashObj The hash value to lookup.
     */
    protected StringBuilder writeAssetLine(StringBuilder builder, String padding, String prefix, GreatQuestHash<?> hashObj) {
        if (hashObj == null)
            throw new NullPointerException("hashObj");

        builder.append(padding).append(prefix).append(": ");
        return builder.append(hashObj.getDisplayString(false));
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

        TResource resource = GreatQuestUtils.findResourceByHash(getParentFile(), getGameInstance(), resourceHash);
        if (resource != null) {
            builder.append(getter.apply(resource));
        } else if (resourceHash != 0 && resourceHash != -1) {
            builder.append(NumberUtils.to0PrefixedHexString(resourceHash));
        } else {
            builder.append("None");
        }

        return builder;
    }
}