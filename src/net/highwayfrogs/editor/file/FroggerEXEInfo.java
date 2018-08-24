package net.highwayfrogs.editor.file;

import lombok.Getter;

/**
 * Information about a specific frogger.exe file.
 * Created by Kneesnap on 8/18/2018.
 */
@Getter
public class FroggerEXEInfo {
    private String md5Hash = "F4760B31D8BB75EFB4C2AF67E9952BFD";
    private int mwiOffset = 0x8B220;
    private int remapOffset = 0x79960; //TODO: Get proper address.


}
