package dev.grimforge.listener;

import dev.grimforge.Keys;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Remembers which blocks players placed, so place-and-break loops don't farm tool XP.
 *
 * Each chunk keeps one bit per block position (at most 12 KB for a chunk that is solid player
 * blocks from bedrock to build limit), so lookups are instant no matter how big the base is.
 * Bits live in memory while the chunk is loaded and are written to the chunk's persistent data
 * on every change, so they survive restarts and crashes.
 */
public final class PlacedBlockTracker implements Listener {

    private record ChunkKey(UUID world, long chunk) {
    }

    private final Map<ChunkKey, BitSet> cache = new HashMap<>();

    public boolean isPlaced(Block block) {
        return bits(block.getChunk()).get(index(block));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        Chunk chunk = block.getChunk();
        BitSet bits = bits(chunk);
        int index = index(block);
        if (!bits.get(index)) {
            bits.set(index);
            save(chunk, bits);
        }
    }

    /** Runs after XpListener (HIGH) has already checked the block. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Chunk chunk = block.getChunk();
        BitSet bits = bits(chunk);
        int index = index(block);
        if (bits.get(index)) {
            bits.clear(index);
            save(chunk, bits);
        }
    }

    @EventHandler
    public void onUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        cache.remove(new ChunkKey(chunk.getWorld().getUID(), chunk.getChunkKey()));
    }

    private BitSet bits(Chunk chunk) {
        return cache.computeIfAbsent(new ChunkKey(chunk.getWorld().getUID(), chunk.getChunkKey()), key -> load(chunk));
    }

    private static BitSet load(Chunk chunk) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        long[] words = pdc.get(Keys.PLACED_BITS, PersistentDataType.LONG_ARRAY);
        if (words != null) {
            return BitSet.valueOf(words);
        }
        // Convert data saved by versions before 0.5, which stored a list of packed positions.
        BitSet bits = new BitSet();
        int[] old = pdc.get(Keys.PLACED, PersistentDataType.INTEGER_ARRAY);
        if (old != null) {
            World world = chunk.getWorld();
            for (int packed : old) {
                int x = packed & 15;
                int z = (packed >> 4) & 15;
                int y = (packed >> 8) - 2048;
                if (y >= world.getMinHeight() && y < world.getMaxHeight()) {
                    bits.set(index(world, x, y, z));
                }
            }
            pdc.remove(Keys.PLACED);
            save(chunk, bits);
        }
        return bits;
    }

    private static void save(Chunk chunk, BitSet bits) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        if (bits.isEmpty()) {
            pdc.remove(Keys.PLACED_BITS);
        } else {
            pdc.set(Keys.PLACED_BITS, PersistentDataType.LONG_ARRAY, bits.toLongArray());
        }
    }

    private static int index(Block block) {
        return index(block.getWorld(), block.getX() & 15, block.getY(), block.getZ() & 15);
    }

    private static int index(World world, int x, int y, int z) {
        return ((y - world.getMinHeight()) << 8) | (z << 4) | x;
    }
}
