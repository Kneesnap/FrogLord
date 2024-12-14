package net.highwayfrogs.editor.games.sony.shared.landscaper;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.data.IBinarySerializable;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.landscaper.mesh.LandscapeMapMesh;
import net.highwayfrogs.editor.games.sony.shared.landscaper.mesh.LandscapeMapMeshNode;
import net.highwayfrogs.editor.system.classlist.GlobalClassRegistry;
import net.highwayfrogs.editor.system.classlist.IndexedClassRegistry;
import net.highwayfrogs.editor.utils.objects.IndexBitArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * A landscape is the full map terrain for a single map level.
 * It can be broken up into nodes which provides different parts of a landscape, with potentially different editing options.
 * The landscape is aggregates all landscape nodes into a single unified terrain mesh.
 * This design has been built to mimic the original "Mappy" editor used to make the PSX Sony Cambridge games.
 * Mappy has likely been lost to time, but enough time has been spent analyzing maps from various Sony Cambridge games and analyzing the OpenInventor project used to make Mappy that I believe this to be a fairly decent recreation.
 * However, Landscapes purely exist in FrogLord, and any game data would need to be converted into a Landscape to be editable as one.
 *
 * TODO:
 *  - 1) See if we can integrate with the old editor.
 *    -> Convert Frogger maps to height-fields.
 *    -> We need to organize in a way that lets FroggerMapFile and CoolFroggerMapFile both be used with the same editors, even if they maybe don't share inheritance.
 *    -> getFilePacket(Packet.class), also a requireFilePacket(Packet.class) method.
 *    -> Allow baking into a real Frogger map.
 *    -> Colors are per-vertex-per-face as well as per-vertex. (Frogger only, LandscapePolygon extension)
 *    -> Extend column to include grid square collision flags (probably, but it depends on how we decide to handle different height-field layers.)
 *    -> When selecting root polygon -> (Must have the root vertex as a vertex with the min x/z pos) -> First min Z, then tiebreaker with min X, then tiebreaker with highest Y.
 *  - 2) Get started on editor.
 *    -> The class registry system should be a toolbox list. Eg: It should allow specifying display name, icons, images, etc.
 *    -> Should have a menu where we can add this stuff into the scene.
 *  - 3) Add static model node.
 *  - 4) Add vertex editing tools.
 *    -> I think I've hit my last major realization.
 *    -> Polygons are irrelevant mostly. Mappy's power came from its ability to edit the height-field vertices, not individual polygons.
 *    -> Operations like wall extensions were almost definitely done using vertices first, polygons second. Boom.
 *    -> Remove unused vertices. Do it in bulk, perhaps right before a save or something.
 *    -> Do we really want the TVertex or TPolygon? Look over usages of LandscapePolygon when we're done.
 * Created by Kneesnap on 7/16/2024.
 */
public abstract class Landscape extends LandscapeBase<LandscapeVertex, LandscapePolygon> implements IBinarySerializable {
    @Getter private int landscapeFormatVersion = CURRENT_LANDSCAPE_FORMAT_VERSION;
    private final List<LandscapeTexture> textures = new ArrayList<>();
    private final List<LandscapeTexture> immutableTextures = Collections.unmodifiableList(this.textures);
    private final List<LandscapeMaterial> materials = new ArrayList<>();
    private final List<LandscapeMaterial> immutableMaterials = Collections.unmodifiableList(this.materials);
    private final List<LandscapeNode<? extends LandscapeVertex, ? extends LandscapePolygon>> nodes = new ArrayList<>();
    private final List<LandscapeNode<? extends LandscapeVertex, ? extends LandscapePolygon>> immutableNodes = Collections.unmodifiableList(this.nodes);
    protected final IndexedClassRegistry<LandscapeNode<? extends LandscapeVertex, ? extends LandscapePolygon>> landscapeNodeClassRegistry;
    protected final IndexedClassRegistry<LandscapeMaterial> materialClassRegistry;
    protected final IndexedClassRegistry<LandscapeTexture> textureClassRegistry;
    protected final IndexBitArray cachedIndexBitArray = new IndexBitArray();
    private LandscapeMapMesh cachedMesh;

