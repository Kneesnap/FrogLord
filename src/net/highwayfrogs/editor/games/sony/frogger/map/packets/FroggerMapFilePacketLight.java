package net.highwayfrogs.editor.games.sony.frogger.map.packets;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.FroggerMapLight;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents lighting data.
 * Created by Kneesnap on 5/25/2024.
 */
@Getter
public class FroggerMapFilePacketLight extends FroggerMapFilePacket {
    public static final String IDENTIFIER = "LITE";
    private final List<FroggerMapLight> lights = new ArrayList<>();

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
    public int getKnownStartAddress() {
        return getParentFile().getGraphicalPacket().getLightPacketAddress();
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList.add("Light Count", this.lights.size());
        return propertyList;
    }
}