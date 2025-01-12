package thut.core.common.genetics.genes;

import net.minecraft.nbt.CompoundTag;
import thut.api.entity.genetics.Gene;

public abstract class GeneFloat implements Gene<Float>
{
    protected Float value = Float.valueOf(0);

    @Override
    public Float getValue()
    {
        return this.value;
    }

    @Override
    public void load(final CompoundTag tag)
    {
        this.value = tag.getFloat("V");
    }

    @Override
    public CompoundTag save()
    {
        final CompoundTag tag = new CompoundTag();
        tag.putFloat("V", this.value);
        return tag;
    }

    @Override
    public void setValue(final Float value)
    {
        this.value = value;
    }

    @Override
    public String toString()
    {
        return "" + this.value;
    }

}
