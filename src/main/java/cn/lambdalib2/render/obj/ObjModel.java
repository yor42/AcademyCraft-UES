package cn.lambdalib2.render.obj;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.common.model.IModelState;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ObjModel {

    public final List<Vertex> vertices = new ArrayList<>();
    public final Multimap<String, Face> faces = LinkedHashMultimap.create();
    public FloatBuffer vertexBuffer;

    public static class Vertex {

        public final Vector3f pos;
        public final Vector2f uv;
        public final Vector3f tangent;
        public final Vector3f normal;

        public Vertex(Vector3f pos, Vector2f uv) {
            this.pos = pos;
            this.uv = uv;
            this.tangent = new Vector3f();
            this.normal = new Vector3f();
        }

    }

    public static class Face {

        public final int i0, i1, i2;
        public final Vector3f tangent = new Vector3f();
        public final Vector3f normal = new Vector3f();

        public Face(int i0, int i1, int i2) {
            this.i0 = i0;
            this.i1 = i1;
            this.i2 = i2;
        }

        public void store(IntBuffer buffer) {
            buffer.put(i0).put(i1).put(i2);
        }

        @Override
        public String toString() {
            return "Face{" +
                    "i0=" + i0 +
                    ", i1=" + i1 +
                    ", i2=" + i2 +
                    '}';
        }
    }

}
