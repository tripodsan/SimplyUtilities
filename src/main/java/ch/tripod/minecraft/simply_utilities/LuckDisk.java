package ch.tripod.minecraft.simply_utilities;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class LuckDisk {

    public static final String NAME = "Luck Disk";

    public static final String[] LORE = {"Stores Luck Crystals."};

    private final ItemStack item;

    private LuckDisk(ItemStack item) {
        this.item = item;
    }

    @Nullable
    public static LuckDisk fromItem(@Nonnull ItemStack item) {
        if (!Utils.checkIsItemByLore(item, LORE)) {
            return null;
        }
        return new LuckDisk(item);
    }

    public void openInventory(Player player) {
        Inventory inv = Bukkit.createInventory(player, 27, NAME);
        InventoryView view = player.openInventory(inv);

        String code = getCrystalCode();
        Log.info("the new code is: %s", code);
        ItemStack[] contents = view.getTopInventory().getContents();
        for (int i = 0; i < code.length(); i++) {
            LuckCrystal crystal = LuckCrystal.fromCode(String.valueOf(code.charAt(i)));
            if (crystal != null) {
                contents[i] = crystal.toItemStack();
            }
        }
        view.getTopInventory().setContents(contents);
    }

    private void setCrystalCode(String code) {
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.getLore();
        if (lore.size() == 1) {
            lore.add(code);
        } else {
            lore.set(1, code);
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    private String getCrystalCode() {
        List<String> lore = item.getItemMeta().getLore();
        return lore.size() > 1 ? lore.get(1) : "";
    }

    public static GUI getGUI(InventoryEvent evt) {
        InventoryView view = evt.getView();
        if (!NAME.equals(view.getTitle())) {
            return null;
        }
        return new GUI();
    }

    public static void delegate(InventoryEvent evt) {
        LuckDisk.GUI gui = LuckDisk.getGUI(evt);
        if (gui != null) {
            try {
                gui.getClass().getMethod("on", evt.getClass()).invoke(gui, evt);
            } catch (Exception e) {
                Log.warn("Unable to delegate event: %s", evt);
            }
        }
    }

    public static class GUI {

        public void on(InventoryCloseEvent evt) {
            Log.info("Closed inventory of %s", evt.getPlayer());

            // TODO: search for luck disk in player inventory
            LuckDisk disk = LuckDisk.fromItem(evt.getPlayer().getInventory().getItemInMainHand());
            if (disk == null) {
                Log.warn("Player doesn't have a luck disk in main hand.");
                return;
            }

            // calculate the code from the inventory contents
            ItemStack[] contents = evt.getInventory().getContents();
            StringBuilder code = new StringBuilder();
            for (int i = 0; i < 27; i++) {
                ItemStack slot = contents[i];
                LuckCrystal crystal = LuckCrystal.fromItemStack(slot);
                if (crystal == null) {
                    code.append(".");
                } else {
                    code.append(crystal.toCode());
                }
            }
            Log.info("disk code is: %s", code);
            disk.setCrystalCode(code.toString());
            evt.getPlayer().getInventory().setItemInMainHand(disk.item);
        }

        public void on(InventoryClickEvent evt) {
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
            if (!LuckCrystal.isLuckCrystal(s)) {
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

        public void on(InventoryDragEvent evt) {
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

}
