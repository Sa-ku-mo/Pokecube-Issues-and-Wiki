package pokecube.world.gen.structures.utils;

import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;
import net.minecraft.world.level.levelgen.structure.pools.EmptyPoolElement;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import pokecube.core.PokecubeCore;
import pokecube.world.gen.structures.configs.ExpandedJigsawConfiguration;
import pokecube.world.gen.structures.pool_elements.ExpandedJigsawPiece;
import thut.core.common.ThutCore;

public class ExpandedJigsawPacement
{
    static final Logger LOGGER = LogUtils.getLogger();

    public static ServerLevel getForGen(final ChunkGenerator chunkGen)
    {
        final MinecraftServer server = ThutCore.proxy.getServer();
        for (final ServerLevel w : server.getAllLevels()) if (w.getChunkSource().getGenerator() == chunkGen) return w;
        throw new IllegalStateException("Did not find a world for this chunk generator!");
    }

    public static Optional<PieceGenerator<ExpandedJigsawConfiguration>> addPieces(
            PieceGeneratorSupplier.Context<ExpandedJigsawConfiguration> context, PieceFactory factory, BlockPos centre,
            boolean bound_checks, boolean on_surface)
    {
        WorldgenRandom worldgenrandom = new WorldgenRandom(new LegacyRandomSource(0L));
        worldgenrandom.setLargeFeatureSeed(context.seed(), context.chunkPos().x, context.chunkPos().z);
        RegistryAccess registryaccess = context.registryAccess();
        ExpandedJigsawConfiguration config = context.config();
        ChunkGenerator chunkgenerator = context.chunkGenerator();
        StructureManager structuremanager = context.structureManager();
        LevelHeightAccessor levelheightaccessor = context.heightAccessor();
        Predicate<Holder<Biome>> predicate = context.validBiome();
        StructureFeature.bootstrap();
        Registry<StructureTemplatePool> registry = registryaccess.registryOrThrow(Registry.TEMPLATE_POOL_REGISTRY);
        Rotation rotation = Rotation.getRandom(worldgenrandom);

        StructureTemplatePool root_pool = config.startPool().value();
        // This one can be completely random, as is just the start pool, this
        // shouldn't have any requirements...
        StructurePoolElement root_element = root_pool.getRandomTemplate(worldgenrandom);

        if (root_element == EmptyPoolElement.INSTANCE)
        {
            return Optional.empty();
        }
        else
        {
            PoolElementStructurePiece root_piece = factory.create(structuremanager, root_element, centre,
                    root_element.getGroundLevelDelta(), rotation,
                    root_element.getBoundingBox(structuremanager, centre, rotation));
            BoundingBox boundingbox = root_piece.getBoundingBox();
            int i = (boundingbox.maxX() + boundingbox.minX()) / 2;
            int j = (boundingbox.maxZ() + boundingbox.minZ()) / 2;
            int dk = config.y_settings.vertical_offset;
            if (on_surface)
            {
                dk = chunkgenerator.getFirstFreeHeight(i, j, config.height_type, levelheightaccessor)
                        + config.y_settings.vertical_offset;
            }
            int k = centre.getY() + dk;

            if (!predicate.test(
                    chunkgenerator.getNoiseBiome(QuartPos.fromBlock(i), QuartPos.fromBlock(k), QuartPos.fromBlock(j))))
            {
                return Optional.empty();
            }
            else
            {
                int l = boundingbox.minY() + root_piece.getGroundLevelDelta();
                root_piece.move(0, k - l, 0);
                return Optional.of((builder, config_context) -> {
                    if (config.maxDepth() > 0)
                    {
                        int max_box_size = 80;
                        int max_box_height = 512;
                        AABB aabb = new AABB((double) (i - max_box_size), (double) (k - max_box_height),
                                (double) (j - max_box_size), (double) (i + max_box_size + 1),
                                (double) (k + max_box_height + 1), (double) (j + max_box_size + 1));
                        List<PoolElementStructurePiece> list = Lists.newArrayList();
                        tries:
                        for (int n = 0; n < 100; n++)
                        {
                            list.clear();
                            list.add(root_piece);
                            Placer placer = new Placer(config, registry, config.maxDepth(), factory, chunkgenerator,
                                    structuremanager, list, worldgenrandom);

                            MutableObject<VoxelShape> bounds = new MutableObject<>(Shapes.join(Shapes.create(aabb),
                                    Shapes.create(AABB.of(boundingbox)), BooleanOp.ONLY_FIRST));

                            PieceState next_state = new PieceState(root_piece, bounds, 0);
                            placer.placing.addLast(next_state);

                            while (!placer.placing.isEmpty())
                            {
                                next_state = placer.placing.removeFirst();
                                placer.tryPlacingChildren(next_state, bound_checks, levelheightaccessor);
                            }

                            if (placer.needed_once.isEmpty()) break;
                            for (String s : placer.needed_once)
                            {
                                if (!placer.added_once.contains(s)) continue tries;
                            }
                            break;
                        }
                        PostProcessor.POSTPROCESS.accept(config_context, list);
                        list.forEach(builder::addPiece);
                    }

                });
            }
        }
    }

