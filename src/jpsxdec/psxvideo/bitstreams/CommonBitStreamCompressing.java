/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2022-2023  Michael Sabin
 * All rights reserved.
 *
 * Redistribution and use of the jPSXdec code or any derivative works are
 * permitted provided that the following conditions are met:
 *
 *  * Redistributions may not be sold, nor may they be used in commercial
 *    or revenue-generating business activities.
 *
 *  * Redistributions that are modified from the original source must
 *    include the complete source code, including the source code for all
 *    components used by a binary built from the modified sources. However, as
 *    a special exception, the source code distributed need not include
 *    anything that is normally distributed (in either source or binary form)
 *    with the major components (compiler, kernel, and so on) of the operating
 *    system on which the executable runs, unless that component itself
 *    accompanies the executable.
 *
 *  * Redistributions must reproduce the above copyright notice, this list
 *    of conditions and the following disclaimer in the documentation and/or
 *    other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package jpsxdec.psxvideo.bitstreams;

import jpsxdec.psxvideo.encode.MacroBlockEncoder;
import jpsxdec.psxvideo.encode.MdecEncoder;
import jpsxdec.psxvideo.mdec.*;
import jpsxdec.util.IncompatibleException;
import lombok.NonNull;

/** Common bitstream compression functions. */
public class CommonBitStreamCompressing {

    public static @NonNull byte[] joinByteArrays(@NonNull byte[] ab1, @NonNull byte[] ab2) {
        byte[] abConcat = new byte[ab1.length + ab2.length];
        System.arraycopy(ab1, 0, abConcat, 0, ab1.length);
        System.arraycopy(ab2, 0, abConcat, ab1.length, ab2.length);
        return abConcat;
    }

    /** Encodes to strings of bits. */
    public interface BitStringEncoder {
        @NonNull String encodeQscaleDc(@NonNull MdecCode code, @NonNull MdecBlock mdecBlock) throws MdecException.TooMuchEnergy, IncompatibleException;
        @NonNull String encode0RlcAc(@NonNull MdecCode code) throws MdecException.TooMuchEnergy;
    }

    public static int compress(@NonNull BitStreamWriter bitWriter,
                               @NonNull MdecInputStream inStream,
                               @NonNull BitStringEncoder encoder,
                               int iMacroBlockCount)
            throws IncompatibleException, MdecException.EndOfStream,
                   MdecException.ReadCorruption, MdecException.TooMuchEnergy
    {
        MdecContext context = new MdecContext();

        final MdecCode code = new MdecCode();

        while (context.getTotalMacroBlocksRead() < iMacroBlockCount) {
            String sBitsToWrite;
            boolean blnEod = inStream.readMdecCode(code);
            if (!code.isValid())
                throw new MdecException.ReadCorruption("Invalid MDEC code " + code);
            if (blnEod) {
                sBitsToWrite = ZeroRunLengthAcLookup_STR.END_OF_BLOCK.getBitString();
                context.nextCodeEndBlock();
            } else {
                if (context.atStartOfBlock()) {
                    sBitsToWrite = encoder.encodeQscaleDc(code, context.getCurrentBlock());
                } else {
                    sBitsToWrite = encoder.encode0RlcAc(code);
                }
                context.nextCode();
            }
            if (BitStreamDebugging.DEBUG)
                System.out.println("Converting " + code + " to " + sBitsToWrite + " at bit " + bitWriter.getBitsWritten());
            bitWriter.write(sBitsToWrite);
        }

        if (!context.atStartOfBlock())
            throw new IllegalStateException("Ended compressing in the middle of a macroblock.");

        return context.getTotalMdecCodesRead();
    }


    public static /*@CheckForNull*/ byte[] singleQscaleCompressFull(int iMaxSize,
                                                                @NonNull String sFrameDescription,
                                                                @NonNull MdecEncoder encoder,
                                                                @NonNull BitStreamCompressor compressor/*,
                                                                @NonNull ILocalizedLogger log*/)
            throws IncompatibleException, MdecException.EndOfStream, MdecException.ReadCorruption
    {
        for (int iQscale = 1; iQscale < 64; iQscale++) {
            //log.log(Level.INFO, I.TRYING_QSCALE(iQscale));

            int[] aiNewQscale = { iQscale, iQscale, iQscale,
                                  iQscale, iQscale, iQscale };

            for (MacroBlockEncoder macblk : encoder) {
                macblk.setToFullEncode(aiNewQscale);
            }

            byte[] abNewDemux;
            try {
                abNewDemux = compressor.compress(encoder.getStream());
            } catch (MdecException.TooMuchEnergy ex) {
                //log.log(Level.INFO, I.COMPRESS_TOO_MUCH_ENERGY(sFrameDescription), ex);
                continue;
            }

            if (abNewDemux.length <= iMaxSize) {
                //log.log(Level.INFO, I.NEW_FRAME_FITS(sFrameDescription, abNewDemux.length, iMaxSize));
                return abNewDemux;
            } else {
                //log.log(Level.INFO, I.NEW_FRAME_DOES_NOT_FIT(sFrameDescription, abNewDemux.length, iMaxSize));
            }
        }
        return null;
    }


    public static /*@CheckForNull*/ byte[] singleQscaleCompressPartial(int iMaxSize,
                                                                   @NonNull String sFrameDescription,
                                                                   @NonNull MdecEncoder encoder,
                                                                   @NonNull BitStreamCompressor compressor,
                                                                   int iFrameQscale/*,
                                                                   @NonNull ILocalizedLogger log*/)
            throws IncompatibleException, MdecException.EndOfStream, MdecException.ReadCorruption
    {
        int[] aiOriginalQscale = { iFrameQscale, iFrameQscale, iFrameQscale,
                                   iFrameQscale, iFrameQscale, iFrameQscale };

        for (int iNewQscale = iFrameQscale; iNewQscale < 64; iNewQscale++) {
            //log.log(Level.INFO, I.TRYING_QSCALE(iNewQscale));

            int[] aiNewQscale = { iNewQscale, iNewQscale, iNewQscale,
                                  iNewQscale, iNewQscale, iNewQscale };

            for (MacroBlockEncoder macblk : encoder) {
                macblk.setToPartialEncode(aiOriginalQscale, aiNewQscale);
            }

            byte[] abNewDemux;
            try {
                abNewDemux = compressor.compress(encoder.getStream());
            } catch (MdecException.TooMuchEnergy ex) {
                //log.log(Level.INFO, I.COMPRESS_TOO_MUCH_ENERGY(sFrameDescription), ex);
                continue;
            }

            if (abNewDemux.length <= iMaxSize) {
                //log.log(Level.INFO, I.NEW_FRAME_FITS(sFrameDescription, abNewDemux.length, iMaxSize));
                return abNewDemux;
            } else {
                //log.log(Level.INFO, I.NEW_FRAME_DOES_NOT_FIT(sFrameDescription, abNewDemux.length, iMaxSize));
            }
        }
        return null;
    }

}
