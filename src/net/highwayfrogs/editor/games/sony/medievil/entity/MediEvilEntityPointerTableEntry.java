package net.highwayfrogs.editor.games.sony.medievil.entity;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilGameInstance;

/**
 * Represents an entry in the MediEvil entity table.
 * Created by RampantSpirit on 3/11/2024.
 */
@Getter
public class MediEvilEntityPointerTableEntry extends SCGameData<MediEvilGameInstance> {
    private long entityDataPointer;
    private int overlayId;

    private static final int SIZE_IN_BYTES = 8;

    public MediEvilEntityPointerTableEntry(MediEvilGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        this.entityDataPointer = reader.readUnsignedIntAsLong();
        this.overlayId = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedInt(this.entityDataPointer);
        writer.writeInt(this.overlayId);
    }

    public static String resolveOverlay(int overlayFlag) {
        // TODO: Once level table entries support different versions properly, we should instead be using the overlay of the active level (obtained from the level table), instead of from here. This is bad since we'd need to configure this function for every single version, whereas once we get the level table entry, it will automatically determine the overlays regardless of version.

        switch(overlayFlag) {
            case 1:
                return "TL.BIN";
            case 1 << 1:
                return "GY1.BIN";
            case 1 << 2 | 1 << 24:
            case 1 << 2:
                return "GY2.BIN";
            case 1 << 3:
                return "CH.BIN";
            case 1 << 4:
                return "DC.BIN";
            case 1 << 5:
                return "SF.BIN";
            case 1 << 6:
                return "CR.BIN";
            case 1 << 7:
                return "AC.BIN";
            case 1 << 8:
                return "CC.BIN";
            case 1 << 9:
                return "PG.BIN";
            case 1 << 10:
                return "PS.BIN";
            case 1 << 11:
                return "SV.BIN";
            case 1 << 12:
                return "PD.BIN";
            case 1 << 13:
                return "AG.BIN";
            case 1 << 14:
                return "IA.BIN";
            case 1 << 15:
                return "EE.BIN";
            case 1 << 16:
                return "GG.BIN";
            case 1 << 17:
                return "HR.BIN";
            case 1 << 18:
                return "HH.BIN";
            case 1 << 19:
                return "GS.BIN";
            case 1 << 20:
                return "EH.BIN";
            case 1 << 21:
                return "TD.BIN";
            case 1 << 22:
                return "LA.BIN";
            case 1 << 23:
                return "ZL.BIN";
            default:
                return null;
        }
    }
}