    public static final int CURRENT_LANDSCAPE_FORMAT_VERSION = 0;

    protected Landscape(SCGameInstance instance) {
        super(instance);
        this.landscapeNodeClassRegistry = new IndexedClassRegistry<>(getGlobalNodeClassRegistry());
        this.materialClassRegistry = new IndexedClassRegistry<>(getGlobalMaterialClassRegistry());
        this.textureClassRegistry = new IndexedClassRegistry<>(getGlobalTextureClassRegistry());
    }

    @Override
    public void load(DataReader reader) {
        this.landscapeFormatVersion = reader.readUnsignedShortAsInt();
        if (this.landscapeFormatVersion > CURRENT_LANDSCAPE_FORMAT_VERSION)
            throw new RuntimeException("FrogLord " + Constants.VERSION + " supports up to landscape format " + CURRENT_LANDSCAPE_FORMAT_VERSION + ", but landscape format we're trying to read is " + this.landscapeFormatVersion + ".");

        // 1) Definition Blocks
        this.nodes.clear(); // 1a) Node Definition Block
        this.landscapeNodeClassRegistry.readClassRefIdBitArray(reader, this::addNode, getClass(), this);
        this.materials.clear(); // 1b) Material Definition Block
        this.materialClassRegistry.readClassRefIdBitArray(reader, this::addMaterial, getClass(), this);
        this.textures.clear(); // 1c) Texture Definition Block
        this.textureClassRegistry.readClassRefIdBitArray(reader, this::addTexture, getClass(), this);

        // 2) Vertex Block
        for (int i = 0; i < this.nodes.size(); i++)
            this.nodes.get(i).readVertices(reader);

        // 3) Polygon Block
        for (int i = 0; i < this.nodes.size(); i++)
            this.nodes.get(i).readPolygons(reader);

        // 4) Texture Block
        for (int i = 0; i < this.textures.size(); i++)
            this.textures.get(i).load(reader);

        // 5) Material Block
        for (int i = 0; i < this.materials.size(); i++)
            this.materials.get(i).load(reader);

        // 6) Node Data Block
        for (int i = 0; i < this.nodes.size(); i++)
            this.nodes.get(i).readNodeData(reader);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(CURRENT_LANDSCAPE_FORMAT_VERSION);

        // 1) Definition Blocks
        this.landscapeNodeClassRegistry.writeClassRefIdsAsBitArray(writer, this.nodes); // 1a) Node Definition Block
        this.materialClassRegistry.writeClassRefIdsAsBitArray(writer, this.materials); // 1b) Material Definition Block
        this.textureClassRegistry.writeClassRefIdsAsBitArray(writer, this.textures); // 1c) Texture Definition Block

        // 2) Vertex Block
        for (int i = 0; i < this.nodes.size(); i++)
            this.nodes.get(i).writeVertices(writer);

        // 3) Polygon Block
        for (int i = 0; i < this.nodes.size(); i++)
            this.nodes.get(i).writePolygons(writer);

        // 4) Texture Block
        for (int i = 0; i < this.textures.size(); i++)
            this.textures.get(i).save(writer);

        // 5) Material Block
        for (int i = 0; i < this.materials.size(); i++)
            this.materials.get(i).save(writer);

        // 6) Node Data Block
        for (int i = 0; i < this.nodes.size(); i++)
            this.nodes.get(i).writeNodeData(writer);
    }

    /**
     * Gets or creates the JavaFX mesh. The mesh is cached once created.
     */
    public LandscapeMapMesh getMesh() {
        if (this.cachedMesh != null)
            return this.cachedMesh;

        return this.cachedMesh = new LandscapeMapMesh(this);
    }

    private LandscapeMapMeshNode getMeshNode() {
        return this.cachedMesh.getMainNode();
    }

