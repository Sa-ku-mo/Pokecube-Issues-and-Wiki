package pokecube.api.data.abilities;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.objectweb.asm.Type;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.forgespi.language.ModFileScanData.AnnotationData;
import pokecube.api.PokecubeAPI;
import pokecube.api.entity.pokemob.IPokemob;
import pokecube.core.moves.implementations.MovesAdder;
import thut.core.common.ThutCore;
import thut.lib.CompatParser.ClassFinder;

public class AbilityManager
{

    private static Map<String, AbilityFactory> nameMap = Maps.newHashMap();
    private static Set<Package> packages = Sets.newHashSet();

    public static void registerAbilityPackage(Package pack)
    {
        packages.add(pack);
    }

    private static Map<String, String> fixed = Maps.newHashMap();

    private static String getAbilityName(String name)
    {
        if (name == null) return null;
        if (AbilityManager.fixed.containsKey(name)) return AbilityManager.fixed.get(name);
        final String original = name;
        name = ThutCore.trim(name);
        AbilityManager.fixed.put(original, name);
        return name;
    }

    public static boolean abilityExists(final String name)
    {
        if (name == null) return false;
        return AbilityManager.nameMap.containsKey(AbilityManager.getAbilityName(name));
    }

    public static void addAbility(final AbilityFactory ability, String name)
    {
        name = AbilityManager.getAbilityName(name);
        AbilityManager.nameMap.put(name, ability);
    }

    public static Ability getAbility(String name, final Object... args)
    {
        if (name == null) return null;
        if (name.startsWith("ability.")) name = name.substring(7);
        if (name.endsWith(".name")) name = name.substring(0, name.length() - 5);
        Ability ability = AbilityManager.makeAbility(AbilityManager.getAbilityName(name), args);
        return ability;
    }

    public static boolean hasAbility(final String abilityName, final IPokemob pokemob)
    {
        final Ability ability = pokemob.getAbility();
        if (ability == null) return false;
        return ability.toString().equalsIgnoreCase(AbilityManager.getAbilityName(abilityName));
    }

    @SuppressWarnings("unchecked")
    public static void init()
    {
        List<Class<?>> foundClasses = Lists.newArrayList();

        Type ANNOTE = Type.getType("Lpokecube/api/data/abilities/AbilityProvider;");
        BiFunction<ModFile, String, Boolean> validClass = (file, name) -> {
            for (final AnnotationData a : file.getScanResult().getAnnotations())
                if (name.equals(a.clazz().getClassName()) && a.annotationType().equals(ANNOTE)) return true;
            return false;
        };

        for (final Package pack : MovesAdder.moveRegistryPackages)
        {
            if (pack == null) continue;
            try
            {
                foundClasses.addAll(ClassFinder.find(pack.getName(), validClass));
            }
            catch (final Exception e)
            {
                e.printStackTrace();
            }
        }
        try
        {
            int num = 0;
            for (final Class<?> candidateClass : foundClasses) if (Ability.class.isAssignableFrom(candidateClass)
                    && !Modifier.isAbstract(candidateClass.getModifiers()))
            {
                // Needs annotation
                if (candidateClass.getAnnotations().length == 0) continue;
                final AbilityProvider details = candidateClass.getAnnotation(AbilityProvider.class);
                if (details != null)
                {
                    for (var key : details.name())
                    {
                        num++;

                        if (details.singleton())
                        {
                            try
                            {
                                Ability ability = (Ability) candidateClass.getConstructor().newInstance();
                                AbilityManager.addAbility(AbilityFactory.forAbility(ability), key);
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }
                        else
                        {
                            Class<? extends Ability> clazz = (Class<? extends Ability>) candidateClass;
                            Supplier<Ability> supplier = () -> {
                                try
                                {
                                    return clazz.getConstructor().newInstance();
                                }
                                catch (Exception e)
                                {
                                    PokecubeAPI.LOGGER.error("Error generating ability {}", key, e);
                                    return new DummyAbility();
                                }
                            };
                            AbilityManager.addAbility(AbilityFactory.forSupplier(supplier), key);
                        }
                    }
                }
            }
            PokecubeAPI.LOGGER.debug("Registered " + num + " Abilities");
        }
        catch (final Exception e)
        {
            e.printStackTrace();
        }
    }

    public static Ability makeAbility(final String name, final Object... args)
    {
        return nameMap.getOrDefault(name, AbilityFactory.DUMMY).create(args).setName(name);
    }
}
