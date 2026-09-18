package org.fentanylsolutions.anextratouch.handlers.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.IChunkProvider;

import org.junit.Test;

public class ItemSwingSoundTest {

    @Test
    public void remoteSwingPastGroundStartsAtEyesInsteadOfFeet() {
        EntityLivingBase entity = actor(new TestWorld(), 15.0F);

        // The old ray immediately enters the floor despite the actor looking above it within swing reach.
        assertEquals(MovingObjectType.BLOCK, entity.rayTrace(3.0D, 1.0F).typeOfHit);
        assertTrue(ItemSoundHandler.freeRemoteSwing(entity));
    }

    @Test
    public void remoteSwingActuallyAimedAtGroundIsStillExcluded() {
        assertFalse(ItemSoundHandler.freeRemoteSwing(actor(new TestWorld(), 80.0F)));
    }

    @Test
    public void remoteSwingAimedAtWallIsStillExcluded() {
        TestWorld world = new TestWorld();
        world.wall = true;
        assertFalse(ItemSoundHandler.freeRemoteSwing(actor(world, 0.0F)));
    }

    private static EntityLivingBase actor(TestWorld world, float pitch) {
        // Like remote players, mobs use feet-level posY. No player inventory or client startup is needed.
        EntityLivingBase entity = new EntityCow(world);
        entity.setPosition(0.5D, 4.0D, 0.5D);
        entity.rotationYaw = 0.0F;
        entity.rotationPitch = pitch;
        return entity;
    }

    /** Flat floor and optional wall, using vanilla block intersection without chunk loading or ticking. */
    private static final class TestWorld extends World {

        private final Block stone = new Block(Material.rock) {};
        private final Block air = new Block(Material.air) {

            @Override
            public boolean canCollideCheck(int metadata, boolean hitLiquids) {
                return false;
            }
        };
        boolean wall;

        TestWorld() {
            super(
                null,
                "item-swing-test",
                new WorldProviderSurface(),
                new WorldSettings(0L, WorldSettings.GameType.SURVIVAL, false, false, WorldType.DEFAULT),
                new Profiler());
            isRemote = true;
        }

        @Override
        public Block getBlock(int x, int y, int z) {
            return y < 4 || wall && z == 2 ? stone : air;
        }

        @Override
        public int getBlockMetadata(int x, int y, int z) {
            return 0;
        }

        @Override
        public ChunkCoordinates getSpawnPoint() {
            return new ChunkCoordinates(0, 4, 0);
        }

        @Override
        protected IChunkProvider createChunkProvider() {
            return null;
        }

        @Override
        protected int func_152379_p() {
            return 0;
        }

        @Override
        public Entity getEntityByID(int entityId) {
            return null;
        }
    }
}
