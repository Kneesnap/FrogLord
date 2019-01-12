package net.highwayfrogs.editor.file.sound;

import javafx.scene.Node;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.GameFile;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Reads data from a PSX VAB Body File.
 * Credit to https://github.com/vgmtrans/vgmtrans/blob/a179e4d57fca6b9f814e3a086b4ef99f20de0cd7/src/main/formats/PSXSPU.cpp for information about the file.
 * Created by Kneesnap on 1/11/2019.
 */
@Getter
public class VABBodyFile extends GameFile {
    private boolean loopOnConversion;
    @Setter private boolean loopStatus;
    @Setter private int loopOffset;
    @Setter private int loopLength;

    private static final int SAMPLE_PADDING = 0x10;

    @Override
    public void load(DataReader reader) {

        if (isLoopOnConversion())
            setLoopStatus(false);

        while (reader.hasMore()) {
            reader.readBytes(SAMPLE_PADDING); // Read padding.

            byte temp = reader.readByte();
            byte range = (byte) (temp * 0xF);
            byte filter = (byte) ((temp & 0xF0) >> 4);

            temp = reader.readByte();
            byte flagEnd = (byte) (temp & 1);
            boolean flagLooping = (temp & 2) > 0;
            boolean flagLoop = (temp & 4) > 0;

            if (isLoopOnConversion()) {
                if (flagLoop) {
                    setLoopOffset(reader.getIndex());
                    setLoopLength(reader.getRemaining());
                }

                if (flagEnd > 0 && flagLooping) {
                    setLoopStatus(true);
                }
            }

            byte[] byteArray = reader.readBytes(14);

            //each decompressed pcm block is 52 bytes   EDIT: (wait, isn't it 56 bytes? or is it 28?)
            /*
            decompressVAGBlock(uncompBuf + ((k * 28) / 16),
                    & theBlock,
                 &prev1,
                 &prev2);
                 */
        }
    }

    private void decompressVAGBlock() {
        /*
        void PSXSamp::DecompVAGBlk(s16 *pSmp, VAGBlk *pVBlk, f32 *prev1, f32 *prev2) {
  u32 i, shift;                                //Shift amount for compressed samples
  f32 t;                                       //Temporary sample
  f32 f1, f2;
  f32 p1, p2;
  static const f32 Coeff[5][2] = {
      {0.0, 0.0},
      {60.0 / 64.0, 0.0},
      {115.0 / 64.0, 52.0 / 64.0},
      {98.0 / 64.0, 55.0 / 64.0},
      {122.0 / 64.0, 60.0 / 64.0}};


  //Expand samples ---------------------------
  shift = pVBlk->range + 16;

  for (i = 0; i < 14; i++) {
    pSmp[i * 2] = ((s32) pVBlk->brr[i] << 28) >> shift;
    pSmp[i * 2 + 1] = ((s32) (pVBlk->brr[i] & 0xF0) << 24) >> shift;
  }

  //Apply ADPCM decompression ----------------
  i = pVBlk->filter;

  if (i) {
    f1 = Coeff[i][0];
    f2 = Coeff[i][1];
    p1 = *prev1;
    p2 = *prev2;

    for (i = 0; i < 28; i++) {
      t = pSmp[i] + (p1 * f1) - (p2 * f2);
      pSmp[i] = (s16) t;
      p2 = p1;
      p1 = t;
    }

    *prev1 = p1;
    *prev2 = p2;
  }
  else {
    *prev2 = pSmp[26];
    *prev1 = pSmp[27];
  }
}
         */
    }

    @Override
    public void save(DataWriter writer) {
        //TODO
    }


    @Override
    public Image getIcon() {
        return VHFile.ICON;
    }

    @Override
    public Node makeEditor() {
        return null; //TODO
    }
}
