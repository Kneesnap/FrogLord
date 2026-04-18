package net.highwayfrogs.editor.games.sony.medievil.map.packet.pathchain;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.games.sony.medievil.map.packet.MediEvilMapPacket;
import net.highwayfrogs.editor.games.sony.medievil.map.packet.spline.MediEvilMap2DSpline;
import net.highwayfrogs.editor.gui.components.propertylist.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implements the 'PCHN' MediEvil map packet.
 * Created by Kneesnap on 2/3/2026.
 */
public class MediEvilMapPathChainPacket extends MediEvilMapPacket implements IPropertyListCreator {
    final List<MediEvilMapPathChain> pathChainNodes = new ArrayList<>();
    private final List<MediEvilMapPathChain> immutablePathChainNodes = Collections.unmodifiableList(this.pathChainNodes);

    public static final String IDENTIFIER = "NHCP"; // 'PCHN'

    public MediEvilMapPathChainPacket(MediEvilMapFile parentFile) {
        super(parentFile, IDENTIFIER);
    }

    /**
     * Gets a list of all nodes tracked as part of a path chain.
     */
    public List<MediEvilMapPathChain> getPathChainNodes() {
        return this.immutablePathChainNodes;
    }

    /**
     * Adds a path chain to the packet.
     * @param pathChain the path chain to add
     * @return true iff the path chain was added successfully
     */
    public boolean addPathChain(MediEvilMapPathChain pathChain) {
        if (pathChain == null)
            throw new NullPointerException("pathChain");
        if (pathChain.getPathChainPacket() != this)
            throw new IllegalArgumentException("Cannot register pathChain intended for another packet.");
        if (this.pathChainNodes.contains(pathChain))
            return false; // Already registered here.
        if (pathChain.getPathSplines().isEmpty())
            throw new IllegalArgumentException("Cannot register a pathChain with no splines!");

        // TODO: Validate things like splines and connections.

        this.pathChainNodes.add(pathChain);
        return true;
    }

    /**
     * Remove a path chain from the packet, disconnecting any connected nodes, so they are now floating in space.
     * @param pathChain the path chain to remove/disconnect
     * @return true iff the path chain was removed.
     */
    public boolean removePathChain(MediEvilMapPathChain pathChain) {
        if (pathChain == null)
            throw new NullPointerException("pathChain");
        if (pathChain.getPathChainPacket() != this)
            return false; // Couldn't be registered here.
        if (!this.pathChainNodes.remove(pathChain))
            return false; // Wasn't registered here.

        // Disconnect the pathChain from its neighbors.
        for (int i = 0; i < pathChain.getPreviousChains().size(); i++)
            pathChain.getPreviousChains().get(i).getNextChains().remove(pathChain);
        for (int i = 0; i < pathChain.getNextChains().size(); i++)
            pathChain.getNextChains().get(i).getPreviousChains().remove(pathChain);

        // TODO: Free splines?

        return true;
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        reader.skipBytesRequireEmpty(Constants.SHORT_SIZE); // Runtime values probably.
        short chainCount = reader.readUnsignedByteAsShort();
        reader.skipByte(); // Padding (garbage)
        int chainDataStartAddress = reader.readInt();

        // Read path chains.
        reader.requireIndex(getLogger(), chainDataStartAddress, "Expected Path chain data");
        this.pathChainNodes.clear();
        for (int i = 0; i < chainCount; i++) {
            MediEvilMapPathChain newPathChain = new MediEvilMapPathChain(this);
            this.pathChainNodes.add(newPathChain);
            newPathChain.load(reader);
        }

        // Read pointer offsets.
        for (int i = 0; i < this.pathChainNodes.size(); i++)
            this.pathChainNodes.get(i).loadPathSplineIdOffset(reader);
        for (int i = 0; i < this.pathChainNodes.size(); i++)
            this.pathChainNodes.get(i).loadCameraSplineIdOffset(reader);
        for (int i = 0; i < this.pathChainNodes.size(); i++)
            this.pathChainNodes.get(i).getPreviousChains().loadChainIdOffset(reader);
        for (int i = 0; i < this.pathChainNodes.size(); i++)
            this.pathChainNodes.get(i).getNextChains().loadChainIdOffset(reader);

        // Read data.
        for (int i = 0; i < this.pathChainNodes.size(); i++)
            this.pathChainNodes.get(i).loadPathSplineIds(reader);
        for (int i = 0; i < this.pathChainNodes.size(); i++)
            this.pathChainNodes.get(i).loadCameraSplineIds(reader);
        for (int i = 0; i < this.pathChainNodes.size(); i++)
            this.pathChainNodes.get(i).getPreviousChains().loadChainIds(reader);
        for (int i = 0; i < this.pathChainNodes.size(); i++)
            this.pathChainNodes.get(i).getNextChains().loadChainIds(reader);

        // Resolve previous/next path chains.
        for (int i = 0; i < this.pathChainNodes.size(); i++)
            this.pathChainNodes.get(i).getPreviousChains().resolveChains();
        for (int i = 0; i < this.pathChainNodes.size(); i++)
            this.pathChainNodes.get(i).getNextChains().resolveChains();
    }

