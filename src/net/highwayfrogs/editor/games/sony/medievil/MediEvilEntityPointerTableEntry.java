package net.highwayfrogs.editor.games.sony.medievil;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData;

/**
 * Represents an entry in the MediEvil entity table.
 * Created by RampantSpirit on 3/11/2024.
 */
@Getter
public class MediEvilEntityPointerTableEntry extends SCGameData<MediEvilGameInstance> {
    @Getter  private long entityDataPointer;
    @Getter private String entityDataLocation;
    private final int overlayOffset;
    private final int byteSize = 8;

    public MediEvilEntityPointerTableEntry(MediEvilGameInstance instance, int overlayOffset) {
        super(instance);
        this.overlayOffset = overlayOffset;
    }

    @Override
    public void load(DataReader reader) {
        int endIndex = reader.getIndex() + getByteSize();
        this.entityDataPointer = reader.readUnsignedIntAsLong();
        this.entityDataLocation = resolveOverlay(reader.readInt());
        if (this.entityDataPointer != 0 && this.entityDataLocation.equals("")) {
            this.entityDataPointer -= getGameInstance().getRamOffset();
        }
        else if (this.entityDataPointer != 0) {
            this.entityDataPointer -= getOverlayOffset();
        }

        reader.setIndex(endIndex);
    }

    private String resolveOverlay(int overlayFlag) {
        String result = "";
        switch(overlayFlag)
        {
            case 1:
                result = "TL.BIN";
                break;
            case 1 << 1:
                result = "GY1.BIN";
                break;
            case 1 << 2 | 1 << 24:
            case 1 << 2:
                result = "GY2.BIN";
                break;
            case 1 << 3:
                result = "CH.BIN";
                break;
            case 1 << 4:
                result = "DC.BIN";
                break;
            case 1 << 5:
                result = "SF.BIN";
                break;
            case 1 << 6:
                result = "CR.BIN";
                break;
            case 1 << 7:
                result = "AC.BIN";
                break;
            case 1 << 8:
                result = "CC.BIN";
                break;
            case 1 << 9:
                result = "PG.BIN";
                break;
            case 1 << 10:
                result = "PS.BIN";
                break;
            case 1 << 11:
                result = "SV.BIN";
                break;
            case 1 << 12:
                result = "PD.BIN";
                break;
            case 1 << 13:
                result = "AG.BIN";
                break;
            case 1 << 14:
                result = "IA.BIN";
                break;
            case 1 << 15:
                result = "EE.BIN";
                break;
            case 1 << 16:
                result = "GG.BIN";
                break;
            case 1 << 17:
                result = "HR.BIN";
                break;
            case 1 << 18:
                result = "HH.BIN";
                break;
            case 1 << 19:
                result = "GS.BIN";
                break;
            case 1 << 20:
                result = "EH.BIN";
                break;
            case 1 << 21:
                result = "TD.BIN";
                break;
            case 1 << 22:
                result = "LA.BIN";
                break;
            case 1 << 23:
                result = "ZL.BIN";
                break;
        }
        return result;
    }

    @Override
    public void save(DataWriter writer) {
        // TODO: IMPLEMENT
    }
}