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
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code Main}...
 */
public class Incinerator implements Listener, PluginUtility {

    private final static String STRUCTURE_PREFIX = "incinerator-";

    private final static String CARBON_TOKEN_TITLE = "Carbon Token";

    private JavaPlugin plugin;

    private StructureVerifier verifier = new StructureVerifier("incinerator").load(
            "{\"dx\":6,\"dy\":2,\"dz\":6,\"matrix\":\"abbbbbcadddddcadddddcaddeddcadddddcadddddcaffffff........bb.bb..adgdc...g.g...adgdc..af.fc........................bbc....a.c....aff................\",\"map\":{\"e\":{\"mat\":\"MAGMA\",\"dat\":0},\"d\":{\"mat\":\"NETHER_BRICK\",\"dat\":0},\"g\":{\"mat\":\"IRON_BLOCK\",\"dat\":0},\"c\":{\"mat\":\"NETHER_BRICK_STAIRS\",\"dat\":3},\".\":{\"mat\":\"AIR\",\"dat\":0},\"a\":{\"mat\":\"NETHER_BRICK_STAIRS\",\"dat\":2},\"f\":{\"mat\":\"NETHER_BRICK_STAIRS\",\"dat\":1},\"b\":{\"mat\":\"NETHER_BRICK_STAIRS\",\"dat\":0}}}"
    );

    private HashMap<String, Structure> structures = new HashMap<>();

    private final static Map<Material, Integer> INCINERATIES = new HashMap<>();
    static {
        INCINERATIES.put(Material.LAVA_BUCKET, 20005);
        INCINERATIES.put(Material.COAL_BLOCK, 16000);
        INCINERATIES.put(Material.BLAZE_ROD, 2400);
        INCINERATIES.put(Material.COAL, 1600);
        INCINERATIES.put(Material.BOAT, 400);
        INCINERATIES.put(Material.BOAT_ACACIA, 400);
        INCINERATIES.put(Material.BOAT_BIRCH, 400);
        INCINERATIES.put(Material.BOAT_DARK_OAK, 400);
        INCINERATIES.put(Material.BOAT_JUNGLE, 400);
        INCINERATIES.put(Material.BOAT_SPRUCE, 400);
        INCINERATIES.put(Material.LOG, 300);
        INCINERATIES.put(Material.LOG_2, 300);
        INCINERATIES.put(Material.WOOD, 300);
        INCINERATIES.put(Material.WOOD_PLATE, 300);
        INCINERATIES.put(Material.FENCE, 300);
        INCINERATIES.put(Material.JUNGLE_FENCE, 300);
        INCINERATIES.put(Material.SPRUCE_FENCE, 300);
        INCINERATIES.put(Material.DARK_OAK_FENCE, 300);
        INCINERATIES.put(Material.ACACIA_FENCE, 300);
        INCINERATIES.put(Material.BIRCH_FENCE, 300);
        INCINERATIES.put(Material.FENCE_GATE, 300);
        INCINERATIES.put(Material.JUNGLE_FENCE_GATE, 300);
        INCINERATIES.put(Material.SPRUCE_FENCE_GATE, 300);
        INCINERATIES.put(Material.DARK_OAK_FENCE_GATE, 300);
        INCINERATIES.put(Material.ACACIA_FENCE_GATE, 300);
        INCINERATIES.put(Material.BIRCH_FENCE_GATE, 300);
        INCINERATIES.put(Material.WOOD_STAIRS, 300);
        INCINERATIES.put(Material.JUNGLE_WOOD_STAIRS, 300);
        INCINERATIES.put(Material.SPRUCE_WOOD_STAIRS, 300);
        INCINERATIES.put(Material.DARK_OAK_STAIRS, 300);
        INCINERATIES.put(Material.ACACIA_STAIRS, 300);
        INCINERATIES.put(Material.BIRCH_WOOD_STAIRS, 300);
        INCINERATIES.put(Material.TRAP_DOOR, 300);
        INCINERATIES.put(Material.WORKBENCH, 300);
        INCINERATIES.put(Material.BOOKSHELF, 300);
        INCINERATIES.put(Material.CHEST, 300);
        INCINERATIES.put(Material.TRAPPED_CHEST, 300);
        INCINERATIES.put(Material.DAYLIGHT_DETECTOR, 300);
        INCINERATIES.put(Material.JUKEBOX, 300);
        INCINERATIES.put(Material.NOTE_BLOCK, 300);
        INCINERATIES.put(Material.HUGE_MUSHROOM_1, 300);
        INCINERATIES.put(Material.HUGE_MUSHROOM_2, 300);
        INCINERATIES.put(Material.BANNER, 300);
        INCINERATIES.put(Material.WOOD_STEP, 150);
        INCINERATIES.put(Material.BOW, 300);
        INCINERATIES.put(Material.FISHING_ROD, 300);
        INCINERATIES.put(Material.LADDER, 300);
        INCINERATIES.put(Material.WOOD_PICKAXE, 200);
        INCINERATIES.put(Material.WOOD_SPADE, 200);
        INCINERATIES.put(Material.WOOD_HOE, 200);
        INCINERATIES.put(Material.WOOD_AXE, 200);
        INCINERATIES.put(Material.WOOD_SWORD, 200);
        INCINERATIES.put(Material.SIGN, 200);
        INCINERATIES.put(Material.WOODEN_DOOR, 200);
        INCINERATIES.put(Material.WOOD_DOOR, 200);
        INCINERATIES.put(Material.JUNGLE_DOOR, 200);
        INCINERATIES.put(Material.SPRUCE_DOOR, 200);
        INCINERATIES.put(Material.DARK_OAK_DOOR, 200);
        INCINERATIES.put(Material.ACACIA_DOOR, 200);
        INCINERATIES.put(Material.BIRCH_DOOR, 200);
        INCINERATIES.put(Material.BOWL, 100);
        INCINERATIES.put(Material.SAPLING, 100);
        INCINERATIES.put(Material.STICK, 100);
        INCINERATIES.put(Material.WOOL, 100);
        INCINERATIES.put(Material.WOOD_BUTTON, 100);
        INCINERATIES.put(Material.CARPET, 67);
    }

