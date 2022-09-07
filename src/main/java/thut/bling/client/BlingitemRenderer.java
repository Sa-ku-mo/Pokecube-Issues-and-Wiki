package thut.bling.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms.TransformType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.IItemRenderProperties;
import thut.wearables.IWearable;
import thut.wearables.ThutWearables;

public class BlingitemRenderer extends BlockEntityWithoutLevelRenderer implements IItemRenderProperties
{
    public static final BlingitemRenderer INSTANCE = new BlingitemRenderer();

    public BlingitemRenderer()
    {
        super(null, null);
    }

    private IWearable getWearable(final ItemStack stack)
    {
        if (stack.getItem() instanceof IWearable) return (IWearable) stack.getItem();
        return stack.getCapability(ThutWearables.WEARABLE_CAP, null).orElse(null);
    }

    @Override
    public void renderByItem(ItemStack item, TransformType transform, PoseStack mat, MultiBufferSource bufs, int light,
            int overlay)
    {
        IWearable wearable = this.getWearable(item);
        if (wearable == null)// || Util.getCustomModel(wearable.getSlot(item),
                             // item) == null)
        {
            IItemRenderProperties.super.getItemStackRenderer().renderByItem(item, transform, mat, bufs, light, overlay);
            return;
        }
        BlingRender.INSTANCE.initModels();
        mat.pushPose();
        mat.scale(1, -1, -1);
        mat.mulPose(Vector3f.YP.rotationDegrees(180));
        mat.mulPose(Vector3f.XP.rotationDegrees(180));
        mat.translate(-0.5, 0, -0.5);
        wearable.renderWearable(mat, bufs, wearable.getSlot(item), 0, Minecraft.getInstance().player, item, 0, light,
                overlay);
        mat.popPose();
    }

    @Override
    public BlockEntityWithoutLevelRenderer getItemStackRenderer()
    {
        return this;
    }
}