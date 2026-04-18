package net.highwayfrogs.editor.games.sony.medievil.map.packet.spline;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.games.sony.medievil.map.packet.MediEvilMapPacket;
import net.highwayfrogs.editor.games.sony.medievil.map.packet.pathchain.MediEvilMapPathChain;
import net.highwayfrogs.editor.gui.components.propertylist.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements the MediEvil map 3D spline packet.
 * Created by Kneesnap on 2/3/2026.
 */
@Getter
public class MediEvilMap3DSplinePacket extends MediEvilMapPacket implements IPropertyListCreator {
    private final List<MediEvilMap3DSpline> splines = new ArrayList<>();

    public static final String IDENTIFIER = "3LPS"; // 'SPL3'
    static final int EXPECTED_START_END_TRAILING_EMPTY_SUB_DIVISIONS = 2;

    public MediEvilMap3DSplinePacket(MediEvilMapFile parentFile) {
        super(parentFile, IDENTIFIER);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        short splineCount = reader.readUnsignedByteAsShort();
        reader.align(Constants.INTEGER_SIZE); // There seems to be garbage data here.
        int splineDataStartIndex = reader.readInt();

        // Read splines.
        reader.requireIndex(getLogger(), splineDataStartIndex, "Expected splines");
        this.splines.clear();
        for (int i = 0; i < splineCount; i++) // Ensure all splines have objects before load, so they can determine if they are the last spline or not.
            this.splines.add(new MediEvilMap3DSpline(this));
        for (int i = 0; i < this.splines.size(); i++)
            this.splines.get(i).load(reader);

        // Read empty subdivisions.
        int actualStartEmptySubDivisions = readEmptyVectors(reader);
        if (actualStartEmptySubDivisions != EXPECTED_START_END_TRAILING_EMPTY_SUB_DIVISIONS && shouldWarnAboutEmptySubDivisions())
            getLogger().warning("Expected %d empty sub-divisions at the start of the subDivision list, but %d were found!",
                    EXPECTED_START_END_TRAILING_EMPTY_SUB_DIVISIONS, actualStartEmptySubDivisions);

        // Read subdivisions.
        for (int i = 0; i < this.splines.size(); i++)
            this.splines.get(i).loadSubDivisions(reader);
    }

    @Override
    protected void loadBodySecondPass(DataReader reader, int endIndex) {
        // Resolve object references.
        for (int i = 0; i < this.splines.size(); i++)
            this.splines.get(i).resolveReferences();

        // Do this here since it's the last packet in the chain to resolve.
        getParentFile().getPathChainPacket().validateReferencesAreValid();
        getParentFile().getSpline2DPacket().validateReferencesAreValid();
        validateReferencesAreValid();
    }

    /**
     * Validates the references found within the splines are valid.
     */
    private void validateReferencesAreValid() {
        for (int i = 0; i < this.splines.size(); i++) {
            MediEvilMap3DSpline spline = this.splines.get(i);
            MediEvilMap2DSpline pathSpline = spline.getPathSpline();
            MediEvilMapPathChain pathChain = spline.getPathChain();
            if (pathSpline == null)
                spline.getLogger().warning("The 3D spline (%s) did not have a 2D path spline linked!", spline);
            if (pathChain == null)
                spline.getLogger().warning("The 3D spline (%s) did not have a path chain linked!", spline);
            if (pathSpline != null && pathSpline.getCameraSpline() != spline)
                spline.getLogger().warning("The 3D spline (%s) had an attached 2D path spline (%s) which was not linked to the 3D spline!", spline, pathSpline);
            if (pathSpline != null && pathChain != null && pathChain != pathSpline.getPathChain())
                spline.getLogger().warning("The 3D spline (%s) was attached to a different pathChainNode (%s) than its 2D path spline!", spline, pathChain);
            if (pathSpline != null && pathSpline.getSubDivisions().size() != spline.getSubDivisions().size())
                spline.getLogger().warning("The 3D spline (%s) had a different number of subDivisions (%d) than its 2D path spline (%d for %s)!", spline, spline.getSubDivisions().size(), pathSpline.getSubDivisions().size(), pathSpline);
        }
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        writer.writeUnsignedByte((short) this.splines.size());
        writer.align(Constants.INTEGER_SIZE); // There seems to be garbage data here.
        int splineDataStartIndex = writer.writeNullPointer();

        // Write splines.
        writer.writeAddressTo(splineDataStartIndex);
        for (int i = 0; i < this.splines.size(); i++)
            this.splines.get(i).save(writer);

        // Write subdivisions.
        writer.writeNull(SVector.PADDED_BYTE_SIZE * EXPECTED_START_END_TRAILING_EMPTY_SUB_DIVISIONS);
        for (int i = 0; i < this.splines.size(); i++)
            this.splines.get(i).saveSubDivisions(writer);
    }

    /**
     * Tests if the spline packet should warn about empty subDivisions.
     */
    boolean shouldWarnAboutEmptySubDivisions() {
        return !getGameInstance().getVersionConfig().isAtOrBeforeEctsAlpha()
                || !"FD_DATA.MAP".equals(getParentFile().getFileDisplayName());
    }

    @Override
    public void clear() {
        this.splines.clear();
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        propertyList.addString(this::addSplines, "3D Splines", String.valueOf(this.splines.size()));
    }

    private void addSplines(PropertyListNode propertyList) {
        for (int i = 0; i < this.splines.size(); i++)
            propertyList.addProperties("Spline " + i, this.splines.get(i));
    }

    static int readEmptyVectors(DataReader reader) {
        // Skip trailing empty subdivisions.
        int emptyVectorCount = 0;
        while (reader.getRemaining() >= SVector.PADDED_BYTE_SIZE) {
            int startIndex = reader.getIndex();
            reader.jumpTemp(startIndex);
            int nextValue = reader.readInt();
            short nextValue2 = reader.readShort();
            // Don't test padding.
            reader.jumpReturn();

            if (nextValue != 0 || nextValue2 != 0)
                break; // Found a non-empty vector, abort.

            emptyVectorCount++;
            reader.skipBytes(SVector.PADDED_BYTE_SIZE);
        }

        return emptyVectorCount;
    }
}
