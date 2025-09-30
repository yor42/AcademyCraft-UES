package cn.academy.client.render.block;

import cn.academy.Resources;
import cn.academy.block.tileentity.TileWindGenMain;
import cn.lambdalib2.multiblock.RenderBlockMulti;
import cn.lambdalib2.registry.mc.RegTileEntityRender;
import cn.lambdalib2.render.obj.ObjLegacyRender;
import cn.lambdalib2.render.obj.ObjVBORenderer;
import cn.lambdalib2.render.obj.ObjVaoRenderer;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.RenderUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * @author WeAthFolD
 */
public class RenderWindGenMain extends RenderBlockMulti<TileWindGenMain> {
    @RegTileEntityRender(TileWindGenMain.class)
    private static final RenderWindGenMain instance = new RenderWindGenMain();

    ObjVaoRenderer
        mdlBody = Resources.getModel("windgen_main"),
        mdlFan = Resources.getModel("windgen_fan");

    ResourceLocation
        texBody = Resources.getTexture("models/windgen_main"),
        texFan = Resources.getTexture("models/windgen_fan");

    @Override
    public void drawAtOrigin(TileWindGenMain gen) {
        GlStateManager.pushMatrix();
        // draw body
        RenderUtils.loadTexture(texBody);
        mdlBody.justRenderAll();


        // draw fan
        if (gen.isFanInstalled() && gen.noObstacle) {
            // update fan rotation
            double time = GameTimer.getTime();
            double dt = gen.lastFrame == -1 ? 0 : time - gen.lastFrame;
            gen.lastFrame = time;
            gen.lastRotation += (float) (gen.getSpinSpeed() * dt);

            GlStateManager.pushMatrix();
            GlStateManager.translate(0, 0.5, 0.82);
            GlStateManager.rotate(gen.lastRotation, 0, 0, -1);
            RenderUtils.loadTexture(texFan);
            mdlFan.renderAll();
            GlStateManager.popMatrix();
        }

        GlStateManager.popMatrix();
    }

}