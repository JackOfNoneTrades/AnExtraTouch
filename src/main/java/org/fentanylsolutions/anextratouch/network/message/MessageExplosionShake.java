package org.fentanylsolutions.anextratouch.network.message;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class MessageExplosionShake implements IMessage {

    public double posX, posY, posZ;
    public float size;

    public MessageExplosionShake() {}

    public MessageExplosionShake(double posX, double posY, double posZ, float size) {
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;

        this.size = size;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.posX = buf.readDouble();
        this.posY = buf.readDouble();
        this.posZ = buf.readDouble();

        this.size = buf.readFloat();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeDouble(posX);
        buf.writeDouble(posY);
        buf.writeDouble(posZ);

        buf.writeFloat(size);
    }
}
