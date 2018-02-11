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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.*;
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
import org.bukkit.map.MapView;
import org.bukkit.material.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * {@code Main}...
 */
public class BlastFurnace implements Listener, PluginUtility {

    private final static String STRUCTURE_PREFIX = "furnace-";

    private JavaPlugin plugin;

    private StructureVerifier verifier = new StructureVerifier("furnace").load(
            "{\"dx\":4,\"dy\":5,\"dz\":4,\"matrix\":\"..a....b..acdda..e....a........fff..fdf..fff............ddd..ddd..ddd............ggg..g.g..gig............fff..fdf..fff..................j............\",\"map\":{\"b\":{\"mat\":\"STONE\"},\"j\":{\"mat\":\"STEP\"},\"a\":{\"mat\":\"CHEST\"},\"g\":{\"mat\":\"IRON_BLOCK\"},\".\":{\"mat\":\"AIR\"},\"c\":{\"mat\":\"GOLD_BLOCK\"},\"h\":{\"mat\":\"STATIONARY_LAVA\"},\"d\":{\"mat\":\"NETHER_BRICK\"},\"e\":{\"mat\":\"COAL_BLOCK\"},\"i\":{\"mat\":\"GLASS\"},\"f\":{\"mat\":\"NETHER_BRICK_STAIRS\"}}}"
    );

    private HashMap<String, Structure> structures = new HashMap<>();

    private final static Map<Material, ItemStack> SMELTIES = new HashMap<>();
    static {
        SMELTIES.put(Material.COBBLESTONE, new ItemStack(Material.STONE, 2));
        SMELTIES.put(Material.SAND, new ItemStack(Material.GLASS, 2));
        SMELTIES.put(Material.GOLD_ORE, new ItemStack(Material.GOLD_INGOT, 2));
        SMELTIES.put(Material.IRON_ORE, new ItemStack(Material.IRON_INGOT, 2));
        SMELTIES.put(Material.COAL_ORE, new ItemStack(Material.COAL, 2));
        SMELTIES.put(Material.LOG, new ItemStack(Material.COAL, 2, (byte) 1));
        SMELTIES.put(Material.SPONGE, new ItemStack(Material.SPONGE, 1));
        SMELTIES.put(Material.LAPIS_ORE, new ItemStack(Material.INK_SACK, 2, (byte) 4));
        SMELTIES.put(Material.DIAMOND_ORE, new ItemStack(Material.DIAMOND, 2));
        SMELTIES.put(Material.REDSTONE_ORE, new ItemStack(Material.REDSTONE, 2));
        SMELTIES.put(Material.CACTUS, new ItemStack(Material.INK_SACK, 2, (byte) 2));
        SMELTIES.put(Material.CLAY, new ItemStack(Material.HARD_CLAY, 2));
        SMELTIES.put(Material.NETHERRACK, new ItemStack(Material.NETHER_BRICK, 2));
        SMELTIES.put(Material.EMERALD_ORE, new ItemStack(Material.EMERALD, 2));
        SMELTIES.put(Material.QUARTZ_ORE, new ItemStack(Material.QUARTZ, 2));
        SMELTIES.put(Material.STAINED_CLAY, new ItemStack(Material.BLACK_GLAZED_TERRACOTTA));
        SMELTIES.put(Material.ENDER_STONE, new ItemStack(Material.END_BRICKS, 2));
        SMELTIES.put(Material.IRON_SPADE, new ItemStack(Material.IRON_NUGGET, 2));
        SMELTIES.put(Material.IRON_PICKAXE, new ItemStack(Material.IRON_NUGGET, 2));
        SMELTIES.put(Material.IRON_AXE, new ItemStack(Material.IRON_NUGGET, 2));
        SMELTIES.put(Material.IRON_SWORD, new ItemStack(Material.IRON_NUGGET, 2));
        SMELTIES.put(Material.GOLD_SWORD, new ItemStack(Material.GOLD_NUGGET, 2));
        SMELTIES.put(Material.GOLD_SPADE, new ItemStack(Material.GOLD_NUGGET, 2));
        SMELTIES.put(Material.GOLD_PICKAXE, new ItemStack(Material.GOLD_NUGGET, 2));
        SMELTIES.put(Material.GOLD_AXE, new ItemStack(Material.GOLD_NUGGET, 2));
        SMELTIES.put(Material.IRON_HOE, new ItemStack(Material.IRON_NUGGET, 2));
        SMELTIES.put(Material.GOLD_HOE, new ItemStack(Material.GOLD_NUGGET, 2));
        SMELTIES.put(Material.IRON_HELMET, new ItemStack(Material.IRON_NUGGET, 2));
        SMELTIES.put(Material.IRON_CHESTPLATE, new ItemStack(Material.IRON_NUGGET, 2));
        SMELTIES.put(Material.IRON_LEGGINGS, new ItemStack(Material.IRON_NUGGET, 2));
        SMELTIES.put(Material.IRON_BOOTS, new ItemStack(Material.IRON_NUGGET, 2));
        SMELTIES.put(Material.GOLD_HELMET, new ItemStack(Material.GOLD_NUGGET, 2));
        SMELTIES.put(Material.GOLD_CHESTPLATE, new ItemStack(Material.GOLD_NUGGET, 2));
        SMELTIES.put(Material.GOLD_LEGGINGS, new ItemStack(Material.GOLD_NUGGET, 2));
        SMELTIES.put(Material.GOLD_BOOTS, new ItemStack(Material.GOLD_NUGGET, 2));
        SMELTIES.put(Material.PORK, new ItemStack(Material.GRILLED_PORK, 2));
        SMELTIES.put(Material.CLAY_BALL, new ItemStack(Material.CLAY_BRICK, 2));
        SMELTIES.put(Material.RAW_FISH, new ItemStack(Material.COOKED_FISH, 2));
        SMELTIES.put(Material.RAW_BEEF, new ItemStack(Material.COOKED_BEEF, 2));
        SMELTIES.put(Material.RAW_CHICKEN, new ItemStack(Material.COOKED_CHICKEN, 2));
        SMELTIES.put(Material.ROTTEN_FLESH, new ItemStack(Material.LEATHER, 2));
        SMELTIES.put(Material.POTATO_ITEM, new ItemStack(Material.BAKED_POTATO, 2));
        SMELTIES.put(Material.RABBIT, new ItemStack(Material.COOKED_RABBIT));
        SMELTIES.put(Material.IRON_BARDING, new ItemStack(Material.IRON_NUGGET, 2));
        SMELTIES.put(Material.GOLD_BARDING, new ItemStack(Material.GOLD_NUGGET, 2));
        SMELTIES.put(Material.MUTTON, new ItemStack(Material.COOKED_MUTTON));
        SMELTIES.put(Material.CHORUS_FRUIT, new ItemStack(Material.CHORUS_FRUIT_POPPED, 2));
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

        private InventoryHolder input;
        private ArmorStand inputTag;

        private InventoryHolder output;
        private ArmorStand outputTag;

        private InventoryHolder slag;
        private ArmorStand slagTag;

        private InventoryHolder fuel;
        private ArmorStand fuelTag;

        private ArmorStand fuelGauge;

        private int fuelLevel = 0;

        private Material smeltee = null;
        private ItemStack smeltend = null;

        private int smeltTime = 0;

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

            l = stand.getLocation().add(0, -3, 0);
            fuelGauge = createTag(l, "Fuel: 0");
            fuelLevel = Integer.parseInt(fuelGauge.getCustomName().substring(6));
            plugin.getLogger().info("fuellevel: " + fuelLevel);
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
            if (fuelGauge != null) {
                fuelGauge.remove();
                fuelGauge = null;
            }
        }

