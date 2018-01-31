package ch.tripod.minecraft.simply_utilities;

import java.util.List;

import org.bukkit.inventory.ItemStack;

public class Utils {

    public static boolean checkIsItemByLore(ItemStack s, String[] lore) {
        List<String> itemLore = s.getItemMeta().getLore();
        return itemLore != null && itemLore.size() > 0 && itemLore.get(0).equals(lore[0]);
    }

}
