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

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

/**
 * {@code Main}...
 */
public class Pads implements PluginUtility {

    private BukkitTask task;

    private JavaPlugin plugin;

    public void enable(JavaPlugin plugin) {
        this.plugin = plugin;
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, new ScanForPads(), 1L, 1L);
    }

    public void disable() {
        if (task != null) {
            task.cancel();
            task  = null;
        }
    }

    public class ScanForPads implements Runnable {

        private final Map<String, Integer> CONVEYOR_SPEED = new HashMap<>();

        private ScanForPads() {
            CONVEYOR_SPEED.put("CONCRETE:14", 1);
            CONVEYOR_SPEED.put("CONCRETE:1", 2);
            CONVEYOR_SPEED.put("CONCRETE:4", 3);
            CONVEYOR_SPEED.put("CONCRETE:5", 4);
            CONVEYOR_SPEED.put("CONCRETE:11", 5);
        }

        @Override
        public void run() {
            for (World w: plugin.getServer().getWorlds()) {
                for (Entity e: w.getEntities()) {
                    Location l = e.getLocation();
                    l = l.add(0, -1, 0);
                    Block b0 = l.getBlock();
                    l = l.add(0, -1, 0);
                    Block b1 = l.getBlock();
                    l = l.add(0, -1, 0);
                    Block b2 = l.getBlock();
                    String key = String.join(",",
                            b0.getType().name() + ":" + b0.getData(),
                            b1.getType().name() + ":" + b1.getData(),
                            b2.getType().name() + ":" + b2.getData());
                    LivingEntity p = null;
                    if (e instanceof LivingEntity) {
                        p = (LivingEntity) e;
                        if ("CONCRETE:15,CONCRETE:4,NETHERRACK:0".equals(key) || "CONCRETE:4,CONCRETE:15,NETHERRACK:0".equals(key)) {
                            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20, 5));
                        } else if ("CONCRETE:15,CONCRETE:5,NETHERRACK:0".equals(key) || "CONCRETE:5,CONCRETE:15,NETHERRACK:0".equals(key)) {
                            Vector v = p.getVelocity();
                            v.setY(-10);
                            p.setVelocity(v);
                            // p.sendRawMessage("Gnoib... " + v);
                        } else if ("CONCRETE:15,CONCRETE:7,NETHERRACK:0".equals(key) || "CONCRETE:7,CONCRETE:15,NETHERRACK:0".equals(key)) {
                            p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 20, 1));
                        } else if ("CONCRETE:15,CONCRETE:7,NETHERRACK:0".equals(key) || "CONCRETE:7,CONCRETE:15,NETHERRACK:0".equals(key)) {
                            p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 20, 1));
                        }
                    }
                    if ("CONCRETE:15,CONCRETE:2,NETHERRACK:0".equals(key) || "CONCRETE:2,CONCRETE:15,NETHERRACK:0".equals(key)) {
                        if (p != null) {
                            p.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
                            p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 150, 100));
                        }
                        Vector v = e.getVelocity();
                        v.setY(10);
                        e.setVelocity(v);
                        // p.sendRawMessage("Boing... ");
                    }
                    else if (b2.getType() == Material.MAGENTA_GLAZED_TERRACOTTA && (b1.getType() == Material.WOOL || b0.getType() == Material.WOOL)) {
                        String k0 = b0.getType().name() + ":" + b0.getData();
                        String k1 = b1.getType().name() + ":" + b1.getData();
                        Integer speed = CONVEYOR_SPEED.get(k0);
                        if (speed == null) {
                            speed = CONVEYOR_SPEED.get(k1);
                        }
                        if (speed != null) {
                            int d = b2.getData();
                            Vector v = e.getVelocity();
                            // p.sendRawMessage("conveyor direction " + d + " speed " + speed);
                            double dp = 0.1 * (double) speed;
                            switch (d) {
                                case 0:
                                    v.setZ(-dp);
                                    break;
                                case 1:
                                    v.setX(dp);
                                    break;
                                case 2:
                                    v.setZ(dp);
                                    break;
                                case 3:
                                    v.setX(-dp);
                                    break;
                            }
                            e.setVelocity(v);
                        }
                    }

                }
            }
        }
    }
}