package net.highwayfrogs.editor.gui.mesh.fxobject;

import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import net.highwayfrogs.editor.system.math.Vector3f;

/**
 * Created by Kneesnap on 5/15/2025.
 */
public class Cone3D {
    /**
     * Create a cone shape. Origin is center of base (bottom circle). Height is along the Y axis.
     * Based on the code found at <a href="https://stackoverflow.com/a/64786861"/>.
     * @param m A given TriangleMesh
     * @param res The resolution of the circles
     * @param radius The base radius
     * @param height The height of the cone along the Y axis
     */
    public static void createCone(TriangleMesh m, int res, float radius, float height) {
        m.setVertexFormat(VertexFormat.POINT_NORMAL_TEXCOORD);

        float[] v = new float[res * 6]; //vertices
        float[] n = new float[(res + 2) * 3]; //face normals
        float[] uv = new float[(res * 8) + 4]; //texture coordinates
        int[] f = new int[18 * (2 * res - 2)]; // faces ((divisions * 18) + ((divisions-2)*18))

        float radPerDiv = ((float) Math.PI * 2f) / res;

        int tv = res * 3; //top plane vertices start index
        int tuv = (res + 1) * 2; //top plane uv start index
        int bcuv = tuv * 2;//(res * 4) + 4; //bottom cap uv start index
        int tcuv = bcuv + (res * 2); //bottom cap uv start index
        for (int i = 0; i < res; i++) {
            int vi = i * 3;
            float cos = (float) Math.cos(radPerDiv * (i));
            float sin = (float) Math.sin(radPerDiv * (i));
            //bottom plane vertices
            v[vi] = cos * radius; //X
            v[vi + 1] = height; //Y
            v[vi + 2] = sin * radius; //Z

            //top plane vertices
            v[tv + vi] = 0; //X
            v[tv + vi + 1] = 0; //Y
            v[tv + vi + 2] = 0; //Z

            int uvi = i * 2;
            //texture coordinate side down
            uv[uvi] = 1f - ((float) i / (float) res);
            uv[uvi + 1] = 1f;

            //texture coordinate side up
            uv[tuv + uvi] = uv[uvi];
            uv[tuv + uvi + 1] = 0;

            //texture coordinate bottom cap
            uv[bcuv + uvi] = (1f + cos) / 2f;
            uv[bcuv + uvi + 1] = (1f + sin) / 2f;

            //texture coordinate top cap
            uv[tcuv + uvi] = (1f - cos) / 2f;
            uv[tcuv + uvi + 1] = (1f + sin) / 2f;

            //face normals
            if (i > 0) {
                Vector3f p0 = new Vector3f(v[vi - 3], v[vi - 2], v[vi - 1]);
                Vector3f p1 = new Vector3f(v[vi], v[vi + 1], v[vi + 2]);
                Vector3f p2 = new Vector3f(v[tv + vi], v[tv + vi + 1], v[tv + vi + 2]);
                p1.subtract(p0);
                p2.subtract(p0);
                p2.crossProduct(p1, p0);
                p0.normalise();
                n[vi - 3] = p0.getX();
                n[vi - 2] = p0.getY();
                n[vi - 1] = p0.getZ();
            }
            if (i == res - 1) {
                Vector3f p0 = new Vector3f(v[vi], v[vi + 1], v[vi + 2]);
                Vector3f p1 = new Vector3f(v[0], v[1], v[2]);
                Vector3f p2 = new Vector3f(v[tv], v[tv + 1], v[tv + 2]);
                p1.subtract(p0);
                p2.subtract(p0);
                p2.crossProduct(p1, p0);
                p0.normalise();
                n[vi] = p0.getX();
                n[vi + 1] = p0.getY();
                n[vi + 2] = p0.getZ();
            }

            //faces around
            int fi = i * 18;
            //first triangle of face
            f[fi] = i; //vertex
            f[fi + 1] = i; //normal
            f[fi + 2] = i; //uv

            f[fi + 3] = res + i; //vertex
            f[fi + 4] = i; //normal
            f[fi + 5] = res + 1 + i; //uv

            f[fi + 6] = i + 1; //vertex
            f[fi + 7] = i + 1; //normal
            f[fi + 8] = i + 1; //uv

            //second triangle of face
            f[fi + 9] = i + 1; //vertex
            f[fi + 10] = i + 1; //normal
            f[fi + 11] = i + 1; //uv

            f[fi + 12] = res + i; //vertex
            f[fi + 13] = i; //normal
            f[fi + 14] = res + 1 + i; //uv

            f[fi + 15] = res + i + 1; //vertex
            f[fi + 16] = i + 1; //normal
            f[fi + 17] = res + 2 + i; //uv

            //wrap around, use the first vertices/normals
            if (i == res - 1) {
                f[fi + 6] = 0; //vertex
                f[fi + 9] = 0; //vertex
                f[fi + 15] = res; //vertex

                f[fi + 7] = 0; //normal
                f[fi + 10] = 0; //normal
                f[fi + 16] = 0; //normal
            }

            //top and bottom caps
            int fi2 = (i * 9) + (res * 18); //start index for bottom cap. Start after cone side is done
            int fi3 = fi2 + (res * 9) - 18; //fi2 + ((divisions - 2) * 9) //start index for top cap. Start after the bottom cap is done
            int uv2 = (res * 2) + 2; //start index of bottom cap texture coordinate
            int uv3 = (res * 3) + 2; //start index of top cap texture coordinate
            if (i < res - 2) {
                //bottom cap
                f[fi2] = 0;
                f[fi2 + 1] = res; //normal
                f[fi2 + 2] = uv2; //uv

                f[fi2 + 3] = i + 1;
                f[fi2 + 4] = res; //normal
                f[fi2 + 5] = uv2 + i + 1; //uv

                f[fi2 + 6] = i + 2;
                f[fi2 + 7] = res; //normal
                f[fi2 + 8] = uv2 + i + 2; //uv

                //top cap
                f[fi3] = res;
                f[fi3 + 1] = res + 1; //normal
                f[fi3 + 2] = uv3; //uv

                f[fi3 + 3] = res + i + 2;
                f[fi3 + 4] = res + 1; //normal
                f[fi3 + 5] = uv3 + i + 2; //uv

                f[fi3 + 6] = res + i + 1;
                f[fi3 + 7] = res + 1; //normal
                f[fi3 + 8] = uv3 + i + 1; //uv
            }
        }

        //smooth normals
        float[] ns = new float[n.length];
        Vector3f n0 = new Vector3f();
        Vector3f n1 = new Vector3f();
        for (int i = 0; i < res; i++) {
            int p0 = i * 3;
            int p1 = (i - 1) * 3;
            if (i == 0)
                p1 = (res - 1) * 3;
            n0.setXYZ(n[p0], n[p0 + 1], n[p0 + 2]);
            n1.setXYZ(n[p1], n[p1 + 1], n[p1 + 2]);
            n0.add(n1);
            n0.normalise();

            ns[p0] = n0.getX();
            ns[p0 + 1] = n0.getY();
            ns[p0 + 2] = n0.getZ();
        }

        int ni = res * 3;
        ns[ni + 1] = -1; //bottom cap normal Y axis
        ns[ni + 4] = 1; //top cap normal Y axis

        uv[tuv - 1] = 1; //bottom ring end uv coordinate

        // Flip to correct winding ordeer.
        for (int i = 0; i < f.length; i += 9) {
            for (int j = 0; j < 3; j++) {
                int temp = f[i + j];
                f[i + j] = f[i + 6 + j];
                f[i + 6 + j] = temp;
            }
        }

        //set all data to mesh
        m.getPoints().setAll(v);
        m.getNormals().setAll(ns);
        m.getTexCoords().setAll(uv);
        m.getFaces().setAll(f);
    }
}
