package cn.lambdalib2.render.obj;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * An optimized OBJ model renderer using Vertex Buffer Objects (VBO) and Vertex Array Objects (VAO).
 * This approach minimizes OpenGL state changes during rendering for significantly better performance.
 */
public class ObjVaoRenderer {

    private final ObjModel model;
    private final Map<String, int[]> partInfo = new HashMap<>(); // partName -> {vertex_offset, vertex_count}

    private int vaoID = -1;
    private int vboID = -1;
    private boolean initialized = false;

    public ObjVaoRenderer(ObjModel model) {
        this.model = model;
    }

    private void initialize() {
        if (initialized) return;

        // 1. Create and buffer VBO data
        vboID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferData(GL_ARRAY_BUFFER, model.vertexBuffer, GL_STATIC_DRAW);

        // 2. Create VAO and encapsulate vertex attribute state
        vaoID = glGenVertexArrays();
        glBindVertexArray(vaoID);

        // Bind the VBO to the VAO's context
        glBindBuffer(GL_ARRAY_BUFFER, vboID);

        // Configure vertex attributes (position, normal, texCoords).
        // This state is now stored inside the VAO.
        glEnableClientState(GL_VERTEX_ARRAY);
        glEnableClientState(GL_NORMAL_ARRAY);
        glEnableClientState(GL_TEXTURE_COORD_ARRAY);

        final int stride = 8 * 4; // (pos(3) + normal(3) + uv(2)) * sizeof(float)
        glVertexPointer(3, GL_FLOAT, stride, 0);
        glNormalPointer(GL_FLOAT, stride, 3 * 4);    // Offset: 3 floats
        glTexCoordPointer(2, GL_FLOAT, stride, 6 * 4); // Offset: 6 floats

        // 3. Unbind VAO and VBO to clean up state
        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glDisableClientState(GL_VERTEX_ARRAY);
        glDisableClientState(GL_NORMAL_ARRAY);
        glDisableClientState(GL_TEXTURE_COORD_ARRAY);


        // Pre-calculate rendering information for each part.
        int offset = 0;
        for (String group : model.faces.keySet()) {
            int count = model.faces.get(group).size() * 3; // 3 vertices per face
            partInfo.put(group, new int[]{offset, count});
            offset += count;
        }

        initialized = true;
    }

    /**
     * Prepares the renderer for drawing. Call this once before a batch of render calls.
     */
    public void begin() {
        if (!initialized) {
            initialize();
        }
        if (vaoID != -1) {
            glBindVertexArray(vaoID);
        }
    }

    /**
     * Renders all parts of the model. Must be called between begin() and end().
     */
    public void renderAll() {
        if (!initialized) {
            initialize();
        }
        if (vaoID == -1 || model.vertices.isEmpty()) return;

        glBindVertexArray(vaoID);
        glDrawArrays(GL_TRIANGLES, 0, model.vertices.size());
        glBindVertexArray(0);
    }

    /**
     * Renders a specific part of the model. Must be called between begin() and end().
     * @param part The name of the part to render.
     */
    public void renderPart(String part) {
        int[] info = partInfo.get(part);
        if (info != null && vaoID != -1) {
            glDrawArrays(GL_TRIANGLES, info[0], info[1]);
        }
    }

    /**
     * Restores the GL state. Call this once after a batch of render calls.
     */
    public void end() {
        if (vaoID != -1) {
            glBindVertexArray(0);
        }
    }

    /**
     * A utility method for simple, one-off rendering.
     * For performance, use the begin()/render...()/end() pattern when drawing multiple times.
     */
    public void justRenderAll() {
        begin();
        renderAll();
        end();
    }

    /**
     * Call this when the model is no longer needed to free up GPU memory.
     */
    public void dispose() {
        if (vboID != -1) {
            glDeleteBuffers(vboID);
            vboID = -1;
        }
        if (vaoID != -1) {
            glDeleteVertexArrays(vaoID);
            vaoID = -1;
        }
        initialized = false;
    }

}