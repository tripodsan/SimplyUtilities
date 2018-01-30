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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang.ObjectUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Directional;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

/**
 * {@code Main}...
 */
public class Alchemy implements Listener, PluginUtility {

    private static class Corners {
        private Location l0;
        private Location l1;
    }

    private static Map<String, Material> matmap = new HashMap<>();
    static {
        matmap.put("c", Material.COAL_ORE);
        matmap.put("l", Material.LAPIS_ORE);
        matmap.put("i", Material.IRON_ORE);
        matmap.put("g", Material.GOLD_ORE);
        matmap.put("d", Material.DIAMOND_ORE);
        matmap.put("e", Material.EMERALD_ORE);
        matmap.put("r", Material.REDSTONE_ORE);
        matmap.put("n", Material.QUARTZ_ORE);
        matmap.put("w", Material.GLOWSTONE);

        matmap.put("C", Material.COAL_BLOCK);
        matmap.put("L", Material.LAPIS_BLOCK);
        matmap.put("I", Material.IRON_BLOCK);
        matmap.put("G", Material.GOLD_BLOCK);
        matmap.put("D", Material.DIAMOND_BLOCK);
        matmap.put("E", Material.EMERALD_BLOCK);
        matmap.put("R", Material.REDSTONE_BLOCK);
        matmap.put("Q", Material.QUARTZ_BLOCK);
        matmap.put("N", Material.NETHERRACK);
        matmap.put("S", Material.STONE);
        matmap.put("A", Material.SEA_LANTERN);
        matmap.put("V", Material.GRAVEL);
    }

    private final static String STRUCTURE_PREFIX = "alchemy-";

    private Map<String, Corners> corners = new HashMap<>();

    private JavaPlugin plugin;

    private StructureVerifier verifier = new StructureVerifier("alchemy").load(
            "{\"dx\":4,\"dy\":4,\"dz\":4,\"matrix\":\"aaaaaaaaaaaaaaaaaaaaaaaaabcdcbce.ecd.e.dce.ecbcdcbb.f.b..g..fghgf..g..b.f.be...e...............e...ei...i...............i...i\",\"map\":{\"a\":{\"mat\":\"STONE\"},\".\":{\"mat\":\"AIR\"},\"f\":{\"mat\":\"IRON_TRAPDOOR\"},\"g\":{\"mat\":\"COBBLESTONE_STAIRS\"},\"i\":{\"mat\":\"GLOWSTONE\"},\"h\":{\"mat\":\"CAULDRON\"},\"e\":{\"mat\":\"IRON_FENCE\"},\"b\":{\"mat\":\"COBBLESTONE\"},\"d\":{\"mat\":\"IRON_BLOCK\"},\"c\":{\"mat\":\"STEP\"}}}");

    private HashMap<String, Altar> altars = new HashMap<>();

    private Random rand = new Random();

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
            double r = rand.nextDouble();

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

        private AlchemyRecipe addOutputs(String mats, double ... weights) {
            double prev = 0;
            for (int i=0; i<mats.length(); i++) {
                addOutput(matmap.get(mats.substring(i, i+1)), prev, weights[i]);
                prev = weights[i];
            }
            return this;
        }

        private AlchemyRecipe addSource(Material mat, int data) {
            this.mat = mat;
            this.data = data;
            return this;
        }

        private AlchemyRecipe addSource(String matCode) {
            this.mat = matmap.get(matCode);
            this.data = -1;
            return this;
        }

