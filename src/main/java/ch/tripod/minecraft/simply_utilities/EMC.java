/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.tripod.minecraft.simply_utilities;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

/**
 * {@code Main}...
 */
public class EMC implements Listener, PluginUtility {

    private static class Corners {
        private Location l0;
        private Location l1;
    }

    private final static String STRUCTURE_PREFIX = "emc-";

    private Map<String, Corners> corners = new HashMap<>();

    private JavaPlugin plugin;

    private StructureVerifier verifier = new StructureVerifier("emc").load(
            "{\"dx\":6,\"dy\":4,\"dz\":6,\"matrix\":\"aaaaaaaabccdbaadbdbcaacdddcaacbdbdaabdccbaaaaaaaaa.....a.e...e...........f...........e...e.a.....a........e...e.......................e...e................e...e.......................e...e................ghhhg..h...h..h...h..h...h..ghhhg........\",\"map\":{\"a\":{\"mat\":\"OBSIDIAN\",\"dat\":0},\"e\":{\"mat\":\"STAINED_GLASS\",\"dat\":2},\"g\":{\"mat\":\"COAL_BLOCK\",\"dat\":0},\"d\":{\"mat\":\"CONCRETE_POWDER\",\"dat\":10},\".\":{\"mat\":\"AIR\",\"dat\":0},\"f\":{\"mat\":\"ENDER_PORTAL_FRAME\"},\"b\":{\"mat\":\"PURPUR_BLOCK\",\"dat\":0},\"c\":{\"mat\":\"QUARTZ_BLOCK\",\"dat\":0},\"h\":{\"mat\":\"STAINED_GLASS_PANE\",\"dat\":2}}}"
    );

    private HashMap<String, Structure> structures = new HashMap<>();

    private void initRecipes() {
    }

    private BukkitTask task;

    public void enable(JavaPlugin plugin) {
        this.plugin = plugin;
        initRecipes();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, new StructureScanner(), 1L, 2L);
    }

    public void disable() {
        if (task != null) {
            task.cancel();
            task  = null;
        }
    }

    private class StructureScanner implements Runnable {

        @Override
        public void run() {
        }
    }

    private Corners getCorners(Player p) {
        return corners.computeIfAbsent(p.getDisplayName(), s -> new Corners());
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event){

        if(event.getBlock().getType() == Material.ENDER_PORTAL_FRAME) {
            if (verifier.verify(event.getPlayer(), event.getBlock().getLocation(), 1)) {
                Structure s = createStructure(event.getPlayer(), event.getBlock().getLocation(), event.getBlock().getState().getData());
                if (s != null) {
                    s.checkActivated();
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        for (World w : plugin.getServer().getWorlds()) {
            for (ArmorStand a : w.getEntitiesByClass(ArmorStand.class)) {
                Structure l = getStructure(a);
                Location loc = event.getBlock().getLocation();
                if (l != null && l.isInside(loc)) {
                    if (!l.isValid(loc)) {
                        removeAltar(l);
                        player.sendMessage("you broke " + l.key);
                    }
                }
            }
        }
    }

    private String getKey(Location loc) {
        return STRUCTURE_PREFIX + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    private Structure createStructure(Player player, Location loc, MaterialData data) {
        String key = getKey(loc);
        loc.add(0.5, 0, 0.5);

        ArmorStand a = player.getWorld().spawn(loc, ArmorStand.class);
        a.teleport(loc);
        a.setCustomName(key);
        a.setCustomNameVisible(true);
        a.setVisible(true);
        a.setGravity(false);
        a.setMarker(true);

        // play effect
        float red = 0f;
        float green = 0.570312f;
        float blue = 0f;
        for(int i = 0; i <360; i+=5){
            Location l = loc.clone();
            l.add(Math.cos(i)*3, 0, Math.sin(i)*3);
            loc.getWorld().spigot().playEffect(l, Effect.COLOURED_DUST, 0, 1, red, green, blue, 1, 0, 64);
        }

        return getStructure(a);
    }

    private boolean removeAltar(Structure a) {
        if (a != null) {
            a.stand.remove();
            a.destroy();
            structures.remove(a.key);
            return true;
        }
        return false;
    }


    private boolean removeAltar(World world, String key) {
        Structure a = getStructure(world, key);
        return removeAltar(a);
    }

    private Structure getStructure(World world, String key) {
        for (ArmorStand a: world.getEntitiesByClass(ArmorStand.class)) {
            if (a.getCustomName().equals(key)) {
                return structures.computeIfAbsent(key, k -> new Structure(a));
            }
        }
        return null;
    }

    private Structure getStructure(ArmorStand a) {
        String key = a.getCustomName();
        if (key!=null && key.startsWith(STRUCTURE_PREFIX)) {
            return structures.computeIfAbsent(key, k -> new Structure(a));
        } else {
            return null;
        }
    }

    private class Structure {

        private final ArmorStand stand;

        private final String key;

        private Vector boxMin;
        private Vector boxMax;

        private Location center;

        private boolean isActive;

        public Structure(ArmorStand stand) {
            this.stand = stand;
            this.key = getKey(stand.getLocation());
            // get item frames
            center = stand.getLocation().add(0, 1, 0);
            Vector d1 = new Vector(4, 4, 4);
            Vector d2 = new Vector(3, 2, 3);
            boxMin = center.toVector().subtract(d1);
            boxMax = center.toVector().add(d2);
        }

        public void checkActivated() {
            boolean a = stand.getLocation().getBlock().getData() >= 4;
            if (a == isActive) {
                return;
            }
            isActive = a;
            drawCeiling();
        }

        public void drawCeiling() {
            Material mat = isActive ? Material.END_GATEWAY : Material.AIR;
            for (int x=-1; x<2; x++) {
                for (int z=-1; z<2; z++) {
                    Location l = center.clone().add(x, 2, z);
                    l.getBlock().setType(mat);
                }
            }
        }

        public boolean isInside(Location loc) {
            return loc.toVector().isInAABB(boxMin, boxMax);
        }

        public boolean isValid(Location forcedAirLocation) {
            return verifier.verify(null, center, 3, forcedAirLocation);
        }

        public void destroy() {
            isActive = false;
            drawCeiling();
        }

    }

}