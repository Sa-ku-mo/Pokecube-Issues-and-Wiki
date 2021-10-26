package pokecube.adventures.capabilities;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import pokecube.adventures.PokecubeAdv;

public class CapabilityNPCAIStates
{
    public static class DefaultAIStates implements IHasNPCAIStates, ICapabilityProvider
    {
        int   state = 0;
        float direction;

        private final LazyOptional<IHasNPCAIStates> holder = LazyOptional.of(() -> this);

        public DefaultAIStates()
        {
            for (final AIState state : AIState.values())
                this.setAIState(state, state._default);
        }

        @Override
        public void deserializeNBT(final CompoundTag nbt)
        {
            this.setTotalState(nbt.getInt("AI"));
            this.setDirection(nbt.getFloat("D"));
        }

        @Override
        public boolean getAIState(final AIState state)
        {
            // These two have config overrides, which ignore the actual ai
            // states.
            if (state == AIState.TRADES_ITEMS && !PokecubeAdv.config.trainersTradeItems) return false;
            if (state == AIState.TRADES_MOBS && !PokecubeAdv.config.trainersTradeMobs) return false;

            return (this.state & state.mask) > 0;
        }

        @Override
        public <T> LazyOptional<T> getCapability(final Capability<T> cap, final Direction side)
        {
            return TrainerCaps.AISTATES_CAP.orEmpty(cap, this.holder);
        }

        @Override
        public float getDirection()
        {
            return this.direction;
        }

        @Override
        public int getTotalState()
        {
            return this.state;
        }

        @Override
        public CompoundTag serializeNBT()
        {
            final CompoundTag tag = new CompoundTag();
            tag.putInt("AI", this.getTotalState());
            tag.putFloat("D", this.getDirection());
            return tag;
        }

        @Override
        public void setAIState(final AIState state, final boolean flag)
        {
            if (flag) this.state = Integer.valueOf(this.state | state.mask);
            else this.state = Integer.valueOf(this.state & -state.mask - 1);
        }

        @Override
        public void setDirection(final float direction)
        {
            this.direction = direction;
        }

        @Override
        public void setTotalState(final int state)
        {
            this.state = state;
        }

    }

    public static interface IHasNPCAIStates extends INBTSerializable<CompoundTag>
    {
        public static enum AIState
        {
            STATIONARY(1 << 0), INBATTLE(1 << 1, true), THROWING(1 << 2, true), PERMFRIENDLY(1 << 3), FIXEDDIRECTION(
                    1 << 4), MATES(1 << 5, false, true), INVULNERABLE(1 << 6), TRADES_ITEMS(1 << 7, false,
                            true), TRADES_MOBS(1 << 8, false, true);

            private final int mask;

            private final boolean temporary;
            private final boolean _default;

            private AIState(final int mask)
            {
                this(mask, false, false);
            }

            private AIState(final int mask, final boolean temporary)
            {
                this(mask, temporary, false);
            }

            private AIState(final int mask, final boolean temporary, final boolean _default)
            {
                this.mask = mask;
                this.temporary = temporary;
                this._default = _default;
            }

            public int getMask()
            {
                return this.mask;
            }

            public boolean isTemporary()
            {
                return this.temporary;
            }
        }

        boolean getAIState(AIState state);

        /** @return Direction to face if FIXEDDIRECTION */
        public float getDirection();

        int getTotalState();

        void setAIState(AIState state, boolean flag);

        /**
         * @param direction
         *            Direction to face if FIXEDDIRECTION
         */
        public void setDirection(float direction);

        void setTotalState(int state);
    }
}