    /**
     * Adds a node to the landscape.
     * @param node the node to add
     * @return true iff the node was added successfully
     */
    public boolean addNode(LandscapeNode<? extends LandscapeVertex, ? extends LandscapePolygon> node) {
        if (node == null)
            throw new NullPointerException("node");
        if (node.getLandscape() != this)
            throw new RuntimeException("The provided node is not applicable to the target Landscape!");
        if (node.isRegistered())
            return false; // Already registered.

        this.nodes.add(node);

        // Register vertices. (Should occur before polygons, so polygons don't reference unregistered vertices)
        if (this.cachedMesh != null)
            this.cachedMesh.getEditableVertices().startBatchInsertion();

        for (int i = 0; i < node.getVertices().size(); i++) {
            LandscapeVertex vertex = node.getVertices().get(i);
            addNodeVertex(node, vertex);
        }

        if (this.cachedMesh != null) {
            this.cachedMesh.getEditableVertices().endBatchInsertion();
            this.cachedMesh.getEditableTexCoords().startBatchInsertion();
            this.cachedMesh.getEditableFaces().startBatchInsertion();
        }

        // Register polygons.
        for (int i = 0; i < node.getPolygons().size(); i++) {
            LandscapePolygon polygon = node.getPolygons().get(i);
            addNodePolygon(node, polygon);
        }

        // Finish insertions
        if (this.cachedMesh != null) {
            this.cachedMesh.getEditableTexCoords().endBatchInsertion();
            this.cachedMesh.getEditableFaces().endBatchInsertion();
        }

        return true;
    }

    /**
     * Adds a node vertex without any verifications.
     * This method is for internal system use only.
     * @param node the node where the vertex came from
     * @param vertex the vertex to register
     */
    void addNodeVertex(LandscapeNode<? extends LandscapeVertex, ? extends LandscapePolygon> node, LandscapeVertex vertex) {
        vertex.setVertexId(this.vertices.size());
        this.vertices.add(vertex);
        if (this.cachedMesh != null)
            getMeshNode().getVertexEntry().addVertexValue(vertex.getNewWorldPosition());
    }

    /**
     * Registers a polygon from a node to the landscape.
     * This method is for internal system use only.
     * @param node the node where the vertex came from
     * @param polygon the polygon to register
     */
    void addNodePolygon(LandscapeNode<? extends LandscapeVertex, ? extends LandscapePolygon> node, LandscapePolygon polygon) {
        polygon.setPolygonId(this.polygons.size());
        this.polygons.add(polygon);

        // Validate polygon. (Consider changing these to register the texture/material to the scene instead of throwing)
        if (polygon.getTexture() != null && !polygon.getTexture().isRegistered())
            throw new RuntimeException("The polygon's texture is not registered to the Landscape, so the polygon can't be registered!");
        if (polygon.getMaterial() != null && !polygon.getMaterial().isRegistered())
            throw new RuntimeException("The polygon's material is not registered to the Landscape, so the polygon can't be registered!");

        // Add the polygon to the vertices connectedPolygon list.
        for (int i = 0; i < polygon.getVertexCount(); i++) {
            LandscapeVertex vertex = polygon.getVertex(i);
            if (vertex == null)
                continue;

            // Validate the vertex.
            if (!vertex.isRegistered())
                throw new RuntimeException("The vertex is not registered to this Landscape, so the polygon using it cannot be registered!");

            // If the vertex is seen multiple times, only add it once.
            boolean addToList = true;
            for (int j = i + 1; j < polygon.getVertexCount(); j++) {
                if (polygon.getVertex(j) == vertex) {
                    addToList = false;
                    break;
                }
            }

            if (addToList)
                vertex.getInternalConnectedPolygonList().add(polygon);
        }

        // Apply new tracking. (If these exist, they are guaranteed to be registered above.)
        if (polygon.getTexture() != null) // Add polygon reference to texture.
            polygon.getTexture().getInternalPolygonList().add(polygon);
        if (polygon.getMaterial() != null) // Add polygon reference to material.
            polygon.getMaterial().getInternalPolygonList().add(polygon);

        if (this.cachedMesh != null)
            getMeshNode().add(polygon);
    }

