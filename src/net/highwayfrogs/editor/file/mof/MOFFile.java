package net.highwayfrogs.editor.file.mof;

import lombok.Cleanup;
import lombok.Getter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.map.view.VertexColor;
import net.highwayfrogs.editor.file.mof.prims.MOFPolyTexture;
import net.highwayfrogs.editor.file.mof.prims.MOFPolygon;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.PSXColorVector;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings.ImageState;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Represents a MOF file.
 * Some files are off by 4-16 bytes. This is caused by us merging prim banks of matching types, meaning the header that was there before is removed.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
public class MOFFile extends MOFBase {
    private byte[] bytes;
    private List<MOFPart> parts = new ArrayList<>();
    private int unknownValue;

    private static final int INCOMPLETE_TEST_ADDRESS = 0x1C;
    private static final int INCOMPLETE_TEST_VALUE = 0x40;
    public static final ImageFilterSettings MOF_EXPORT_FILTER = new ImageFilterSettings(ImageState.EXPORT)
            .setTrimEdges(true).setAllowTransparency(true).setAllowFlip(true);

    public MOFFile(MOFHolder holder) {
        super(holder);
    }

    @Override
    public void onLoad(DataReader reader) {
        int partCount = reader.readInt();

        reader.jumpTemp(INCOMPLETE_TEST_ADDRESS);
        boolean isIncomplete = (reader.readInt() == INCOMPLETE_TEST_VALUE);
        reader.jumpReturn();

        if (isIncomplete) { // Just copy the MOF directly.
            getHolder().setIncomplete(true);
            reader.jumpTemp(0);
            this.bytes = reader.readBytes(reader.getRemaining());
            reader.jumpReturn();

            String oldName = Utils.stripExtensionWin95(getHolder().getCompleteMOF().getFileEntry().getDisplayName());
            String newName = Utils.stripExtensionWin95(getFileEntry().getDisplayName());
            getConfig().getAnimationBank().linkChildBank(oldName, newName); // Link animation names.
        }

        for (int i = 0; i < partCount; i++) {
            MOFPart part = new MOFPart(this);
            part.load(reader);
            if (isIncomplete)
                getHolder().getCompleteMOF().getStaticFile().getParts().get(i).copyToIncompletePart(part);

            parts.add(part);
        }

        this.unknownValue = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        if (getHolder().isIncomplete() && getBytes() != null) { // If the MOF is incomplete, save the incomplete mof.
            writer.writeBytes(this.bytes);
            return;
        }

        super.save(writer);
    }

    @Override
    public void onSave(DataWriter writer) {
        writer.writeInt(getParts().size());
        getParts().forEach(part -> part.save(writer));
        writer.writeInt(this.unknownValue);
        getParts().forEach(part -> part.saveExtra(writer));
    }

