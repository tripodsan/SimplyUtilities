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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.material.Dye;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * {@code ExampleListener}...
 */
public class AdvancedEChest implements Listener, PluginUtility {

    private static final List<String> MY_LORE = Collections.singletonList("Shares its items with other Advanced Ender Chests of the same channel.");

    private static final String ECHEST_LORE_CHANNEL_PREFIX = "+Channel: ";

    private JavaPlugin plugin;

    private BukkitTask task;

    private HashMap<String, Echest> echests = new HashMap<>();

    private ShapedRecipe echestRecipe;
    private char[] echestRecipeShape;

    public void enable(JavaPlugin plugin) {
        this.plugin = plugin;
        createRecipes();

        task = plugin.getServer().getScheduler().runTaskTimer(plugin, new Scanner(), 1L, 2L);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void createRecipes() {
        {
            ItemStack echest = new ItemStack(Material.ENDER_STONE);
            ItemMeta im = echest.getItemMeta();
            im.setDisplayName(ChatColor.DARK_PURPLE + "Advanced Ender Chest");
            im.setLore(MY_LORE);
            echest.setItemMeta(im);
            echestRecipe = new ShapedRecipe(new NamespacedKey(plugin, "echest"), echest);
            echestRecipe.shape("OWO", "WEW", "OPO");
            echestRecipe.setIngredient('O', Material.OBSIDIAN);
            echestRecipe.setIngredient('W', Material.WOOL);
            echestRecipe.setIngredient('E', Material.ENDER_CHEST);
            echestRecipe.setIngredient('P', Material.ENDER_PORTAL_FRAME);
            echestRecipeShape = "OWOWEWOPO".toCharArray();
        }
    }

    public void disable() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        plugin.getServer().resetRecipes();
    }

    private String getKey(Location loc) {
        return "echest-" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event){
        Player player = event.getPlayer();
        if(event.getBlock().getType() == Material.ENDER_STONE) {
            ItemMeta im = player.getItemInHand().getItemMeta();

            if (im.getLore() != null && im.getLore().size() > 0 && MY_LORE.get(0).equals(im.getLore().get(0))) {
                String channel = "";
                if (im.getLore().size() > 1) {
                    channel = im.getLore().get(1).substring(ECHEST_LORE_CHANNEL_PREFIX.length());
                }
                Echest l = createEchest(player, event.getBlock().getLocation(), channel);
                player.sendMessage(ChatColor.AQUA + "You have placed down a Echest! " + l.key);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if(event.getBlock().getType() == Material.ENDER_STONE) {
            String key = getKey(event.getBlock().getLocation());
            if (removeEchest(event.getBlock().getWorld(), key)) {
                player.sendMessage("you broke " + key);
            }
        }
    }


    private Echest createEchest(Player player, Location loc, String channel) {

        String key = getKey(loc);
        loc.add(0.5, 0, 0.5);

        ArmorStand a = player.getWorld().spawn(loc, ArmorStand.class);
        a.teleport(loc);
        a.setCustomName(key);
        a.setCustomNameVisible(true);
        a.setVisible(true);
        a.setGravity(false);
        a.setMarker(true);
        a.setArms(true);
        a.setLeftArmPose(new EulerAngle(Math.toRadians(170), Math.toRadians(170), Math.toRadians(31)));

        ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) SkullType.PLAYER.ordinal());
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setDisplayName(channel);
        meta.setOwner(player.getName());
        head.setItemMeta(meta);
        a.setHelmet(head);

        // play effect
        float red = 0f;
        float green = 0.570312f;
        float blue = 0f;
        for(int i = 0; i <360; i+=5){
            Location l = loc.clone();
            l.add(Math.cos(i)*3, 0, Math.sin(i)*3);
            loc.getWorld().spigot().playEffect(l, Effect.COLOURED_DUST, 0, 1, red, green, blue, 1, 0, 64);
        }

        return getEchest(a);
    }

    private boolean removeEchest(World world, String key) {
        Echest a = getEchest(world, key);
        if (a != null) {
            a.stand.remove();
            echests.remove(key);
            return true;
        }
        return false;
    }

    private Echest getEchest(World world, String key) {
        for (ArmorStand a: world.getEntitiesByClass(ArmorStand.class)) {
            if (key.equals(a.getCustomName())) {
                return echests.computeIfAbsent(key, k -> new Echest(a));
            }
        }
        return null;
    }

    private Echest getEchest(Block block) {
        if (block.getType() != Material.ENDER_STONE) {
            return null;
        }
        String key = getKey(block.getLocation());
        return getEchest(block.getLocation().getWorld(), key);
    }

    private Echest getEchest(ArmorStand a) {
        String key = a.getCustomName();
        if (key != null && key.startsWith("echest-")) {
            return echests.computeIfAbsent(key, k -> new Echest(a));
        } else {
            return null;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCraftColoredPrismEvent(PrepareItemCraftEvent event) {
        // plugin.getLogger().info("prepare: " + event);
        ItemStack[] ss = event.getInventory().getMatrix();
        if (ss.length != echestRecipeShape.length) {
            return;
        }
        String channel = "";
        for (int i=0; i<ss.length; i++) {
            ItemStack craftStack = ss[i];
            ItemStack recpStack = echestRecipe.getIngredientMap().get(echestRecipeShape[i]);
            if (craftStack == null || recpStack == null || craftStack.getType() != recpStack.getType()) {
                return;
            }
            if (craftStack.getType() == Material.WOOL) {
            channel += Integer.toHexString(craftStack.getData().getData());
            }
        }
        ItemStack echest = new ItemStack(echestRecipe.getResult());
        ItemMeta im = echest.getItemMeta();
        List<String> newLore = new ArrayList<>(MY_LORE);
        im.setDisplayName(echestRecipe.getResult().getItemMeta().getDisplayName());
        newLore.add(ECHEST_LORE_CHANNEL_PREFIX + channel);
        im.setLore(newLore);
        echest.setItemMeta(im);
        event.getInventory().setResult(echest);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getHand() == EquipmentSlot.HAND) {
            Echest echest = getEchest(event.getClickedBlock());
            if (echest != null) {
                event.setCancelled(true);
                openGUI(event.getPlayer(), echest);
            }
        }
    }

    private void openGUI(Player player, Echest echest) {
        Inventory inv = Bukkit.createInventory(player, InventoryType.CHEST, "Advanced Ender Chest (" + echest.channel + ")");
        player.openInventory(inv); //YAAAAAAAAAA
    }

    private class Echest {

        private final ArmorStand stand;

        private final String key;

        private String channel;

        private Echest(ArmorStand stand) {
            this.stand = stand;
            key = getKey(stand.getLocation());
            channel = stand.getHelmet().getItemMeta().getDisplayName();
        }

        private void update() {

        }

        public Block getBlock() {
            return stand.getLocation().getBlock();
        }



    }

    private class Scanner implements Runnable {

        @Override
        public void run() {
            for (World w : plugin.getServer().getWorlds()) {
                for (ArmorStand a : w.getEntitiesByClass(ArmorStand.class)) {
                    Echest l = getEchest(a);
                    if (l != null) {
                        l.update();
                    }
                }
            }

        }
    }
}