    /**
     * Removes a node from the landscape.
     * @param node the node to remove
     * @return true iff the node was removed successfully
     */
    public boolean removeNode(LandscapeNode<? extends LandscapeVertex, ? extends LandscapePolygon> node) {
        if (node == null || !this.nodes.remove(node))
            return false; // Already Wasn't removed.

        // Start batching polygon operations.
        if (this.cachedMesh != null) {
            this.cachedMesh.getEditableFaces().startBatchRemoval();
            this.cachedMesh.getEditableTexCoords().startBatchRemoval();
        }

        // Unregister polygons. (Should occur before vertices, so polygons don't reference unregistered vertices)
        this.cachedIndexBitArray.clear();
        for (int i = 0; i < node.getPolygons().size(); i++) {
            LandscapePolygon polygon = node.getPolygons().get(i);
            removeNodePolygon(node, polygon, this.cachedIndexBitArray);
        }
        removeValuesAndUpdateIds(this.polygons, this.cachedIndexBitArray, LandscapePolygon::setPolygonId);

        // Update batching polygon operations.
        if (this.cachedMesh != null) {
            this.cachedMesh.getEditableFaces().endBatchRemoval();
            this.cachedMesh.getEditableTexCoords().endBatchRemoval();
            this.cachedMesh.getEditableVertices().startBatchRemoval();
        }

        // Unregister vertices.
        this.cachedIndexBitArray.clear();
        for (int i = 0; i < node.getVertices().size(); i++) {
            LandscapeVertex vertex = node.getVertices().get(i);
            removeNodeVertex(node, vertex, this.cachedIndexBitArray);
        }
        removeValuesAndUpdateIds(this.vertices, this.cachedIndexBitArray, LandscapeVertex::setVertexId);

        if (this.cachedMesh != null)
            this.cachedMesh.getEditableVertices().endBatchRemoval();

        return true;
    }

    /**
     * Removes a node vertex without any verifications.
     * This method is for internal system use only.
     * @param node the node where the vertex came from
     * @param vertex the vertex to remove
     * @param batchIndices if this is provided, we are batching the removals to occur all at once later
     */
    void removeNodeVertex(LandscapeNode<? extends LandscapeVertex, ? extends LandscapePolygon> node, LandscapeVertex vertex, IndexBitArray batchIndices) {
        int vertexIndex = indexOfThrow(this.vertices, vertex, vertex.getVertexId());
        vertex.setVertexId(-1);
        if (batchIndices != null) { // Batch removal.
            this.vertices.set(vertexIndex, null);
            batchIndices.setBit(vertexIndex, true);
        } else {
            this.vertices.remove(vertexIndex);
            for (int i = vertexIndex; i < this.vertices.size(); i++) // Update ids.
                this.vertices.get(i).setVertexId(i);
        }

        if (this.cachedMesh != null) // Update mesh.
            getMeshNode().getVertexEntry().removeVertexValue(vertexIndex);
    }

    /**
     * Removes a node polygon without any verifications.
     * This method is for internal system use only.
     * @param node the node where the vertex came from
     * @param polygon the polygon to remove
     */
    void removeNodePolygon(LandscapeNode<? extends LandscapeVertex, ? extends LandscapePolygon> node, LandscapePolygon polygon, IndexBitArray batchIndices) {
        // Remove the polygon from the vertices connectedPolygon list.
        // We don't need to test for duplicate vertices on the polygon since it's getting removed regardless.
        for (int i = 0; i < polygon.getVertexCount(); i++) {
            LandscapeVertex vertex = polygon.getVertex(i);
            if (vertex != null)
                vertex.getInternalConnectedPolygonList().remove(polygon);
        }

        // Remove polygon reference in texture.
        if (polygon.getTexture() != null)
            polygon.getTexture().getInternalPolygonList().remove(polygon);

        // Remove polygon reference in material.
        if (polygon.getMaterial() != null)
            polygon.getMaterial().getInternalPolygonList().remove(polygon);

        // Unregister the polygon.
        int polygonIndex = indexOfThrow(this.polygons, polygon, polygon.getPolygonId());
        polygon.setPolygonId(-1);
        if (batchIndices != null) { // Batch removal.
            this.polygons.set(polygonIndex, null);
            batchIndices.setBit(polygonIndex, true);
        } else {
            this.polygons.remove(polygonIndex);
            for (int i = polygonIndex; i < this.polygons.size(); i++) // Update ids.
                this.polygons.get(i).setPolygonId(i);
        }

        if (this.cachedMesh != null)
            getMeshNode().remove(polygon);
    }

