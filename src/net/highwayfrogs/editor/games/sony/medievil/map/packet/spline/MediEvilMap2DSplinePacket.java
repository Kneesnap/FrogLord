package net.highwayfrogs.editor.games.sony.medievil.map.packet.spline;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
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
 * Implements the MediEvil map 2D spline packet.
 * Created by Kneesnap on 2/3/2026.
 */
@Getter
public class MediEvilMap2DSplinePacket extends MediEvilMapPacket implements IPropertyListCreator {
    private final List<MediEvilMap2DSpline> splines = new ArrayList<>();

    public static final String IDENTIFIER = "2LPS"; // 'SPL2'

    public MediEvilMap2DSplinePacket(MediEvilMapFile parentFile) {
        super(parentFile, IDENTIFIER);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        short splineCount = reader.readUnsignedByteAsShort();
        reader.align(Constants.INTEGER_SIZE); // There seems to be garbage data here.
        int splineDataStartIndex = reader.readInt();

        reader.requireIndex(getLogger(), splineDataStartIndex, "Expected splines");
        this.splines.clear();
        for (int i = 0; i < splineCount; i++) {
            MediEvilMap2DSpline newSpline = new MediEvilMap2DSpline(this);
            this.splines.add(newSpline);
            newSpline.load(reader);
        }

        // Read subdivisions.
        for (int i = 0; i < this.splines.size(); i++)
            this.splines.get(i).loadSubDivisions(reader);
    }

    @Override
    protected void loadBodySecondPass(DataReader reader, int endIndex) {
        // Resolve object references.
        for (int i = 0; i < this.splines.size(); i++)
            this.splines.get(i).resolveReferences();
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
        for (int i = 0; i < this.splines.size(); i++)
            this.splines.get(i).saveSubDivisions(writer);
    }

    /**
     * Validates the references found within the splines are valid.
     */
    void validateReferencesAreValid() {
        for (int i = 0; i < this.splines.size(); i++) {
            MediEvilMap2DSpline spline = this.splines.get(i);
            MediEvilMap3DSpline cameraSpline = spline.getCameraSpline();
            MediEvilMapPathChain pathChain = spline.getPathChain();
            boolean shouldHaveCameraSpline = (spline.getFlags() & MediEvilMap2DSpline.FLAG_NO_CAMERA) == 0;
            if ((cameraSpline != null) ^ shouldHaveCameraSpline)
                spline.getLogger().warning("The 2D spline (%s) was expected %s have a camera spline, but %s.",
                        spline, shouldHaveCameraSpline ? "to" : "NOT to", (cameraSpline != null ? "it had " + cameraSpline + " linked" : "it did not have one linked"));
            if (pathChain == null)
                spline.getLogger().warning("The 2D spline (%s) did not have a path chain linked!", spline);
            if (cameraSpline != null && cameraSpline.getPathSpline() != spline)
                spline.getLogger().warning("The 2D spline (%s) had an attached camera spline (%s) which was not linked to the spline!", spline, cameraSpline);
            if (pathChain != null && !pathChain.getPathSplines().contains(spline))
                spline.getLogger().warning("The 2D spline (%s) was attached to a path chain node (%s) which did not seem to know about the spline!", spline, pathChain);
        }
    }

    @Override
    public void clear() {
        this.splines.clear();
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        propertyList.addString(this::addSplines, "2D Splines", String.valueOf(this.splines.size()));
    }

    private void addSplines(PropertyListNode propertyList) {
        for (int i = 0; i < this.splines.size(); i++)
            propertyList.addProperties("Spline " + i, this.splines.get(i));
    }
}
