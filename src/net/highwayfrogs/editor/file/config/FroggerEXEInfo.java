package net.highwayfrogs.editor.file.config;

import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;

/**
 * Information about a specific frogger.exe file.
 * Created by Kneesnap on 8/18/2018.
 */
@Getter
public class FroggerEXEInfo extends Config {
    public FroggerEXEInfo(InputStream inputStream) throws IOException {
        super(inputStream);
    }

    /**
     * Get the remap table address.
     * @return remapOffset
     */
    public int getRemapAddress() {
        return getInt("remapOffset");
    }

}
