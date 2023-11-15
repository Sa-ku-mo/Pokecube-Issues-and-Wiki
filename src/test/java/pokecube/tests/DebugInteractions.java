package pokecube.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandlerModifiable;
import pokecube.api.PokecubeAPI;
import pokecube.gimmicks.builders.builders.JigsawBuilder;
import pokecube.gimmicks.builders.builders.StructureBuilder;
import pokecube.gimmicks.builders.builders.StructureBuilder.BoMRecord;
import pokecube.world.gen.structures.configs.ExpandedJigsawConfiguration;
import pokecube.world.gen.structures.configs.ExpandedJigsawConfiguration.AvoidanceSettings;
import pokecube.world.gen.structures.configs.ExpandedJigsawConfiguration.ClearanceSettings;
import pokecube.world.gen.structures.configs.ExpandedJigsawConfiguration.YSettings;
import pokecube.world.gen.structures.pieces.ExpandedPoolElementStructurePiece;
import pokecube.world.gen.structures.utils.ExpandedJigsawPacement;
import thut.api.ThutCaps;
import thut.api.Tracker;
import thut.api.maths.Vector3;
import thut.api.util.JsonUtil;
import thut.api.world.WorldTickManager;
import thut.lib.TComponent;

@Mod.EventBusSubscriber
public class DebugInteractions
{

