package net.highwayfrogs.editor.games.tgq.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * A registry of different components which make up a vertex format.
 * Created by Kneesnap on 6/22/2023.
 */
@Getter
@AllArgsConstructor
public enum kcVertexFormatComponent {
    NULL(-1, -1), // 0
    POSITION_XYZF(12, 6), // 1
    POSITION_XYZWF(16, 8), // 2
    NORMAL_XYZF(12, 6), // 3
    NORMAL_XYZWF(16, 8), // 4
    DIFFUSE_RGBF(12, 6), // 5
    DIFFUSE_RGBAF(16, 8), // 6
    DIFFUSE_RGBAI(4, 4), // 7
    DIFFUSE_RGBA255F(16, 8), // 8
    SPECULAR_RGBF(12, 6), // 9
    SPECULAR_RGBAF(16, 8), // 10|0A
    SPECULAR_RGBAI(4, 4), // 11|0B
    SPECULAR_RGBA255F(16, 8), // 12|0C
    WEIGHT1F(4, 2), // 13|0D
    WEIGHT2F(8, 4), // 14|0E
    TEX1F(8, 4), // 15|0F
    TEX2F(16, 8), // 16|10
    TEX1_STQP(16, 8), // 17|11
    WEIGHT3F(12, 6), // 18|12
    WEIGHT4F(16, 8), // 19|13
    MATRIX_INDICES(16, 8), // 20|14
    PSIZE(4, 2); // 21|15

    private final int stride;
    private final int compressedStride;
}