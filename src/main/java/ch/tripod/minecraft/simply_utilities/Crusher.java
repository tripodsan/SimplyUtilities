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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * {@code Main}...
 */
public class Crusher implements Listener, PluginUtility {

    private final static String STRUCTURE_PREFIX = "crusher-";

    private JavaPlugin plugin;

    private StructureVerifier verifier = new StructureVerifier("crusher").load(
            "{\"dx\":2,\"dy\":3,\"dz\":5,\"matrix\":\"aaaaaabaaaaaaaaaaacdceeefddeeecdceee...ggg...ggg...ggg...hhh...hhh...hhh\",\"map\":{\"g\":{\"mat\":\"NETHER_FENCE\",\"dat\":0},\"e\":{\"mat\":\"DROPPER\",\"dat\":1},\"f\":{\"mat\":\"CHEST\",\"dat\":2},\"b\":{\"mat\":\"COAL_BLOCK\",\"dat\":0},\"d\":{\"mat\":\"WOOL\",\"dat\":0},\"a\":{\"mat\":\"NETHER_BRICK\",\"dat\":0},\".\":{\"mat\":\"AIR\",\"dat\":0},\"c\":{\"mat\":\"IRON_BLOCK\",\"dat\":0},\"h\":{\"mat\":\"HOPPER\",\"dat\":0}}}"
    );

    private HashMap<String, Structure> structures = new HashMap<>();

    private final static Map<Material, ItemStack> CRUSHIES = new HashMap<>();
    static {
                CRUSHIES.put(Material.COBBLESTONE, new ItemStack(Material.GRAVEL, 1));
//                CRUSHIES.put(Material.STONE, new ItemStack(Material.AIR, 1));
//                CRUSHIES.put(Material.WOOD, new ItemStack(Material.AIR, 1));
//                CRUSHIES.put(Material.SAND, new ItemStack(Material.AIR, 1));
//                CRUSHIES.put(Material.GRAVEL, new ItemStack(Material.AIR, 1));
//                CRUSHIES.put(Material.GOLD_ORE, new ItemStack(Material.AIR, 1));
//                CRUSHIES.put(Material.IRON_ORE, new ItemStack(Material.AIR, 1));
//                CRUSHIES.put(Material.COAL_ORE, new ItemStack(Material.AIR, 1));
//                CRUSHIES.put(Material.LOG, new ItemStack(Material.AIR, 1));
//                CRUSHIES.put(Material.LEAVES, new ItemStack(Material.AIR, 1));
//                CRUSHIES.put(Material.LAPIS_ORE, new ItemStack(Material.AIR, 1));
//                CRUSHIES.put(Material.LAPIS_BLOCK, new ItemStack(Material.AIR, 1));
//                CRUSHIES.put(Material.SANDSTONE, new ItemStack(Material.AIR, 1));
//                CRUSHIES.put(Material.DEAD_BUSH, new ItemStack(Material.AIR, 1));
//                CRUSHIES.put(Material.WOOL, new ItemStack(Material.AIR, 1));
//                CRUSHIES.put(Material.YELLOW_FLOWER, new ItemStack(Material.AIR, 1));
//                CRUSHIES.put(Material.RED_ROSE, new ItemStack(Material.AIR, 1));
//                CRUSHIES.put(Material.BROWN_MUSHROOM, new ItemStack(Material.AIR, 1));
//                CRUSHIES.put(Material.RED_MUSHROOM, new ItemStack(Material.AIR, 1));
//                CRUSHIES.put(Material.GOLD_BLOCK, new ItemStack(Material.AIR, 1));
//                CRUSHIES.put(Material.IRON_BLOCK, new ItemStack(Material.AIR, 1));
//                CRUSHIES.put(Material.BRICK, new ItemStack(Material.AIR, 1));
//                CRUSHIES.put(Material.OBSIDIAN, new ItemStack(Material.AIR, 1));
//                CRUSHIES.put(Material.DIAMOND_ORE, new ItemStack(Material.AIR, 1));
//                CRUSHIES.put(Material.DIAMOND_BLOCK, new ItemStack(Material.AIR, 1));
//                CRUSHIES.put(Material.REDSTONE_ORE, new ItemStack(Material.AIR, 1));
//                CRUSHIES.put(Material.NETHER_BRICK, new ItemStack(Material.AIR, 1));
//                CRUSHIES.put(Material.EMERALD_ORE, new ItemStack(Material.AIR, 1));
//                CRUSHIES.put(Material.EMERALD_BLOCK, new ItemStack(Material.AIR, 1));
//                CRUSHIES.put(Material.REDSTONE_BLOCK, new ItemStack(Material.AIR, 1));
//                CRUSHIES.put(Material.QUARTZ_ORE, new ItemStack(Material.AIR, 1));
//                CRUSHIES.put(Material.QUARTZ_BLOCK, new ItemStack(Material.AIR, 1));
//                CRUSHIES.put(Material.COAL_BLOCK, new ItemStack(Material.AIR, 1));
//                CRUSHIES.put(Material.RED_SANDSTONE, new ItemStack(Material.AIR, 1));
//                CRUSHIES.put(Material.PURPUR_BLOCK, new ItemStack(Material.AIR, 1));
//                CRUSHIES.put(Material.NETHER_WART_BLOCK, new ItemStack(Material.AIR, 1));
//                CRUSHIES.put(Material.RED_NETHER_BRICK, new ItemStack(Material.AIR, 1));
//                CRUSHIES.put(Material.BONE_BLOCK, new ItemStack(Material.AIR, 1));

//                CRUSHIES.put(Material.COAL, new ItemStack(Material.AIR, 1));
//                CRUSHIES.put(Material.STICK, new ItemStack(Material.AIR, 1));
//                CRUSHIES.put(Material.FLINT, new ItemStack(Material.AIR, 1));
//                CRUSHIES.put(Material.SUGAR_CANE, new ItemStack(Material.AIR, 1));
//                CRUSHIES.put(Material.BONE, new ItemStack(Material.AIR, 1));
//                CRUSHIES.put(Material.BLAZE_ROD, new ItemStack(Material.AIR, 1));
    }

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

