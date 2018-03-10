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
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import javax.xml.stream.events.EntityReference;
import java.util.*;

/**
 * {@code ExampleListener}...
 */
public class ProjectileShield implements Listener, PluginUtility {

    private static final List<String> SHIELD_LORE = Collections.singletonList("Causes projectiles to drop.");

    private JavaPlugin plugin;

    private BukkitTask task;

    private HashMap<String, Shield> shields = new HashMap<>();

    public static boolean isProjectile(Entity e) {
        return (e instanceof Projectile) || e instanceof TNTPrimed;
    }

    public void enable(JavaPlugin plugin) {
        this.plugin = plugin;
        createRecipes();

        task = plugin.getServer().getScheduler().runTaskTimer(plugin, new Scanner(), 1L, 2L);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void createRecipes() {
        // create shield recipe
        {
            ItemStack placer = new ItemStack(Material.IRON_BLOCK);
            ItemMeta im = placer.getItemMeta();
            im.setDisplayName(ChatColor.AQUA + "Projectile Shield");
            im.setLore(SHIELD_LORE);
            placer.setItemMeta(im);
            ShapedRecipe recp = new ShapedRecipe(new NamespacedKey(plugin, "shield"), placer);
            recp.shape("IAI", "SCS", "IAI");
            recp.setIngredient('I', Material.IRON_INGOT);
            recp.setIngredient('S', Material.SHIELD);
            recp.setIngredient('A', Material.ARROW);
            recp.setIngredient('C', Material.CHORUS_FRUIT);
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
        return "shield-" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event){
        Player player = event.getPlayer();
        if(event.getBlock().getType() == Material.IRON_BLOCK) {
            ItemMeta im = player.getItemInHand().getItemMeta();
            if (SHIELD_LORE.equals(im.getLore())) {
                Shield l = createShield(player, event.getBlock().getLocation());
                player.sendMessage(ChatColor.AQUA + "You have placed down a shield! " + l.key);
            }
        }
    }



    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if(event.getBlock().getType() == Material.IRON_BLOCK) {
            String key = getKey(event.getBlock().getLocation());
            if (removeShield(event.getBlock().getWorld(), key)) {
                player.sendMessage("you broke " + key);
            }
        }
    }


    private Shield createShield(Player player, Location loc) {
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

        return getShield(a);
    }

    private boolean removeShield(World world, String key) {
        Shield a = getShield(world, key);
        if (a != null) {
            a.stand.remove();
            shields.remove(key);
            return true;
        }
        return false;
    }

    private Shield getShield(World world, String key) {
        for (ArmorStand a: world.getEntitiesByClass(ArmorStand.class)) {
            if (key.equals(a.getCustomName())) {
                return shields.computeIfAbsent(key, k -> new Shield(a));
            }
        }
        return null;
    }

    private Shield getShield(Block block) {
        if (block.getType() != Material.IRON_BLOCK) {
            return null;
        }
        String key = getKey(block.getLocation());
        return getShield(block.getLocation().getWorld(), key);
    }

    private Shield getShield(ArmorStand a) {
        String key = a.getCustomName();
        if (key != null && key.startsWith("shield-")) {
            return shields.computeIfAbsent(key, k -> new Shield(a));
        } else {
            return null;
        }
    }

    class Hit {
        private Location pos;

        private double r = 1;

        public Hit(Location pos) {
            this.pos = pos;
        }

        public boolean isInside(Location l) {
            double d = pos.distanceSquared(l);
            double r2 = r*r;
            return d < r2 && d > (r2-7);
        }
    }

    private List<Hit> hits = new LinkedList<>();

    private class Shield {

        private final ArmorStand stand;

        private final Location center;

        private final String key;

        private int radius = 5;

        private int radius2 = 25;

        private Shield(ArmorStand stand) {
            this.stand = stand;
            key = getKey(stand.getLocation());
            Location l = stand.getLocation();
            this.center = new Location(l.getWorld(), l.getBlockX(), l.getBlockY(), l.getBlockZ()).add(0.5, 0.5, 0.5);
        }

        private Location polarToLocation(double ph, double th) {
            double sp = Math.sin(ph);
            double cp = Math.cos(ph);
            double st = Math.sin(th);
            double ct = Math.cos(th);
            Location l = center.clone();
            l.add(radius * sp * ct,
                    radius * sp * st,
                    radius * cp);
            return l;
        }

        private void update(Set<Entity> projectiles) {
            // play effect
//            int num = 100;
//            int max = 1000;
//            while (num > 0 && max > 0) {
//                double ph = Math.random() * Math.PI * 2.0;
//                double th = Math.random() * Math.PI;
//                Location l = polarToLocation(ph, th);
//                boolean draw = true;
//                for (Shield s: shields.values()) {
//                    if (s != this && s.isInside(l)) {
//                        draw = false;
//                        break;
//                    }
//                }
//
//                if (draw) {
//                    boolean isHit = false;
//                    for (Hit h: hits) {
//                        if (h.isInside(l)) {
//                            isHit = true;
//                            break;
//                        }
//                    }
//                    if (isHit) {
//                        center.getWorld().spigot().playEffect(l, Effect.COLOURED_DUST, 1, 0, 0.1f, 0.1f, 0.1f, 0, 10, 64);
//                    } else {
//                        center.getWorld().spawnParticle(Particle.END_ROD, l, 1, 0, 0, 0, 0);
//                    }
//                    num--;
//                }
//                max--;
//            }

            for (double ph = 0; ph < Math.PI * 2; ph += 0.1) {
                for (double th = 0; th < Math.PI; th += 0.2) {
                    Location l = polarToLocation(ph, th);
                    boolean draw = true;
                    for (Shield s: shields.values()) {
                        if (s != this && s.isInside(l)) {
                            draw = false;
                            break;
                        }
                    }
                    if (!draw) {
                        continue;
                    }
                    boolean isHit = false;
                    for (Hit h: hits) {
                        if (h.isInside(l)) {
                            isHit = true;
                            break;
                        }
                    }
                    if (isHit) {
                        center.getWorld().spigot().playEffect(l, Effect.COLOURED_DUST, 1, 0, 0.1f, 0.1f, 0.1f, 0, 10, 64);
                    } else {
                        center.getWorld().spawnParticle(Particle.END_ROD, l, 1, 0, 0, 0, 0);
                    }

                }
            }

            Iterator<Entity> iter = projectiles.iterator();
            while (iter.hasNext()) {
                Entity e = iter.next();
                if (isInside(e.getLocation())) {
                    Vector n = center.toVector().subtract(e.getLocation().toVector()).normalize();
                    Vector hit = n.clone().multiply(-radius).add(center.toVector());
                    Vector v = e.getVelocity();
                    double d = -v.dot(n) * 2;
                    Vector r = n.multiply(d).add(e.getVelocity());
                    e.setVelocity(r);
                    hits.add(new Hit(new Location(center.getWorld(), hit.getX(), hit.getY(), hit.getZ())));
//                    hits.add(new Hit(e.getLocation().clone()));
                    iter.remove();
                }
            }
        }

        public boolean isInside(Location l) {
            return l.distanceSquared(center) < radius2;
        }

        public Block getBlock() {
            return stand.getLocation().getBlock();
        }

    }

    private class Scanner implements Runnable {

        @Override
        public void run() {
            for (World w : plugin.getServer().getWorlds()) {
                Set<Entity> projectiles = new HashSet<>();
                for (Entity e: w.getEntities()) {
                    if (isProjectile(e)) {
                        projectiles.add(e);
                    }
                }

                for (ArmorStand a : w.getEntitiesByClass(ArmorStand.class)) {
                    Shield l = getShield(a);
                    if (l != null) {
                        l.update(projectiles);
                    }
                }
            }

            Iterator<Hit> iter = hits.iterator();
            while (iter.hasNext()) {
                Hit h = iter.next();
                h.r += 0.3;
                if (h.r > 5) {
                    iter.remove();
                }
            }

        }
    }
}