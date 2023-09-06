package thut.wearables;

import java.util.Map;
import java.util.function.Supplier;

import com.google.common.collect.Maps;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thut.lib.RegHelper;

public class RecipeDye extends CustomRecipe
{
    private static Map<DyeColor, TagKey<Item>> DYE_TAGS = Maps.newHashMap();

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister
            .create(ForgeRegistries.RECIPE_SERIALIZERS, ThutWearables.MODID);

    public static final RegistryObject<SimpleCraftingRecipeSerializer<RecipeDye>> SERIALIZER = RecipeDye.RECIPE_SERIALIZERS
            .register("dye", RecipeDye.special(RecipeDye::new));

    private static <T extends CraftingRecipe> Supplier<SimpleCraftingRecipeSerializer<T>> special(
            final SimpleCraftingRecipeSerializer.Factory<T> create)
    {
        return () -> new SimpleCraftingRecipeSerializer<>(create);
    }

    public static Map<DyeColor, TagKey<Item>> getDyeTagMap()
    {
        if (RecipeDye.DYE_TAGS.isEmpty()) for (final DyeColor colour : DyeColor.values())
        {
            final ResourceLocation tag = new ResourceLocation("forge", "dyes/" + colour.getName());
            RecipeDye.DYE_TAGS.put(colour, TagKey.create(RegHelper.ITEM_REGISTRY, tag));
        }
        return RecipeDye.DYE_TAGS;
    }

    public RecipeDye(final ResourceLocation location, CraftingBookCategory bookCategory)
    {
        super(location, bookCategory);
    }

    @Override
    public boolean canCraftInDimensions(final int width, final int height)
    {
        return width * height > 1;
    }

    @Override
    public ItemStack assemble(final CraftingContainer inv, RegistryAccess access)
    {
        ItemStack wearable = ItemStack.EMPTY;
        ItemStack dye = ItemStack.EMPTY;
        for (int i = 0; i < inv.getContainerSize(); i++)
        {
            final ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            IWearable wear = stack.getCapability(ThutWearables.WEARABLE_CAP, null).orElse(null);
            if (wear == null && stack.getItem() instanceof IWearable) wear = (IWearable) stack.getItem();
            if (wear != null && wear.dyeable(stack))
            {
                wearable = stack;
                continue;
            }
            final TagKey<Item> dyeTag = Tags.Items.DYES;
            if (stack.is(dyeTag))
            {
                dye = stack;
                continue;
            }
            return ItemStack.EMPTY;
        }
        final ItemStack output = wearable.copy();
        if (!output.hasTag()) output.setTag(new CompoundTag());
        DyeColor dyeColour = null;

        final Map<DyeColor, TagKey<Item>> tags = RecipeDye.getDyeTagMap();
        for (final DyeColor colour : DyeColor.values()) if (dye.is(tags.get(colour)))
        {
            dyeColour = colour;
            break;
        }
        output.getTag().putInt("dyeColour", dyeColour.getId());
        return output;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(final CraftingContainer inv)
    {
        final NonNullList<ItemStack> nonnulllist = NonNullList.<ItemStack>withSize(inv.getContainerSize(),
                ItemStack.EMPTY);
        for (int i = 0; i < nonnulllist.size(); ++i)
        {
            final ItemStack itemstack = inv.getItem(i);
            nonnulllist.set(i, this.toKeep(i, itemstack, inv));
        }
        return nonnulllist;
    }

    @Override
    public RecipeSerializer<?> getSerializer()
    {
        return RecipeDye.SERIALIZER.get();
    }

    @Override
    public boolean matches(final CraftingContainer inv, final Level worldIn)
    {
        boolean wearable = false;
        boolean dye = false;
        for (int i = 0; i < inv.getContainerSize(); i++)
        {
            final ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            IWearable wear = stack.getCapability(ThutWearables.WEARABLE_CAP, null).orElse(null);
            if (wear == null && stack.getItem() instanceof IWearable w) wear = w;
            // Let vanilla recipe handle this one.
            if (stack.getItem() instanceof DyeableLeatherItem) return false;
            if (wear != null && wear.dyeable(stack))
            {
                if (wearable) return false;
                wearable = true;
                continue;
            }
            final TagKey<Item> dyeTag = Tags.Items.DYES;
            if (stack.is(dyeTag))
            {
                if (dye) return false;
                dye = true;
                continue;
            }
            return false;
        }
        return dye && wearable;
    }

    public ItemStack toKeep(final int slot, final ItemStack stackIn, final CraftingContainer inv)
    {
        return net.minecraftforge.common.ForgeHooks.getCraftingRemainingItem(stackIn);
    }

}