    @Override
    protected void loadBodySecondPass(DataReader reader, int endIndex) {
        for (int i = 0; i < this.pathChainNodes.size(); i++)
            this.pathChainNodes.get(i).resolvePathSplines();
        for (int i = 0; i < this.pathChainNodes.size(); i++)
            this.pathChainNodes.get(i).resolveCameraSplines();
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        writer.writeNull(Constants.SHORT_SIZE); // Runtime values probably.
        writer.writeUnsignedByte((short) this.pathChainNodes.size());
        writer.writeByte(Constants.NULL_BYTE); // Padding (garbage)
        int chainDataStartAddress = writer.writeNullPointer();

        // Write path chains.
        writer.writeAddressTo(chainDataStartAddress);
        for (int i = 0; i < this.pathChainNodes.size(); i++)
            this.pathChainNodes.get(i).save(writer);

        // Write pointer offsets.
        for (int i = 0; i < this.pathChainNodes.size(); i++)
            this.pathChainNodes.get(i).savePathSplineIdOffset(writer);
        for (int i = 0; i < this.pathChainNodes.size(); i++)
            this.pathChainNodes.get(i).saveCameraSplineIdOffset(writer);
        for (int i = 0; i < this.pathChainNodes.size(); i++)
            this.pathChainNodes.get(i).getPreviousChains().saveChainIdOffset(writer);
        for (int i = 0; i < this.pathChainNodes.size(); i++)
            this.pathChainNodes.get(i).getNextChains().saveChainIdOffset(writer);

        // Write data.
        for (int i = 0; i < this.pathChainNodes.size(); i++)
            this.pathChainNodes.get(i).savePathSplineIds(writer);
        for (int i = 0; i < this.pathChainNodes.size(); i++)
            this.pathChainNodes.get(i).saveCameraSplineIds(writer);
        for (int i = 0; i < this.pathChainNodes.size(); i++)
            this.pathChainNodes.get(i).getPreviousChains().saveChainIds(writer);
        for (int i = 0; i < this.pathChainNodes.size(); i++)
            this.pathChainNodes.get(i).getNextChains().saveChainIds(writer);
    }

    /**
     * Validates the references found within the path chains are valid.
     */
    public void validateReferencesAreValid() {
        for (int i = 0; i < this.pathChainNodes.size(); i++) {
            MediEvilMapPathChain pathChainNode = this.pathChainNodes.get(i);
            List<MediEvilMap2DSpline> pathSplines = pathChainNode.getPathSplines();
            for (int j = 0; j < pathSplines.size(); j++) {
                MediEvilMap2DSpline spline = pathSplines.get(j);
                if (spline.getPathChain() != pathChainNode)
                    pathChainNode.getLogger().warning("The pathChainNode (%s) had an attached path spline (%s) which was not linked to the path chain node!", pathChainNode, spline);
            }

            pathChainNode.getPreviousChains().validateConnectionDirections();
            pathChainNode.getNextChains().validateConnectionDirections();
        }
    }

    @Override
    public void clear() {
        this.pathChainNodes.clear();
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        propertyList.addString(this::addPathChains, "Path Chain Nodes", String.valueOf(this.pathChainNodes.size()));
    }

    private void addPathChains(PropertyListNode propertyList) {
        for (int i = 0; i < this.pathChainNodes.size(); i++)
            propertyList.addProperties("Path Chain Node " + i, this.pathChainNodes.get(i));
    }
}
