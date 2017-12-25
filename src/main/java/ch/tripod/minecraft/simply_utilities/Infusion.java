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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Directional;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import static org.bukkit.event.block.Action.LEFT_CLICK_BLOCK;
import static org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;

/**
 * {@code Main}...
 */
public class Infusion implements Listener, CommandExecutor, PluginUtility {

    private static final String WAND_NAME = ChatColor.DARK_GREEN + "Simply Wand";
    private static final List<String> WAND_LORE = Collections.singletonList(ChatColor.WHITE + "Left/right click to set corners.");

    //private BukkitTask task;

    private static class Corners {
        private Location l0;
        private Location l1;
    }

    private Map<String, Corners> corners = new HashMap<>();

    private Main plugin;

    private StructureVerifier verifier = new StructureVerifier("infusion").load(
            "{\"dx\":6,\"dy\":6,\"dz\":6,\"matrix\":\".aaaaa.abcccbaacdadcaacaeacaacdadcaabcccba.aaaaa....f.................f..g..f.................f......h.................h.....h.................h......f.................f..i..f.................f......j......j..........jj.k.jj..........j......j.............l...........l.k.l...........l....................l......l....lllll....l......l..........\",\"map\":{\"k\":{\"mat\":\"STAINED_GLASS_PANE\"},\"d\":{\"mat\":\"BLACK_GLAZED_TERRACOTTA\"},\"j\":{\"mat\":\"NETHER_BRICK_STAIRS\"},\"l\":{\"mat\":\"STAINED_GLASS\"},\"a\":{\"mat\":\"RED_NETHER_BRICK\"},\"i\":{\"mat\":\"END_ROD\"},\"h\":{\"mat\":\"CONCRETE\"},\"g\":{\"mat\":\"CAULDRON\"},\"f\":{\"mat\":\"NETHER_BRICK\"},\".\":{\"mat\":\"AIR\"},\"e\":{\"mat\":\"BARRIER\"},\"b\":{\"mat\":\"REDSTONE_BLOCK\"},\"c\":{\"mat\":\"CONCRETE_POWDER\"}}}");

    private HashMap<String, Altar> altars = new HashMap<>();

    private class InfusionRecipe  {

        private List<ItemStack> output = new LinkedList<>();

        private List<ItemStack> ingredients = new LinkedList<>();

        public List<ItemStack> getOutput() {
            return output;
        }

        private void addOutput(Material mat, int count) {
            output.add(new ItemStack(mat, count));
        }

        private void addIngredient(Material mat, int count) {
            ingredients.add(new ItemStack(mat, count));
        }

        public List<ItemStack> getIngredientList() {
            ArrayList<ItemStack> result = new ArrayList<ItemStack>(ingredients.size());
            for (ItemStack ingredient : ingredients) {
                result.add(ingredient.clone());
            }
            return result;
        }


        boolean matches(List<ItemStack> items) {
            List<ItemStack> ingredients = getIngredientList();
            for (ItemStack item: items) {
                for (ItemStack i: ingredients) {
                    if (i.getType() == item.getType()) {
                        i.setAmount(i.getAmount() - 1);
                        item.setAmount(0);
                        plugin.getLogger().info("matched ingredient:" + i);
                    }
                }
            }
            for (ItemStack i: ingredients) {
                // plugin.getLogger().info("ingredient:" + i);
                if (i.getAmount() != 0) {
                    return false;
                }
            }
            for (ItemStack i: items) {
                // plugin.getLogger().info("item:" + i);
                if (i.getAmount() != 0) {
                    return false;
                }
            }
            return true;
        }
    }

    private List<InfusionRecipe> infusionRecipes = new LinkedList<>();
    private void initRecipies() {
        {
            InfusionRecipe r = new InfusionRecipe();
            r.addIngredient(Material.WATER_BUCKET, 3);
            r.addIngredient(Material.SNOW_BALL, 1);
            r.addOutput(Material.ICE, 3);
            r.addOutput(Material.BUCKET, 3);
            infusionRecipes.add(r);
        }
        {
            InfusionRecipe r = new InfusionRecipe();
            r.addIngredient(Material.ENDER_PEARL, 1);
            r.addIngredient(Material.GLOWSTONE_DUST, 2);
            r.addIngredient(Material.STONE, 1);
            r.addOutput(Material.ENDER_STONE, 2);
            infusionRecipes.add(r);
        }
    }

