package org.fentanylsolutions.anextratouch.handlers.client.effects;

import net.coderbot.iris.block_rendering.BlockMaterialMapping;
import net.coderbot.iris.block_rendering.BlockRenderingSettings;
import net.coderbot.iris.gbuffer_overrides.matching.SpecialCondition;
import net.coderbot.iris.gl.shader.ProgramCreator;
import net.coderbot.iris.layer.GbufferPrograms;
import net.coderbot.iris.pipeline.WorldRenderingPhase;
import net.coderbot.iris.vertices.ExtendedDataHelper;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import com.gtnewhorizons.angelica.glsm.GLStateManager;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;

/** Optional Angelica bridge for existing meshes. Does not modify shader sources. */
@SideOnly(Side.CLIENT)
final class AngelicaWaterRenderer implements AngelicaShaderHelper.WaterRenderScope {

    private final SpecialCondition previousCondition;
    private final Boolean previousTranslucency;
    private final WorldRenderingPhase previousPhase;

    static AngelicaShaderHelper.WaterRenderScope begin() {
        if (IrisApi.getInstance()
            .isRenderingShadowPass()) return null;
        Reference2ObjectMap<Block, Int2IntMap> mappings = BlockRenderingSettings.INSTANCE.getBlockMetaMatches();
        if (mappings == null) return null;
        Int2IntMap water = mappings.get(Blocks.water);
        int id = water == null ? -1 : BlockMaterialMapping.resolveId(water, 0);
        if (id < 0) {
            water = mappings.get(Blocks.flowing_water);
            id = water == null ? -1 : BlockMaterialMapping.resolveId(water, 0);
        }
        if (id < 0) return null;

        return begin(WorldRenderingPhase.TERRAIN_TRANSLUCENT, (short) id, ExtendedDataHelper.FLUID_RENDER_TYPE);
    }

    static AngelicaShaderHelper.WaterRenderScope beginOverlay() {
        if (IrisApi.getInstance()
            .isRenderingShadowPass()) return null;
        // Use ordinary textured/colored shading so the pack does not replace foam RGB or alpha.
        return begin(WorldRenderingPhase.NONE, (short) -1, (short) -1);
    }

    private static AngelicaShaderHelper.WaterRenderScope begin(WorldRenderingPhase phase, short id, short type) {
        AngelicaWaterRenderer scope = new AngelicaWaterRenderer();
        try {
            GbufferPrograms.setupSpecialRenderCondition(null);
            GbufferPrograms.setTranslucencyDeclaration(Boolean.TRUE);
            GbufferPrograms.setOverridePhase(phase);
            // The immediate Tessellator path consumes the constant mc_Entity attribute.
            // Its water ID comes from this pack's block.properties, never a fixed numeric ID.
            GLStateManager.glVertexAttrib2s(ProgramCreator.MC_ENTITY, id, type);
            return scope;
        } catch (RuntimeException | LinkageError e) {
            scope.close();
            throw e;
        }
    }

    private AngelicaWaterRenderer() {
        previousCondition = GbufferPrograms.getSpecialCondition();
        previousTranslucency = GbufferPrograms.getDeclaredTranslucency();
        previousPhase = GbufferPrograms.getOverridePhase();
    }

    @Override
    public void close() {
        try {
            // These draws run at the main-world water boundary, outside entity renderers.
            // Match Angelica's own material reset through its backend (including non-GL backends).
            GLStateManager.glVertexAttrib2s(ProgramCreator.MC_ENTITY, (short) -1, (short) -1);
        } finally {
            GbufferPrograms.setupSpecialRenderCondition(previousCondition);
            GbufferPrograms.setTranslucencyDeclaration(previousTranslucency);
            GbufferPrograms.setOverridePhase(previousPhase);
        }
    }
}
