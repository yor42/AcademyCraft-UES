package cn.lambdalib2.render.obj;

import cn.lambdalib2.render.obj.ObjModel.Face;
import cn.lambdalib2.render.obj.ObjModel.Vertex;
import cn.lambdalib2.util.Debug;
import cn.lambdalib2.util.ResourceUtils;
import com.google.common.base.Throwables;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.obj.OBJModel;
import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.FloatBuffer;
import java.util.*;

public class ObjParser {

    public static ObjModel parse(ResourceLocation res) {
        try {
            return parse(new InputStreamReader(ResourceUtils.getResourceStream(res)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse obj model: " + res, e);
        }
    }

    private static ObjModel parse(Reader rdr0) {
        List<Vector3f> vs = new ArrayList<>();
        List<Vector2f> vts = new ArrayList<>();
        // Use a LinkedHashMap to preserve insertion order for groups
        Map<String, List<ObjFace>> faces = new LinkedHashMap<>();

        try (BufferedReader rdr = new BufferedReader(rdr0)) {
            try {
                String currentGroup = "Default";
                faces.put(currentGroup, new ArrayList<>());

                String ln;
                while ((ln = rdr.readLine()) != null) {
                    ln = ln.trim();
                    if (ln.isEmpty() || ln.startsWith("#")) {
                        continue;
                    }

                    String[] parts = ln.split("\\s+");
                    switch (parts[0]) {
                        case "v":
                            vs.add(new Vector3f(Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3])));
                            break;
                        case "vt":
                            vts.add(new Vector2f(Float.parseFloat(parts[1]), Float.parseFloat(parts[2])));
                            break;
                        case "g":
                            currentGroup = parts[1];
                            if (!faces.containsKey(currentGroup)) {
                                faces.put(currentGroup, new ArrayList<>());
                            }
                            break;
                        case "f":
                            int[] v1 = parseFaceVertex(parts[1]);
                            int[] v2 = parseFaceVertex(parts[2]);
                            int[] v3 = parseFaceVertex(parts[3]);

                            List<ObjFace> groupFaces = faces.get(currentGroup);
                            if (parts.length == 5) { // Quad
                                int[] v4 = parseFaceVertex(parts[4]);
                                groupFaces.add(new ObjFace(v1, v2, v3));
                                groupFaces.add(new ObjFace(v1, v3, v4));
                            } else { // Triangle
                                groupFaces.add(new ObjFace(v1, v2, v3));
                            }
                            break;
                        case "usemtl":
                        case "mtllib":
                        case "vn":
                        case "s":
                            break;
                        default:
                            Debug.log("ObjParser: Unknown token '" + parts[0] + "'");
                            break;
                    }
                }
            } catch (IOException ex) {
                Throwables.propagate(ex);
            }
        } catch (IOException ex) {
            // Ignore
        }

        ObjModel ret = new ObjModel();
        List<Vertex> allVertices = new ArrayList<>();
        Map<VertexIdt, Vector3f> smoothNormals = new HashMap<>();

        // First pass: Calculate smooth normals for all vertices
        for (List<ObjFace> faceList : faces.values()) {
            for (ObjFace face : faceList) {
                VertexIdt idt0 = new VertexIdt(face.v0[0], face.v0[1]);
                VertexIdt idt1 = new VertexIdt(face.v1[0], face.v1[1]);
                VertexIdt idt2 = new VertexIdt(face.v2[0], face.v2[1]);

                Vector3f pos0 = vs.get(idt0.vert);
                Vector3f pos1 = vs.get(idt1.vert);
                Vector3f pos2 = vs.get(idt2.vert);

                Vector3f faceNormal = Vector3f.cross(Vector3f.sub(pos1, pos0, null), Vector3f.sub(pos2, pos0, null), null);

                addNormal(smoothNormals, idt0, faceNormal);
                addNormal(smoothNormals, idt1, faceNormal);
                addNormal(smoothNormals, idt2, faceNormal);
            }
        }

        // Normalize all the accumulated normals
        for(Vector3f normal : smoothNormals.values()) {
            normal.normalise();
        }

        // Second pass: Build vertices list and FloatBuffer in the correct order
        for (String group : faces.keySet()) {
            for (ObjFace face : faces.get(group)) {
                Vertex v0 = createVertex(vs, vts, smoothNormals, face.v0);
                Vertex v1 = createVertex(vs, vts, smoothNormals, face.v1);
                Vertex v2 = createVertex(vs, vts, smoothNormals, face.v2);

                ret.vertices.add(v0);
                ret.vertices.add(v1);
                ret.vertices.add(v2);
                // We no longer need the separate `faces` map as vertices are ordered
            }
        }

        // Create the FloatBuffer for VBO
        FloatBuffer buffer = BufferUtils.createFloatBuffer(ret.vertices.size() * 8); // pos(3) + normal(3) + uv(2)
        for (Vertex v : ret.vertices) {
            buffer.put(v.pos.x).put(v.pos.y).put(v.pos.z);
            buffer.put(v.normal.x).put(v.normal.y).put(v.normal.z);
            buffer.put(v.uv.x).put(1 - v.uv.y); // Y-flip for standard OpenGL UV
        }
        buffer.flip();
        ret.vertexBuffer = buffer;

        // Populate the face map for ObjLegacyRender compatibility if needed, though it's not used by VBO renderer
        int i = 0;
        for (String group : faces.keySet()) {
            for (int j = 0; j < faces.get(group).size(); ++j) {
                ret.faces.put(group, new Face(i, i + 1, i + 2));
                i += 3;
            }
        }

        return ret;
    }

    private static void addNormal(Map<VertexIdt, Vector3f> map, VertexIdt idt, Vector3f normal) {
        Vector3f current = map.get(idt);
        if (current == null) {
            current = new Vector3f();
            map.put(idt, current);
        }
        Vector3f.add(current, normal, current);
    }

    private static Vertex createVertex(List<Vector3f> vs, List<Vector2f> vts, Map<VertexIdt, Vector3f> smoothNormals, int[] data) {
        VertexIdt idt = new VertexIdt(data[0], data[1]);
        Vertex vert = new Vertex(vs.get(idt.vert), vts.get(idt.tex));
        vert.normal.set(smoothNormals.get(idt));
        return vert;
    }

    private static int[] parseFaceVertex(String v) {
        String[] arr = v.split("/");
        int[] ret = new int[2];
        ret[0] = Integer.parseInt(arr[0]) - 1;
        if (arr.length > 1 && !arr[1].isEmpty()) {
            ret[1] = Integer.parseInt(arr[1]) - 1;
        } else {
            ret[1] = ret[0]; // Fallback if no texture coord
        }
        return ret;
    }

    private static class ObjFace {
        final int[] v0, v1, v2;
        public ObjFace(int[] v0, int[] v1, int[] v2) {
            this.v0 = v0;
            this.v1 = v1;
            this.v2 = v2;
        }
    }

    private static class VertexIdt {
        final int vert, tex;
        public VertexIdt(int vert, int tex) {
            this.vert = vert;
            this.tex = tex;
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VertexIdt vertexIdt = (VertexIdt) o;
            return vert == vertexIdt.vert && tex == vertexIdt.tex;
        }
        @Override
        public int hashCode() {
            return 31 * vert + tex;
        }
    }
}