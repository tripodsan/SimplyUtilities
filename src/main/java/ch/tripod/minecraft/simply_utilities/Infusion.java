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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import static org.bukkit.event.block.Action.LEFT_CLICK_BLOCK;
import static org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;

/**
 * {@code Main}...
 */
public class Infusion implements Listener, CommandExecutor {

    private static final String WAND_NAME = ChatColor.DARK_GREEN + "Simply Wand";
    private static final List<String> WAND_LORE = Collections.singletonList(ChatColor.WHITE + "Left/right click to set corners.");

    //private BukkitTask task;

    private static class Corners {
        private Location l0;
        private Location l1;
    }

    private Map<String, Corners> corners = new HashMap<>();

    private JavaPlugin plugin;

    private StructureVerifier verifier = new StructureVerifier().load(
            "{\"dx\":6,\"dy\":6,\"dz\":6,\"matrix\":\".aaaaa.abcccbaacdadcaacaeacaacdadcaabcccba.aaaaa....f.................f..g..f.................f......h.................h.....h.................h......f.................f..i..f.................f......j......j..........jj.k.jj..........j......j.............l...........l.k.l...........l....................l......l....lllll....l......l..........\",\"map\":{\"k\":{\"mat\":\"STAINED_GLASS_PANE\"},\"d\":{\"mat\":\"BLACK_GLAZED_TERRACOTTA\"},\"j\":{\"mat\":\"NETHER_BRICK_STAIRS\"},\"l\":{\"mat\":\"STAINED_GLASS\"},\"a\":{\"mat\":\"RED_NETHER_BRICK\"},\"i\":{\"mat\":\"END_ROD\"},\"h\":{\"mat\":\"CONCRETE\"},\"g\":{\"mat\":\"CAULDRON\"},\"f\":{\"mat\":\"NETHER_BRICK\"},\".\":{\"mat\":\"AIR\"},\"e\":{\"mat\":\"BARRIER\"},\"b\":{\"mat\":\"REDSTONE_BLOCK\"},\"c\":{\"mat\":\"CONCRETE_POWDER\"}}}");

    void enable(JavaPlugin plugin) {
        this.plugin = plugin;
        //task = plugin.getServer().getScheduler().runTaskTimer(plugin, new ScanForPads(), 1L, 1L);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getCommand("simply").setExecutor(this);
    }

    void disable() {
//        if (task != null) {
//            task.cancel();
//            task  = null;
//        }
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
            if (verifier.verify(event.getPlayer(), event.getBlock().getLocation())) {

            }
        }
    }


}