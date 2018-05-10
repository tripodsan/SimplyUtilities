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

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;

/**
 * {@code Main}...
 */
public class Stargate implements Listener, PluginUtility {

    private final static String STRUCTURE_PREFIX = "stargate-";

    private JavaPlugin plugin;

    private StructureVerifier verifier = new StructureVerifier("stargate").load(
            "{\"dx\":2,\"dy\":12,\"dz\":12,\"matrix\":\".....aba..........cdc..........aba........ccefecc......gc...cg......ccefecc.....bhi...ihb....g.......g....bhi...ihb...ch.......hc..g.........g..ch.......hc..cj.......jc..c.........c..cj.......jc.ae.........eac...........cae.........eabf.........fbg...........gbf.........fbae.........eac...........cae.........ea.cj.......jc..c.........c..cj.......jc..ch.......hc..g.........g..ch.......hc...bhi...ihb....g.......g....bhi...ihb.....ccklkcc......gc...cg......ccklkcc........aba..........cgc..........aba.....\",\"map\":{\"k\":{\"mat\":\"STONE\",\"dat\":2},\"b\":{\"mat\":\"GOLD_BLOCK\",\"dat\":0},\"e\":{\"mat\":\"STONE\",\"dat\":4},\"f\":{\"mat\":\"STONE\",\"dat\":3},\"d\":{\"mat\":\"ENDER_PORTAL_FRAME\",\"dat\":2},\"h\":{\"mat\":\"QUARTZ_BLOCK\",\"dat\":1},\"j\":{\"mat\":\"QUARTZ_BLOCK\",\"dat\":2},\".\":{\"mat\":\"AIR\",\"dat\":0},\"c\":{\"mat\":\"SMOOTH_BRICK\",\"dat\":0},\"i\":{\"mat\":\"QUARTZ_BLOCK\",\"dat\":4},\"a\":{\"mat\":\"PRISMARINE\",\"dat\":2},\"l\":{\"mat\":\"REDSTONE_LAMP_OFF\",\"dat\":0},\"g\":{\"mat\":\"PRISMARINE\",\"dat\":0}}}"
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
            task = null;
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
    public void onBlockPlace(BlockPlaceEvent event) {

        if (event.getBlock().getType() == Material.ENDER_PORTAL_FRAME) {
            if (verifier.verify(event.getPlayer(), event.getBlock().getLocation(), 0)) {
                Structure s = createStructure(event.getPlayer(), event.getBlock().getLocation(), event.getBlock().getState().getData());
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        for (World w : plugin.getServer().getWorlds()) {
            for (ArmorStand a : w.getEntitiesByClass(ArmorStand.class)) {
                Structure l = getStructure(a);
                Location loc = event.getBlock().getLocation();
                if (l != null && l.isInside(loc)) {
                    if (!l.isValid(loc)) {
                        removeAltar(l);
                        player.sendMessage("you broke " + l.key);
                    }
                }
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
        for (int i = 0; i < 360; i += 5) {
            Location l = loc.clone();
            l.add(Math.cos(i) * 3, 0, Math.sin(i) * 3);
            loc.getWorld().spigot().playEffect(l, Effect.COLOURED_DUST, 0, 1, red, green, blue, 1, 0, 64);
        }

        return getStructure(a);
    }

    private boolean removeAltar(Structure a) {
        if (a != null) {
            a.stand.remove();
            a.destroy();
            structures.remove(a.key);
            return true;
        }
        return false;
    }


    private boolean removeAltar(World world, String key) {
        Structure a = getStructure(world, key);
        return removeAltar(a);
    }

    private Structure getStructure(World world, String key) {
        for (ArmorStand a : world.getEntitiesByClass(ArmorStand.class)) {
            if (a.getCustomName().equals(key)) {
                return structures.computeIfAbsent(key, k -> new Structure(a));
            }
        }
        return null;
    }

    private Structure getStructure(ArmorStand a) {
        String key = a.getCustomName();
        if (key != null && key.startsWith(STRUCTURE_PREFIX)) {
            return structures.computeIfAbsent(key, k -> new Structure(a));
        } else {
            return null;
        }
    }

    private class Structure {

        private final ArmorStand stand;

        private final String key;

        private Vector boxMin;
        private Vector boxMax;

        private Location center;

        private int timer = 0;

        private int rotation = 0;

        private int direction = 1;

        /**
         * 5     101
         * 4   32   23
         * 3  4       4
         * 2  4       4
         * 1 5         5
         * 0 5    o    5
         * 1 5         5
         * 2  4       4
         * 3  4       4
         * 4   32   23
         * 5     101
         */
        private int ring[] = {
                0, 5,
                1, 5,
                2, 4,
                3, 4,
                4, 3,
                4, 2,
                5, 1,
                5, 0,
                5, -1,
                4, -2,
                4, -3,
                3, -4,
                2, -4,
                1, -5,
                0, -5,
                -1, -5,
                -2, -4,
                -3, -4,
                -4, -3,
                -4, -2,
                -5, -1,
                -5, 0,
                -5, 1,
                -4, 2,
                -4, 3,
                -3, 4,
                -2, 4,
                -1, 5
        };

        public Structure(ArmorStand stand) {
            this.stand = stand;
            this.key = getKey(stand.getLocation());

            center = stand.getLocation().add(0, 6, 0);
            Vector d1 = new Vector(1, 6, 6);
            Vector d2 = new Vector(1, 6, 6);
            boxMin = center.toVector().subtract(d1);
            boxMax = center.toVector().add(d2);
        }

        public boolean isInside(Location loc) {
            return loc.toVector().isInAABB(boxMin, boxMax);
        }

        public boolean isValid(Location forcedAirLocation) {
            return verifier.verify(null, stand.getLocation(), 0, forcedAirLocation);
        }

        public void update() {
            if (++timer < 2) {
                return;
            }
            timer = 0;
            if (direction > 0) {
                Block b = getRingBlock(0);
                Material m0 = b.getType();
                byte d0 = b.getData();
                for (int i=0; i < 27; i++) {
                    b = getRingBlock(i + 1);
                    setRingBlock(i, b.getType(), b.getData());
                }
                setRingBlock(27, m0, d0);
            } else {
                Block b = getRingBlock(27);
                Material m0 = b.getType();
                byte d0 = b.getData();
                for (int i=27; i > 0; i--) {
                    b = getRingBlock(i - 1);
                    setRingBlock(i, b.getType(), b.getData());
                }
                setRingBlock(0, m0, d0);
            }
        }

        Block getRingBlock(int n) {
            Location c = center.clone();
            return c.add(1, ring[n*2 + 1], ring[n*2]).getBlock();
        }

        void setRingBlock(int n, Material m, byte d) {
            Block b = getRingBlock(n);
            b.setType(m);
            b.setData(d);
            b = b.getLocation().add(-2, 0, 0).getBlock();
            b.setType(m);
            b.setData(d);
        }

        public void destroy() {
        }

    }

}