        private void setFuelLevel(int level) {
            fuelLevel = level;
            fuelGauge.setCustomName("Fuel: " + fuelLevel);
        }
        public void update() {
            if (fuelLevel < 128) {
                int idx = fuel.getInventory().first(Material.FIREBALL);
                if (idx >= 0) {
                    setFuelLevel(fuelLevel+1);
                    ItemStack charge = fuel.getInventory().getContents()[idx];
                    charge.setAmount(charge.getAmount()-1);
                }
            }

            if (smeltee != null) {
                Location loc =  stand.getLocation().add(0, -1.5, 0);
                loc.getWorld().spigot().playEffect(loc, Effect.FLAME, 0, 0, 0.3f, 0.25f, 0.3f, 0.01f, 50, 64);
                smeltTime++;
                if (smeltTime > 20) {
                    Log.info("Smelting of %s into %s finished.", smeltee, smeltend.getType());
                    stand.getWorld().playSound(stand.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, 0);
                    output.getInventory().addItem(smeltend);
                    slag.getInventory().addItem(createSlag());
                    setFuelLevel(fuelLevel-1);
                    smeltee = null;
                    smeltend = null;
                } else {
                    stand.getWorld().playSound(stand.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.1f, 0);
                }
            } else if (fuelLevel > 0) {
                for (ItemStack item: input.getInventory().getContents()) {
                    if (item == null) {
                        continue;
                    }
                    smeltend = SMELTIES.get(item.getType());
                    if (smeltend != null) {
                        smeltee = item.getType();
                        item.setAmount(item.getAmount() - 1);
                        Log.info("Smelting of %s into %s started.", smeltee, smeltend.getType());
                        smeltTime = 0;
                        stand.getWorld().playSound(stand.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1, 0);
                        break;
                    }
                }
            }
        }

    }

}