    public static void addPieces(RegistryAccess reg_access, PoolElementStructurePiece root_piece, int max_depth,
            PieceFactory factory, ChunkGenerator chunk_gen, StructureManager structure_manager,
            List<? super PoolElementStructurePiece> pieces, Random random, LevelHeightAccessor height_accessor)
    {
        Registry<StructureTemplatePool> registry = reg_access.registryOrThrow(Registry.TEMPLATE_POOL_REGISTRY);
        Placer placer = new Placer(null, registry, max_depth, factory, chunk_gen, structure_manager, pieces, random);
        placer.placing.addLast(new PieceState(root_piece, new MutableObject<>(Shapes.INFINITY), 0));

        while (!placer.placing.isEmpty())
        {
            ExpandedJigsawPacement.PieceState piece = placer.placing.removeFirst();
            placer.tryPlacingChildren(piece, false, height_accessor);
        }
    }

    public interface PieceFactory
    {
        PoolElementStructurePiece create(StructureManager p_210301_, StructurePoolElement p_210302_, BlockPos p_210303_,
                int p_210304_, Rotation p_210305_, BoundingBox p_210306_);
    }

    static final class PieceState
    {
        final PoolElementStructurePiece piece;
        final Set<StructureBlockInfo> used_jigsaws = Sets.newHashSet();
        final MutableObject<VoxelShape> free;
        final int depth;

        PieceState(PoolElementStructurePiece piece, MutableObject<VoxelShape> free, int depth)
        {
            this.piece = piece;
            this.free = free;
            this.depth = depth;
        }
    }

    static final class Placer
    {
        final Registry<StructureTemplatePool> pools;
        final int maxDepth;
        final ExpandedJigsawPacement.PieceFactory factory;
        final ChunkGenerator chunkGenerator;
        final StructureManager structureManager;
        final List<? super PoolElementStructurePiece> pieces;
        final Random random;
        final Set<String> needed_once;
        final Set<String> added_once = Sets.newHashSet();
        final ExpandedJigsawConfiguration config;
        final Deque<ExpandedJigsawPacement.PieceState> placing = Queues.newArrayDeque();
        MutableObject<VoxelShape> rigid_bounds = new MutableObject<>();
        MutableObject<VoxelShape> non_rigid_bounds = new MutableObject<>();

        Placer(ExpandedJigsawConfiguration config, Registry<StructureTemplatePool> pools, int max_depth,
                ExpandedJigsawPacement.PieceFactory factory, ChunkGenerator chunkGenerator,
                StructureManager structureManager, List<? super PoolElementStructurePiece> pieces, Random random)
        {
            this.pools = pools;
            this.maxDepth = max_depth;
            this.factory = factory;
            this.chunkGenerator = chunkGenerator;
            this.structureManager = structureManager;
            this.pieces = pieces;
            this.random = random;
            this.needed_once = Sets.newHashSet(config.required_parts);
            this.config = config;
        }

