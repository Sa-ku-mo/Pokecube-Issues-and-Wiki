package pokecube.legends.items;

import java.util.List;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.IItemTier;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class LegendsSword extends SwordItem
{
	String  tooltipname;
	boolean hasTooltip = true;
	boolean hasShiny = false;

    public LegendsSword(final IItemTier material, final int bonusDamage, final float attackSpeed, final Properties properties, final ItemGroup group)
    {
        super(material, bonusDamage, attackSpeed, properties.tab(group));
    }

    public LegendsSword setTooltipName(final String tooltipname)
    {
        this.tooltipname = tooltipname;
        return this;
    }

    public LegendsSword setShiny(){
    	this.hasShiny = true;
    	return this;
    }

    public LegendsSword noTooltop()
    {
        this.hasTooltip = false;
        return this;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(final ItemStack stack, final World worldIn, final List<ITextComponent> tooltip,
            final ITooltipFlag flagIn)
    {
        if (!this.hasTooltip) return;
        String message;
        if (Screen.hasShiftDown()) message = I18n.get("legends." + this.tooltipname + ".tooltip", TextFormatting.GOLD, TextFormatting.BOLD, TextFormatting.RESET);
        else message = I18n.get("pokecube.tooltip.advanced");
        tooltip.add(new TranslationTextComponent(message));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean isFoil(final ItemStack itemstack)
    {
        return this.hasShiny;
    }
}