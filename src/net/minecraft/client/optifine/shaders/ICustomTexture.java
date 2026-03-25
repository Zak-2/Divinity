package net.minecraft.client.optifine.shaders;

public interface ICustomTexture {
    int getTextureId();

    int getTextureUnit();

    void deleteTexture();

    int getTarget();
}