        List<StructureTemplate.StructureBlockInfo> getShuffledJigsaws(StructurePoolElement structurepoolelement,
                BlockPos blockpos, Rotation rotation)
        {
            List<StructureTemplate.StructureBlockInfo> shuffled = structurepoolelement
                    .getShuffledJigsawBlocks(this.structureManager, blockpos, rotation, this.random);
            return shuffled;
        }

        List<StructurePoolElement> getShuffledParts(int depth, Optional<StructureTemplatePool> target_pool,
                Optional<StructureTemplatePool> fallback_pool)
        {
            List<StructurePoolElement> list = Lists.newArrayList();
            if (depth != this.maxDepth)
            {
                list.addAll(target_pool.get().getShuffledTemplates(this.random));
                Set<ResourceLocation> extra_pools = Sets.newHashSet();
                for (StructurePoolElement e : list)
                {
                    if (e instanceof ExpandedJigsawPiece p)
                    {
                        extra_pools.addAll(p.extra_pools);
                    }
                }
                for (ResourceLocation l : extra_pools)
                {
                    Optional<StructureTemplatePool> target_pool_e = this.pools.getOptional(l);
                    if (target_pool_e.isPresent())
                    {
                        list.addAll(target_pool_e.get().getShuffledTemplates(this.random));
                    }
                }
                Collections.shuffle(list);
                if (!needed_once.isEmpty())
                {
                    List<StructurePoolElement> to_remove = Lists.newArrayList();
                    List<StructurePoolElement> needed = Lists.newArrayList();
                    for (StructurePoolElement e : list)
                    {
                        outer:
                        if (e instanceof ExpandedJigsawPiece p)
                        {
                            boolean need = false;
                            for (String flag : p._flags)
                            {
                                // If we have, mark to remove and skip
                                if (added_once.contains(flag))
                                {
                                    to_remove.add(e);
                                    break outer;
                                }
                                // Otherwise, if we need it, mark as needed
                                // We only skip this if not removed by another
                                // flag!
                                if (needed_once.contains(flag))
                                {
                                    need = true;
                                }
                            }
                            if (need) needed.add(p);
                        }
                    }
                    // Remove the removes
                    list.removeAll(to_remove);
                    // also the needed
                    list.removeAll(needed);
                    // Re-add the needed at the front
                    list.addAll(0, needed);
                }
            }
            list.addAll(fallback_pool.get().getShuffledTemplates(this.random));
            return list;
        }

