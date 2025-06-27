package net.highwayfrogs.editor.games.sony.frogger.map.packets;

import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.FroggerMapLight;
import net.highwayfrogs.editor.games.sony.shared.SCChunkedFile;
import net.highwayfrogs.editor.games.sony.shared.SCChunkedFile.SCFilePacket;
import net.highwayfrogs.editor.games.sony.shared.misc.MRLightType;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents lighting data.
 * Created by Kneesnap on 5/25/2024.
 */
public class FroggerMapFilePacketLight extends FroggerMapFilePacket {
    public static final String IDENTIFIER = "LITE";
    private final List<FroggerMapLight> lights = new ArrayList<>();
    private final List<FroggerMapLight> immutableLights = Collections.unmodifiableList(this.lights);

    /**
     * MR_LIGHT.C/MRUpdateViewportLightMatrix shows that there's a maximum of three parallel/point lights allowed.
     * However, because point lights and spotlights are skipped in-game, that leaves just parallel lights for that limit.
     */
    public static final int MAX_NON_AMBIENT_LIGHT_COUNT = 3;

    public FroggerMapFilePacketLight(FroggerMapFile parentFile) {
        super(parentFile, IDENTIFIER);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        this.lights.clear();
        int lightCount = reader.readInt();
        for (int i = 0; i < lightCount; i++) {
            FroggerMapLight newLight = new FroggerMapLight(getParentFile());
            newLight.load(reader);
            this.lights.add(newLight);
        }
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        writer.writeInt(this.lights.size());
        for (int i = 0; i < this.lights.size(); i++)
            this.lights.get(i).save(writer);
    }

    @Override
    public void clear() {
        this.lights.clear();
    }

    @Override
    public void copyAndConvertData(SCFilePacket<? extends SCChunkedFile<FroggerGameInstance>, FroggerGameInstance> newChunk) {
        if (!(newChunk instanceof FroggerMapFilePacketLight))
            throw new ClassCastException("The provided chunk was of type " + Utils.getSimpleName(newChunk) + " when " + FroggerMapFilePacketLight.class.getSimpleName() + " was expected.");

        FroggerMapFilePacketLight newLightChunk = (FroggerMapFilePacketLight) newChunk;
        for (int i = 0; i < this.lights.size(); i++) {
            FroggerMapLight oldLight = this.lights.get(i);
            newLightChunk.addLight(oldLight.clone(newLightChunk.getParentFile()));
        }
    }

    @Override
    public int getKnownStartAddress() {
        return getParentFile().getGraphicalPacket().getLightPacketAddress();
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList.add("Light Count", this.lights.size());
        return propertyList;
    }

    /**
     * Gets the lights tracked by this packet.
     */
    public List<FroggerMapLight> getLights() {
        return this.immutableLights;
    }

    /**
     * Returns true iff there is an ambient light already registered.
     * This indicates it is not possible to add another ambient light.
     */
    public boolean hasAmbientLight() {
        for (int i = 0; i < this.lights.size(); i++)
            if (this.lights.get(i).getLightType() == MRLightType.AMBIENT)
                return true; // Can't add more than one ambient light.

        return false;
    }

    /**
     * Returns true if the number of parallel lights found is at or greater than the maximum number of allowed parallel lights.
     */
    public boolean hasMaxNumberOfParallelLights() {
        int parallelLightCount = 0;
        for (int i = 0; i < this.lights.size(); i++)
            if (this.lights.get(i).getLightType() == MRLightType.PARALLEL)
                parallelLightCount++;

        return parallelLightCount >= MAX_NON_AMBIENT_LIGHT_COUNT; // Reached the limit for number of parallel lights.
    }

    /**
     * Adds the given light to the packet.
     * @param light the light to attempt to add
     * @return if the light was added successfully
     */
    public boolean addLight(FroggerMapLight light) {
        if (light == null)
            throw new NullPointerException("light");
        if (this.lights.contains(light))
            return false; // Can't add the same light twice.

        if (light.getLightType() == MRLightType.AMBIENT) {
            if (hasAmbientLight())
                return false; // Can't add more than one ambient light.
        } else if (light.getLightType() == MRLightType.PARALLEL) {
            if (hasMaxNumberOfParallelLights())
                return false; // Reached the limit for number of parallel lights.
        }
        // Point lights and spotlights are ignored in-game.

        this.lights.add(light);
        return true;
    }

    /**
     * Removes the provided light from the packet.
     * @param light the light to remove
     * @return if the light was removed successfully
     */
    public boolean removeLight(FroggerMapLight light) {
        return this.lights.remove(light);
    }
}