        if(event.getBlock().getType() == Material.CHEST) {
            if (verifier.verify(event.getPlayer(), event.getBlock().getLocation(), 1,1, 0, null)) {
                createStructure(event.getPlayer(), event.getBlock().getLocation(), event.getBlock().getState().getData());
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if(event.getBlock().getType() == Material.CHEST) {
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

        private InventoryHolder[] input;

        private Material crushee = null;
        private ItemStack crushend = null;

        private int crushTime = 0;

        public Structure(ArmorStand stand) {
            this.stand = stand;
            this.key = getKey(stand.getLocation());

            Block chest = stand.getLocation().getBlock();
            if (!(chest.getState() instanceof InventoryHolder)) {
                Log.info("expected chest is missing: %s", chest);
                return;
            }
            output = (InventoryHolder) chest.getState();
            outputTag = createTag(stand.getLocation(), "Output");

            input = new InventoryHolder[9];
            for (int x = 0 ; x<3; x++) {
                for (int z = 0; z<3; z++) {
                    Location il = stand.getLocation().add(-1 + x, 2, 3 + z);
                    Block hop = il.getBlock();
                    if (!(hop.getState() instanceof InventoryHolder)) {
                        Log.info("expected hopper is missing: %s", hop);
                    } else {
                        input[x * 3 + z] = (InventoryHolder) hop.getState();
                        Log.info("input: %s", hop);
                    }
                }
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
            if (outputTag != null) {
                outputTag.remove();
                outputTag = null;
            }
        }

        public void update() {
            if (crushee != null) {
                Location loc =  stand.getLocation().add(0, 0.5, 4);
                loc.getWorld().spigot().playEffect(loc, Effect.CRIT, 0, 0, 1, 0.25f, 1, 0.5f, 50, 64);
                crushTime++;
                if (crushTime > 20) {
                    Log.info("Crushing of %s into %s finished.", crushee, crushend.getType());
                    stand.getWorld().playSound(stand.getLocation(), Sound.UI_TOAST_OUT, 1, 2);
                    output.getInventory().addItem(crushend);
                    crushee = null;
                    crushend = null;
                } else {
                    if (crushTime % 2 == 0) {
                        stand.getWorld().playSound(stand.getLocation(), Sound.ENTITY_EVOCATION_FANGS_ATTACK, 0.05f, 0.5f);
                    }
                }
            } else {
                for (InventoryHolder i: input) {
                    for (ItemStack item: i.getInventory().getContents()) {
                        if (item == null) {
                            continue;
                        }
                        crushend = CRUSHIES.get(item.getType());
                        if (crushend != null) {
                            crushee = item.getType();
                            item.setAmount(item.getAmount() - 1);
                            Log.info("Crushing of %s into %s started.", crushee, crushend.getType());
                            crushTime = 0;
                            stand.getWorld().playSound(stand.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.1f, 0.5f);
                            break;
                        }
                    }
                    if (crushend != null) {
                        break;
                    }
                }
            }
        }
    }
}