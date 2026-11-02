package com.voxel.sculptor;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import java.util.*;

public class UndoManager {
    public record BlockRecord(Location location, BlockData blockData) {}

    private final Map<UUID, Stack<List<BlockRecord>>> undoHistory = new HashMap<>();
    private final Map<UUID, Stack<List<BlockRecord>>> redoHistory = new HashMap<>();

    public void saveUndo(UUID uuid, List<BlockRecord> records) {
        undoHistory.computeIfAbsent(uuid, k -> new Stack<>()).push(records);
    }

    public List<BlockRecord> popUndo(UUID uuid) {
        Stack<List<BlockRecord>> stack = undoHistory.get(uuid);
        return (stack != null && !stack.isEmpty()) ? stack.pop() : null;
    }

    public void saveRedo(UUID uuid, List<BlockRecord> records) {
        redoHistory.computeIfAbsent(uuid, k -> new Stack<>()).push(records);
    }

    public List<BlockRecord> popRedo(UUID uuid) {
        Stack<List<BlockRecord>> stack = redoHistory.get(uuid);
        return (stack != null && !stack.isEmpty()) ? stack.pop() : null;
    }

    public void clearHistory(UUID uuid) {
        undoHistory.remove(uuid);
        redoHistory.remove(uuid);
    }
}