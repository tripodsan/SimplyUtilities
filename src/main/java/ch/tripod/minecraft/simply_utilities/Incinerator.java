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

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.HashMap;

/**
 * {@code Main}...
 */
public class Incinerator implements Listener, PluginUtility {

    private final static String STRUCTURE_PREFIX = "incinerator-";

    private JavaPlugin plugin;

    private StructureVerifier verifier = new StructureVerifier("incinerator").load(
            "{\"dx\":6,\"dy\":2,\"dz\":6,\"matrix\":\"abbbbbcadddddcadddddcaddeddcadddddcadddddcaffffff........bb.bb..adgdc...g.g...adgdc..af.fc........................bbc....a.c....aff................\",\"map\":{\"e\":{\"mat\":\"MAGMA\",\"dat\":0},\"d\":{\"mat\":\"NETHER_BRICK\",\"dat\":0},\"g\":{\"mat\":\"IRON_BLOCK\",\"dat\":0},\"c\":{\"mat\":\"NETHER_BRICK_STAIRS\",\"dat\":3},\".\":{\"mat\":\"AIR\",\"dat\":0},\"a\":{\"mat\":\"NETHER_BRICK_STAIRS\",\"dat\":2},\"f\":{\"mat\":\"NETHER_BRICK_STAIRS\",\"dat\":1},\"b\":{\"mat\":\"NETHER_BRICK_STAIRS\",\"dat\":0}}}"
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

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event){

        if(event.getBlock().getType() == Material.MAGMA) {
            if (verifier.verify(event.getPlayer(), event.getBlock().getLocation(), 3,0, 3, null)) {
                createStructure(event.getPlayer(), event.getBlock().getLocation(), event.getBlock().getState().getData());
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if(event.getBlock().getType() == Material.MAGMA) {
            // todo: recheck structure
            String key = getKey(event.getBlock().getLocation());
            if (removeStructure(event.getBlock().getWorld(), key)) {
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

    private boolean removeStructure(World world, String key) {
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
            if (key.equals(a.getCustomName())) {
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

    private ItemStack createSlag() {
        ItemStack slag = new ItemStack(Material.SULPHUR, 1);
        ItemMeta meta = slag.getItemMeta();
        meta.setDisplayName("Slag");
        meta.setLore(Arrays.asList("Waste from Blast Furnace smelting."));
        slag.setItemMeta(meta);
        return slag;
    }

    private class Structure {

        private final ArmorStand stand;

        private final String key;

        private InventoryHolder output;
        private ArmorStand outputTag;

        private Item burnee = null;
        private ItemStack burnend = null;

        private int burnTime = 0;

        public Structure(ArmorStand stand) {
            this.stand = stand;
            this.key = getKey(stand.getLocation());

            for (int d = 0; d < 4; d++) {
                int dx = d < 2 ? d%2 * 4 - 2 : 0;
                int dz = d >=2 ? d%2 * 4 - 2 : 0;

                Block chest = stand.getLocation().add(dx, 1, dz).getBlock();
                Log.info("chest: %s", chest);
                if (chest.getState() instanceof InventoryHolder) {
                    Log.info("found output chest: %s", chest);
                    output = (InventoryHolder) chest.getState();
                    break;
                }
            }
//            outputTag = createTag(stand.getLocation(), "Output");

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
            if (outputTag != null) {
                outputTag.remove();
                outputTag = null;
            }
        }

        public void update() {
            if (burnee != null) {
                Location loc =  stand.getLocation().add(0, 2, 0);
                loc.getWorld().spigot().playEffect(loc, Effect.LAVA_POP, 0, 0, 0.1f, 0.1f, 0.1f, 1f, 10, 20);
                burnTime++;
                if (burnTime > 20) {
                    Log.info("Incineration of %s into %s finished.", burnee, burnend.getType());
                    stand.getWorld().playSound(stand.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 2);
                    if (output == null) {
                        Location dropLoc = stand.getLocation().add(0, 2, 0);
                        Item dropped = dropLoc.getWorld().dropItem(dropLoc, burnend);
                        dropped.setVelocity(new Vector(Math.random()-0.5, 1, Math.random()-0.5));
                    } else {
                        output.getInventory().addItem(burnend);
                    }
                    burnee = null;
                    burnend = null;
                } else {
                    stand.getWorld().playSound(stand.getLocation(), Sound.ITEM_FIRECHARGE_USE, 0.5f, 1.5f);
                }
            } else {
                for (Entity e: stand.getLocation().getWorld().getNearbyEntities(stand.getLocation(), 2, 2, 2)) {
                    if (e instanceof Item) {
                        ItemStack s = ((Item) e).getItemStack();
                        if (s.getType() == Material.COAL) {
                            continue;
                        }
                        s.setAmount(s.getAmount() - 1);
                        burnee = (Item) e;
                        //e.remove();
                        burnend = new ItemStack(Material.COAL, 1);

                        Log.info("Incineration of %s into %s started.", burnee, burnend.getType());
                        burnTime = 0;
                        stand.getWorld().playSound(stand.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.1f, 0.5f);
                        break;
                    }
                }
            }
        }
    }
}