package net.highwayfrogs.editor.games.sony.medievil.map.packet.pathchain;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.games.sony.SCGameObject;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.map.packet.spline.MediEvilMap2DSpline;
import net.highwayfrogs.editor.gui.components.propertylist.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.StringUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.AppendInfoLoggerWrapper;

import java.util.*;

/**
 * Tracks the connections of a path chain to other path chains.
 * Created by Kneesnap on 2/18/2026.
 */
public class MediEvilMapPathChainConnections extends SCGameObject<MediEvilGameInstance> implements IPropertyListCreator, Collection<MediEvilMapPathChain> {
    @Getter @NonNull private final MediEvilMapPathChain ownerPathChain;
    @Getter @NonNull private final String name;
    @NonNull private final List<MediEvilMapPathChain> connections = new ArrayList<>();

    private byte tempConnectionDirections;
    int tempChainIdOffset = -1;
    byte[] tempChainIds;

    public static final int MAXIMUM_CONNECTIONS = 8; // There can be no more than 8 connections because the game only tracks the connection direction in a byte.

    public MediEvilMapPathChainConnections(@NonNull MediEvilMapPathChain ownerPathChain, @NonNull String name) {
        super(ownerPathChain.getGameInstance());
        this.ownerPathChain = ownerPathChain;
        this.name = name;
    }

    @Override
    public ILogger getLogger() {
        return new AppendInfoLoggerWrapper(this.ownerPathChain.getLogger(), this.name + "Chains", AppendInfoLoggerWrapper.TEMPLATE_COMMA_SEPARATOR);
    }

    /**
     * Gets the direction of a connection at the given index.
     * @param connectionIndex the index of the connection to obtain
     * @return connectionDirection
     */
    public MediEvilMapPathDirection getDirection(int connectionIndex) {
        if (connectionIndex < 0 || connectionIndex >= this.connections.size())
            throw new IndexOutOfBoundsException("connectionIndex (" + connectionIndex + ") must be within [0, " + this.connections.size() + ").");

        if (this == this.ownerPathChain.getPreviousChains()) {
            MediEvilMapPathChain prevPathChain = this.connections.get(connectionIndex);
            if (this.ownerPathChain == prevPathChain) // If connected to self...
                return MediEvilMapPathDirection.PATH_ENDS;

            SVector firstSubDivision = this.ownerPathChain.getPathSplines().get(0).getSubDivisions().get(0);

            MediEvilMap2DSpline prevLastSpline = prevPathChain.getPathSplines().get(prevPathChain.getPathSplines().size() - 1);
            boolean prevChainConnectedAtEnd = firstSubDivision.equals(prevLastSpline.getSubDivisions().get(prevLastSpline.getSubDivisions().size() - 1)); // This previous chain is connected at its end.
            SVector prevFirstSubDivision = prevPathChain.getPathSplines().get(0).getSubDivisions().get(0);
            boolean prevChainConnectedAtStart = firstSubDivision.equals(prevFirstSubDivision); // This previous chain is connected at its start.

            if (prevChainConnectedAtEnd && prevChainConnectedAtStart) {
                // If the same node is connected twice, the first one is "PATH_STARTS", the second one is "PATH_ENDS".
                if (connectionIndex > this.connections.indexOf(prevPathChain)) {
                    return MediEvilMapPathDirection.PATH_ENDS;
                } else {
                    return MediEvilMapPathDirection.PATH_STARTS;
                }
            } else if (prevChainConnectedAtEnd) {
                return MediEvilMapPathDirection.PATH_ENDS;
            } else if (prevChainConnectedAtStart) {
                return MediEvilMapPathDirection.PATH_STARTS;
            }
        } else if (this == this.ownerPathChain.getNextChains()) {
            MediEvilMapPathChain nextPathChain = this.connections.get(connectionIndex);
            if (this.ownerPathChain == nextPathChain) // If connected to self...
                return MediEvilMapPathDirection.PATH_STARTS;

            MediEvilMap2DSpline lastSpline = this.ownerPathChain.getPathSplines().get(this.ownerPathChain.getPathSplines().size() - 1);
            SVector lastSubDivision = lastSpline.getSubDivisions().get(lastSpline.getSubDivisions().size() - 1);

            boolean nextChainConnectedAtStart = lastSubDivision.equals(nextPathChain.getPathSplines().get(0).getSubDivisions().get(0)); // This next chain is connected at its start.
            MediEvilMap2DSpline nextLastSpline = nextPathChain.getPathSplines().get(nextPathChain.getPathSplines().size() - 1);
            boolean nextChainConnectedAtEnd = lastSubDivision.equals(nextLastSpline.getSubDivisions().get(nextLastSpline.getSubDivisions().size() - 1)); // The next chain is connected at its end.

            if (nextChainConnectedAtStart && nextChainConnectedAtEnd) {
                // If the same node is connected twice, the first one is "PATH_STARTS", the second one is "PATH_ENDS".
                if (connectionIndex > this.connections.indexOf(nextPathChain)) {
                    return MediEvilMapPathDirection.PATH_ENDS;
                } else {
                    return MediEvilMapPathDirection.PATH_STARTS;
                }
            } else if (nextChainConnectedAtEnd) {
                return MediEvilMapPathDirection.PATH_ENDS;
            } else if (nextChainConnectedAtStart) {
                return MediEvilMapPathDirection.PATH_STARTS;
            }
        } else {
            throw new UnsupportedOperationException("MediEvilMapPathChainConnections could not identify if it was the 'previous' tracker or the 'next' tracker.");
        }

        // TODO: This triggered when expanding path chain node zero.previousConnections in GY1_DATA.MAP
        throw new IllegalStateException(this.ownerPathChain.getPathChainPacket().getLoggerInfo() + "/" + this.name + "Chains[" + connectionIndex + "] did not appear to be connected to the pathChain.");
    }

