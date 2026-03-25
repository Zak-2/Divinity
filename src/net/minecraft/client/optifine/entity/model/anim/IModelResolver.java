package net.minecraft.client.optifine.entity.model.anim;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.optifine.expr.IExpressionResolver;

public interface IModelResolver extends IExpressionResolver {
    ModelRenderer getModelRenderer(String var1);

    ModelVariableFloat getModelVariable(String var1);
}