    /**
     * Export this object to wavefront obj.
     * Not the cleanest thing in the world, but it doesn't need to be.
     */
    @SneakyThrows
    public void exportObject(File folder, VLOArchive vloTable, String cleanName) {
        boolean exportTextures = vloTable != null;

        String mtlName = cleanName + ".mtl";
        @Cleanup PrintWriter objWriter = new PrintWriter(new File(folder, cleanName + ".obj"));

        objWriter.write("# FrogLord MOF Export" + Constants.NEWLINE);
        objWriter.write("# Exported: " + Calendar.getInstance().getTime().toString() + Constants.NEWLINE);
        objWriter.write("# MOF Name: " + getFileEntry().getDisplayName() + Constants.NEWLINE);
        objWriter.write(Constants.NEWLINE);

        if (exportTextures) {
            objWriter.write("mtllib " + mtlName + Constants.NEWLINE);
            objWriter.write(Constants.NEWLINE);
        }

        // Write Vertices.
        int partCount = 0;
        int verticeStart = 0;
        for (MOFPart part : getParts()) {
            part.setTempVertexStart(verticeStart);
            objWriter.write("# Part " + (partCount++) + ":" + Constants.NEWLINE);
            MOFPartcel partcel = part.getPartcels().get(0); // 0 is the animation frame.
            verticeStart += partcel.getVertices().size();

            for (SVector vertex : partcel.getVertices())
                objWriter.write(vertex.toOBJString() + Constants.NEWLINE);
        }

        objWriter.write(Constants.NEWLINE);

        // Write Faces.
        Map<MOFPolygon, MOFPart> ownerMap = new HashMap<>();
        List<MOFPolygon> allPolygons = new ArrayList<>();
        getParts().forEach(part -> part.getMofPolygons().values().forEach(polys -> {
            allPolygons.addAll(polys);
            for (MOFPolygon poly : polys)
                ownerMap.put(poly, part);
        }));

        // Register textures.
        if (exportTextures) {
            allPolygons.sort(Comparator.comparingInt(MOFPolygon::getOrderId));
            objWriter.write("# Vertex Textures" + Constants.NEWLINE);

            for (MOFPolygon poly : allPolygons) {
                if (poly instanceof MOFPolyTexture) {
                    MOFPolyTexture mofTex = (MOFPolyTexture) poly;
                    for (int i = mofTex.getUvs().length - 1; i >= 0; i--)
                        objWriter.write(mofTex.getObjUVString(i) + Constants.NEWLINE);
                }
            }
        }

        objWriter.write("# Faces" + Constants.NEWLINE);

        AtomicInteger textureId = new AtomicInteger(Integer.MIN_VALUE);
        AtomicInteger counter = new AtomicInteger();

        Map<Integer, GameImage> textureMap = new HashMap<>();
        List<PSXColorVector> faceColors = new ArrayList<>();
        Map<PSXColorVector, List<MOFPolygon>> facesWithColors = new HashMap<>();

        allPolygons.forEach(polygon -> {
            if (!(polygon instanceof MOFPolyTexture)) {
                PSXColorVector color = polygon.getColor();
                if (!faceColors.contains(color))
                    faceColors.add(color);
                facesWithColors.computeIfAbsent(color, key -> new ArrayList<>()).add(polygon);
            } else {
                MOFPolyTexture texture = (MOFPolyTexture) polygon;

                if (exportTextures) {
                    int newTextureId = texture.getImageId();

                    GameImage image = textureMap.computeIfAbsent(newTextureId, key ->
                            vloTable.getImageByTextureId(texture.getImageId()));
                    newTextureId = image.getTextureId();

                    if (newTextureId != textureId.get()) { // It's time to change the texture.
                        textureId.set(newTextureId);
                        objWriter.write(Constants.NEWLINE);
                        objWriter.write("usemtl tex" + newTextureId + Constants.NEWLINE);
                    }
                }

                objWriter.write(polygon.toObjFaceCommand(exportTextures, counter, ownerMap.get(polygon)) + Constants.NEWLINE);
            }
        });

        objWriter.append(Constants.NEWLINE);
        objWriter.append("# Faces without textures.").append(Constants.NEWLINE);
        for (Entry<PSXColorVector, List<MOFPolygon>> mapEntry : facesWithColors.entrySet()) {
            objWriter.write("usemtl color" + faceColors.indexOf(mapEntry.getKey()) + Constants.NEWLINE);
            mapEntry.getValue().forEach(poly -> objWriter.write(poly.toObjFaceCommand(exportTextures, null, ownerMap.get(poly)) + Constants.NEWLINE));
        }


        // Write MTL file.
        if (exportTextures) {
            @Cleanup PrintWriter mtlWriter = new PrintWriter(new File(folder, mtlName));

            for (GameImage image : textureMap.values()) {
                mtlWriter.write("newmtl tex" + image.getTextureId() + Constants.NEWLINE);
                mtlWriter.write("Kd 1 1 1" + Constants.NEWLINE); // Diffuse color.
                // "d 0.75" = Partially transparent, if we want to support this later.
                mtlWriter.write("map_Kd " + vloTable.getImages().indexOf(image) + ".png" + Constants.NEWLINE);
                mtlWriter.write(Constants.NEWLINE);
            }

            for (int i = 0; i < faceColors.size(); i++) {
                PSXColorVector color = faceColors.get(i);
                mtlWriter.write("newmtl color" + i + Constants.NEWLINE);
                if (i == 0)
                    mtlWriter.write("d 1" + Constants.NEWLINE); // All further textures should be completely solid.
                mtlWriter.write("Kd " + Utils.unsignedByteToFloat(color.getRed()) + " " + Utils.unsignedByteToFloat(color.getGreen()) + " " + Utils.unsignedByteToFloat(color.getBlue()) + Constants.NEWLINE); // Diffuse color.
                mtlWriter.write(Constants.NEWLINE);
            }
        }

        System.out.println("MOF Exported.");
    }

    /**
     * Run some behavior on each mof polygon.
     * @param handler The behavior to run.
     */
    public void forEachPolygon(Consumer<MOFPolygon> handler) {
        getParts().forEach(part -> part.getMofPolygons().values().forEach(list -> list.forEach(handler)));
    }

    /**
     * Create a map of textures which were generated
     * @return texMap
     */
    public Map<VertexColor, BufferedImage> makeVertexColorTextures() {
        Map<VertexColor, BufferedImage> texMap = new HashMap<>();

        forEachPolygon(prim -> {
            if (!(prim instanceof VertexColor))
                return;

            VertexColor vertexColor = (VertexColor) prim;
            BufferedImage image = vertexColor.makeTexture();
            texMap.put(vertexColor, image);
        });

        return texMap;
    }

    /**
     * Check if this file has any texture animations.
     * @return hasTextureAnimation
     */
    public boolean hasTextureAnimation() {
        for (MOFPart part : getParts())
            if (!part.getPartPolyAnims().isEmpty())
                return true;
        return false;
    }
}