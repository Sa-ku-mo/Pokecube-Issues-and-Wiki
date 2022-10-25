/**
 *
 */
package pokecube.api.moves.utils;

import java.util.function.Predicate;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Mob;
import pokecube.api.PokecubeAPI;
import pokecube.api.data.PokedexEntry;
import pokecube.api.entity.pokemob.IPokemob;
import thut.api.item.ItemList;

/** @author Manchou */
public interface IMoveConstants extends IMoveNames
{
    static ResourceLocation ANTS = new ResourceLocation(PokecubeAPI.MODID, "ants");

    static ResourceLocation BURROWS = new ResourceLocation(PokecubeAPI.MODID, "burrowers");

    static final Predicate<IPokemob> isBee = pokemob -> {
        final Mob entity = pokemob.getEntity();
        final boolean isBee = entity.getType().is(EntityTypeTags.BEEHIVE_INHABITORS);
        // Only care about bees
        if (!isBee) return false;
        // Only process stock pokemobs
        if (!pokemob.getPokedexEntry().stock) return false;
        return true;
    };

    static final Predicate<IPokemob> isAnt = pokemob -> {
        final Mob entity = pokemob.getEntity();
        final boolean isAnt = ItemList.is(IMoveConstants.ANTS, entity);
        // Only care about bees
        if (!isAnt) return false;
        // Only process stock pokemobs
        if (!pokemob.getPokedexEntry().stock) return false;
        return true;
    };

    static final Predicate<IPokemob> burrows = pokemob -> {
        final Mob entity = pokemob.getEntity();
        final boolean isAnt = ItemList.is(IMoveConstants.BURROWS, entity);
        // Only care about bees
        if (!isAnt) return false;
        // Only process stock pokemobs
        if (!pokemob.getPokedexEntry().stock) return false;
        return true;
    };

    static final Predicate<IPokemob> canFly = pokemob -> {
        final PokedexEntry entry = pokemob.getPokedexEntry();
        // These are our 3 criteria for if something is able to stay airborne
        return entry.shouldFly || entry.floats() || entry.flys();
    };

    static final Predicate<IPokemob> canOpenDoors = pokemob -> {
        // Only tame pokemobs can do this.
        return pokemob.isPlayerOwned();
    };

    public static enum AIRoutine
    {
        //@formatter:off
        //Does the pokemob gather item drops and harvest crops.
        GATHER,
        //Does the pokemob act like a vanilla bee
        BEEAI(true, IMoveConstants.isBee),
        //Does the pokemob act like an ant
        ANTAI(true, IMoveConstants.isAnt),
        //Does the pokemob make burrows
        BURROWS(true, IMoveConstants.burrows),
        //Does the pokemob store its inventory when full.
        STORE(false),
       //Does the pokemob wander around randomly
        WANDER,
        //Does the pokemob breed.
        MATE,
        //Does the pokemob follow its owner.
        FOLLOW,
        //Does the pokemob find targets to attack.
        AGRESSIVE,
        //Does the pokemob fly around, or can it only walk.
        AIRBORNE(true, IMoveConstants.canFly),
        //Can the pokemob open and close doors
        USEDOORS(true, IMoveConstants.canOpenDoors);
        //@formatter:on

        private final boolean default_;

        private final Predicate<IPokemob> isAllowed;

        private AIRoutine()
        {
            this(true);
        }

        private AIRoutine(final boolean value)
        {
            this(value, p -> true);
        }

        private AIRoutine(final boolean value, final Predicate<IPokemob> isAllowed)
        {
            this.default_ = value;
            this.isAllowed = isAllowed;
        }

        /** @return default state for this routine. */
        public boolean getDefault()
        {
            return this.default_;
        }

        public boolean isAllowed(final IPokemob pokemob)
        {
            return this.isAllowed.test(pokemob);
        }
    }

    /*
     * exclusive Status Effects
     */
    int STATUS_NON = 0;
    int STATUS_BRN = 1;
    int STATUS_FRZ = 2;
    int STATUS_PAR = 4;
    int STATUS_PSN = 8, STATUS_PSN2 = 24;
    int STATUS_SLP = 32;
    /*
     * non-exclusive status effects
     */
    int CHANGE_NONE = 64;
    int CHANGE_CONFUSED = 128;
    int CHANGE_FLINCH = 256;
    int CHANGE_CURSE = 512;
    /*
     * Stats Modifiers
     */
    byte ATTACK = 1;
    byte DEFENSE = 2;
    byte SPATACK = 4;
    byte SPDEFENSE = 8;
    byte VIT = 16;
    byte ACCURACY = 32;

    byte EVASION = 64;
    /*
     * Stats Changes
     */
    byte HARSH = -2;
    byte FALL = -1;
    byte RAISE = 1;
    byte SHARP = 2;

    byte DRASTICALLY = 3;
    /*
     * Move Categories
     */
    byte CATEGORY_CONTACT = 1;
    byte CATEGORY_DISTANCE = 2;
    /*
     * Move damage category
     */
    byte SPECIAL = 1;

    byte PHYSICAL = 2;
    // Special Moves, ie ones needed for specific logic
    // No move move for just sitting there
    String MOVE_NONE = "none";

    String DEFAULT_MOVE = "tackle";
    /*
     * Flavours
     */
    byte SPICY = 0; // red
    byte DRY = 1; // blue
    byte SWEET = 2; // pink
    byte BITTER = 3; // green

    byte SOUR = 4; // yellow
}
