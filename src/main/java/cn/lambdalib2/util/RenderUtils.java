package cn.lambdalib2.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

import java.lang.reflect.Field;

/**
 * Modernized rendering utilities for MC 1.12.2
 * Uses BufferBuilder and GlStateManager instead of direct GL calls
 * @author WeAthFolD (modernized)
 */
public class RenderUtils {

    private static int textureState = -1;

    /**
     * @deprecated Use addVertexWithUV with BufferBuilder instead.
     * This method is provided for backward compatibility but uses immediate mode.
     * Immediate mode is slower and should be avoided in new code.
     */
    @Deprecated
    public static void addVertexLegacy(Vec3d vertex, double u, double v) {
        GL11.glTexCoord2d(u, v);
        GL11.glVertex3d(vertex.x, vertex.y, vertex.z);
    }

    //-----------------Texture State Management-----------------------------

    /**
     * Stores the current texture state using direct GL calls
     * Note: We avoid GlStateManager.pushAttributes() due to cache invalidation bug
     * See: <a href="https://github.com/MinecraftForge/MinecraftForge/issues/1637">...</a>
     */
    public static void pushTextureState() {
        if (textureState != -1) {
            throw new RuntimeException("RenderUtils:Texture State Overflow");
        }
        textureState = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
    }

    /**
     * Restores the stored texture state
     */
    public static void popTextureState() {
        if (textureState == -1) {
            throw new RuntimeException("RenderUtils:Texture State Underflow");
        }
        GlStateManager.bindTexture(textureState);
        textureState = -1;
    }

    //-----------------Vertex Helpers-----------------------------

    /**
     * Add a vertex with UV coordinates to the BufferBuilder
     * Use this with Minecraft's Tessellator system
     */
    public static void addVertexWithUV(BufferBuilder buffer, Vec3d vertex, double u, double v) {
        buffer.pos(vertex.x, vertex.y, vertex.z).tex(u, v).endVertex();
    }

    public static void addVertexWithUV(BufferBuilder buffer, double x, double y, double z, double u, double v) {
        buffer.pos(x, y, z).tex(u, v).endVertex();
    }

    //-----------------State Management-----------------------------

    public static void glTranslate(Vec3d v) {
        GlStateManager.translate(v.x, v.y, v.z);
    }

    public static void loadTexture(ResourceLocation src) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(src);
    }

    //-----------------Drawing Methods-----------------------------

    public static void drawEquippedItem(double width, ResourceLocation front, ResourceLocation back) {
        drawEquippedItem(width, front, back, 0, 0, 1, 1, false);
    }

    /**
     * Draws an equipped item using BufferBuilder for better performance
     * This batches all vertices before submitting to GPU
     */
    private static void drawEquippedItem(double w, ResourceLocation front, ResourceLocation back,
                                         double u1, double v1, double u2, double v2, boolean faceOnly) {

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        // Pre-calculate vertices (reusable)
        Vec3d[] vertices = {
                new Vec3d(0, 0, w),  // a1
                new Vec3d(1, 0, w),  // a2
                new Vec3d(1, 1, w),  // a3
                new Vec3d(0, 1, w),  // a4
                new Vec3d(0, 0, -w), // a5
                new Vec3d(1, 0, -w), // a6
                new Vec3d(1, 1, -w), // a7
                new Vec3d(0, 1, -w)  // a8
        };

        GlStateManager.pushMatrix();

        // Back face
        loadTexture(back);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(vertices[0].x, vertices[0].y, vertices[0].z).tex(u2, v2).endVertex();
        buffer.pos(vertices[1].x, vertices[1].y, vertices[1].z).tex(u1, v2).endVertex();
        buffer.pos(vertices[2].x, vertices[2].y, vertices[2].z).tex(u1, v1).endVertex();
        buffer.pos(vertices[3].x, vertices[3].y, vertices[3].z).tex(u2, v1).endVertex();
        tessellator.draw();

        // Front face
        loadTexture(front);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(vertices[7].x, vertices[7].y, vertices[7].z).tex(u2, v1).endVertex();
        buffer.pos(vertices[6].x, vertices[6].y, vertices[6].z).tex(u1, v1).endVertex();
        buffer.pos(vertices[5].x, vertices[5].y, vertices[5].z).tex(u1, v2).endVertex();
        buffer.pos(vertices[4].x, vertices[4].y, vertices[4].z).tex(u2, v2).endVertex();
        tessellator.draw();

        // Side faces (only if not faceOnly)
        if (!faceOnly) {
            int tileSize = 32;
            float tx = 1.0f / (32 * tileSize);
            float tz = 1.0f / tileSize;

            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);

            for (int i = 0; i < tileSize; i++) {
                float segment = (float) i / tileSize;
                double uCoord = u2 - (u2 - u1) * segment - tx;

                // Left side
                buffer.pos(segment, 0.0, -w).tex(uCoord, v2).endVertex();
                buffer.pos(segment, 0.0, w).tex(uCoord, v2).endVertex();
                buffer.pos(segment, 1.0, w).tex(uCoord, v1).endVertex();
                buffer.pos(segment, 1.0, -w).tex(uCoord, v1).endVertex();

                // Right side
                buffer.pos(segment, 1.0, w).tex(uCoord, v1).endVertex();
                buffer.pos(segment, 0.0, w).tex(uCoord, v2).endVertex();
                buffer.pos(segment, 0.0, -w).tex(uCoord, v2).endVertex();
                buffer.pos(segment, 1.0, -w).tex(uCoord, v1).endVertex();
            }

            tessellator.draw();
        }

        GlStateManager.popMatrix();
    }

    //-----------------Shader Mod Core Support-----------------------------

    static final String _shadersClassName = "shadersmodcore.client.Shaders";
    static boolean smcSupportInit = false;
    static boolean smcPresent = false;
    static Field fIsShadowPass;

    /**
     * Check if rendering in shadow pass (for ShaderMod compatibility)
     * @return true if in shadow pass, false otherwise
     */
    public static boolean isInShadowPass() {
        if (!smcSupportInit) {
            initSMCSupport();
        }

        if (smcPresent) {
            try {
                return fIsShadowPass.getBoolean(null);
            } catch (Exception e) {
                Debug.error("Exception in isInShadowPass", e);
            }
        }
        return false;
    }

    private static void initSMCSupport() {
        try {
            Class<?> shadersClass = Class.forName(_shadersClassName);
            fIsShadowPass = shadersClass.getField("isShadowPass");
            smcPresent = true;
            Debug.log("LambdaLib SMC support successfully initialized.");
        } catch (Exception e) {
            Debug.error("LambdaLib SMC support isn't initialized.");
            smcPresent = false;
        }

        smcSupportInit = true;
    }

    //-----------------Screen Effects-----------------------------

    /**
     * Draws a semi-transparent black overlay
     * Modernized to use GlStateManager
     */
    public static void drawBlackout() {
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.disableCull();
        GlStateManager.disableDepth();
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();

        // Setup orthographic projection
        GL11.glOrtho(0, 1, 1, 0, -1, 1);

        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // Draw the blackout quad
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(0, 0, 0).color(0, 0, 0, 178).endVertex(); // 0.7 * 255 ≈ 178
        buffer.pos(1, 0, 0).color(0, 0, 0, 178).endVertex();
        buffer.pos(1, 1, 0).color(0, 0, 0, 178).endVertex();
        buffer.pos(0, 1, 0).color(0, 0, 0, 178).endVertex();
        tessellator.draw();

        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();

        GlStateManager.popMatrix();
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.popMatrix();

        GlStateManager.enableDepth();
        GlStateManager.enableCull();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
    }
}