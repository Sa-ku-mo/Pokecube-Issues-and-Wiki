package pokecube.legends.fluids;

import com.mojang.math.Vector3f;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;
import pokecube.legends.PokecubeLegends;
import pokecube.legends.init.FluidInit;
import pokecube.legends.init.ItemInit;

public class DistorticWaterType extends FluidType
{
    public static final ResourceLocation UNDERWATER_LOCATION = new ResourceLocation("pokecube_legends:textures/misc/distortic_underwater.png");
    public static final ResourceLocation DISTORTIC_WATER_STILL = new ResourceLocation("pokecube_legends:block/distortic_water_still");
    public static final ResourceLocation DISTORTIC_WATER_FLOWING = new ResourceLocation("pokecube_legends:block/distortic_water_flowing");
    public static final ResourceLocation DISTORTIC_WATER_OVERLAY = new ResourceLocation("pokecube_legends:block/distortic_water_overlay");

    public DistorticWaterType(Properties properties)
    {
        super(properties);
    }

    public static ForgeFlowingFluid.Properties makeProperties()
    {
        return new ForgeFlowingFluid.Properties(DISTORTIC_WATER_TYPE, FluidInit.DISTORTIC_WATER,
                FluidInit.DISTORTIC_WATER_FLOWING).bucket(ItemInit.DISTORTIC_WATER_BUCKET)
                .block(FluidInit.DISTORTIC_WATER_BLOCK);
    }


    public static final RegistryObject<FluidType> DISTORTIC_WATER_TYPE = PokecubeLegends.FLUID_TYPES.register("distortic_water", () -> new FluidType(
            FluidType.Properties.create().descriptionId("block.pokecube_legends.distortic_water")
                    .density(1000).temperature(100).viscosity(1000).lightLevel(0).supportsBoating(true)
                    .canExtinguish(true).canConvertToSource(true).canHydrate(true).rarity(Rarity.RARE)) {
        @Override
        public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
            consumer.accept(new IClientFluidTypeExtensions() {
                @Override
                public ResourceLocation getStillTexture() {
                    return DISTORTIC_WATER_STILL;
                }

                @Override
                public ResourceLocation getFlowingTexture() {
                    return DISTORTIC_WATER_FLOWING;
                }

                @Nullable
                @Override
                public ResourceLocation getOverlayTexture() {
                    return DISTORTIC_WATER_OVERLAY;
                }

                @Override
                public ResourceLocation getRenderOverlayTexture(Minecraft mc) {
                    return UNDERWATER_LOCATION;
                }

                @Override
                public int getTintColor() {
                    return 0x99d7efcc;
                }

                @Override
                public @NotNull Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount, Vector3f fluidFogColor) {
                    int color = this.getTintColor();
                    return new Vector3f((color >> 16 & 0xFF) / 255F, (color >> 8 & 0xFF) / 255F, (color & 0xFF) / 255F);
                }
            });
        }
    });

}