        @SuppressWarnings("deprecation")
        void tryPlacingChildren(PieceState root_state, boolean bound_check, LevelHeightAccessor heightmap)
        {
            PoolElementStructurePiece current_root = root_state.piece;
            MutableObject<VoxelShape> free = root_state.free;
            int depth = root_state.depth;

            StructurePoolElement root_element = current_root.getElement();
            BlockPos blockpos = current_root.getPosition();
            Rotation rotation = current_root.getRotation();
            StructureTemplatePool.Projection root_projection = root_element.getProjection();
            boolean root_rigid = root_projection == StructureTemplatePool.Projection.RIGID;
            BoundingBox root_bounding_box = current_root.getBoundingBox();
            int root_min_y = root_bounding_box.minY();
            AABB root_box = AABB.of(root_bounding_box);

            Heightmap.Types _default;
            Heightmap.Types heightmap$types;
            boolean water = false;

            if (root_element instanceof ExpandedJigsawPiece p)
            {
                water = p.water_terrain_match;
                _default = water ? Heightmap.Types.OCEAN_FLOOR_WG : Heightmap.Types.WORLD_SURFACE_WG;
            }
            else
            {
                _default = Heightmap.Types.WORLD_SURFACE_WG;
            }

            if (heightmap instanceof ServerLevel)
            {
                heightmap$types = water ? Heightmap.Types.OCEAN_FLOOR : Heightmap.Types.WORLD_SURFACE;
            }
            else
            {
                heightmap$types = _default;
            }

            List<StructureBlockInfo> root_jigsaws = this.getShuffledJigsaws(root_element, blockpos, rotation);
            root_jigsaws:
            for (StructureBlockInfo root_block_info : root_jigsaws)
            {
                if (root_state.used_jigsaws.contains(root_block_info))
                {
                    PokecubeCore.LOGGER.debug("Skipping used part!");
                    continue root_jigsaws;
                }
                Direction direction = JigsawBlock.getFrontFacing(root_block_info.state);
                BlockPos raw_jigsaw_pos = root_block_info.pos;
                BlockPos connecting_jigsaw_pos = raw_jigsaw_pos.relative(direction);
                boolean next_pick_inside = root_bounding_box.isInside(connecting_jigsaw_pos);

                int dy = raw_jigsaw_pos.getY() - root_min_y;
                int k = -1;
                ResourceLocation resourcelocation = new ResourceLocation(root_block_info.nbt.getString("pool"));
                Optional<StructureTemplatePool> next_pool = this.pools.getOptional(resourcelocation);

                boolean valid_next_pool = next_pool.isPresent()
                        && (next_pool.get().size() != 0 || Objects.equals(resourcelocation, Pools.EMPTY.location()));

                if (valid_next_pool)
                {
                    ResourceLocation resourcelocation1 = next_pool.get().getFallback();
                    Optional<StructureTemplatePool> fallback_pool = this.pools.getOptional(resourcelocation1);

                    boolean valid_fallback = fallback_pool.isPresent() && (fallback_pool.get().size() != 0
                            || Objects.equals(resourcelocation1, Pools.EMPTY.location()));

                    if (valid_fallback)
                    {

                        for (StructurePoolElement next_picked_element : getShuffledParts(depth, next_pool,
                                fallback_pool))
                        {
                            if (next_picked_element == EmptyPoolElement.INSTANCE) break;
                            for (Rotation rotation1 : Rotation.getShuffled(this.random))
                            {
                                List<StructureBlockInfo> next_jigsaws = next_picked_element.getShuffledJigsawBlocks(
                                        this.structureManager, BlockPos.ZERO, rotation1, this.random);
                                BoundingBox picked_box = next_picked_element.getBoundingBox(this.structureManager,
                                        BlockPos.ZERO, rotation1);
                                int l;
                                if (bound_check && picked_box.getYSpan() <= 16)
                                {
                                    l = next_jigsaws.stream().mapToInt((structure_info) -> {
                                        if (!picked_box.isInside(structure_info.pos
                                                .relative(JigsawBlock.getFrontFacing(structure_info.state))))
                                        {
                                            return 0;
                                        }
                                        else
                                        {
                                            ResourceLocation id = new ResourceLocation(
                                                    structure_info.nbt.getString("pool"));
                                            Optional<StructureTemplatePool> pool_entry = this.pools.getOptional(id);
                                            Optional<StructureTemplatePool> pool_fallback = pool_entry
                                                    .flatMap((pool) ->
                                                    {
                                                        return this.pools.getOptional(pool.getFallback());
                                                    });
                                            int y_1 = pool_entry.map((pool) -> {
                                                return pool.getMaxSize(this.structureManager);
                                            }).orElse(0);
                                            int y_2 = pool_fallback.map((pool) -> {
                                                return pool.getMaxSize(this.structureManager);
                                            }).orElse(0);
                                            return Math.max(y_1, y_2);
                                        }
                                    }).max().orElse(0);
                                }
                                else
                                {
                                    l = 0;
                                }
                                pick_jigsaws:
                                for (StructureBlockInfo next_block_info : next_jigsaws)
                                {
                                    if (JigsawBlock.canAttach(root_block_info, next_block_info))
                                    {
                                        BlockPos next_pos_raw = next_block_info.pos;
                                        BlockPos next_jigsaw_pos = connecting_jigsaw_pos.subtract(next_pos_raw);
                                        BoundingBox next_pick_box = next_picked_element
                                                .getBoundingBox(this.structureManager, next_jigsaw_pos, rotation1);
                                        int next_min_y = next_pick_box.minY();
                                        StructureTemplatePool.Projection next_projection = next_picked_element
                                                .getProjection();
                                        boolean next_pick_rigid = next_projection == StructureTemplatePool.Projection.RIGID;
                                        int raw_pos_y = next_pos_raw.getY();
                                        int jigsaw_block_dy = dy - raw_pos_y
                                                + JigsawBlock.getFrontFacing(root_block_info.state).getStepY();
                                        int l1;
                                        if (root_rigid && next_pick_rigid)
                                        {
                                            l1 = root_min_y + jigsaw_block_dy;
                                        }
                                        else
                                        {
                                            if (k == -1)
                                            {
                                                k = this.chunkGenerator.getFirstFreeHeight(raw_jigsaw_pos.getX(),
                                                        raw_jigsaw_pos.getZ(), heightmap$types, heightmap);
                                            }
                                            l1 = k - raw_pos_y;
                                        }

                                        int i2 = l1 - next_min_y;
                                        BoundingBox next_pick_box_shifted_y = next_pick_box.moved(0, i2, 0);
                                        BlockPos blockpos5 = next_jigsaw_pos.offset(0, i2, 0);
                                        if (l > 0)
                                        {
                                            int j2 = Math.max(l + 1,
                                                    next_pick_box_shifted_y.maxY() - next_pick_box_shifted_y.minY());
                                            next_pick_box_shifted_y.encapsulate(new BlockPos(
                                                    next_pick_box_shifted_y.minX(), next_pick_box_shifted_y.minY() + j2,
                                                    next_pick_box_shifted_y.minZ()));
                                        }

                                        AABB test_box = AABB.of(next_pick_box_shifted_y).deflate(0.25D);

                                        // Never allow intersection of rigid
                                        // objects.
                                        if (rigid_bounds.getValue() != null)
                                        {
                                            VoxelShape new_shape = Shapes.create(test_box);
                                            if (Shapes.joinIsNotEmpty(rigid_bounds.getValue(), new_shape,
                                                    BooleanOp.AND))
                                                continue pick_jigsaws;
                                        }

                                        // And then don't let non-rigids overlap
                                        // each other
                                        if (non_rigid_bounds.getValue() != null)
                                        {
                                            VoxelShape new_shape = Shapes.create(test_box);
                                            if (Shapes.joinIsNotEmpty(non_rigid_bounds.getValue(), new_shape,
                                                    BooleanOp.AND))
                                                continue pick_jigsaws;
                                        }

                                        // Unless we are actually inside, do not
                                        // allow intersections with the root
                                        // box.
                                        if (!next_pick_inside)
                                        {
                                            if (test_box.intersects(root_box)) continue pick_jigsaws;
                                        }

                                        // If we are rigid, add the boundary
                                        // now, so we don't conflict with future
                                        // possible additions
                                        if (next_pick_rigid)
                                        {
                                            // Some space below to prevent roads
                                            // going too close under buildings.
                                            int room_below = 5;
                                            int h_clearance = config.clearances.h_clearance;
                                            int v_clearance = config.clearances.v_clearance;
                                            if (next_picked_element instanceof ExpandedJigsawPiece p)
                                            {
                                                room_below = p.space_below;
                                            }
                                            // If it was rigid, add it to the
                                            // rigid bounds
                                            AABB next_box = AABB.of(next_pick_box_shifted_y)
                                                    .expandTowards(0, -room_below, 0)
                                                    .inflate(h_clearance, v_clearance, h_clearance);
                                            VoxelShape new_shape = Shapes.create(next_box);

                                            if (rigid_bounds.getValue() != null)
                                            {
                                                rigid_bounds.setValue(Shapes.or(rigid_bounds.getValue(), new_shape));
                                            }
                                            else
                                            {
                                                rigid_bounds.setValue(new_shape);
                                            }
                                        }

                                        int root_y_offset = current_root.getGroundLevelDelta();
                                        int next_y_offset;

                                        if (next_pick_rigid)
                                        {
                                            next_y_offset = root_y_offset - jigsaw_block_dy;
                                        }
                                        else
                                        {
                                            next_y_offset = next_picked_element.getGroundLevelDelta();
                                        }

                                        PoolElementStructurePiece next_piece = this.factory.create(
                                                this.structureManager, next_picked_element, blockpos5, next_y_offset,
                                                rotation1, next_pick_box_shifted_y);

                                        int root_junction_y_offset;
                                        if (root_rigid)
                                        {
                                            root_junction_y_offset = root_min_y + dy;
                                        }
                                        else if (next_pick_rigid)
                                        {
                                            root_junction_y_offset = l1 + raw_pos_y;
                                        }
                                        else
                                        {
                                            if (k == -1)
                                            {
                                                k = this.chunkGenerator.getFirstFreeHeight(raw_jigsaw_pos.getX(),
                                                        raw_jigsaw_pos.getZ(), heightmap$types, heightmap);
                                            }
                                            root_junction_y_offset = k + jigsaw_block_dy / 2;
                                        }

                                        JigsawJunction root_junction = new JigsawJunction(connecting_jigsaw_pos.getX(),
                                                root_junction_y_offset - dy + root_y_offset,
                                                connecting_jigsaw_pos.getZ(), jigsaw_block_dy, next_projection);

                                        JigsawJunction next_junction = new JigsawJunction(raw_jigsaw_pos.getX(),
                                                root_junction_y_offset - raw_pos_y + next_y_offset,
                                                raw_jigsaw_pos.getZ(), -jigsaw_block_dy, root_projection);

                                        current_root.addJunction(root_junction);
                                        next_piece.addJunction(next_junction);

                                        this.pieces.add(next_piece);
                                        if (next_picked_element instanceof ExpandedJigsawPiece p)
                                        {
                                            if (p.only_once) for (String s : p._flags) added_once.add(s);
                                            // Mark it as added if we needed
                                            // this part.
                                            for (String s : p._flags) if (needed_once.contains(s)) added_once.add(s);
                                        }
                                        if (depth + 1 <= this.maxDepth)
                                        {
                                            PieceState next_piece_state = new PieceState(next_piece, free, depth + 1);
                                            next_piece_state.used_jigsaws.add(next_block_info);
                                            this.placing.addLast(next_piece_state);
                                        }
                                        continue root_jigsaws;
                                    }
                                }
                            }
                        }
                    }
                    else
                    {
                        PokecubeCore.LOGGER.warn("Empty or non-existent fallback pool: {}", (Object) resourcelocation1);
                    }
                }
                else
                {
                    PokecubeCore.LOGGER.warn("Empty or non-existent pool: {}", (Object) resourcelocation);
                }
            }

            // After adding in all the sub parts, then we actually update the
            // bounding boxes if it wasn't rigid.
            int non_rigid_clearance = 10;
            if (!root_rigid)
            {
                AABB next_box = AABB.of(root_bounding_box).inflate(0, non_rigid_clearance, 0);
                VoxelShape new_shape = Shapes.create(next_box);
                if (non_rigid_bounds.getValue() != null)
                {
                    non_rigid_bounds.setValue(Shapes.or(non_rigid_bounds.getValue(), new_shape));
                }
                else
                {
                    non_rigid_bounds.setValue(new_shape);
                }
            }
        }
    }
}