    private byte generateConnectionDirectionByte() {
        byte result = 0;
        for (int i = 0; i < this.connections.size(); i++)
            if (getDirection(i) == MediEvilMapPathDirection.PATH_ENDS) // bit true.
                result |= (byte) (1 << i);

        return result;
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        for (int i = 0; i < this.connections.size(); i++) {
            MediEvilMapPathChain pathChain = this.connections.get(i);
            propertyList.addString(pathChain, this.name + "[" + i + ",type=" + getDirection(i) + "]", "ID: " + pathChain.getId());
        }
    }

    /**
     * Add the contents as a single entry to the property list.
     * @param propertyList the property list to add to
     * @return the added node.
     */
    public PropertyListNode addNestedPropertyList(PropertyListNode propertyList) {
        return propertyList.addString(this, StringUtils.capitalize(this.name) + " Connections", String.valueOf(this.connections.size()));
    }

    void loadChainIdOffset(DataReader reader) {
        if (this.tempChainIdOffset <= 0) // This is not observed to be 0/empty.
            throw new RuntimeException("Cannot load " + this.name + "ChainIdOffset, the pointer " + NumberUtils.toHexString(this.tempChainIdOffset) + " is invalid.");

        requireReaderIndex(reader, this.tempChainIdOffset, "Expected " + this.name + "ChainIdOffset");
        this.tempChainIdOffset = reader.readInt();
        this.tempConnectionDirections = reader.readByte();
        reader.alignRequireByte((byte) 0xFF, Constants.INTEGER_SIZE);
    }

    void saveChainIdOffset(DataWriter writer) {
        if (this.tempChainIdOffset <= 0) // This is not observed to be 0/empty.
            throw new RuntimeException("Cannot save " + this.name + "ChainIdOffset, the pointer " + NumberUtils.toHexString(this.tempChainIdOffset) + " is invalid.");

        writer.writeAddressTo(this.tempChainIdOffset);
        this.tempChainIdOffset = writer.writeNullPointer();
        writer.writeByte(generateConnectionDirectionByte());
        writer.align(Constants.INTEGER_SIZE, (byte) 0xFF);
    }

    void loadChainIds(DataReader reader) {
        if (this.tempChainIdOffset <= 0) // This is not observed to be 0/empty.
            throw new RuntimeException("Cannot load " + this.name + "ChainIds, the pointer " + NumberUtils.toHexString(this.tempChainIdOffset) + " is invalid.");

        requireReaderIndex(reader, this.tempChainIdOffset, "Expected " + this.name + "ChainIds");
        this.tempChainIdOffset = -1;
        reader.readBytes(this.tempChainIds);
    }

