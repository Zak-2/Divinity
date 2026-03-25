package net.minecraft.client.optifine.config;

import net.minecraft.client.optifine.util.EntityUtils;
import net.minecraft.util.ResourceLocation;

public class EntityClassLocator implements IObjectLocator {
    public Object getObject(ResourceLocation loc) {
        Class oclass = EntityUtils.getEntityClassByName(loc.getResourcePath());
        return oclass;
    }
}
