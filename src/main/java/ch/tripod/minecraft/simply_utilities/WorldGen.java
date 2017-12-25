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

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * {@code Main}...
 */
public class WorldGen implements Listener, PluginUtility {

    private static String TUSSOCK = "{\"dx\":2,\"dy\":3,\"dz\":2,\"matrix\":\".a.aba.a..c.cac.c..d.dcd.d.....d....\",\"map\":{\"d\":{\"mat\":\"DOUBLE_PLANT\",\"dat\":10},\"a\":{\"mat\":\"DIRT\",\"dat\":2},\"c\":{\"mat\":\"DOUBLE_PLANT\",\"dat\":2},\"b\":{\"mat\":\"CHEST\",\"dat\":3},\".\":{\"mat\":\"AIR\",\"dat\":0}}}";
    private BukkitTask task;

    private JavaPlugin plugin;

    private StructureVerifier tussock = new StructureVerifier("tussock").load(TUSSOCK);

    public void enable(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getLogger().info("WorldGen enabled");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void disable() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Location loc = event.getTo().clone();
        loc.setX(loc.getBlockX()-loc.getBlockX()%16+8);
        loc.setZ(loc.getBlockZ()-loc.getBlockZ()%16+8);
        loc.setY(100);

        boolean found = false;
        for (Entity e: event.getPlayer().getWorld().getNearbyEntities(loc, 1, 1, 1)) {
            if (e instanceof ArmorStand) {
                ArmorStand a = (ArmorStand) e;
                if (a.getCustomName()!=null && a.getCustomName().equals("chunk-marker")) {
                    found = true;
                    break;
                }
            }
        }

        if (!found) {
            ArmorStand a = event.getPlayer().getWorld().spawn(loc, ArmorStand.class);
            a.teleport(loc);
            a.setCustomName("chunk-marker");
            a.setCustomNameVisible(true);
            a.setVisible(true);
            a.setGravity(false);
            a.setMarker(true);
            plugin.getLogger().info("Created Chunk Marker For " + loc);

            tussock.build(loc);
        }

    }

    public void resetChunks(Player p) {
        for (ArmorStand a: p.getWorld().getEntitiesByClass(ArmorStand.class)) {
            if (a.getCustomName()!=null && a.getCustomName().equals("chunk-marker")) {
                a.remove();
            }
        }
    }
}