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
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.material.Dispenser;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * {@code ExampleListener}...
 */
public class Placers implements Listener, PluginUtility {

    private static final List<String> PLACER_LORE = Collections.singletonList("Places blocks from its inventory in front of it.");

    private JavaPlugin plugin;

    private BukkitTask task;

    private HashMap<String, Placer> placers = new HashMap<>();

    public void enable(JavaPlugin plugin) {
        this.plugin = plugin;
        createRecipes();

        task = plugin.getServer().getScheduler().runTaskTimer(plugin, new Scanner(), 1L, 2L);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void createRecipes() {
        // create placer recipe
        {
            ItemStack placer = new ItemStack(Material.DISPENSER);
            ItemMeta im = placer.getItemMeta();
            im.setDisplayName(ChatColor.GREEN + "Placer");
            im.setLore(PLACER_LORE);
            placer.setItemMeta(im);
            ShapedRecipe recp = new ShapedRecipe(new NamespacedKey(plugin, "placer"), placer);
            recp.shape("ISI", "LPG", "IDI");
            recp.setIngredient('I', Material.IRON_INGOT);
            recp.setIngredient('S', Material.SLIME_BALL);
            recp.setIngredient('L', Material.INK_SACK, 10);
            recp.setIngredient('P', Material.PISTON_BASE);
            recp.setIngredient('G', Material.GLASS);
            recp.setIngredient('D', Material.DROPPER);
            plugin.getServer().addRecipe(recp);
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
        return "placer-" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event){
        Player player = event.getPlayer();
        if(event.getBlock().getType() == Material.DISPENSER) {
            Collection<ItemStack> stack = event.getBlock().getDrops();
            ItemMeta im = stack.iterator().next().getItemMeta();
            // player.sendMessage("stack is " + im.getDisplayName());
            im = player.getItemInHand().getItemMeta();
            if (PLACER_LORE.equals(im.getLore())) {
                Dispenser dispenser = (Dispenser) event.getBlock().getState().getData();

                Placer l = createPlacer(player, event.getBlock().getLocation(), dispenser);
                player.sendMessage(ChatColor.AQUA + "You have placed down a placer! " + l.key);
            }
        }
    }



    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if(event.getBlock().getType() == Material.DISPENSER) {
            String key = getKey(event.getBlock().getLocation());
            if (removePlacer(event.getBlock().getWorld(), key)) {
                player.sendMessage("you broke " + key);
            }
        }
    }


    private Placer createPlacer(Player player, Location loc, Dispenser dispenser) {
        BlockFace face = dispenser.getFacing();
        Vector dir = new Vector(face.getModX(), face.getModY(), face.getModZ());

        String key = getKey(loc);
        loc.add(0.5, 0, 0.5);
        loc.setDirection(dir);

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

        return getPlacer(a);
    }

    private boolean removePlacer(World world, String key) {
        Placer a = getPlacer(world, key);
        if (a != null) {
            a.stand.remove();
            placers.remove(key);
            return true;
        }
        return false;
    }

    private Placer getPlacer(World world, String key) {
        for (ArmorStand a: world.getEntitiesByClass(ArmorStand.class)) {
            if (key.equals(a.getCustomName())) {
                return placers.computeIfAbsent(key, k -> new Placer(a));
            }
        }
        return null;
    }

    private Placer getPlacer(Block block) {
        if (block.getType() != Material.DISPENSER) {
            return null;
        }
        String key = getKey(block.getLocation());
        return getPlacer(block.getLocation().getWorld(), key);
    }

    private Placer getPlacer(ArmorStand a) {
        String key = a.getCustomName();
        if (key != null && key.startsWith("placer-")) {
            return placers.computeIfAbsent(key, k -> new Placer(a));
        } else {
            return null;
        }
    }

    private class Placer {

        private final ArmorStand stand;

        private final String key;

        private int delay = -1;

        private Placer(ArmorStand stand) {
            this.stand = stand;
            key = getKey(stand.getLocation());
        }

        private void update() {
            Location l = stand.getLocation();
            l.add(0, 0.5, 0);
            Block dispBlock = l.getBlock();

            Vector v = l.getDirection();
            l.add(v);
            if (l.getBlock().getType() != Material.AIR) {
                return;
            }
            if (delay>0) {
                delay--;
                return;
            }
            if (delay<0) {
                delay=5;
                return;
            }
            delay--;
            if (dispBlock.getType() == Material.DISPENSER) {
                InventoryHolder holder = (InventoryHolder) dispBlock.getState();
                Inventory i = holder.getInventory();
                for (ItemStack is: i.getContents()) {
                    if (is == null) {
                        continue;
                    }
                    if (!is.getType().isBlock()) {
                        continue;
                    }
                    l.getBlock().setType(is.getType());
                    l.getBlock().setData(is.getData().getData());
                    is.setAmount(is.getAmount()-1);
                    break;
                }

            } else {
                plugin.getLogger().warning("ERROR: NON-DISPENSER ERROR (SU-557)");
            }


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
                    Placer l = getPlacer(a);
                    if (l != null) {
                        l.update();
                    }
                }
            }

        }
    }
}