    private static final int[] TOKEN_VALUES = {1000000, 500000, 100000, 50000, 10000, 5000, 1000, 500, 100, 50, 10, 5, 1};

    private void initRecipes() {
        {
            ShapelessRecipe recp = new ShapelessRecipe(new NamespacedKey(plugin, "coal_token_i"), createCarbonToken(3));
            recp.addIngredient(1, Material.COAL);
            plugin.getServer().addRecipe(recp);
        }
        {
            ShapelessRecipe recp = new ShapelessRecipe(new NamespacedKey(plugin, "coal_token_v"), createCarbonToken(7));
            recp.addIngredient(5, Material.COAL);
            plugin.getServer().addRecipe(recp);
        }
        {
            ShapelessRecipe recp = new ShapelessRecipe(new NamespacedKey(plugin, "coal_token_x"), createCarbonToken(13));
            recp.addIngredient(2, Material.COAL);
            plugin.getServer().addRecipe(recp);
        }
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

    private static ItemStack createCarbonToken(int burnTime) {
        ItemStack token = new ItemStack(Material.COAL, 1);
        ItemMeta meta = token.getItemMeta();
        meta.setDisplayName(CARBON_TOKEN_TITLE);
        meta.setLore(Arrays.asList("Output from incinerator; burns for " + burnTime + " ticks."));
        token.setItemMeta(meta);
        return token;
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
    public void onFurnaceBurnEvent(FurnaceBurnEvent evt) {
        ItemStack s = evt.getFuel();
        int time = getBurnTime(s);
        if (time > 0) {
            Log.info("set furnace burn time to %d", time);
            evt.setBurnTime(time);
        }
    }

    public int getBurnTime(ItemStack s) {
        if (s == null) {
            return -1;
        }
        ItemMeta m = s.getItemMeta();
        if (m != null && m.hasLore() && CARBON_TOKEN_TITLE.equals(m.getDisplayName())) {
            String[] lore = m.getLore().get(0).split(" ");
            return Integer.parseInt(lore[5]);
        }
        return -1;
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

    @EventHandler(ignoreCancelled = true)
    public void onCraftTokenEvent(PrepareItemCraftEvent event) {
        CraftingInventory i = event.getInventory();
        int totalBurnTime = 0;
        int count = 0;
        for (ItemStack s: i.getMatrix()) {
            if (s == null) {
                continue;
            }
            int b = getBurnTime(s);
            if (b < 0) {
                return;
            }
            count++;
            totalBurnTime += b;
        }
        Log.info("total burntime: %d", totalBurnTime);
        switch (totalBurnTime) {
            case 5:
            case 10:
            case 50:
            case 100:
            case 500:
            case 1000:
            case 5000:
            case 10000:
            case 50000:
            case 100000:
            case 500000:
            case 1000000:
                ItemStack result;
                if (count == 1) {
                    if (String.valueOf(totalBurnTime).charAt(0) == '5') {
                        result = createCarbonToken(totalBurnTime / 5);
                        result.setAmount(5);
                    } else {
                        result = createCarbonToken(totalBurnTime / 2);
                        result.setAmount(2);
                    }
                } else {
                    result = createCarbonToken(totalBurnTime);
                }
                i.setResult(result);
                break;
            default:
                i.setResult(null);
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

    private class Structure {

        private final ArmorStand stand;

        private final String key;

        private ArmorStand outputTag;

        private Item burnee = null;

        private int fueltime = 0;

        private int burnTime = 0;

        public Structure(ArmorStand stand) {
            this.stand = stand;
            this.key = getKey(stand.getLocation());

//            outputTag = createTag(stand.getLocation(), "Output");

        }

        private InventoryHolder getOutput() {
            for (int d = 0; d < 4; d++) {
                int dx = d < 2 ? d%2 * 4 - 2 : 0;
                int dz = d >=2 ? d%2 * 4 - 2 : 0;

                Block chest = stand.getLocation().add(dx, 1, dz).getBlock();
//                Log.info("chest: %s", chest);
                if (chest.getState() instanceof InventoryHolder) {
                    Log.info("found output chest: %s", chest);
                    return (InventoryHolder) chest.getState();
                }
            }
            return null;
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
                    Log.info("Incineration of %s into token of value %d finished.", burnee, fueltime);
                    stand.getWorld().playSound(stand.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 2);
                    InventoryHolder output = getOutput();
                    while (fueltime > 0) {
                        for (int b: TOKEN_VALUES) {
                            if (fueltime >= b) {
                                fueltime -= b;
                                ItemStack token = createCarbonToken(b);
                                if (output == null) {
                                    Location dropLoc = stand.getLocation().add(0, 2, 0);
                                    Item dropped = dropLoc.getWorld().dropItem(dropLoc, token);
                                    dropped.setVelocity(new Vector(Math.random()-0.5, 1, Math.random()-0.5));
                                } else {
                                    output.getInventory().addItem(token);
                                }
                                break;
                            }
                        }
                    }
                    burnee = null;
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
                        fueltime = INCINERATIES.getOrDefault(s.getType(), 5);
                        Log.info("Incineration of %s into token of value %d started.", s.getType(), fueltime);

                        s.setAmount(s.getAmount() - 1);
                        burnee = (Item) e;
                        burnTime = 0;
                        stand.getWorld().playSound(stand.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.1f, 0.5f);
                        break;
                    }
                }
            }
        }
    }
}