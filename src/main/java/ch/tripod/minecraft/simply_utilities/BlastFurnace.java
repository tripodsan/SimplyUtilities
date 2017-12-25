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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Chest;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

/**
 * {@code Main}...
 */
public class BlastFurnace implements Listener, PluginUtility {

    private static class Corners {
        private Location l0;
        private Location l1;
    }

    private final static String STRUCTURE_PREFIX = "furnace-";

    private Map<String, Corners> corners = new HashMap<>();

    private JavaPlugin plugin;

    private StructureVerifier verifier = new StructureVerifier("furnace").load(
            "{\"dx\":4,\"dy\":5,\"dz\":4,\"matrix\":\"..a....b..acdda..e....a........fff..fdf..fff............ddd..ddd..ddd............ggg..g.g..gig............fff..fdf..fff..................j............\",\"map\":{\"b\":{\"mat\":\"STONE\"},\"j\":{\"mat\":\"STEP\"},\"a\":{\"mat\":\"CHEST\"},\"g\":{\"mat\":\"IRON_BLOCK\"},\".\":{\"mat\":\"AIR\"},\"c\":{\"mat\":\"GOLD_BLOCK\"},\"h\":{\"mat\":\"STATIONARY_LAVA\"},\"d\":{\"mat\":\"NETHER_BRICK\"},\"e\":{\"mat\":\"COAL_BLOCK\"},\"i\":{\"mat\":\"GLASS\"},\"f\":{\"mat\":\"NETHER_BRICK_STAIRS\"}}}"
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
            for (World w : plugin.getServer().getWorlds()) {
                for (ArmorStand a : w.getEntitiesByClass(ArmorStand.class)) {
                    Structure l = getStructure(a);
                    if (l != null) {
                        l.update();
                    }
                }
            }
        }
    }

    private Corners getCorners(Player p) {
        return corners.computeIfAbsent(p.getDisplayName(), s -> new Corners());
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event){

        if(event.getBlock().getType() == Material.STEP) {
            if (verifier.verify(event.getPlayer(), event.getBlock().getLocation(), 5)) {
                createStructure(event.getPlayer(), event.getBlock().getLocation(), event.getBlock().getState().getData());
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if(event.getBlock().getType() == Material.STEP) {
            // todo: recheck structure
            String key = getKey(event.getBlock().getLocation());
            if (removeAltar(event.getBlock().getWorld(), key)) {
                player.sendMessage("you broke " + key);
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

    private boolean removeAltar(World world, String key) {
        Structure a = getStructure(world, key);
        if (a != null) {
            a.stand.remove();
            a.destroy();
            structures.remove(key);
            return true;
        }
        return false;
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

        private InventoryHolder input;
        private ArmorStand inputTag;

        private InventoryHolder output;
        private ArmorStand outputTag;

        private InventoryHolder slag;
        private ArmorStand slagTag;

        private InventoryHolder fuel;
        private ArmorStand fuelTag;



        public Structure(ArmorStand stand) {
            this.stand = stand;
            this.key = getKey(stand.getLocation());
            Location l = stand.getLocation().add(1, -5, 0);

            detectChest(l, 1, 0);
            l.add(-2, 0, 0);
            detectChest(l, -1, 0);
            l.add(1, 0, 1);
            detectChest(l, 0, 1);
            l.add(0, 0, -2);
            detectChest(l, 0, -1);

            plugin.getLogger().info("input: " + input);
            plugin.getLogger().info("output: " + output);
            plugin.getLogger().info("slag: " + slag);
            plugin.getLogger().info("fuel: " + fuel);
        }

        private void detectChest(Location l, int x, int z) {
            Material type = l.getBlock().getType();
            l = l.clone();
            l.add(x, 0, z);
            switch (type) {
                case COAL_BLOCK:
                    fuel = (InventoryHolder) l.getBlock().getState();
                    fuelTag = createTag(l, "Fuel");
                    break;
                case STONE:
                    slag = (InventoryHolder) l.getBlock().getState();
                    slagTag = createTag(l, "Slag");
                    break;
                case GOLD_BLOCK:
                    output = (InventoryHolder) l.getBlock().getState();
                    outputTag = createTag(l, "Output");
                    break;
                case NETHER_BRICK:
                    input = (InventoryHolder) l.getBlock().getState();
                    inputTag = createTag(l, "Input");
                    break;
                default:
                    plugin.getLogger().warning("invalid block at " + l + ": " + type);

            }
        }

        private ArmorStand createTag(Location l, String title) {
            l.add(0, 1, 0);
            for (Entity e: l.getWorld().getNearbyEntities(l, 1, 1, 1)) {
                plugin.getLogger().info("entity near " + l + ": " + e);
                if (e instanceof  ArmorStand) {
                    return (ArmorStand) e;
                }
            }
            ArmorStand a = l.getWorld().spawn(l, ArmorStand.class);
            a.teleport(l);
            a.setCustomName(title);
            a.setCustomNameVisible(true);
            a.setVisible(false);
            a.setGravity(false);
            a.setMarker(true);
            return a;
        }

        public void destroy() {
            if (inputTag != null) {
                inputTag.remove();
                inputTag = null;
            }
            if (outputTag != null) {
                outputTag.remove();
                outputTag = null;
            }
            if (slagTag != null) {
                slagTag.remove();
                slagTag = null;
            }
            if (fuelTag != null) {
                fuelTag.remove();
                fuelTag = null;
            }
        }
        public void update() {
            int count = 0;
            for (ItemStack i: fuel.getInventory().getContents()) {
                if (i != null) {
                    if (i.getType() == Material.FIREBALL) {
                        count += i.getAmount();
                    }
                }
            }
            Location l = stand.getLocation().add(0, -2, 0);
            if (count == 0) {
                l.getBlock().setType(Material.AIR);
            } else if (count >= 128) {
                l.getBlock().setType(Material.STATIONARY_LAVA);
            } else {
                int level = 7 - count / 16;
                l.getBlock().setType(Material.LAVA, false);
                l.getBlock().setData((byte) level, false);
            }
        }

    }

}