    /**
     * Removes all unused vertices from the Landscape.
     * @return numberOfVerticesRemoved
     */
    @SuppressWarnings("unchecked")
    public int removeUnusedVertices() {
        if (this.cachedMesh != null)
            this.cachedMesh.getEditableVertices().startBatchRemoval();

        // Populate bit array with the vertices to remove.
        this.cachedIndexBitArray.clear();
        for (int i = 0; i < this.vertices.size(); i++) {
            LandscapeVertex vertex = this.vertices.get(i);
            if (vertex == null || !vertex.getConnectedPolygons().isEmpty())
                continue;

            ((LandscapeNode<LandscapeVertex, ?>) vertex.getOwner()).removeVertexFromNode(vertex); // Remove from the nodes too.
            if (vertex.isRegistered())
                this.cachedIndexBitArray.setBit(i, true);
        }

        // Remove the vertices from the list.
        int removedVertexCount = this.cachedIndexBitArray.getBitCount();
        removeValuesAndUpdateIds(this.vertices, this.cachedIndexBitArray, LandscapeVertex::setVertexId);

        // Update mesh data.
        if (this.cachedMesh != null)
            this.cachedMesh.getEditableVertices().endBatchRemoval();

        // Done.
        this.cachedIndexBitArray.clear();
        return removedVertexCount;
    }

    /**
     * Get the nodes registered to the landscape.
     */
    public List<LandscapeNode<? extends LandscapeVertex, ? extends LandscapePolygon>> getNodes() {
        return this.immutableNodes;
    }

    /**
     * Adds a texture to the landscape.
     * @param texture the texture to add
     * @return true iff the texture was added successfully
     */
    public boolean addTexture(LandscapeTexture texture) {
        if (texture == null)
            throw new NullPointerException("texture");
        if (texture.getLandscape() != this)
            throw new RuntimeException("The provided texture is not applicable to the target Landscape!");
        if (texture.isRegistered())
            return false; // Already registered.

        texture.setTextureId(this.textures.size());
        this.textures.add(texture);
        return true;
    }

    /**
     * Removes a texture from the landscape.
     * @param texture the texture to remove
     * @return true iff the texture was removed successfully
     */
    public boolean removeTexture(LandscapeTexture texture) {
        if (texture == null)
            return false;
        if (!texture.getMaterials().isEmpty())
            throw new RuntimeException(texture.getMaterials().size() + " material(s) are still using the texture, so it cannot be removed!");
        if (!texture.getPolygons().isEmpty())
            throw new RuntimeException(texture.getPolygons().size() + " polygon(s) are still using the texture, so it cannot be removed!");

        // Abort if not registered.
        int textureIndex = indexOf(this.textures, texture, texture.getTextureId());
        if (textureIndex < 0)
            return false;

        // Remove texture.
        this.textures.remove(textureIndex);
        texture.setTextureId(-1);

        // Update subsequent IDs.
        for (int i = textureIndex; i < this.textures.size(); i++)
            this.textures.get(i).setTextureId(i);

        return true;
    }

    /**
     * Get the textures registered to the landscape.
     */
    public List<LandscapeTexture> getTextures() {
        return this.immutableTextures;
    }

    /**
     * Adds a material to the landscape.
     * @param material the material to add
     * @return true iff the material was added successfully
     */
    public boolean addMaterial(LandscapeMaterial material) {
        if (material == null)
            throw new NullPointerException("node");
        if (material.getLandscape() != this)
            throw new RuntimeException("The provided material is not applicable to the target Landscape!");
        if (material.isRegistered())
            return false; // Already registered.

        // If the material's texture is not registered, registering the material adds reliance on an unregistered texture, which is problematic.
        // We could consider causing the texture to become registered due to this.
        if (material.getTexture() != null && !material.getTexture().isRegistered())
            throw new RuntimeException("The texture is not registered in the Landscape, so the material using it is unable to be registered!");

        material.setMaterialId(this.materials.size());
        this.materials.add(material);

        // Add reference to the material. (Guaranteed to be registered above.)
        if (material.getTexture() != null)
            material.getTexture().getInternalMaterialList().add(material);

        return true;
    }

