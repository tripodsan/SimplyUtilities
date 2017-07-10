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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.SkullType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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
import org.bukkit.material.Banner;
import org.bukkit.material.Dispenser;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

/**
 * {@code ExampleListener}...
 */
public class Lazers implements Listener {

    private static final List<String> LAZER_LORE = Collections.singletonList("SHOOP DA WOOP!");
    public static final String KEY_DAMAGE = "lazers:damage";

    private NamespacedKey NS_SIMPLY_UTILITIES_LAZERS;

    private JavaPlugin plugin;

    private BukkitTask task;

    void enable(JavaPlugin plugin) {
        this.plugin = plugin;
        NS_SIMPLY_UTILITIES_LAZERS = new NamespacedKey(plugin, "lazers");

        ItemStack lazer = new ItemStack(Material.DISPENSER);
        ItemMeta im = lazer.getItemMeta();
        im.setDisplayName(ChatColor.DARK_RED + "Lazer");
        im.setLore(LAZER_LORE);
        lazer.setItemMeta(im);
        ShapedRecipe lazerRecp = new ShapedRecipe(NS_SIMPLY_UTILITIES_LAZERS, lazer);
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

    void disable() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        plugin.getServer().resetRecipes();
    }

    private String getKey(Location loc) {
        return "lazer-" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
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
                Dispenser dispenser = (Dispenser) event.getBlock().getState().getData();
                BlockFace face = dispenser.getFacing();
                Vector dir = new Vector(face.getModX(), face.getModY(), face.getModZ());
                player.sendMessage(ChatColor.AQUA + "You have placed down a lazer! " + dir);

                Location loc = event.getBlock().getLocation();
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

                loc.add(0, 0.5, 0);
                float red = 0f;
                float green = 0.570312f;
                float blue = 0f;
                for(int i = 0; i <360; i+=5){
                    Location l = loc.clone();
                    l.add(Math.cos(i)*5 - 2.5, 0, Math.sin(i)*5 - 2.5);
                    loc.getWorld().spigot().playEffect(l, Effect.COLOURED_DUST, 0, 1, red, green, blue, 1, 0, 64);
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if(event.getBlock().getType() == Material.DISPENSER) {
            String key = getKey(event.getBlock().getLocation());
            Lazer a = getLazer(event.getBlock().getWorld(), key);
            if (a != null) {
                player.sendMessage("you broke " + key);
                a.stand.remove();
            }
        }
    }

    private Lazer getLazer(World world, String key) {
        for (ArmorStand a: world.getEntitiesByClass(ArmorStand.class)) {
            if (a.getCustomName().equals(key)) {
                return new Lazer(a);
            }
        }
        return null;
    }

    private boolean hit(Location l, Vector v) {
        Block b = l.getBlock();
        if (b.getType() == Material.BANNER) {
            // get normal of banner face
            Banner banner = (Banner) b.getState().getData();
            BlockFace face = banner.getFacing();
            Vector n = new Vector(face.getModX(), face.getModY(), face.getModZ());
            n.normalize();

            // reflect direction V' = -2*(V dot N)*N + V
            Vector vo = v.clone();
            v.dot(n);
            v.multiply(n);
            v.multiply(-2);
            v.add(vo);
            v.normalize();

            return false;
        }
        else if (b.getType() == Material.SAND) {
            List<MetadataValue> meta = b.getState().getMetadata(KEY_DAMAGE);
            int damage = 0;
            if (meta != null && !meta.isEmpty()) {
                damage = meta.get(0).asInt();
            }
            damage++;
            plugin.getLogger().info("block at " + b.getLocation() + " damaged by lazer: " + damage);
            if (damage > 20) {
                b.breakNaturally();
            } else {
                b.getState().setMetadata(KEY_DAMAGE, new FixedMetadataValue(plugin, damage));
            }
            return true;
        }

        return b.getType() != Material.AIR;
    }


    private class Photon {

        private float[] color;

        private Location l;

        private Vector v;

        private long age;

        private String prevBlock = "";

        public Photon(Location l, Vector v, float[] color) {
            this.l = l;
            this.v = v;
            this.color = color;
        }

        boolean update() {
            l.add(v);
            Block b = l.getBlock();
            String key = getKey(l);
            if (!key.equals(prevBlock) && hit(l, v)) {
                return false;
            }
            prevBlock = key;
            paint();

            return age++ <= 100;
        }

        private void paint() {
            l.getWorld().spigot().playEffect(l, Effect.COLOURED_DUST, 0, 1, color[0], color[1], color[2], 1, 0, 64);
        }

    }


    private class Lazer {

        private final float[] COLOR_GOLD = {0f, 0.570312f, 0f};

        private final ArmorStand stand;

        private final World.Spigot spigot;

        private float phase = 0;

        private int MAX_PHOTONS = 10000;

        private List<Photon> photons = new LinkedList<>();

        private Lazer(ArmorStand stand) {
            this.stand = stand;
            spigot = stand.getWorld().spigot();
        }

        private Lazer setPhase(float phase) {
            this.phase = phase;
            return this;
        }

        private void update() {
            Location l = stand.getLocation();
            l.add(0, 0.5, 0);
            Vector v = l.getDirection();
            v.multiply(0.25);
            photons.add(new Photon(l, v, COLOR_GOLD));

            Iterator<Photon> it = photons.iterator();
            while (it.hasNext()) {
                Photon p = it.next();
                if (!p.update()) {
                    it.remove();
                }
            }
        }

        private void trace() {
            // init with location and direction from armour stand
            Location l = stand.getLocation();
            Vector v = l.getDirection();
            double x0 = l.getX();
            double y0 = l.getY() + 0.5;
            double z0 = l.getZ();

            // trace the ray using a linear function
            double distance = 20;
            String prevBlock = "";
            for (double i=0; i<distance; i+= 0.25) {
                Vector v1 = v.clone();
                v1.multiply(i + phase);
                l.setX(x0 + v.getX());
                l.setY(y0 + v.getY());
                l.setZ(z0 + v.getZ());
                Block b = l.getBlock();
                String key = getKey(b.getLocation());
                if (!key.equals(prevBlock)) {
                    prevBlock = key;
                    if (b.getType() == Material.BANNER) {
                        // get normal of banner face
                        Banner banner = (Banner) b.getState().getData();
                        BlockFace face = banner.getFacing();
                        Vector n = new Vector(face.getModX(), face.getModY(), face.getModZ());
                        n.normalize();

                        // reflect direction V' = -2*(V dot N)*N + V
                        Vector vp = v.clone();
                        vp.dot(n);
                        vp.multiply(n);
                        vp.multiply(-2);
                        vp.add(v);
                        vp.normalize();
                        v = vp;

                        // reset distance
                        distance = 20;
                    }
                    else if (b.getType() == Material.SAND) {
                        List<MetadataValue> meta = b.getState().getMetadata(KEY_DAMAGE);
                        int damage = 0;
                        if (meta != null && !meta.isEmpty()) {
                            damage = meta.get(0).asInt();
                        }
                        damage++;
                        plugin.getLogger().info("block at " + b.getLocation() + " damaged by lazer: " + damage);
                        if (damage > 20) {
                            b.breakNaturally();
                        } else {
                            b.getState().setMetadata(KEY_DAMAGE, new FixedMetadataValue(plugin, damage));
                        }
                        break;
                    }
                    else if (b.getType() != Material.AIR) {
                        // else, stop tracing
                        break;
                    }
                }
                paint(l);
            }
        }

        private void paint(Location l) {
            spigot.playEffect(l, Effect.COLOURED_DUST, 0, 1, COLOR_GOLD[0], COLOR_GOLD[1], COLOR_GOLD[2], 1, 0, 64);
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
                    new Lazer(a).setPhase(phase).trace();
                }
            }
            phase += 0.05;
            if (phase >= 0.25) {
                phase = 0;
            }
        }
    }
}