    @SubscribeEvent
    public static void onBlockRightClick(final PlayerInteractEvent.RightClickBlock evt)
    {
        if (!(evt.getPlayer() instanceof ServerPlayer player)
                || !(evt.getPlayer().getLevel() instanceof ServerLevel level))
            return;
        boolean isStructureMaker = evt.getItemStack().getDisplayName().getString().contains("structure_maker");

        var te = level.getBlockEntity(evt.getPos());

        long tick = Tracker.instance().getTick();
        if (player.getPersistentData().getLong("__debug_interaction__") == tick) return;
        player.getPersistentData().putLong("__debug_interaction__", tick);

        if (te instanceof ChestBlockEntity chest && isStructureMaker)
        {
            IItemHandlerModifiable itemSource = (IItemHandlerModifiable) ThutCaps.getInventory(chest);
            BlockPos origin = evt.getPos();
            ItemStack key = itemSource.getStackInSlot(0);
            check:
            if (key.hasTag() && key.getOrCreateTag().get("pages") instanceof ListTag list && !list.isEmpty()
                    && list.get(0) instanceof StringTag entry)
            {
                try
                {
                    ResourceLocation toMake = null;
                    String string = entry.getAsString();
                    BlockPos shift = new BlockPos(0, 0, 0);
                    if (!string.startsWith("{")) string = "{\"text\":\"" + string + "\"}";
                    var parsed = JsonUtil.gson.fromJson(string, JsonObject.class);
                    String[] lines = parsed.get("text").getAsString().strip().split("\n");

                    String type = lines[0].replace("t:", "").strip();

                    if (!(type.equals("jigsaw") || type.equals("building"))) break check;

                    if (lines.length >= 2) toMake = new ResourceLocation(lines[1]);
                    String rotation = "NONE";
                    String offset = "0 0 0";
                    String mirror = "NONE";
                    int jigsawDepth = 4;

                    for (int i = 2; i < lines.length; i++)
                    {
                        String line = lines[i];
                        if (line.startsWith("p:")) offset = line.replace("p:", "").strip();
                        if (line.startsWith("r:")) rotation = line.replace("r:", "").strip();
                        if (line.startsWith("m:")) rotation = line.replace("m:", "").strip();
                        if (line.startsWith("d:")) jigsawDepth = Integer.parseInt(line.replace("d:", "").strip());
                    }

                    Rotation rot = Rotation.NONE;
                    try
                    {
                        rot = Rotation.valueOf(rotation.toUpperCase(Locale.ROOT));
                    }
                    catch (Exception e)
                    {
                        PokecubeAPI.LOGGER.error(e);
                    }

                    Mirror mir = Mirror.NONE;
                    try
                    {
                        mir = Mirror.valueOf(mirror.toUpperCase(Locale.ROOT));
                    }
                    catch (Exception e)
                    {
                        PokecubeAPI.LOGGER.error(e);
                    }

                    var args = offset.contains(",") ? offset.split(",") : offset.split(" ");
                    int dx = 0;
                    int dy = 0;
                    int dz = 0;

                    if (args.length == 1)
                    {
                        dy = Integer.parseInt(args[0]);
                    }
                    else if (args.length == 3)
                    {
                        dx = Integer.parseInt(args[0]);
                        dy = Integer.parseInt(args[1]);
                        dz = Integer.parseInt(args[2]);
                    }
                    shift = new BlockPos(dx, dy, dz);

                    if (toMake != null)
                    {
                        if (type.equals("building"))
                        {
                            StructureBuilder builder = new StructureBuilder(origin, rot, mir, itemSource);
                            builder.creative = true;
                            builder.addBoMRecord(new BoMRecord(() -> itemSource.getStackInSlot(1),
                                    _book -> itemSource.setStackInSlot(1, _book)));
                            WorldTickManager.addWorldData(level.dimension(), builder);
                        }
                        else
                        {
                            var poolHolder = level.registryAccess().registryOrThrow(Registry.TEMPLATE_POOL_REGISTRY)
                                    .getHolderOrThrow(ResourceKey.create(Registry.TEMPLATE_POOL_REGISTRY, toMake));
                            ExpandedJigsawConfiguration config = new ExpandedJigsawConfiguration(poolHolder, jigsawDepth,
                                    YSettings.DEFAULT, ClearanceSettings.DEFAULT, new ArrayList<>(), "", "", "none",
                                    Heightmap.Types.WORLD_SURFACE_WG, new ArrayList<>(), 0, 0,
                                    AvoidanceSettings.DEFAULT);
                            var context = new PieceGeneratorSupplier.Context<ExpandedJigsawConfiguration>(
                                    level.getChunkSource().getGenerator(),
                                    level.getChunkSource().getGenerator().getBiomeSource(), level.getSeed(),
                                    new ChunkPos(origin), config, level, Predicates.alwaysTrue(),
                                    level.getStructureManager(), level.registryAccess());
                            WorldgenRandom worldgenrandom = new WorldgenRandom(new LegacyRandomSource(0L));
                            worldgenrandom.setLargeFeatureSeed(context.seed(), context.chunkPos().x,
                                    context.chunkPos().z);
                            var make = ExpandedJigsawPacement.addPieces(context, ExpandedPoolElementStructurePiece::new,
                                    origin, false, false, worldgenrandom, rot);
                            if (make.isPresent())
                            {
                                StructurePiecesBuilder structurepiecesbuilder = new StructurePiecesBuilder();

                                var buildContext = new PieceGenerator.Context<ExpandedJigsawConfiguration>(config,
                                        level.getChunkSource().getGenerator(), level.getStructureManager(),
                                        new ChunkPos(origin), level, worldgenrandom, level.getSeed());
                                make.get().generatePieces(structurepiecesbuilder, buildContext);
                                var builder = new JigsawBuilder(structurepiecesbuilder, shift, itemSource, level);
                                for (var b : builder.builders)
                                {
                                    b.creative = true;
                                    b.addBoMRecord(new BoMRecord(() -> itemSource.getStackInSlot(1),
                                            _book -> itemSource.setStackInSlot(1, _book)));
                                    WorldTickManager.addWorldData(level.dimension(), b);
                                }
                            }
                        }
                    }
                }
                catch (Exception e)
                {
                    PokecubeAPI.LOGGER.error(e);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onItemRightClick(final PlayerInteractEvent.RightClickItem evt)
    {
        if (!(evt.getPlayer() instanceof ServerPlayer player)
                || !(evt.getPlayer().getLevel() instanceof ServerLevel level))
            return;
        boolean isStructureDebug = evt.getItemStack().getDisplayName().getString().contains("structure_debug");
        Vector3 v = new Vector3().set(player);
        if (isStructureDebug)
        {
            var registry = level.registryAccess().registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY);
            var list = registry.stream().toList();
            List<ResourceLocation> found = Lists.newArrayList();
            List<ResourceLocation> not_found = Lists.newArrayList();
            Map<ResourceLocation, Pair<Integer, BlockPos>> found_map = Maps.newHashMap();
            thut.lib.ChatHelper.sendSystemMessage(player, TComponent.literal("Searching for Structures!"));
            for (var feature : list)
            {
                var name = registry.getKey(feature);
                if (name.toString().startsWith("pokecube"))
                {
                    thut.lib.ChatHelper.sendSystemMessage(player, TComponent.literal("Checking " + name));
                    final ResourceKey<ConfiguredStructureFeature<?, ?>> structure = ResourceKey
                            .create(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY, name);
                    var holder = registry.getHolderOrThrow(structure);
                    HolderSet<ConfiguredStructureFeature<?, ?>> holderset = HolderSet.direct(holder);
                    Pair<BlockPos, Holder<ConfiguredStructureFeature<?, ?>>> thing = level.getChunkSource()
                            .getGenerator().findNearestMapFeature(level, holderset, v.getPos(), 100, false);
                    if (thing != null)
                    {
                        found.add(name);
                        found_map.put(name,
                                Pair.of((int) Math.sqrt(thing.getFirst().distSqr(v.getPos())), thing.getFirst()));
                    }
                    else
                    {
                        not_found.add(name);
                    }
                }
            }
            thut.lib.ChatHelper.sendSystemMessage(player, TComponent.literal("Search Complete"));
            found.sort(null);
            not_found.sort(null);
            PokecubeAPI.LOGGER.info("Structures Found:");
            for (var name : found)
            {
                PokecubeAPI.LOGGER.info("{}\t{}\t{}", found_map.get(name).getFirst(), found_map.get(name).getSecond(),
                        name);
            }
            PokecubeAPI.LOGGER.info("Structures Missing:");
            for (var name : not_found)
            {
                PokecubeAPI.LOGGER.info(name);
            }
        }
    }
}
