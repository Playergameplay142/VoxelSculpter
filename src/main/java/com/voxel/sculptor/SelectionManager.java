package com.voxel.sculptor;

import org.bukkit.Location;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SelectionManager {
    private final Map<UUID, Location> pos1Map = new HashMap<>();
    private final Map<UUID, Location> pos2Map = new HashMap<>();

    public void setPos1(UUID uuid, Location loc) { pos1Map.put(uuid, loc); }
    public void setPos2(UUID uuid, Location loc) { pos2Map.put(uuid, loc); }
    public Location getPos1(UUID uuid) { return pos1Map.get(uuid); }
    public Location getPos2(UUID uuid) { return pos2Map.get(uuid); }

    public void clear(UUID uuid) {
        pos1Map.remove(uuid);
        pos2Map.remove(uuid);
    }

    public boolean hasSelection(UUID uuid) {
        Location p1 = pos1Map.get(uuid);
        Location p2 = pos2Map.get(uuid);
        return p1 != null && p2 != null && p1.getWorld() != null && p1.getWorld().equals(p2.getWorld());
    }

    public int getVolume(UUID uuid) {
        if (!hasSelection(uuid)) return 0;
        Location p1 = pos1Map.get(uuid);
        Location p2 = pos2Map.get(uuid);
        return (Math.abs(p1.getBlockX() - p2.getBlockX()) + 1) *
               (Math.abs(p1.getBlockY() - p2.getBlockY()) + 1) *
               (Math.abs(p1.getBlockZ() - p2.getBlockZ()) + 1);
    }
}