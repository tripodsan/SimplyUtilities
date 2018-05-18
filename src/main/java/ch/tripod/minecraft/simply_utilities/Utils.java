package ch.tripod.minecraft.simply_utilities;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Utils {

    public static boolean checkIsItemByLore(ItemStack s, String[] lore) {
        List<String> itemLore = s.getItemMeta().getLore();
        return itemLore != null && itemLore.size() > 0 && itemLore.get(0).equals(lore[0]);
    }

    public static void broadcast(Location loc, String msg, int distance) {
        for (Entity e: loc.getWorld().getNearbyEntities(loc, distance, distance, distance)) {
            if (e instanceof Player) {
                e.sendMessage(msg);
            }
        }
    }

}
