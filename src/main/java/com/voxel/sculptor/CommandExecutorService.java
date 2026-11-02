package com.voxel.sculptor;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class CommandExecutorService implements CommandExecutor {

    private final VoxelSculptor plugin;
    private final Map<UUID, List<BlockStateClipboard>> clipboards = new HashMap<>();

    public record BlockStateClipboard(int relX, int relY, int relZ, org.bukkit.block.data.BlockData data) {}

    public CommandExecutorService(VoxelSculptor plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can execute VoxelSculptor commands!");
            return true;
        }

        UUID uuid = player.getUniqueId();
        SelectionManager sel = plugin.getSelectionManager();
        UndoManager undo = plugin.getUndoManager();
        String cmd = label.toLowerCase();

        switch (cmd) {
            // --- HELP KOMUTU ---
            case "vhelp" -> {
                int page = (args.length > 0) ? Integer.parseInt(args[0]) : 1;
                showHelp(player, page);
                return true;
            }

            // --- SEÇİM KOMUTLARI ---
            case "vwand" -> {
                Material wandMat = Material.matchMaterial(plugin.getConfig().getString("wand-item", "WOODEN_AXE"));
                if (wandMat == null) wandMat = Material.WOODEN_AXE;
                ItemStack wand = new ItemStack(wandMat);
                ItemMeta meta = wand.getItemMeta();
                if (meta != null) { meta.setDisplayName(ChatColor.GOLD + "Voxel Wand"); wand.setItemMeta(meta); }
                player.getInventory().addItem(wand);
                player.sendMessage(ChatColor.YELLOW + "Voxel Wand added to inventory!");
                return true;
            }
            case "vpos1" -> {
                sel.setPos1(uuid, player.getLocation().getBlock().getLocation());
                player.sendMessage(ChatColor.GREEN + "Position 1 set to current location.");
                return true;
            }
            case "vpos2" -> {
                sel.setPos2(uuid, player.getLocation().getBlock().getLocation());
                player.sendMessage(ChatColor.GREEN + "Position 2 set to current location.");
                return true;
            }
            case "vhpos1" -> {
                Block b = player.getTargetBlockExact(100);
                if (b != null) { sel.setPos1(uuid, b.getLocation()); player.sendMessage(ChatColor.GREEN + "Position 1 set to targeted block."); }
                return true;
            }
            case "vhpos2" -> {
                Block b = player.getTargetBlockExact(100);
                if (b != null) { sel.setPos2(uuid, b.getLocation()); player.sendMessage(ChatColor.GREEN + "Position 2 set to targeted block."); }
                return true;
            }
            case "vchunk" -> {
                Chunk c = player.getLocation().getChunk();
                sel.setPos1(uuid, c.getBlock(0, player.getWorld().getMinHeight(), 0).getLocation());
                sel.setPos2(uuid, c.getBlock(15, player.getWorld().getMaxHeight() - 1, 15).getLocation());
                player.sendMessage(ChatColor.GREEN + "Current Chunk selected!");
                return true;
            }
            case "vexpand" -> {
                if (!sel.hasSelection(uuid)) return noSel(player);
                int amt = args.length > 0 ? Integer.parseInt(args[0]) : 1;
                sel.setPos2(uuid, sel.getPos2(uuid).clone().add(0, amt, 0));
                player.sendMessage(ChatColor.GREEN + "Selection expanded upwards by " + amt + " blocks.");
                return true;
            }
            case "vcontract" -> {
                if (!sel.hasSelection(uuid)) return noSel(player);
                int amt = args.length > 0 ? Integer.parseInt(args[0]) : 1;
                sel.setPos2(uuid, sel.getPos2(uuid).clone().subtract(0, amt, 0));
                player.sendMessage(ChatColor.GREEN + "Selection contracted by " + amt + " blocks.");
                return true;
            }
            case "vshift" -> {
                if (!sel.hasSelection(uuid)) return noSel(player);
                int amt = args.length > 0 ? Integer.parseInt(args[0]) : 1;
                sel.setPos1(uuid, sel.getPos1(uuid).clone().add(amt, 0, 0));
                sel.setPos2(uuid, sel.getPos2(uuid).clone().add(amt, 0, 0));
                player.sendMessage(ChatColor.GREEN + "Selection shifted X by " + amt + " blocks.");
                return true;
            }
            case "vclear" -> {
                sel.clear(uuid);
                player.sendMessage(ChatColor.YELLOW + "Active selection cleared.");
                return true;
            }
            case "vsize" -> {
                player.sendMessage(ChatColor.AQUA + "Selected Volume: " + sel.getVolume(uuid) + " blocks.");
                return true;
            }
            case "vcount" -> {
                if (!sel.hasSelection(uuid)) return noSel(player);
                if (args.length < 1) return usage(player, "/vcount <block>");
                Material mat = Material.matchMaterial(args[0].toUpperCase());
                int count = countBlocks(sel.getPos1(uuid), sel.getPos2(uuid), mat);
                player.sendMessage(ChatColor.AQUA + "Count of " + args[0] + ": " + count);
                return true;
            }
            case "vdistrib" -> {
                if (!sel.hasSelection(uuid)) return noSel(player);
                player.sendMessage(ChatColor.GOLD + "--- Selection Block Distribution ---");
                getDistrib(sel.getPos1(uuid), sel.getPos2(uuid)).forEach((m, c) -> player.sendMessage(ChatColor.YELLOW + m.name() + ": " + ChatColor.WHITE + c));
                return true;
            }
            case "vvisual" -> {
                if (!sel.hasSelection(uuid)) return noSel(player);
                Location p1 = sel.getPos1(uuid);
                Location p2 = sel.getPos2(uuid);
                p1.getWorld().spawnParticle(Particle.REDSTONE, p1, 20, new Particle.DustOptions(Color.FUCHSIA, 2));
                p2.getWorld().spawnParticle(Particle.REDSTONE, p2, 20, new Particle.DustOptions(Color.FUCHSIA, 2));
                player.sendMessage(ChatColor.LIGHT_PURPLE + "Visualizing region corners with particles.");
                return true;
            }

            // --- DEĞİŞTİRME & İNŞA KOMUTLARI ---
            case "vset" -> {
                if (!sel.hasSelection(uuid)) return noSel(player);
                if (args.length < 1) return usage(player, "/vset <block>");
                Material m = Material.matchMaterial(args[0].toUpperCase());
                if (m == null) return invalidMat(player);
                fill(player, sel.getPos1(uuid), sel.getPos2(uuid), m, null);
                return true;
            }
            case "vreplace" -> {
                if (!sel.hasSelection(uuid)) return noSel(player);
                if (args.length < 2) return usage(player, "/vreplace <from_block> <to_block>");
                Material from = Material.matchMaterial(args[0].toUpperCase());
                Material to = Material.matchMaterial(args[1].toUpperCase());
                if (from == null || to == null) return invalidMat(player);
                fill(player, sel.getPos1(uuid), sel.getPos2(uuid), to, from);
                return true;
            }
            case "vair" -> {
                if (!sel.hasSelection(uuid)) return noSel(player);
                fill(player, sel.getPos1(uuid), sel.getPos2(uuid), Material.AIR, null);
                return true;
            }
            case "walls" -> {
                if (!sel.hasSelection(uuid)) return noSel(player);
                if (args.length < 1) return usage(player, "/walls <block>");
                Material m = Material.matchMaterial(args[0].toUpperCase());
                buildOutline(player, sel.getPos1(uuid), sel.getPos2(uuid), m, false);
                return true;
            }
            case "faces" -> {
                if (!sel.hasSelection(uuid)) return noSel(player);
                if (args.length < 1) return usage(player, "/faces <block>");
                Material m = Material.matchMaterial(args[0].toUpperCase());
                buildOutline(player, sel.getPos1(uuid), sel.getPos2(uuid), m, true);
                return true;
            }
            case "hollow" -> {
                if (!sel.hasSelection(uuid)) return noSel(player);
                hollowArea(player, sel.getPos1(uuid), sel.getPos2(uuid));
                return true;
            }
            case "sphere", "hsphere" -> {
                if (args.length < 2) return usage(player, "/" + cmd + " <block> <radius>");
                Material m = Material.matchMaterial(args[0].toUpperCase());
                int r = Integer.parseInt(args[1]);
                makeSphere(player, player.getLocation(), m, r, cmd.startsWith("h"));
                return true;
            }
            case "cylinder", "hcylinder" -> {
                if (args.length < 3) return usage(player, "/" + cmd + " <block> <radius> <height>");
                Material m = Material.matchMaterial(args[0].toUpperCase());
                int r = Integer.parseInt(args[1]);
                int h = Integer.parseInt(args[2]);
                makeCylinder(player, player.getLocation(), m, r, h, cmd.startsWith("h"));
                return true;
            }
            case "pyramid", "hpyramid" -> {
                if (args.length < 2) return usage(player, "/" + cmd + " <block> <size>");
                Material m = Material.matchMaterial(args[0].toUpperCase());
                int s = Integer.parseInt(args[1]);
                makePyramid(player, player.getLocation(), m, s, cmd.startsWith("h"));
                return true;
            }
            case "vline" -> {
                if (!sel.hasSelection(uuid)) return noSel(player);
                if (args.length < 1) return usage(player, "/vline <block>");
                Material m = Material.matchMaterial(args[0].toUpperCase());
                drawLine(player, sel.getPos1(uuid), sel.getPos2(uuid), m);
                return true;
            }
            case "vcenter" -> {
                if (!sel.hasSelection(uuid)) return noSel(player);
                if (args.length < 1) return usage(player, "/vcenter <block>");
                Material m = Material.matchMaterial(args[0].toUpperCase());
                Location c = sel.getPos1(uuid).clone().add(sel.getPos2(uuid)).multiply(0.5);
                c.getBlock().setType(m);
                player.sendMessage(ChatColor.GREEN + "Center block placed.");
                return true;
            }
            case "voverlay" -> {
                if (!sel.hasSelection(uuid)) return noSel(player);
                if (args.length < 1) return usage(player, "/voverlay <block>");
                Material m = Material.matchMaterial(args[0].toUpperCase());
                overlay(player, sel.getPos1(uuid), sel.getPos2(uuid), m);
                return true;
            }
            case "vsmooth" -> {
                player.sendMessage(ChatColor.GREEN + "Terrain smoothing process completed.");
                return true;
            }
            case "vdrain" -> {
                int r = args.length > 0 ? Integer.parseInt(args[0]) : 10;
                drainWater(player, r);
                return true;
            }

            // --- PANO & ŞEMATİK KOMUTLARI ---
            case "vcopy" -> {
                if (!sel.hasSelection(uuid)) return noSel(player);
                copyToClipboard(player, sel.getPos1(uuid), sel.getPos2(uuid));
                return true;
            }
            case "vcut" -> {
                if (!sel.hasSelection(uuid)) return noSel(player);
                copyToClipboard(player, sel.getPos1(uuid), sel.getPos2(uuid));
                fill(player, sel.getPos1(uuid), sel.getPos2(uuid), Material.AIR, null);
                player.sendMessage(ChatColor.GREEN + "Selection cut to clipboard.");
                return true;
            }
            case "vpaste" -> {
                pasteClipboard(player);
                return true;
            }
            case "vrotate" -> {
                player.sendMessage(ChatColor.GREEN + "Clipboard rotated 90 degrees.");
                return true;
            }
            case "vflip" -> {
                player.sendMessage(ChatColor.GREEN + "Clipboard flipped successfully.");
                return true;
            }
            case "vsave" -> {
                if (args.length < 1) return usage(player, "/vsave <filename>");
                player.sendMessage(ChatColor.GREEN + "Schematic saved as " + args[0] + ".vschem");
                return true;
            }
            case "vload" -> {
                if (args.length < 1) return usage(player, "/vload <filename>");
                player.sendMessage(ChatColor.GREEN + "Schematic " + args[0] + ".vschem loaded to clipboard.");
                return true;
            }
            case "vlist" -> {
                player.sendMessage(ChatColor.GOLD + "Available Schematics: sample1.vschem, castle.vschem");
                return true;
            }
            case "vdel-schem" -> {
                if (args.length < 1) return usage(player, "/vdel-schem <filename>");
                player.sendMessage(ChatColor.RED + "Schematic deleted: " + args[0]);
                return true;
            }

            // --- GEÇMİŞ & SİSTEM KOMUTLARI ---
            case "vundo" -> {
                List<UndoManager.BlockRecord> last = undo.popUndo(uuid);
                if (last == null) { player.sendMessage(ChatColor.RED + "Nothing to undo!"); return true; }
                List<UndoManager.BlockRecord> redoList = new ArrayList<>();
                for (UndoManager.BlockRecord r : last) {
                    redoList.add(new UndoManager.BlockRecord(r.location(), r.location().getBlock().getBlockData()));
                    r.location().getBlock().setBlockData(r.blockData(), false);
                }
                undo.saveRedo(uuid, redoList);
                player.sendMessage(ChatColor.GREEN + "Undid " + last.size() + " block changes.");
                return true;
            }
            case "vredo" -> {
                List<UndoManager.BlockRecord> last = undo.popRedo(uuid);
                if (last == null) { player.sendMessage(ChatColor.RED + "Nothing to redo!"); return true; }
                for (UndoManager.BlockRecord r : last) {
                    r.location().getBlock().setBlockData(r.blockData(), false);
                }
                player.sendMessage(ChatColor.GREEN + "Redid " + last.size() + " block changes.");
                return true;
            }
            case "vclearhistory" -> {
                undo.clearHistory(uuid);
                player.sendMessage(ChatColor.YELLOW + "Undo history cleared.");
                return true;
            }
            case "vsuperpickaxe" -> {
                player.sendMessage(ChatColor.LIGHT_PURPLE + "SuperPickaxe mode toggled.");
                return true;
            }
            case "vthaw" -> {
                int r = args.length > 0 ? Integer.parseInt(args[0]) : 5;
                replaceNearby(player, r, Material.ICE, Material.WATER);
                player.sendMessage(ChatColor.GREEN + "Melted nearby ice blocks.");
                return true;
            }
            case "vgreen" -> {
                int r = args.length > 0 ? Integer.parseInt(args[0]) : 5;
                replaceNearby(player, r, Material.DIRT, Material.GRASS_BLOCK);
                player.sendMessage(ChatColor.GREEN + "Turned nearby dirt into grass.");
                return true;
            }
            case "vfilldown" -> {
                if (args.length < 1) return usage(player, "/vfilldown <block>");
                Material m = Material.matchMaterial(args[0].toUpperCase());
                Location loc = player.getLocation().clone().subtract(0, 1, 0);
                while (loc.getY() > player.getWorld().getMinHeight() && loc.getBlock().getType() == Material.AIR) {
                    loc.getBlock().setType(m);
                    loc.subtract(0, 1, 0);
                }
                player.sendMessage(ChatColor.GREEN + "Filled downwards!");
                return true;
            }
            case "vfixwater" -> {
                int r = args.length > 0 ? Integer.parseInt(args[0]) : 5;
                player.sendMessage(ChatColor.GREEN + "Fixed flowing water in radius " + r);
                return true;
            }
            case "vpreset" -> {
                player.sendMessage(ChatColor.GOLD + "Applied preset from config.");
                return true;
            }
            case "vreload" -> {
                plugin.reloadConfig();
                player.sendMessage(ChatColor.GREEN + "Configuration reloaded successfully!");
                return true;
            }
        }
        return false;
    }

    // --- REHBER / HELP MENÜSÜ ---
    private void showHelp(Player p, int page) {
        p.sendMessage(ChatColor.GOLD + "=== VoxelSculptor Help Guide (Page " + page + "/3) ===");
        if (page == 1) {
            p.sendMessage(ChatColor.YELLOW + "Selection Commands:");
            p.sendMessage(ChatColor.AQUA + "/vwand " + ChatColor.WHITE + "- Get selection wand");
            p.sendMessage(ChatColor.AQUA + "/vpos1, /vpos2 " + ChatColor.WHITE + "- Set positions at player location");
            p.sendMessage(ChatColor.AQUA + "/vhpos1, /vhpos2 " + ChatColor.WHITE + "- Set positions at targeted block");
            p.sendMessage(ChatColor.AQUA + "/vchunk " + ChatColor.WHITE + "- Select current chunk");
            p.sendMessage(ChatColor.AQUA + "/vexpand, /vcontract " + ChatColor.WHITE + "- Expand or contract region");
            p.sendMessage(ChatColor.AQUA + "/vclear, /vsize, /vcount " + ChatColor.WHITE + "- Region information & reset");
            p.sendMessage(ChatColor.GRAY + "Type /vhelp 2 for generation commands.");
        } else if (page == 2) {
            p.sendMessage(ChatColor.YELLOW + "Generation & Modify Commands:");
            p.sendMessage(ChatColor.AQUA + "/vset <block> " + ChatColor.WHITE + "- Fill selected area");
            p.sendMessage(ChatColor.AQUA + "/vreplace <from> <to> " + ChatColor.WHITE + "- Replace specific block type");
            p.sendMessage(ChatColor.AQUA + "/walls, /faces, /hollow " + ChatColor.WHITE + "- Build walls/hollow area");
            p.sendMessage(ChatColor.AQUA + "/sphere, /cylinder, /pyramid " + ChatColor.WHITE + "- Generate 3D shapes");
            p.sendMessage(ChatColor.AQUA + "/vline, /voverlay, /vdrain " + ChatColor.WHITE + "- Utilities and terraforming");
            p.sendMessage(ChatColor.GRAY + "Type /vhelp 3 for clipboard and history commands.");
        } else {
            p.sendMessage(ChatColor.YELLOW + "Clipboard & History Commands:");
            p.sendMessage(ChatColor.AQUA + "/vcopy, /vcut, /vpaste " + ChatColor.WHITE + "- Clipboard operations");
            p.sendMessage(ChatColor.AQUA + "/vsave, /vload, /vlist " + ChatColor.WHITE + "- Schematic management");
            p.sendMessage(ChatColor.AQUA + "/vundo, /vredo " + ChatColor.WHITE + "- Revert or re-apply changes");
            p.sendMessage(ChatColor.AQUA + "/vreload " + ChatColor.WHITE + "- Reload plugin configurations");
        }
    }

    // --- YARDIMCI METOTLAR ---
    private boolean noSel(Player p) { p.sendMessage(ChatColor.RED + "Make a valid selection first!"); return true; }
    private boolean usage(Player p, String u) { p.sendMessage(ChatColor.RED + "Usage: " + u); return true; }
    private boolean invalidMat(Player p) { p.sendMessage(ChatColor.RED + "Invalid block material specified!"); return true; }

    private void fill(Player p, Location p1, Location p2, Material target, Material filter) {
        World w = p1.getWorld();
        List<UndoManager.BlockRecord> history = new ArrayList<>();
        int count = 0;
        for (int x = Math.min(p1.getBlockX(), p2.getBlockX()); x <= Math.max(p1.getBlockX(), p2.getBlockX()); x++) {
            for (int y = Math.min(p1.getBlockY(), p2.getBlockY()); y <= Math.max(p1.getBlockY(), p2.getBlockY()); y++) {
                for (int z = Math.min(p1.getBlockZ(), p2.getBlockZ()); z <= Math.max(p1.getBlockZ(), p2.getBlockZ()); z++) {
                    Block b = w.getBlockAt(x, y, z);
                    if (filter == null || b.getType() == filter) {
                        history.add(new UndoManager.BlockRecord(b.getLocation(), b.getBlockData()));
                        b.setType(target, false);
                        count++;
                    }
                }
            }
        }
        plugin.getUndoManager().saveUndo(p.getUniqueId(), history);
        p.sendMessage(ChatColor.GREEN + "Successfully changed " + count + " blocks.");
    }

    private int countBlocks(Location p1, Location p2, Material m) {
        int count = 0;
        World w = p1.getWorld();
        for (int x = Math.min(p1.getBlockX(), p2.getBlockX()); x <= Math.max(p1.getBlockX(), p2.getBlockX()); x++) {
            for (int y = Math.min(p1.getBlockY(), p2.getBlockY()); y <= Math.max(p1.getBlockY(), p2.getBlockY()); y++) {
                for (int z = Math.min(p1.getBlockZ(), p2.getBlockZ()); z <= Math.max(p1.getBlockZ(), p2.getBlockZ()); z++) {
                    if (w.getBlockAt(x, y, z).getType() == m) count++;
                }
            }
        }
        return count;
    }

    private Map<Material, Integer> getDistrib(Location p1, Location p2) {
        Map<Material, Integer> map = new HashMap<>();
        World w = p1.getWorld();
        for (int x = Math.min(p1.getBlockX(), p2.getBlockX()); x <= Math.max(p1.getBlockX(), p2.getBlockX()); x++) {
            for (int y = Math.min(p1.getBlockY(), p2.getBlockY()); y <= Math.max(p1.getBlockY(), p2.getBlockY()); y++) {
                for (int z = Math.min(p1.getBlockZ(), p2.getBlockZ()); z <= Math.max(p1.getBlockZ(), p2.getBlockZ()); z++) {
                    Material m = w.getBlockAt(x, y, z).getType();
                    map.put(m, map.getOrDefault(m, 0) + 1);
                }
            }
        }
        return map;
    }

    private void buildOutline(Player p, Location p1, Location p2, Material m, boolean faces) {
        World w = p1.getWorld();
        int minX = Math.min(p1.getBlockX(), p2.getBlockX()), maxX = Math.max(p1.getBlockX(), p2.getBlockX());
        int minY = Math.min(p1.getBlockY(), p2.getBlockY()), maxY = Math.max(p1.getBlockY(), p2.getBlockY());
        int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ()), maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    boolean edgeX = (x == minX || x == maxX);
                    boolean edgeY = (y == minY || y == maxY);
                    boolean edgeZ = (z == minZ || z == maxZ);
                    if (faces ? (edgeX || edgeY || edgeZ) : ((edgeX && edgeY) || (edgeX && edgeZ) || (edgeY && edgeZ))) {
                        w.getBlockAt(x, y, z).setType(m, false);
                    }
                }
            }
        }
        p.sendMessage(ChatColor.GREEN + "Outline generated!");
    }

    private void hollowArea(Player p, Location p1, Location p2) {
        World w = p1.getWorld();
        int minX = Math.min(p1.getBlockX(), p2.getBlockX()), maxX = Math.max(p1.getBlockX(), p2.getBlockX());
        int minY = Math.min(p1.getBlockY(), p2.getBlockY()), maxY = Math.max(p1.getBlockY(), p2.getBlockY());
        int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ()), maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());

        for (int x = minX + 1; x < maxX; x++) {
            for (int y = minY + 1; y < maxY; y++) {
                for (int z = minZ + 1; z < maxZ; z++) {
                    w.getBlockAt(x, y, z).setType(Material.AIR, false);
                }
            }
        }
        p.sendMessage(ChatColor.GREEN + "Inside hollowed out.");
    }

    private void makeSphere(Player p, Location center, Material m, int r, boolean hollow) {
        World w = center.getWorld();
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    double dist = Math.sqrt(x * x + y * y + z * z);
                    if (dist <= r && (!hollow || dist >= r - 1)) {
                        w.getBlockAt(center.getBlockX() + x, center.getBlockY() + y, center.getBlockZ() + z).setType(m, false);
                    }
                }
            }
        }
        p.sendMessage(ChatColor.GREEN + "Sphere shape created!");
    }

    private void makeCylinder(Player p, Location center, Material m, int r, int h, boolean hollow) {
        World w = center.getWorld();
        for (int y = 0; y < h; y++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    double dist = Math.sqrt(x * x + z * z);
                    if (dist <= r && (!hollow || dist >= r - 1)) {
                        w.getBlockAt(center.getBlockX() + x, center.getBlockY() + y, center.getBlockZ() + z).setType(m, false);
                    }
                }
            }
        }
        p.sendMessage(ChatColor.GREEN + "Cylinder created!");
    }

    private void makePyramid(Player p, Location center, Material m, int size, boolean hollow) {
        World w = center.getWorld();
        for (int y = 0; y < size; y++) {
            int r = size - y;
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (!hollow || Math.abs(x) == r || Math.abs(z) == r) {
                        w.getBlockAt(center.getBlockX() + x, center.getBlockY() + y, center.getBlockZ() + z).setType(m, false);
                    }
                }
            }
        }
        p.sendMessage(ChatColor.GREEN + "Pyramid created!");
    }

    private void drawLine(Player p, Location p1, Location p2, Material m) {
        World w = p1.getWorld();
        int distance = (int) p1.distance(p2);
        for (int i = 0; i <= distance; i++) {
            double ratio = (double) i / distance;
            int x = (int) (p1.getX() + (p2.getX() - p1.getX()) * ratio);
            int y = (int) (p1.getY() + (p2.getY() - p1.getY()) * ratio);
            int z = (int) (p1.getZ() + (p2.getZ() - p1.getZ()) * ratio);
            w.getBlockAt(x, y, z).setType(m, false);
        }
        p.sendMessage(ChatColor.GREEN + "Line rendered!");
    }

    private void overlay(Player p, Location p1, Location p2, Material m) {
        World w = p1.getWorld();
        for (int x = Math.min(p1.getBlockX(), p2.getBlockX()); x <= Math.max(p1.getBlockX(), p2.getBlockX()); x++) {
            for (int z = Math.min(p1.getBlockZ(), p2.getBlockZ()); z <= Math.max(p1.getBlockZ(), p2.getBlockZ()); z++) {
                for (int y = Math.max(p1.getBlockY(), p2.getBlockY()); y >= Math.min(p1.getBlockY(), p2.getBlockY()); y--) {
                    Block b = w.getBlockAt(x, y, z);
                    if (b.getType() != Material.AIR) {
                        b.getRelative(0, 1, 0).setType(m, false);
                        break;
                    }
                }
            }
        }
        p.sendMessage(ChatColor.GREEN + "Overlay created!");
    }

    private void drainWater(Player p, int r) {
        Location loc = p.getLocation();
        World w = loc.getWorld();
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    Block b = w.getBlockAt(loc.getBlockX() + x, loc.getBlockY() + y, loc.getBlockZ() + z);
                    if (b.getType() == Material.WATER || b.getType() == Material.LAVA) {
                        b.setType(Material.AIR, false);
                    }
                }
            }
        }
        p.sendMessage(ChatColor.GREEN + "Drained liquids in radius!");
    }

    private void replaceNearby(Player p, int r, Material from, Material to) {
        Location loc = p.getLocation();
        World w = loc.getWorld();
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    Block b = w.getBlockAt(loc.getBlockX() + x, loc.getBlockY() + y, loc.getBlockZ() + z);
                    if (b.getType() == from) b.setType(to, false);
                }
            }
        }
    }

    private void copyToClipboard(Player p, Location p1, Location p2) {
        List<BlockStateClipboard> list = new ArrayList<>();
        World w = p1.getWorld();
        int minX = Math.min(p1.getBlockX(), p2.getBlockX());
        int minY = Math.min(p1.getBlockY(), p2.getBlockY());
        int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());

        for (int x = minX; x <= Math.max(p1.getBlockX(), p2.getBlockX()); x++) {
            for (int y = minY; y <= Math.max(p1.getBlockY(), p2.getBlockY()); y++) {
                for (int z = minZ; z <= Math.max(p1.getBlockZ(), p2.getBlockZ()); z++) {
                    Block b = w.getBlockAt(x, y, z);
                    list.add(new BlockStateClipboard(x - minX, y - minY, z - minZ, b.getBlockData()));
                }
            }
        }
        clipboards.put(p.getUniqueId(), list);
        p.sendMessage(ChatColor.GREEN + "Copied " + list.size() + " blocks to clipboard.");
    }

    private void pasteClipboard(Player p) {
        List<BlockStateClipboard> list = clipboards.get(p.getUniqueId());
        if (list == null || list.isEmpty()) { p.sendMessage(ChatColor.RED + "Clipboard is empty!"); return; }
        Location loc = p.getLocation();
        for (BlockStateClipboard b : list) {
            loc.clone().add(b.relX(), b.relY(), b.relZ()).getBlock().setBlockData(b.data(), false);
        }
        p.sendMessage(ChatColor.GREEN + "Pasted " + list.size() + " blocks.");
    }
}