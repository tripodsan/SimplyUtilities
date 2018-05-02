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
import org.bukkit.entity.*;
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * {@code ExampleListener}...
 */
public class ReArmourer implements Listener, PluginUtility {

    private static final List<String> REARMOR_LORE = Collections.singletonList("Puts armor on entities in front of it.");

    private JavaPlugin plugin;

    private BukkitTask task;

    private HashMap<String, Rearmor> rearmors = new HashMap<>();

    public void enable(JavaPlugin plugin) {
        this.plugin = plugin;
        createRecipes();

        task = plugin.getServer().getScheduler().runTaskTimer(plugin, new Scanner(), 1L, 2L);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void createRecipes() {
        // create rearmor recipe
        {
            ItemStack rearmor = new ItemStack(Material.DISPENSER);
            ItemMeta im = rearmor.getItemMeta();
            im.setDisplayName(ChatColor.GREEN + "Re-Armourer");
            im.setLore(REARMOR_LORE);
            rearmor.setItemMeta(im);
            ShapedRecipe recp = new ShapedRecipe(new NamespacedKey(plugin, "rearmor"), rearmor);
            recp.shape("ICI", "PAP", "IPI");
            recp.setIngredient('I', Material.IRON_INGOT);
            recp.setIngredient('A', Material.ARMOR_STAND);
            recp.setIngredient('C', Material.CHEST);
            recp.setIngredient('P', Material.PISTON_BASE);
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
        return "rearmor-" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event){
        Player player = event.getPlayer();
        if(event.getBlock().getType() == Material.DISPENSER) {
            Collection<ItemStack> stack = event.getBlock().getDrops();
            ItemMeta im = stack.iterator().next().getItemMeta();
            // player.sendMessage("stack is " + im.getDisplayName());
            im = player.getItemInHand().getItemMeta();
            if (REARMOR_LORE.equals(im.getLore())) {
                Dispenser dispenser = (Dispenser) event.getBlock().getState().getData();

                Rearmor l = createRearmor(player, event.getBlock().getLocation(), dispenser);
                player.sendMessage(ChatColor.AQUA + "You have placed down a re-armorer! " + l.key);
            }
        }
    }



    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if(event.getBlock().getType() == Material.DISPENSER) {
            String key = getKey(event.getBlock().getLocation());
            if (removeRearmor(event.getBlock().getWorld(), key)) {
                player.sendMessage("you broke " + key);
            }
        }
    }


    private Rearmor createRearmor(Player player, Location loc, Dispenser dispenser) {
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

        return getRearmor(a);
    }

    private boolean removeRearmor(World world, String key) {
        Rearmor a = getRearmor(world, key);
        if (a != null) {
            a.stand.remove();
            rearmors.remove(key);
            return true;
        }
        return false;
    }

    private Rearmor getRearmor(World world, String key) {
        for (ArmorStand a: world.getEntitiesByClass(ArmorStand.class)) {
            if (key.equals(a.getCustomName())) {
                return rearmors.computeIfAbsent(key, k -> new Rearmor(a));
            }
        }
        return null;
    }

    private Rearmor getRearmor(Block block) {
        if (block.getType() != Material.DISPENSER) {
            return null;
        }
        String key = getKey(block.getLocation());
        return getRearmor(block.getLocation().getWorld(), key);
    }

    private Rearmor getRearmor(ArmorStand a) {
        String key = a.getCustomName();
        if (key != null && key.startsWith("rearmor-")) {
            return rearmors.computeIfAbsent(key, k -> new Rearmor(a));
        } else {
            return null;
        }
    }

    private static boolean isEmpty(ItemStack s) {
        return s == null || s.getType() == Material.AIR;
    }

    private class Rearmor {

        private final ArmorStand stand;

        private final String key;

        private int delay = -1;

        private Rearmor(ArmorStand stand) {
            this.stand = stand;
            key = getKey(stand.getLocation());
        }

        private void update() {
            Location l = stand.getLocation();
            l.add(0, 0.5, 0);
            Block dispBlock = l.getBlock();
            if (dispBlock.getType() != Material.DISPENSER) {
                plugin.getLogger().warning("ERROR: NON-DISPENSER ERROR (SU-557)");
                return;
            }

            Vector v = l.getDirection();
            l.add(v);
            LivingEntity p = null;
            for (Entity e: l.getWorld().getNearbyEntities(l, 0.5, 0.5, 0.5)) {
                if (e.getLocation().getBlockX() == l.getBlockX() && e.getLocation().getBlockY() == l.getBlockY() && e.getLocation().getBlockZ() == l.getBlockZ()) {
                    if (e instanceof LivingEntity) {
                        p = (LivingEntity) e;
                        break;
                    }
                }
            }
            if (p != null) {
                EntityEquipment eq = p.getEquipment();
                Log.info("%s is infront with %s", p, eq.getChestplate());

                InventoryHolder holder = (InventoryHolder) dispBlock.getState();
                Inventory i = holder.getInventory();
                for (ItemStack is: i.getContents()) {
                    if (is == null) {
                        continue;
                    }
                    switch (is.getType()) {
                        case CHAINMAIL_CHESTPLATE:
                        case DIAMOND_CHESTPLATE:
                        case GOLD_CHESTPLATE:
                        case IRON_CHESTPLATE:
                        case LEATHER_CHESTPLATE:
                            if (isEmpty(eq.getChestplate())) {
                                eq.setChestplate(is);
                                is.setAmount(is.getAmount()-1);
                            }
                            break;
                        case LEATHER_LEGGINGS:
                        case CHAINMAIL_LEGGINGS:
                        case DIAMOND_LEGGINGS:
                        case GOLD_LEGGINGS:
                        case IRON_LEGGINGS:
                            if (isEmpty(eq.getLeggings())) {
                                eq.setLeggings(is);
                                is.setAmount(is.getAmount()-1);
                            }
                            break;
                        case LEATHER_BOOTS:
                        case CHAINMAIL_BOOTS:
                        case DIAMOND_BOOTS:
                        case GOLD_BOOTS:
                        case IRON_BOOTS:
                            if (isEmpty(eq.getBoots())) {
                                eq.setBoots(is);
                                is.setAmount(is.getAmount()-1);
                            }
                            break;
                        default:
                            if (isEmpty(eq.getHelmet())) {
                                int a = is.getAmount();
                                is.setAmount(1);
                                eq.setHelmet(is);
                                is.setAmount(a-1);
                            }

                    }

                }

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
                    Rearmor l = getRearmor(a);
                    if (l != null) {
                        l.update();
                    }
                }
            }

        }
    }
}