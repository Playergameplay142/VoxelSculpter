package com.voxel.sculptor;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;

public class VoxelSculptor extends JavaPlugin {

    private SelectionManager selectionManager;
    private UndoManager undoManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        createFolders();

        this.selectionManager = new SelectionManager();
        this.undoManager = new UndoManager();

        CommandExecutorService executor = new CommandExecutorService(this);

        String[] commands = {
            "vhelp", "vwand", "vpos1", "vpos2", "vhpos1", "vhpos2", "vchunk", "vexpand", "vcontract",
            "vshift", "vclear", "vsize", "vcount", "vdistrib", "vvisual", "vset", "vreplace",
            "vair", "walls", "faces", "hollow", "sphere", "hsphere", "cylinder", "hcylinder",
            "pyramid", "hpyramid", "vline", "vcenter", "voverlay", "vsmooth", "vdrain",
            "vcopy", "vcut", "vpaste", "vrotate", "vflip", "vsave", "vload", "vlist",
            "vdel-schem", "vundo", "vredo", "vclearhistory", "vsuperpickaxe", "vthaw",
            "vgreen", "vfilldown", "vfixwater", "vpreset", "vreload"
        };

        for (String cmd : commands) {
            if (getCommand(cmd) != null) {
                getCommand(cmd).setExecutor(executor);
            }
        }

        getServer().getPluginManager().registerEvents(new SelectionListener(this), this);
        getLogger().info("VoxelSculptor v1.0 successfully enabled!");
    }

    private void createFolders() {
        new File(getDataFolder(), "schematics").mkdirs();
        new File(getDataFolder(), "history").mkdirs();
        new File(getDataFolder(), "presets").mkdirs();
    }

    @Override
    public void onDisable() {
        getLogger().info("VoxelSculptor disabled!");
    }

    public SelectionManager getSelectionManager() { return selectionManager; }
    public UndoManager getUndoManager() { return undoManager; }
}