    /**
     * Removes a material from the landscape.
     * @param material the material to remove
     * @return true iff the material was removed successfully
     */
    public boolean removeMaterial(LandscapeMaterial material) {
        if (material == null)
            return false;
        if (!material.getPolygons().isEmpty())
            throw new RuntimeException(material.getPolygons().size() + " polygon(s) are still using the material, so it cannot be removed!");

        // Abort if not registered.
        int materialIndex = indexOf(this.materials, material, material.getMaterialId());
        if (materialIndex < 0)
            return false;

        // Remove material.
        this.materials.remove(materialIndex);
        material.setMaterialId(-1);
        if (material.getTexture() != null) // Remove material reference from texture.
            material.getTexture().getInternalMaterialList().remove(material);

        // Update subsequent IDs.
        for (int i = materialIndex; i < this.materials.size(); i++)
            this.materials.get(i).setMaterialId(i);

        return true;
    }

    /**
     * Get the materials registered to the landscape.
     */
    public List<LandscapeMaterial> getMaterials() {
        return this.immutableMaterials;
    }

    /**
     * Gets the class registry containing all available landscape node classes for this landscape type.
     */
    protected abstract GlobalClassRegistry<LandscapeNode<? extends LandscapeVertex, ? extends LandscapePolygon>> getGlobalNodeClassRegistry();

    /**
     * Gets the class registry containing all available landscape node classes for this landscape type.
     */
    protected abstract GlobalClassRegistry<LandscapeMaterial> getGlobalMaterialClassRegistry();

    /**
     * Gets the class registry containing all available landscape texture classes for this landscape type.
     */
    protected abstract GlobalClassRegistry<LandscapeTexture> getGlobalTextureClassRegistry();

    private static <T> int indexOfThrow(List<? super T> list, T object, int objectId) {
        int index;
        if (objectId >= 0 && list.size() > objectId && object == list.get(objectId)) {
            index = objectId;
        } else if ((index = list.indexOf(object)) < 0) {
            // Frankly something has probably gone wrong if the ID doesn't match to begin with.
            // But, it might be recoverable if we can still find it so... shrug.
            throw new RuntimeException("Couldn't find the object in the list. Something has gone wrong!");
        }

        return index;
    }

    private static <T> int indexOf(List<? super T> list, T object, int objectId) {
        if (objectId < 0 || (list.size() > objectId && object == list.get(objectId))) {
            return objectId;
        } else {
            // Frankly something has probably gone wrong if the ID doesn't match.
            // But, it might be recoverable if we can still find it so... shrug.
            return list.indexOf(object);
        }
    }

    private static <T> void removeValuesAndUpdateIds(List<T> list, IndexBitArray indices, BiConsumer<? super T, Integer> idSetter) {
        if (list.isEmpty())
            return;

        // Perform the queued removals.
        int totalIndexCount = indices.getBitCount();
        int removedGroups = 0;
        int currentIndex = indices.getFirstBitIndex();
        for (int i = 0; i < totalIndexCount; i++) {
            int nextIndex = ((totalIndexCount > i + 1) ? indices.getNextBitIndex(currentIndex) : list.size() - 1);
            int copyLength = (nextIndex - currentIndex);

            // If copy length is 0, that means there is a duplicate index (impossible), and we can just safely skip it.
            if (copyLength > 0) {
                int removeIndex = currentIndex - removedGroups;
                removedGroups++;
                for (int j = 0; j < copyLength; j++) {
                    // Unregister old object.
                    int dstIndex = removeIndex + j;
                    T deletedObject = list.get(dstIndex);
                    if (deletedObject != null)
                        idSetter.accept(deletedObject, -1);

                    // Move new object.
                    int srcIndex = removeIndex + removedGroups + j;
                    T srcObject = list.get(srcIndex);
                    list.set(dstIndex, srcObject);
                    if (srcObject != null) {
                        idSetter.accept(srcObject, dstIndex);
                        // We clear the object to prevent us reaching it later and setting its ID to -1.
                        // We want the object instance to be valid after it gets moved into dstIndex so -1 doesn't fly.
                        list.set(srcIndex, null);
                    }
                }
            }

            currentIndex = nextIndex;
        }

        // Remove values from the end which are now unused.
        for (int i = 0; i < totalIndexCount; i++)
            list.remove(list.size() - 1);
    }
}