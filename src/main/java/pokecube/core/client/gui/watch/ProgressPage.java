package pokecube.core.client.gui.watch;

import java.util.List;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import pokecube.core.PokecubeCore;
import pokecube.core.client.gui.watch.progress.GlobalProgress;
import pokecube.core.client.gui.watch.progress.PerMobProgress;
import pokecube.core.client.gui.watch.progress.PerTypeProgress;
import pokecube.core.client.gui.watch.progress.Progress;
import pokecube.core.client.gui.watch.util.PageWithSubPages;

public class ProgressPage extends PageWithSubPages<Progress>
{
    public static List<Class<? extends Progress>> PAGELIST = Lists.newArrayList();

    static
    {
        ProgressPage.PAGELIST.add(GlobalProgress.class);
        ProgressPage.PAGELIST.add(PerTypeProgress.class);
        ProgressPage.PAGELIST.add(PerMobProgress.class);
    }

    private static Progress makePage(final Class<? extends Progress> clazz, final GuiPokeWatch parent)
    {
        try
        {
            return clazz.getConstructor(GuiPokeWatch.class).newInstance(parent);
        }
        catch (final Exception e)
        {
            PokecubeCore.LOGGER.error("Error with making a page for watch", e);
            return null;
        }
    }

    public ProgressPage(final GuiPokeWatch watch)
    {
        super(new TranslationTextComponent("pokewatch.progress.main.title"), watch);
    }

    @Override
    protected Progress createPage(final int index)
    {
        return ProgressPage.makePage(ProgressPage.PAGELIST.get(index), this.watch);
    }

    @Override
    protected int pageCount()
    {
        return ProgressPage.PAGELIST.size();
    }

    @Override
    public void prePageDraw(final MatrixStack mat, final int mouseX, final int mouseY, final float partialTicks)
    {
        final int x = (this.watch.width - GuiPokeWatch.GUIW) / 2 + 80;
        final int y = (this.watch.height - GuiPokeWatch.GUIH) / 2 + 8;
        AbstractGui.drawCenteredString(mat, this.font, this.getTitle().getString(), x, y, 0xFF78C850);
        AbstractGui.drawCenteredString(mat, this.font, this.current_page.getTitle().getString(), x, y + 10, 0xFF78C850);
    }

    @Override
    public void preSubOpened()
    {
        final int x = this.watch.width / 2;
        final int y = this.watch.height / 2 - 5;
        final ITextComponent next = new StringTextComponent(">");
        final ITextComponent prev = new StringTextComponent("<");
        this.addButton(new Button(x + 64, y - 70, 12, 12, next, b ->
        {
            this.changePage(this.index + 1);
        }));
        this.addButton(new Button(x - 76, y - 70, 12, 12, prev, b ->
        {
            this.changePage(this.index - 1);
        }));
    }
}