    void saveChainIds(DataWriter writer) {
        if (this.tempChainIdOffset <= 0) // This is not observed to be 0/empty.
            throw new RuntimeException("Cannot save " + this.name + "ChainIds, the pointer " + NumberUtils.toHexString(this.tempChainIdOffset) + " is invalid.");

        writer.writeAddressTo(this.tempChainIdOffset);
        this.tempChainIdOffset = -1;
        for (int i = 0; i < this.connections.size(); i++)
            writer.writeUnsignedByte((short) this.connections.get(i).getId());
    }

    void resolveChains() {
        if (this.tempChainIds == null || this.tempChainIdOffset != -1)
            throw new RuntimeException("Cannot resolve " + this.name + " chains, the data was not setup properly.");

        this.connections.clear();
        List<MediEvilMapPathChain> pathChains = this.ownerPathChain.getPathChainPacket().getPathChainNodes();
        for (int i = 0; i < this.tempChainIds.length; i++) {
            int pathChainIndex = (this.tempChainIds[i] & 0xFF);
            if (pathChainIndex >= pathChains.size())
                throw new IllegalArgumentException("Invalid pathChainIndex: " + pathChainIndex);

            this.connections.add(pathChains.get(pathChainIndex));
        }

        if (this.connections.size() > MAXIMUM_CONNECTIONS)
            getLogger().severe("There were %d chains loaded, which exceeds the supported maximum of %d!", this.connections.size(), MAXIMUM_CONNECTIONS);

        this.tempChainIds = null;
    }

    void validateConnectionDirections() {
        byte chainDirections = generateConnectionDirectionByte();
        if (chainDirections != this.tempConnectionDirections) {
            // Sometimes, the byte is wrong when just the same connection is present twice, and has both directions, but in a different order from the original game data.
            // We should not warn in that case.
            boolean wrongOrderOfDuplicatePathChain = true;
            MediEvilMapPathChain lastMismatchPathChain = null;
            for (int i = 0; i < MAXIMUM_CONNECTIONS; i++) {
                if ((chainDirections & (1 << i)) == (this.tempConnectionDirections & (1 << i)))
                    continue;

                if (lastMismatchPathChain != null) {
                    MediEvilMapPathChain pathChain = this.connections.get(i);
                    if (pathChain != lastMismatchPathChain)
                        wrongOrderOfDuplicatePathChain = false;
                    lastMismatchPathChain = null;
                } else {
                    lastMismatchPathChain = this.connections.get(i);
                }
            }

            if (lastMismatchPathChain != null || !wrongOrderOfDuplicatePathChain) {
                getLogger().warning("FrogLord calculated the connection directions to be %02X, but the game data reports %02X.", chainDirections, this.tempConnectionDirections);

                // TODO: Do we want to keep this debug code?
                for (int i = 0; i < MAXIMUM_CONNECTIONS; i++) {
                    if ((chainDirections & (1 << i)) == (this.tempConnectionDirections & (1 << i)))
                        continue;

                    getLogger().warning("[Connection %d]:", i);

                    if (this == this.ownerPathChain.getPreviousChains()) {
                        getLogger().warning(" Chains: Previous");
                        MediEvilMapPathChain prevPathChain = this.connections.get(i);

                        SVector firstSubDivision = this.ownerPathChain.getPathSplines().get(0).getSubDivisions().get(0);
                        getLogger().warning(" Splines: %d", this.ownerPathChain.getPathSplines().size());
                        getLogger().warning(" First Spline SubDivCount: %d", this.ownerPathChain.getPathSplines().get(0).getSubDivisions().size());
                        getLogger().warning(" First SubDivision: %s", firstSubDivision);

                        SVector prevFirstSubDivision = prevPathChain.getPathSplines().get(0).getSubDivisions().get(0);
                        getLogger().warning(" PATH_STARTS: %b (%s)", firstSubDivision.equals(prevFirstSubDivision), prevFirstSubDivision);

                        MediEvilMap2DSpline prevLastSpline = prevPathChain.getPathSplines().get(prevPathChain.getPathSplines().size() - 1);
                        SVector prevLastSplineSubDiv = prevLastSpline.getSubDivisions().get(prevLastSpline.getSubDivisions().size() - 1);
                        getLogger().warning(" PATH_ENDS: %b (%s)", firstSubDivision.equals(prevLastSplineSubDiv), prevLastSplineSubDiv);
                    } else if (this == this.ownerPathChain.getNextChains()) {
                        getLogger().warning(" Chains: Next");
                        MediEvilMapPathChain nextPathChain = this.connections.get(i);

                        MediEvilMap2DSpline lastSpline = this.ownerPathChain.getPathSplines().get(this.ownerPathChain.getPathSplines().size() - 1);
                        SVector lastSubDivision = lastSpline.getSubDivisions().get(lastSpline.getSubDivisions().size() - 1);
                        getLogger().warning(" Splines: %d", this.ownerPathChain.getPathSplines().size());
                        getLogger().warning(" Last Spline SubDivCount: %d", lastSpline.getSubDivisions().size());
                        getLogger().warning(" Last SubDivision: %s", lastSubDivision);

                        // This next chain is connected at its start.
                        getLogger().warning(" PATH_STARTS: %b (%s)", lastSubDivision.equals(nextPathChain.getPathSplines().get(0).getSubDivisions().get(0)), nextPathChain.getPathSplines().get(0).getSubDivisions().get(0));

                        // This previous chain is connected at its end.
                        MediEvilMap2DSpline nextLastSpline = nextPathChain.getPathSplines().get(nextPathChain.getPathSplines().size() - 1);
                        getLogger().warning(" PATH_ENDS: %b (%s)", lastSubDivision.equals(nextLastSpline.getSubDivisions().get(nextLastSpline.getSubDivisions().size() - 1)), nextLastSpline.getSubDivisions().get(nextLastSpline.getSubDivisions().size() - 1));
                    }
                }
            }
        }

        this.tempConnectionDirections = -1;
    }

