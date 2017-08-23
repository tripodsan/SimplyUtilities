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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Directional;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

/**
 * {@code Main}...
 */
public class Alchemy implements Listener {

    private static class Corners {
        private Location l0;
        private Location l1;
    }

    private final static String STRUCTURE_PREFIX = "alchemy-";

    private Map<String, Corners> corners = new HashMap<>();

    private JavaPlugin plugin;

    private StructureVerifier verifier = new StructureVerifier("alchemy").load(
            "{\"dx\":4,\"dy\":4,\"dz\":4,\"matrix\":\"aaaaaaaaaaaaaaaaaaaaaaaaabcdcbce.ecd.e.dce.ecbcdcbb.f.b..g..fghgf..g..b.f.be...e...............e...ei...i...............i...i\",\"map\":{\"a\":{\"mat\":\"STONE\"},\".\":{\"mat\":\"AIR\"},\"f\":{\"mat\":\"IRON_TRAPDOOR\"},\"g\":{\"mat\":\"COBBLESTONE_STAIRS\"},\"i\":{\"mat\":\"GLOWSTONE\"},\"h\":{\"mat\":\"CAULDRON\"},\"e\":{\"mat\":\"IRON_FENCE\"},\"b\":{\"mat\":\"COBBLESTONE\"},\"d\":{\"mat\":\"IRON_BLOCK\"},\"c\":{\"mat\":\"STEP\"}}}");

    private HashMap<String, Altar> altars = new HashMap<>();

    private class AlchemyRecipe  {

        private class Drop {

            private Material output;

            private double min;

            private double max;


            public Drop(Material mat, double min, double max) {
                output = mat;
                this.min = min;
                this.max = max;
            }
        }

        private List<Drop> output = new LinkedList<>();

        private Material mat;

        private int data;

        public Material getOutput() {
            double r = Math.random();
            for (Drop d: output) {
                if (r >= d.min && r < d.max) {
                    return d.output;
                }
            }
            return output.get(0).output;
        }

        private AlchemyRecipe addOutput(Material mat, double min, double max) {
            output.add(new Drop(mat, min, max));
            return this;
        }

        private AlchemyRecipe addSource(Material mat, int data) {
            this.mat = mat;
            this.data = data;
            return this;
        }

        boolean matches(Block block) {
            return block.getType() == mat && (data < 0 || data == block.getData());
        }
    }

    private List<AlchemyRecipe> recipes = new LinkedList<>();
    private void initRecipes() {
        {
            AlchemyRecipe r = new AlchemyRecipe()
                    .addSource(Material.COAL_ORE, -1)
                    .addOutput(Material.STONE, 0, 0.4)
                    .addOutput(Material.COAL_BLOCK, 0.4, 0.7)
                    .addOutput(Material.LAPIS_BLOCK, 0.7, 1);
            recipes.add(r);
        }
    }

    private BukkitTask task;

    void enable(JavaPlugin plugin) {
        this.plugin = plugin;
        initRecipes();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, new AltarScanner(), 1L, 2L);
    }

    void disable() {
        if (task != null) {
            task.cancel();
            task  = null;
        }
    }

    private class AltarScanner implements Runnable {

        @Override
        public void run() {
            for (World w : plugin.getServer().getWorlds()) {
                for (ArmorStand a : w.getEntitiesByClass(ArmorStand.class)) {
                    Altar l = getAltar(a);
                    if (l != null) {
                        l.update();
                    }
                }
            }

        }
    }

    private Corners getCorners(Player p) {
        return corners.computeIfAbsent(p.getDisplayName(), s -> new Corners());
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event){

        if(event.getBlock().getType() == Material.CAULDRON) {
            if (verifier.verify(event.getPlayer(), event.getBlock().getLocation(), 2)) {
                createAltar(event.getPlayer(), event.getBlock().getLocation(), event.getBlock().getState().getData());
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if(event.getBlock().getType() == Material.CAULDRON) {
            // todo: recheck structure
            String key = getKey(event.getBlock().getLocation());
            if (removeAltar(event.getBlock().getWorld(), key)) {
                player.sendMessage("you broke " + key);
            }
        }
    }

    private String getKey(Location loc) {
        return STRUCTURE_PREFIX + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    private Altar createAltar(Player player, Location loc, MaterialData data) {
        String key = getKey(loc);
        loc.add(0.5, 0, 0.5);
        if (data instanceof Directional) {
            BlockFace face = ((Directional) data).getFacing();
            Vector dir = new Vector(face.getModX(), face.getModY(), face.getModZ());
            loc.setDirection(dir);
        }

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

        return getAltar(a);
    }

    private boolean removeAltar(World world, String key) {
        Altar a = getAltar(world, key);
        if (a != null) {
            a.stand.remove();
            altars.remove(key);
            return true;
        }
        return false;
    }

    private Altar getAltar(World world, String key) {
        for (ArmorStand a: world.getEntitiesByClass(ArmorStand.class)) {
            if (a.getCustomName().equals(key)) {
                return altars.computeIfAbsent(key, k -> new Altar(a));
            }
        }
        return null;
    }

    private Altar getAltar(Block block) {
        if (block.getType() != Material.CAULDRON) {
            return null;
        }
        String key = getKey(block.getLocation());
        return getAltar(block.getLocation().getWorld(), key);
    }

    private Altar getAltar(ArmorStand a) {
        String key = a.getCustomName();
        if (key.startsWith(STRUCTURE_PREFIX)) {
            return altars.computeIfAbsent(key, k -> new Altar(a));
        } else {
            return null;
        }
    }

    private class Altar {

        private final ArmorStand stand;

        private final String key;

        private final Location center;

        private int time;

        private Block currentBlock;

        private AlchemyRecipe recipe;

        public Altar(ArmorStand stand) {
            this.stand = stand;
            this.key = getKey(stand.getLocation());

            // get item frames
            center = stand.getLocation().add(0, 1, 0);
        }

        public void update() {
            Block cb = center.getBlock();
            if (currentBlock != null && cb.equals(currentBlock)) {
                if (++time > 50) {
                    Material mat = recipe.getOutput();
                    plugin.getLogger().info("stop alchemy. creating " + mat);
                    cb.setType(mat);
                    currentBlock = null;
                    stand.getWorld().playSound(stand.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1, 1);
                }
            } else {
                currentBlock = null;
                for (AlchemyRecipe r: recipes) {
                    if (r.matches(cb)) {
                        recipe = r;
                        currentBlock = cb;
                        time = 0;
                        plugin.getLogger().info("start alchemy with " + currentBlock);
                    }
                }
            }
        }

    }


}