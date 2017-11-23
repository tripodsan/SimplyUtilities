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
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Effect;
import org.bukkit.Keyed;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.SkullType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.material.Banner;
import org.bukkit.material.Dispenser;
import org.bukkit.material.Dye;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import static org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;

/**
 * {@code ExampleListener}...
 */
public class Lazers implements Listener {

    private static final List<String> LAZER_LORE = Collections.singletonList("SHOOP DA WOOP!");

    private static final List<String> MIRROR_LORE = Collections.singletonList("Use this to reflect lazers.");

    private static final List<String> SPANNER_LORE = Collections.singletonList("Use this to turn mirrors.");

    public static final String KEY_DAMAGE = "lazers:damage";

    private JavaPlugin plugin;

    private BukkitTask task;

    private static final int MAX_PHOTONS = 10000;

    private List<Photon> photons = new ArrayList<>(MAX_PHOTONS);

    private HashMap<String, Lazer> lazers = new HashMap<>();

    void enable(JavaPlugin plugin) {
        this.plugin = plugin;
        createRecipes();

        task = plugin.getServer().getScheduler().runTaskTimer(plugin, new LazerScanner(), 1L, 2L);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private class MyRecp implements Recipe, Keyed {

        private final NamespacedKey key;

        private final ItemStack stack;

        public MyRecp(NamespacedKey key, ItemStack stack) {
            this.key = key;
            this.stack = stack;
        }

        @Override
        public NamespacedKey getKey() {
            return key;
        }

        @Override
        public ItemStack getResult() {
            return stack;
        }
    }

    private void createRecipes() {
        // create lazer recipe
        {
            ItemStack lazer = new ItemStack(Material.DISPENSER);
            ItemMeta im = lazer.getItemMeta();
            im.setDisplayName(ChatColor.DARK_RED + "Lazer");
            im.setLore(LAZER_LORE);
            lazer.setItemMeta(im);
            ShapedRecipe recp = new ShapedRecipe(new NamespacedKey(plugin, "lazer"), lazer);
            recp.shape("IDI", "RER", "IGI");
            recp.setIngredient('I', Material.IRON_INGOT);
            recp.setIngredient('D', Material.DIAMOND);
            recp.setIngredient('R', Material.REDSTONE);
            recp.setIngredient('E', Material.ENDER_PEARL);
            recp.setIngredient('G', Material.GLASS);
            plugin.getServer().addRecipe(recp);
        }

        // create prism recipe
        {
            ShapedRecipe recp = new ShapedRecipe(new NamespacedKey(plugin, "prism_boost"), new LazerPrism().toItemStack());
            recp.shape("/#/", "G*G", "/#/");
            recp.setIngredient('/', Material.STICK);
            recp.setIngredient('#', Material.IRON_FENCE);
            recp.setIngredient('G', Material.THIN_GLASS);
            recp.setIngredient('*', Material.PRISMARINE_SHARD);
            plugin.getServer().addRecipe(recp);
        }

        // create spanner recipe
        {
            ItemStack shears = new ItemStack(Material.SHEARS);
            ItemMeta im = shears.getItemMeta();
            im.setDisplayName(ChatColor.AQUA + "Spanner");
            im.setLore(SPANNER_LORE);
            shears.setItemMeta(im);
            ShapedRecipe recp = new ShapedRecipe(new NamespacedKey(plugin, "spanner"), shears);
            recp.shape(" # ", " /#", "/  ");
            recp.setIngredient('/', Material.STICK);
            recp.setIngredient('#', Material.IRON_INGOT);
            plugin.getServer().addRecipe(recp);
        }

        // create mirror recipe
        {
            ItemStack mirror = new ItemStack(Material.BANNER);
            // Banner banner = (Banner) mirror.getData();
            // banner.setData((byte) 0);
            BannerMeta im = (BannerMeta) mirror.getItemMeta();
            im.setDisplayName(ChatColor.WHITE + "Mirror");
            im.setLore(MIRROR_LORE);

            im.setBaseColor(DyeColor.BLACK);
            List<Pattern> patterns = new ArrayList<>();
            patterns.add(new Pattern(DyeColor.SILVER, PatternType.BORDER));
            patterns.add(new Pattern(DyeColor.SILVER, PatternType.RHOMBUS_MIDDLE));
            patterns.add(new Pattern(DyeColor.SILVER, PatternType.CIRCLE_MIDDLE));
            patterns.add(new Pattern(DyeColor.WHITE, PatternType.CROSS));
            patterns.add(new Pattern(DyeColor.BLACK, PatternType.FLOWER));
            im.setPatterns(patterns);

            mirror.setItemMeta(im);
            ShapedRecipe recp = new ShapedRecipe(new NamespacedKey(plugin, "mirror"), mirror);
            recp.shape("/IG", "/OG", "/IG");
            recp.setIngredient('/', Material.STICK);
            recp.setIngredient('I', Material.IRON_INGOT);
            recp.setIngredient('O', Material.GOLD_INGOT);
            recp.setIngredient('G', Material.THIN_GLASS);
            plugin.getServer().addRecipe(recp);
        }
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
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == RIGHT_CLICK_BLOCK) {
            Block b = event.getClickedBlock();
            ItemStack item = event.getItem();
            if (b == null || item == null) {
                return;
            }
            if (SPANNER_LORE.equals(item.getItemMeta().getLore()) && b.getType() == Material.STANDING_BANNER) {
                b.setData((byte) ((b.getData() + 1) & 0x0f));
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event){
        Player player = event.getPlayer();
        if(event.getBlock().getType() == Material.DISPENSER) {
            Collection<ItemStack> stack = event.getBlock().getDrops();
            ItemMeta im = stack.iterator().next().getItemMeta();
            // player.sendMessage("stack is " + im.getDisplayName());
            im = player.getItemInHand().getItemMeta();
            if (LAZER_LORE.equals(im.getLore())) {
                Dispenser dispenser = (Dispenser) event.getBlock().getState().getData();

                Lazer l = createLazer(player, event.getBlock().getLocation(), dispenser);
                player.sendMessage(ChatColor.AQUA + "You have placed down a lazer! " + l.key);
            }
        }
    }

//    @EventHandler
//    public void onBlockRedstoneEvent(BlockRedstoneEvent event) {
//        for (BlockFace face : new BlockFace[] { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN }) {
//            if (event.getBlock().getRelative(face).getType() == Material.DISPENSER) {
//
//
//                Bukkit.getPluginManager().callEvent(new RedstoneToggleEvent(event.getBlock().getRelative(face), powered));
//            }
//        }
//        plugin.getLogger().info("redstone event :at " + event.getBlock());
//        Lazer lazer = getLazer(event.getBlock());
//        if (lazer != null) {
//            plugin.getLogger().info("redstone even: " + event.getNewCurrent());
//            event.setNewCurrent(0);
//        }
//    }

    @EventHandler
    public void onDispenseItem(BlockDispenseEvent event) {
        Lazer lazer = getLazer(event.getBlock());
        if (lazer != null) {
            lazer.toggle();
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if(event.getBlock().getType() == Material.DISPENSER) {
            String key = getKey(event.getBlock().getLocation());
            if (removeLazer(event.getBlock().getWorld(), key)) {
                player.sendMessage("you broke " + key);
            }
        }
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        event.getWhoClicked().sendMessage("player crafted: " + event.toString());
    }

    @EventHandler(ignoreCancelled = true)
    public void onCraftColoredPrismEvent(PrepareItemCraftEvent event) {
        // plugin.getLogger().info("prepare: " + event);
        ItemStack[] ss = event.getInventory().getMatrix();
        Color c = null;
        LazerPrism oldPrism = null;
        boolean hasColor = false;
        for (ItemStack s: ss) {
            if (s == null) {
                continue;
            }
            // plugin.getLogger().info("m: " + s.getType() + " d:" + s.getData());
            if (s.getType() == Material.PRISMARINE_SHARD) {
                oldPrism = LazerPrism.fromItemStack(s);
                if (oldPrism == null) {
                    return;
                }
            } else if (s.getType() == Material.INK_SACK) {
                DyeColor dye = ((Dye) s.getData()).getColor();
                if (c == null) {
                    c = dye.getColor();
                } else {
                    c = c.mixDyes(dye);
                }
                hasColor = true;
            } else {
                return;
            }
        }
        if (oldPrism == null || !hasColor) {
            return;
        }

        oldPrism.mixColor(c);
        event.getInventory().setResult(oldPrism.toItemStack());
    }

    private Lazer createLazer(Player player, Location loc, Dispenser dispenser) {
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

        return getLazer(a);
    }

    private boolean removeLazer(World world, String key) {
        Lazer a = getLazer(world, key);
        if (a != null) {
            a.stand.remove();
            lazers.remove(key);
            return true;
        }
        return false;
    }

    private Lazer getLazer(World world, String key) {
        for (ArmorStand a: world.getEntitiesByClass(ArmorStand.class)) {
            if (a.getCustomName().equals(key)) {
                return lazers.computeIfAbsent(key, k -> new Lazer(a));
            }
        }
        return null;
    }

    private Lazer getLazer(Block block) {
        if (block.getType() != Material.DISPENSER) {
            return null;
        }
        String key = getKey(block.getLocation());
        return getLazer(block.getLocation().getWorld(), key);
    }

    private Lazer getLazer(ArmorStand a) {
        String key = a.getCustomName();
        if (key!=null && key.startsWith("lazer-")) {
            return lazers.computeIfAbsent(key, k -> new Lazer(a));
        } else {
            return null;
        }
    }

    private class Photon implements Comparable<Photon> {

        private float[] color;

        private Location l;

        private Vector v;

        private int age;

        private String prevBlock = "";

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
         * @return {@code false} if the photon is no longer valid
         */
        boolean update() {
            l.add(v);
            String key = getKey(l);
            if (!key.equals(prevBlock)) {
                prevBlock = key;
                Block b = l.getBlock();

                if (b.getType() == Material.STANDING_BANNER) {
                    // get normal of banner face
                    Banner banner = (Banner) b.getState().getData();
                    BlockFace face = banner.getFacing();
                    Vector n = new Vector(face.getModX(), face.getModY(), face.getModZ()).normalize();

                    // reflect direction V' = -2*(V dot N)*N + V
                    v.add(n.multiply(-2 * v.dot(n)));

                    // snap to 45 degs
                    double a = Math.toDegrees(v.angle(new Vector(1, 0, 0)));
                    a = Math.toRadians(Math.round(a / 45) * 45);
                    double s = v.length();
                    v = new Vector(Math.cos(a) * s, 0, Math.sin(a) * s);

                    l.setX(b.getX() + 0.5);
                    l.setY(b.getY() + 0.5);
                    l.setZ(b.getZ() + 0.5);
                    age = 0;
                }
                else if (b.getType() == Material.SAND) {
                    List<MetadataValue> meta = b.getState().getMetadata(KEY_DAMAGE);
                    int damage = 0;
                    if (meta != null && !meta.isEmpty()) {
                        damage = meta.get(0).asInt();
                    }
                    damage++;
                    if (damage > 20) {
                        b.getState().removeMetadata(KEY_DAMAGE, plugin);
                        b.breakNaturally();
                        plugin.getLogger().info("block at " + b.getLocation() + " broken by lazer.");
                    } else {
                        b.getState().setMetadata(KEY_DAMAGE, new FixedMetadataValue(plugin, damage));
                    }
                }
                else if (b.getType() != Material.AIR) {
                    return false;
                }
            }
            damp(v);
            paint();
            return age++ <= 100;
        }

        private void paint() {
            l.getWorld().spigot().playEffect(l, Effect.COLOURED_DUST, 0, 1, color[0], color[1], color[2], 1, 0, 64);
        }

        @Override
        public int compareTo(Photon o) {
            return Integer.compare(age, o.age);
        }
    }

    private static class LazerPrism {

        private static final List<String> PRISM_LORE = Collections.singletonList("Use this to alter lazers.");

        private static final String COLOR_PREFIX = ChatColor.WHITE + "+Color: ";
        private final List<String> lore;

        private Color color;

        public LazerPrism() {
            lore = new ArrayList<>(PRISM_LORE);
        }

        public LazerPrism(List<String> lore, Color color) {
            this.lore = lore;
            this.color = color;
        }

        private void mixColor(Color c) {
            if (color == null) {
                color = c;
            } else {
                color = color.mixColors(c);
            }
        }

        private ItemStack toItemStack() {
            ItemStack prism = new ItemStack(Material.PRISMARINE_SHARD);
            ItemMeta im = prism.getItemMeta();
            List<String> newLore = new ArrayList<>(lore);
            if (color != null) {
                im.setDisplayName(ChatColor.AQUA + "Colored Lazer Prism");
                newLore.add(COLOR_PREFIX + Integer.toHexString(color.asRGB()));
            } else {
                im.setDisplayName(ChatColor.AQUA + "Lazer Prism");
            }
            im.setLore(newLore);
            prism.setItemMeta(im);
            return prism;
        }

        private static LazerPrism fromItemStack(ItemStack s) {
            if (s.getType() != Material.PRISMARINE_SHARD) {
                return null;
            }
            if (!s.getItemMeta().hasLore()) {
                return null;
            }
            Color c = null;
            ArrayList<String> lore = new ArrayList<>();
            for (String ll : s.getItemMeta().getLore()) {
                if (lore.isEmpty()) {
                    if (!ll.equals(PRISM_LORE.get(0))) {
                        return null;
                    }
                }
                if (ll.startsWith(COLOR_PREFIX)) {
                    String cStr = ll.substring(COLOR_PREFIX.length());
                    Color pColor = Color.fromRGB(Integer.parseInt(cStr, 16));
                    // plugin.getLogger().info("existing prism color: " + pColor);
                    if (c == null) {
                        c = pColor;
                    } else {
                        c = c.mixColors(pColor);
                    }
                } else {
                    lore.add(ll);
                }
            }
            return new LazerPrism(lore, c);
        }

    }

    private class Lazer {

        private final float[] COLOR_GOLD = {0f, 0.570312f, 0f};
        private final float[] COLOR_RED = {0f, 0f, 0f};

        private final ArmorStand stand;

        private final String key;

        private boolean power;

        private Lazer(ArmorStand stand) {
            this.stand = stand;
            key = getKey(stand.getLocation());
        }

        private void update() {
            Location l = stand.getLocation();

            Color color = Color.RED;
            power = false;
            if (l.getBlock().getType() == Material.DISPENSER) {
                InventoryHolder holder = (InventoryHolder) l.getBlock().getState();
                Inventory i = holder.getInventory();
                LazerPrism prism = null;
                for (ItemStack is: i.getContents()) {
                    if (is == null) {
                        continue;
                    }
                    if (is.getType() == Material.PRISMARINE_SHARD) {
                        prism = LazerPrism.fromItemStack(is);
                        if (prism != null) {
                            if (prism.color != null) {
                                color = prism.color;
                            }
                        }
                    } else if (is.getType() == Material.REDSTONE) {
                        power = true;
                    }
                }

            } else {
                // hmm what to do?
            }
            if (!power) {
                return;
            }
            l.add(0, 0.5, 0);
            Vector v = l.getDirection();
            damp(v);
            l.add(v);
            v.multiply(0.25);

            photons.add(new Photon(l, v, color));
        }

        public Block getBlock() {
            return stand.getLocation().getBlock();
        }

        public void toggle() {
            Block b = getBlock();
            if (b.getType() != Material.DISPENSER) {
                return;
            }
            InventoryHolder holder = (InventoryHolder) b.getState();
            Inventory i = holder.getInventory();
            plugin.getLogger().info("toggle...:" + i.contains(Material.REDSTONE));
            if (i.contains(Material.REDSTONE)) {
                i.remove(Material.REDSTONE);
            } else {
                i.addItem(new ItemStack(Material.REDSTONE));
            }

        }
    }

    private static void damp(Vector v) {
        if (Math.abs(v.getX()) < 0.0001) {
            v.setX(0);
        }
        if (Math.abs(v.getY()) < 0.0001) {
            v.setY(0);
        }
        if (Math.abs(v.getZ()) < 0.0001) {
            v.setZ(0);
        }
    }

    private class LazerScanner implements Runnable {

        float phase = 0;

        @Override
        public void run() {
            for (World w : plugin.getServer().getWorlds()) {
                for (ArmorStand a : w.getEntitiesByClass(ArmorStand.class)) {
                    Lazer l = getLazer(a);
                    if (l != null) {
                        l.update();
                    }
                }
            }

            // remove photons
            if (photons.size() > MAX_PHOTONS) {
                Collections.sort(photons);
                photons = photons.subList(0, MAX_PHOTONS);
            }

            // update the photons
            photons.removeIf(p -> !p.update());

            phase += 0.05;
            if (phase >= 0.25) {
                phase = 0;
            }
        }
    }
}