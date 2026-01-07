package net.highwayfrogs.editor.games.sony.shared.mof2.mesh;

import lombok.Getter;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.utils.objects.ReferencableBuffer;
import net.highwayfrogs.editor.utils.objects.ReferencableBuffer.LazyReferencableBuffer;

/**
 * Contains data shared across the entire static Mof (shared across different mof parts), used when saving/loading.
 * This should never need to be touched by model importers/exporters, it only applies when saving/loading to the MOF format, which is handled by the mof classes.
 * Created by Kneesnap on 2/22/2025.
 */
@Getter
class MRStaticMofDataContext {
    private final ReferencableBuffer<SVector> partCelVectors = new LazyReferencableBuffer<>(SVector.PADDED_BYTE_SIZE, SVector::readWithPadding, (writer, vector) -> vector.saveWithPadding(writer), false);
}