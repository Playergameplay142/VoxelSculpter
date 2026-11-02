package com.voxel.sculptor;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class SelectionListener implements Listener {

    private final VoxelSculptor plugin;

    public SelectionListener(VoxelSculptor plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("voxel.admin")) return;

        String configuredWand = plugin.getConfig().getString("wand-item", "WOODEN_AXE");
        Material wandMat = Material.matchMaterial(configuredWand);
        if (wandMat == null) wandMat = Material.WOODEN_AXE;

        if (player.getInventory().getItemInMainHand().getType() == wandMat) {
            if (event.getClickedBlock() == null) return;

            if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                event.setCancelled(true);
                plugin.getSelectionManager().setPos1(player.getUniqueId(), event.getClickedBlock().getLocation());
                player.sendMessage(ChatColor.GREEN + "Pos 1 set to: (" + event.getClickedBlock().getX() + ", " + event.getClickedBlock().getY() + ", " + event.getClickedBlock().getZ() + ")");
            } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
                plugin.getSelectionManager().setPos2(player.getUniqueId(), event.getClickedBlock().getLocation());
                player.sendMessage(ChatColor.GREEN + "Pos 2 set to: (" + event.getClickedBlock().getX() + ", " + event.getClickedBlock().getY() + ", " + event.getClickedBlock().getZ() + ")");
            }
        }
    }
}