    private BukkitTask task;

    public void enable(JavaPlugin plugin) {
        this.plugin = (Main) plugin;
        initRecipies();
        //task = plugin.getServer().getScheduler().runTaskTimer(plugin, new ScanForPads(), 1L, 1L);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getCommand("simply").setExecutor(this);
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, new AltarScanner(), 1L, 2L);
    }

    public void disable() {
        if (task != null) {
            task.cancel();
            task  = null;
        }
    }

    private class AltarScanner implements Runnable {

        float phase = 0;

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

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if ("simply".equalsIgnoreCase(cmd.getName())) {
            if (!(sender instanceof Player)) {
                return true;
            }
            Player p = (Player) sender;
            if (args.length > 0 && "wand".equals(args[0])) {
                ItemStack is = p.getItemInHand();
                ItemMeta meta = is.getItemMeta();
                meta.setDisplayName(WAND_NAME);
                meta.setLore(WAND_LORE);
                meta.setUnbreakable(true);
                meta.addEnchant(Enchantment.MENDING, 1, true);
                is.setItemMeta(meta);
                sender.sendMessage("Your " + is.getType() + " turned into a simply wand!");
            }
            else if (args.length > 0 && "scan".equals(args[0])) {
                Corners c = getCorners(p);
                if (c.l0 == null || c.l1 == null) {
                    sender.sendMessage("set first and second corner first");
                    return true;
                }
                String json = StructureVerifier.scan(p, c.l0, c.l1);
                plugin.getLogger().info(json);
                p.sendMessage("scanned structure:");
                p.sendMessage(json);
            }
            else if (args.length > 0 && "reset_Chunks".equals(args[0])) {
                plugin.onResetChunks(p);
            }
            else {
                sender.sendMessage("usage: /simply wand");
            }
            return true;
        }
        return false;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        ItemStack is = p.getItemInHand();
        ItemMeta meta = is == null ? null : is.getItemMeta();
        if (meta == null) {
            return;
        }
        if (WAND_NAME.equals(meta.getDisplayName())) {
            Corners c = getCorners(p);
            if (event.getAction() == LEFT_CLICK_BLOCK) {
                c.l0 = event.getClickedBlock().getLocation();
                p.sendMessage("first corner set: " + c.l0);
            }
            if (event.getAction() == RIGHT_CLICK_BLOCK) {
                c.l1 = event.getClickedBlock().getLocation();
                p.sendMessage("second corner set: " + c.l1);
                p.sendMessage("use " + ChatColor.GREEN + "/simply scan" + ChatColor.WHITE + " to scan the structure");
            }
            event.setCancelled(true);
        }
    }


    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event){

        if(event.getBlock().getType() == Material.CAULDRON) {
            if (verifier.verify(event.getPlayer(), event.getBlock().getLocation(), 1)) {
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
        return "altar-" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
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
        a.setArms(true);
        a.setLeftArmPose(new EulerAngle(Math.toRadians(170), Math.toRadians(170), Math.toRadians(31)));

        //ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) SkullType.PLAYER.ordinal());
        //SkullMeta meta = (SkullMeta) head.getItemMeta();
        //meta.setOwner(player.getName());
        //head.setItemMeta(meta);
        // a.setHelmet(head);

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
        if (key!=null && key.startsWith("altar-")) {
            return altars.computeIfAbsent(key, k -> new Altar(a));
        } else {
            return null;
        }
    }

    private class ItemHolder {

        private final ItemFrame frame;

        private final Location containerLoc;

        public ItemHolder(ItemFrame frame, Location containerLoc) {
            this.frame = frame;
            this.containerLoc = containerLoc;
        }

        boolean hasItem() {
            return frame.getItem().getType() != Material.AIR;
        }

        ItemStack removeItem() {
            ItemStack item = frame.getItem();
            frame.setItem(null);
            return item;
        }

        public void update() {
            if (!hasItem()) {
                Block b = containerLoc.getBlock();
                if (b.getState() instanceof Container) {
                    Inventory inv = ((Container) b.getState()).getInventory();
                    ItemStack[] contents = inv.getContents();
                    for (ItemStack stack: contents) {
                        if (stack != null) {
                            int amount = stack.getAmount();
                            if (amount > 0) {
                                amount--;
                                frame.setItem(new ItemStack(stack.getType(), 1));
                                stack.setAmount(amount);
                                inv.setContents(contents);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    private class Altar {

        private final ArmorStand stand;

        private final String key;

        private final Vector center;

        private final Vector rod;

        private final List<ItemHolder> frames = new ArrayList<>(4);

        private InfusionRecipe infusing = null;

        private int time = 0;

        public Altar(ArmorStand stand) {
            this.stand = stand;
            this.key = getKey(stand.getLocation());

            // get item frames
            center = stand.getLocation().toVector();
            rod = center.clone();
            rod.add(new Vector(0, 2.5, 0));
            center.setX(center.getBlockX());
            center.setY(center.getBlockY());
            center.setZ(center.getBlockZ());
            for (ItemFrame i: stand.getLocation().getWorld().getEntitiesByClass(ItemFrame.class)) {
                Vector v = i.getLocation().toVector();
                v.setX(v.getBlockX());
                v.setY(v.getBlockY());
                v.setZ(v.getBlockZ());
                // plugin.getLogger().info("distance is: " + v.distanceSquared(center));
                if (v.distanceSquared(center) == 5) {
                    v.subtract(center.toBlockVector()).setY(0);
                    v.multiply(2);
                    Location l = new Location(stand.getWorld(), center.getX(), center.getY() + 1, center.getZ());
                    l.add(v);
                    // plugin.getLogger().info("found item frame: " + i + " at " + b);
                    frames.add(new ItemHolder(i, l));
                }
            }
        }

        public void update() {

            if (infusing != null) {
                animateItems();
            } else {
                List<ItemStack> items = new ArrayList<>(4);
                for (ItemHolder holder: frames) {
                    holder.update();
                    if (holder.hasItem()) {
                        items.add(new ItemStack(holder.frame.getItem()));
                    }
                }

                for (InfusionRecipe r: infusionRecipes) {
                    if (r.matches(items)) {
                        startInfusion(r);
                        break;
                    }
                }
            }
        }

        private void endInfusion() {
            for (ItemHolder frame: frames) {
                frame.removeItem();
            }

            Location loc = stand.getLocation().clone().add(0, 0.5, 0);
            for (ItemStack i: infusing.getOutput()) {
                plugin.getLogger().info("location is: " + loc);
                Item item = stand.getWorld().dropItem(loc, i);
                item.setVelocity(new Vector());
                item.teleport(loc);
            }
            infusing = null;
        }

        private void startInfusion(InfusionRecipe r) {
            infusing = r;
            time = 0;
            plugin.getLogger().info("start infusion of: " + r);
            animateItems();
            stand.getWorld().playSound(stand.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1, 1);
        }

        private void animateItems() {
            if (time < 10) {
                for (ItemHolder frame: frames) {
                    Vector s0 = frame.frame.getLocation().toVector();
                    Vector d  = rod.clone().subtract(s0);
                    d.multiply(0.1);
                    Location l = frame.frame.getLocation();
                    photons.add(new Photon(l, d, Color.YELLOW, rod));
                }
            }
            if (time > 25) {
                endInfusion();
            }
            time++;
        }
    }

    private List<Photon> photons = new ArrayList<>(10000);

    private class Photon implements Comparable<Photon> {

        private float[] color;

        private Location l;

        private Vector v;

        private Vector rod;

        private int age;

        public Photon(Location l, Vector v, float[] color, Vector rod) {
            this.l = l;
            this.v = v;
            this.color = color;
            this.rod = rod;
        }

        public Photon(Location l, Vector v, Color color, Vector rod) {
            this(l, v, new float[]{
                    ((float) color.getRed() / 255) - 1.0f,
                    (float) color.getGreen() / 255,
                    (float) color.getBlue() / 255}, rod);
        }

        /**
         * updates the photon.
         * @return {@code false} if the photon is no longer valid
         */
        boolean update() {
            l.add(v);
            if (l.toVector().distanceSquared(rod) < 0.05) {
                v = new Vector(0, -0.1, 0);
            }
            paint();
            return age++ <= 30;
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