    /**
     * Gets the path chain connection at the given index
     * @param connectionIndex the index to get the connection from
     * @return pathChain
     */
    public MediEvilMapPathChain get(int connectionIndex) {
        if (connectionIndex < 0 || connectionIndex >= this.connections.size())
            throw new IndexOutOfBoundsException("connectionIndex (" + connectionIndex + ") must be within [0, " + this.connections.size() + ").");

        return this.connections.get(connectionIndex);
    }

    @Override
    public int size() {
        return this.connections.size();
    }

    @Override
    public boolean isEmpty() {
        return this.connections.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return this.connections.contains(o);
    }

    @Override
    public Iterator<MediEvilMapPathChain> iterator() {
        return Collections.unmodifiableList(this.connections).iterator();
    }

    @Override
    public Object[] toArray() {
        return this.connections.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return this.connections.toArray(a);
    }

    @Override
    public boolean add(MediEvilMapPathChain pathChain) {
        if (pathChain == null)
            throw new NullPointerException("pathChain");
        if (this.connections.contains(pathChain))
            return false; // Prevent adding duplicates.
        if (this.connections.size() >= MAXIMUM_CONNECTIONS)
            throw new UnsupportedOperationException("There can only be a maximum of " + MAXIMUM_CONNECTIONS + " \"" + this.name + "\" connections per path chain node!");

        return this.connections.add(pathChain);
    }

    @Override
    public boolean remove(Object o) {
        return (o instanceof MediEvilMapPathChainPacket) && this.connections.remove(o);
    }

    @Override
    @SuppressWarnings("SlowListContainsAll") // We must implement the method.
    public boolean containsAll(Collection<?> c) {
        return this.connections.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends MediEvilMapPathChain> c) {
        Iterator<?> iterator = c.iterator();
        boolean modified = false;
        while (iterator.hasNext())
            if (add((MediEvilMapPathChain) iterator.next()))
                modified = true;

        return modified;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        Iterator<?> iterator = c.iterator();
        boolean modified = false;
        while (iterator.hasNext())
            if (remove(iterator.next()))
                modified = true;

        return modified;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return this.connections.retainAll(c);
    }

    @Override
    public void clear() {
        this.connections.clear();
    }
}
