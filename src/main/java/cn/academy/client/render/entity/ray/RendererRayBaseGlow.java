package cn.academy.client.render.entity.ray;

import cn.academy.entity.EntityRayBase;
import cn.academy.entity.IRay;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.RenderUtils;
import cn.lambdalib2.util.VecUtils;
import cn.lambdalib2.util.ViewOptimize;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.opengl.GL11.*;

/**
 * Renderer to draw glow texture
 * @author WeAthFolD
 */
public abstract class RendererRayBaseGlow<T extends EntityRayBase &IRay> extends Render<T> {
    
    {
        this.shadowOpaque = 0;
    }

    public RendererRayBaseGlow(RenderManager renderManager) {
        super(renderManager);
    }

    @Override
    public void doRender(T entity, double x, double y, double z,
            float yaw, float partialTicks) {

        Minecraft mc = Minecraft.getMinecraft();
        
        glPushMatrix();
        
        doTransform(entity);
        
        Vec3d position = entity.getRayPosition();
        Vec3d relativePosition = VecUtils.subtract(position,
                new Vec3d(x, y, y));
        glTranslated(x, y, z);
        
        //Calculate the most appropriate 'billboard-up' direction.
        //The ray viewing direction.
        Vec3d dir = VecUtils.toDirVector(entity, partialTicks);
        //Pick two far enough start and end point.
        Vec3d start = VecUtils.multiply(dir, entity.getStartFix()),
            end = VecUtils.add(start, VecUtils.multiply(dir, entity.getLength() - entity.getStartFix()));
        
        Vec3d upDir;
        boolean firstPerson = ViewOptimize.isFirstPerson(entity);
        if(firstPerson) {
            upDir = new Vec3d(0, 1, -0.5);
        } else {
            //Get closest point for view judging.
            Vec3d pt = new Vec3d(0, 0, 0);
            
            //The player viewing direction towards pt.
            Vec3d perpViewDir = VecUtils.add(pt, relativePosition);
            
            // cross product to get the 'up' vector
            upDir = VecUtils.crossProduct(perpViewDir, dir);
        }
        
        upDir = upDir.normalize();
        
        //DEBUG
//        GL11.glDisable(GL11.GL_TEXTURE_2D);
//        Tessellator t = Tessellator.instance;
//        
//        t.startDrawing(GL11.GL_LINES);
//        //VecUtils.tessellate(start);
//        //VecUtils.tessellate(end);
//        
//        VecUtils.tessellate(pt);
//        VecUtils.tessellate(VecUtils.add(pt, VecUtils.scalarMultiply(upDir, 5)));
//        t.draw();
//        
//        GL11.glEnable(GL11.GL_TEXTURE_2D);
        //DEBUG END
        
        if(entity.needsViewOptimize()) {
            yaw = MathUtils.lerpDegree(entity.prevRotationYaw, entity.rotationYaw, partialTicks);
            Vec3d vec = ViewOptimize.getFixVector(entity);
            vec = vec.rotateYaw((float) ((270 - yaw) / 180 * Math.PI));
            start = VecUtils.add(start, vec);
            
            // Don't fix end to get accurate pointing direction
            // end = VecUtils.add(end, vec);
        }
        
        doTransform(entity);
        
        //Now delegate to the render itself~
        draw(entity, start, end, upDir);
        
        glPopMatrix();
    }
    
    protected void doPostTransform(T ray) {}
    
    protected void doTransform(T ray) {}
    
    protected void drawBoard(Vec3d start, Vec3d end, Vec3d upDir, double width) {
        width /= 2;
        Vec3d
            v1 = VecUtils.add(start, VecUtils.multiply(upDir, width)),
            v2 = VecUtils.add(start, VecUtils.multiply(upDir, -width)),
            v3 = VecUtils.add(end,      VecUtils.multiply(upDir, -width)),
            v4 = VecUtils.add(end,   VecUtils.multiply(upDir, width));
        
        Tessellator t = Tessellator.getInstance();
        BufferBuilder buf = t.getBuffer();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
        RenderUtils.addVertexWithUV(buf, v1, 0, 1);
        RenderUtils.addVertexWithUV(buf, v2, 0, 0);
        RenderUtils.addVertexWithUV(buf, v3, 1, 0);
        RenderUtils.addVertexWithUV(buf, v4, 1, 1);
        t.draw();
    }
    
    /**
     * Draw the ray at the origin. The ray's heading direction should be toward x+, 
     * and normal is always in z direction.
     * @param start The start point
     * @param end The end point
     * @param sideDir the suggested billboard-up direction. You can ignore this if not drawing a billboard.
     */
    protected abstract void draw(T ray, Vec3d start, Vec3d end, Vec3d sideDir);

    @Override
    protected ResourceLocation getEntityTexture(T p_110775_1_) {
        return null;
    }

}