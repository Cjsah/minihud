package fi.dy.masa.minihud.info.player;

import fi.dy.masa.malilib.util.data.Constants;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.data.EntitiesDataManager;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineChunkCache;
import fi.dy.masa.minihud.info.InfoLineContext;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.warden.WardenSpawnTracker;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.LevelChunk;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class InfoLineSculkWarningLevel extends InfoLine
{
    private static final String LEVEL_KEY = Reference.MOD_ID+".info_line.sculk_warning_level";

    public InfoLineSculkWarningLevel(InfoToggle type)
    {
        super(type);
    }

    public InfoLineSculkWarningLevel()
    {
        super(InfoToggle.SCULK_WARNING_LEVEL);
    }

    @Override
    public boolean succeededType() { return false; }

    @Override
    public List<Entry> parse(@NotNull InfoLineContext ctx)
    {
        if (ctx.world() == null || ctx.ent() == null)
        {
            return null;
        }

        List<Entry> list = new ArrayList<>();

        int warningLevel = this.getEntWarningLevel(ctx.world(), ctx.ent());

        if (warningLevel < 0) return null;

        boolean inDeepDarkChunk = false;

        if (ctx.chunkPos() != null && ctx.pos() != null)
        {
            LevelChunk clientChunk = InfoLineChunkCache.INSTANCE.getClientChunk(ctx.chunkPos());

            if (!clientChunk.isEmpty())
            {
                Biome biome = this.mc().level.getBiome(ctx.pos()).value();
                Identifier id = this.mc().level.registryAccess().lookupOrThrow(Registries.BIOME).getKey(biome);
                inDeepDarkChunk = Biomes.DEEP_DARK.identifier().equals(id);
            }
        }

        if (warningLevel > 0 || inDeepDarkChunk)
        {
            list.add(this.generateEntry(warningLevel));
        }

        return list;
    }

    private int getEntWarningLevel(@NotNull Level world, @NotNull Entity ent)
    {
        if (world instanceof ServerLevel serverLevel)
        {
            List<ServerPlayer> players = serverLevel.getPlayers(it -> it.getId() == ent.getId());

            if (players.isEmpty())
            {
                return -1;
            }

            return players
                .getFirst()
                .getWardenSpawnTracker()
                .map(WardenSpawnTracker::getWarningLevel)
                .orElse(-1);
        }
        else
        {
            Pair<Entity, CompoundData> pair = EntitiesDataManager.getInstance().requestEntity(world, ent.getId());

            if (pair != null)
            {
                CompoundData compound = pair.getRight();

                if (compound.contains("warden_spawn_tracker", Constants.NBT.TAG_COMPOUND))
                {
                    return compound.getCompound("warden_spawn_tracker").getInt("warning_level");
                }
            }
        }

        return -1;
    }

    private Entry generateEntry(int warningLevel)
    {
        char color = switch (warningLevel)
        {
            case 0 -> 'a';
            case 1 -> 'e';
            case 2 -> '6';
            case 3, 4 -> 'c';
            default -> 'r';
        };

        return this.translate(LEVEL_KEY, "§%s%s§r".formatted(color, warningLevel));
    }
}