        boolean matches(Block block) {
            return block.getType() == mat && (data < 0 || data == block.getData());
        }
    }

    private List<AlchemyRecipe> recipes = new LinkedList<>();
    private void initRecipes() {
        recipes.add(new AlchemyRecipe().addSource("c").addOutputs("SCL", 0.2, 0.6, 1));
        recipes.add(new AlchemyRecipe().addSource("l").addOutputs("SLI", 0.2, 0.6, 1));
        recipes.add(new AlchemyRecipe().addSource("i").addOutputs("SIG", 0.2, 0.6, 1));
        recipes.add(new AlchemyRecipe().addSource("g").addOutputs("SGD", 0.2, 0.6, 1));
        recipes.add(new AlchemyRecipe().addSource("d").addOutputs("SDE", 0.2, 0.6, 1));
        recipes.add(new AlchemyRecipe().addSource("e").addOutputs("SE", 0.4, 1));
        recipes.add(new AlchemyRecipe().addSource("r").addOutputs("SR", 0.4, 1));
        recipes.add(new AlchemyRecipe().addSource("n").addOutputs("NQ", 0.4, 1));
        recipes.add(new AlchemyRecipe().addSource("w").addOutputs("VA", 0.4, 1));
        recipes.add(new AlchemyRecipe().addSource(Material.SAND, 1)
                .addOutput(Material.SAND, 0, 0.4)
                .addOutput(Material.SOUL_SAND, 0.4, 1.0));
        recipes.add(new AlchemyRecipe().addSource(Material.CONCRETE_POWDER, 9)
                .addOutput(Material.SAND, 0, 0.4)
                .addOutput(Material.PRISMARINE, 0.4, 1.0));
    }

    private BukkitTask task;

    public void enable(JavaPlugin plugin) {
        this.plugin = plugin;
        initRecipes();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, new AltarScanner(), 1L, 2L);
    }

    public void disable() {
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

            // update the photons
            photons.removeIf(p -> !p.update());
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
        for (World w : plugin.getServer().getWorlds()) {
            for (ArmorStand a : w.getEntitiesByClass(ArmorStand.class)) {
                Altar l = getAltar(a);
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
        a.setVisible(false);
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

    private boolean removeAltar(Altar a) {
        if (a != null) {
            a.stand.remove();
            altars.remove(a.key);
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
        if (key!=null && key.startsWith(STRUCTURE_PREFIX)) {
            return altars.computeIfAbsent(key, k -> new Altar(a));
        } else {
            return null;
        }
    }

    private Map<InventoryView, Integer> openLuckDisks = new HashMap<>();

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR
                && event.getHand() == EquipmentSlot.HAND) {
            ItemStack s = event.getItem();
            List<String> lore = s.getItemMeta().getLore();
            if (lore != null && lore.size() > 0 && lore.get(0).equals(LuckDisk.LORE[0])) {
                Inventory inv = Bukkit.createInventory(event.getPlayer(), 27, "Luck Disk");
                InventoryView view = event.getPlayer().openInventory(inv);
                openLuckDisks.put(view , event.getPlayer().getInventory().getHeldItemSlot());
                String code = lore.size() > 1 ? lore.get(1) : "";
                plugin.getLogger().info("the new code is:" + code);
                ItemStack[] contents = view.getTopInventory().getContents();
                for (int i=0; i<code.length(); i++) {
                    Infusion.LuckCrystal crystal = Infusion.LuckCrystal.fromCode(code.charAt(i));
                    if (crystal != null) {
                        contents[i] = crystal.toItemStack();
                    }
                }
                view.getTopInventory().setContents(contents);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent evt) {
        if ("Luck Disk".equals(evt.getInventory().getName())) {
            int low = 50;
            for (Map.Entry<Integer, ItemStack> e: evt.getNewItems().entrySet()) {
                int idx = e.getKey();
                if (idx < low) {
                    low = idx;
                }
            }
            if (low < 27) {
                evt.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent evt) {
        InventoryView view = evt.getView();
        // plugin.getLogger().info("item " + evt.getCursor() + " action: " + evt.getAction() + " current: " + evt.getCurrentItem());
        if ("Luck Disk".equals(evt.getClickedInventory().getName())) {
            switch (evt.getAction()) {
                case PLACE_ALL:
                case SWAP_WITH_CURSOR:
                case PLACE_ONE:
                case PLACE_SOME:
                    break;
                default:
                    return;
            }
            ItemStack s = evt.getCursor();
            ItemStack c = evt.getCurrentItem();
            evt.setCancelled(true);
            if (!Infusion.LuckCrystal.isLuckCrystal(s)) {
                return;
            }
            if (c.getType() != Material.AIR) {
                return;
            }
            ItemStack newItem = new ItemStack(s);
            newItem.setAmount(1);
            evt.setCurrentItem(newItem);
            s.setAmount(s.getAmount() - 1);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        InventoryView view = event.getView();
        if ("Luck Disk".equals(view.getTitle())) {
            Integer itemSlot = openLuckDisks.remove(view);
            if (itemSlot == null) {
                plugin.getLogger().info("haaa?");
            } else {
                plugin.getLogger().info("closed inventory of " + itemSlot);
                ItemStack luckDisk = event.getPlayer().getInventory().getItemInMainHand();
                plugin.getLogger().info("item in main hand: " + luckDisk);

                ItemStack[] contents = event.getInventory().getContents();
                StringBuilder code = new StringBuilder();
                for (int i=0; i<27; i++) {
                    ItemStack slot = contents[i];
                    Infusion.LuckCrystal crystal = Infusion.LuckCrystal.fromItem(slot);
                    if (crystal == null) {
                        code.append(".");
                    } else {
                        code.append(crystal.toCode());
                    }
                }
                plugin.getLogger().info("code is: " + code);

                ItemMeta meta = luckDisk.getItemMeta();
                List<String> lore = meta.getLore();
                if (lore.size() == 1) {
                    lore.add(code.toString());
                } else {
                    lore.set(1, code.toString());
                }
                meta.setLore(lore);
                luckDisk.setItemMeta(meta);
                event.getPlayer().getInventory().setItemInMainHand(luckDisk);
            }
        }
    }

    public static class LuckDisk {

        public static final String NAME = "Luck Disk";

        public static final String[] LORE = {"Stores Luck Crystals."};

    }

    private class Altar {

        private final ArmorStand stand;

        private final String key;

        private final Location center;

        private int time;

        private Material curMaterial;

        private byte curData;

        private AlchemyRecipe recipe;

        private Vector boxMin;
        private Vector boxMax;

        public Altar(ArmorStand stand) {
            this.stand = stand;
            this.key = getKey(stand.getLocation());

            // get item frames
            center = stand.getLocation().add(0, 1, 0);
            Vector d1 = new Vector(3, 3, 3);
            Vector d2 = new Vector(2, 1, 2);
            boxMin = center.toVector().subtract(d1);
            boxMax = center.toVector().add(d2);
        }

        public boolean isInside(Location loc) {
            return loc.toVector().isInAABB(boxMin, boxMax);
        }

        public boolean isValid(Location forcedAirLocation) {
            return verifier.verify(null, center, 3, forcedAirLocation);
        }

        public void update() {
            Block cb = center.getBlock();
            if (recipe != null && cb.getType() == curMaterial && cb.getData() == curData) {

                double t = ((double) time)*1.5;
                double r = 1.15;
                double dx = Math.cos(t / Math.PI) * r;
                double dz = Math.sin(t / Math.PI) * r;
                Location l = center.clone();
                Vector v = new Vector(0, 0.1, 0);
                l.add(dx, 0, dz);
                photons.add(new Photon(l, v, Color.LIME));

                if (++time > 50) {
                    Material mat = recipe.getOutput();
                    plugin.getLogger().info("stop alchemy. creating " + mat);
                    cb.setType(mat);
                    stand.getWorld().playSound(stand.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1, 1);
                    recipe = null;
                }
            } else {
                recipe = null;
                for (AlchemyRecipe r: recipes) {
                    if (r.matches(cb)) {
                        recipe = r;
                        curMaterial = cb.getType();
                        curData = cb.getData();
                        time = 0;
                        plugin.getLogger().info("start alchemy with " + cb);
                        return;
                    }
                }
            }
        }

    }

    private List<Photon> photons = new ArrayList<>(10000);

    private class Photon implements Comparable<Photon> {

        private float[] color;

        private Location l;

        private Vector v;

        private int age;

        public Photon(Location l, Vector v, float[] color) {
            this.l = l;
            this.v = v;
            this.color = color;
        }

        public Photon(Location l, Vector v, Color color) {
            this(l, v, new float[]{
                    ((float) color.getRed() / 255) - 1.0f,
                    (float) color.getGreen() / 255,
                    (float) color.getBlue() / 255});
        }

        /**
         * updates the photon.
         *
         * @return {@code false} if the photon is no longer valid
         */
        boolean update() {
            l.add(v);
            paint();
            return age++ <= 20;
        }

        private void paint() {
            l.getWorld().spigot().playEffect(l, Effect.COLOURED_DUST, 0, 1, color[0], color[1], color[2], 1, 0, 64);
        }

        @Override
        public int compareTo(Photon o) {
            return Integer.compare(age, o.age);
        }
    }
}