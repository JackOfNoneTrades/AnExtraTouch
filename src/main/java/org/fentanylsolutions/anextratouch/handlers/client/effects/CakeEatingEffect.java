package org.fentanylsolutions.anextratouch.handlers.client.effects;

import java.util.Random;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public final class CakeEatingEffect {

    private static final int PARTICLE_COUNT = 5;

    private CakeEatingEffect() {}

    public static void spawn(EntityPlayer player) {
        if (player == null || player.worldObj == null || !player.worldObj.isRemote) {
            return;
        }

        World world = player.worldObj;
        Random random = player.getRNG();
        ItemStack cake = new ItemStack(Items.cake);
        world.playSound(
            player.posX,
            player.posY - (double) player.yOffset,
            player.posZ,
            "random.eat",
            1.0F,
            1.0F,
            false);

        String particleName = "iconcrack_" + Item.getIdFromItem(cake.getItem());
        if (cake.getHasSubtypes()) {
            particleName = particleName + "_" + cake.getItemDamage();
        }

        for (int i = 0; i < PARTICLE_COUNT; ++i) {
            Vec3 velocity = Vec3
                .createVectorHelper(((double) random.nextFloat() - 0.5D) * 0.1D, Math.random() * 0.1D + 0.1D, 0.0D);
            velocity.rotateAroundX(-player.rotationPitch * (float) Math.PI / 180.0F);
            velocity.rotateAroundY(-player.rotationYaw * (float) Math.PI / 180.0F);

            Vec3 position = Vec3.createVectorHelper(
                ((double) random.nextFloat() - 0.5D) * 0.3D,
                (double) (-random.nextFloat()) * 0.6D - 0.3D,
                0.6D);
            position.rotateAroundX(-player.rotationPitch * (float) Math.PI / 180.0F);
            position.rotateAroundY(-player.rotationYaw * (float) Math.PI / 180.0F);
            position = position.addVector(player.posX, player.posY + (double) player.getEyeHeight(), player.posZ);

            world.spawnParticle(
                particleName,
                position.xCoord,
                position.yCoord,
                position.zCoord,
                velocity.xCoord,
                velocity.yCoord + 0.05D,
                velocity.zCoord);
        }
    }
}
