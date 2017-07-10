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
import java.util.Collection;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.SkullType;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

/**
 * {@code ExampleListener}...
 */
public class Lazers implements Listener {

    public static final List<String> LAZER_LORE = Arrays.asList( "SHOOP DA WOOP!");

    private JavaPlugin plugin;

    private BukkitTask task;

    public void enable(JavaPlugin plugin) {
        this.plugin = plugin;
        NamespacedKey NS_SIMPLY_UTILITIES_LAZERS = new NamespacedKey(plugin, "lazers");
        ItemStack lazer = new ItemStack(Material.DISPENSER);

        ItemMeta im = lazer.getItemMeta();
        im.setDisplayName(ChatColor.DARK_RED + "Lazer");
        im.setLore(LAZER_LORE);
        lazer.setItemMeta(im);

        ShapedRecipe lazerRecp =     new ShapedRecipe(NS_SIMPLY_UTILITIES_LAZERS, lazer);
        lazerRecp.shape("IDI","RER","IGI");
        lazerRecp.setIngredient('I', Material.IRON_INGOT);
        lazerRecp.setIngredient('D', Material.DIAMOND);
        lazerRecp.setIngredient('R', Material.REDSTONE);
        lazerRecp.setIngredient('E', Material.ENDER_PEARL);
        lazerRecp.setIngredient('G', Material.GLASS);
        plugin.getServer().addRecipe(lazerRecp);

        task = plugin.getServer().getScheduler().runTaskTimer(plugin, new LazerScanner(), 1L, 7L);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void disable() {
        task.cancel();

    }

    public String getKey(Location loc) {
        return "lazer-" + loc.getX() + ":" + loc.getY() + ":" + loc.getZ();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.setJoinMessage("Welcome, " + event.getPlayer().getName() + "!");
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event){
        Player player = event.getPlayer();
        if(event.getBlock().getType() == Material.DISPENSER) {
            Collection<ItemStack> stack = event.getBlock().getDrops();
            ItemMeta im = stack.iterator().next().getItemMeta();
            player.sendMessage("stack is " + im.getDisplayName());
            im = player.getItemInHand().getItemMeta();
            if (LAZER_LORE.equals(im.getLore())) {

                float angle = 0;
                switch (event.getBlock().getData()) {
                    case 2:
                        angle = 90;//(float) Math.toRadians(90);
                        break;
                    case 3:
                        angle = 180;//(float) Math.toRadians(180);
                        break;
                    case 4:
                        angle = 270;//(float) Math.toRadians(270);
                        break;
                }

                player.sendMessage(ChatColor.AQUA + "You have placed down a lazer! " + angle);

                Location loc = event.getBlock().getLocation();
                String key = getKey(loc);
                loc.add(0.5, 0, 0.5);
                loc.setPitch(0);
                loc.setYaw(angle);
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

                // a.setBodyPose(new EulerAngle(0, angle, 0));
                for(int i = 0; i <360; i+=5){
                    Location flameloc = loc;
                    flameloc.setZ(flameloc.getZ() + Math.cos(i)*5 - 2.5);
                    flameloc.setX(flameloc.getX() + Math.sin(i)*5 - 2.5);
                    float red = 0f;
                    float green = 0.570312f;
                    float blue = 0f;
                    loc.getWorld().spigot().playEffect(flameloc, Effect.COLOURED_DUST, 0, 1, red, green, blue, 1, 0, 64);
                }
            }
        }
    }

    public ArmorStand getStand(World world, String key) {
        for (ArmorStand a: world.getEntitiesByClass(ArmorStand.class)) {
            if (a.getCustomName().equals(key)) {
                return a;
            }
        }
        return null;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if(event.getBlock().getType() == Material.DISPENSER) {
            String key = getKey(event.getBlock().getLocation());
            ArmorStand a = getStand(event.getBlock().getWorld(), key);
            if (a != null) {
                player.sendMessage("you broke " + key);
                a.remove();
            }
        }
    }

    private void shoot(Location loc, float phase) {
        float red = 0f;
        float green = 0.570312f;
        float blue = 0f;
        Vector dir = new Vector(1,0 ,0);
        for (float d = 0; d<20; d+=0.25) {
            Location l = loc.clone();
            l.add(phase, 0.5f, (Math.random()-0.5f)*0.3f);
            Vector v = dir.clone();
            v.multiply(d);
            l.add(v);
            loc.getWorld().spigot().playEffect(l, Effect.COLOURED_DUST, 0, 1, red, green, blue, 1, 0, 64);
        }
    }

    private class LazerScanner implements Runnable {

        float phase = 0;

        @Override
        public void run() {
            for (World w : plugin.getServer().getWorlds()) {
                for (ArmorStand a : w.getEntitiesByClass(ArmorStand.class)) {
                    if (!a.getCustomName().startsWith("lazer-")) {
                        continue;
                    }
                    shoot(a.getLocation(), phase);
                }
            }
            phase += 0.05;
            if (phase >= 0.25) {
                phase = 0;
            }
        }
    }
}