package ch.tripod.minecraft.simply_utilities;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class LuckCrystal {

    private static String LORE_LINE_0 = "Increases chance of transmution or infusion.";

    public static final LuckCrystal[] TYPES = {
        new LuckCrystal("A", "Quartz", Material.QUARTZ),
        new LuckCrystal("B", "Redstone", Material.REDSTONE),
        new LuckCrystal("C", "Emerald", Material.EMERALD),
        new LuckCrystal("D", "Diamond", Material.DIAMOND),
        new LuckCrystal("E", "Gold", Material.GOLD_INGOT),
        new LuckCrystal("F", "Iron", Material.IRON_INGOT),
        new LuckCrystal("G", "Lapis", Material.INK_SACK),
        new LuckCrystal("H", "Coal", Material.COAL),
        new LuckCrystal("I", "Glowstone", Material.GLOWSTONE_DUST),
        new LuckCrystal("J", "Soul Sand", Material.NETHER_STALK),
        new LuckCrystal("K", "Prismarine", Material.PRISMARINE_SHARD)
    };
    private static final Map<String, LuckCrystal> TYPES_BY_CODE = new HashMap<>();
    static {
        for (LuckCrystal c: TYPES) {
            TYPES_BY_CODE.put(c.code, c);
        }
    }

    public final String code;

    public final String name;

    public final Material mat;

    public LuckCrystal(String code, String name, Material mat) {
        this.code = code;
        this.name = name;
        this.mat = mat;
    }

    public static LuckCrystal fromCode(String c) {
        return TYPES_BY_CODE.get(c);
    }

    public static LuckCrystal fromItemStack(ItemStack s) {
        if (s == null) {
            return null;
        }
        for (LuckCrystal t: TYPES) {
            if (t.mat == s.getType()) {
                return t;
            }
        }
        return null;
    }

    public static boolean isLuckCrystal(ItemStack s) {
        List<String> lore = s.getItemMeta().getLore();
        return lore != null && lore.size() > 0 && lore.get(0).equals(LORE_LINE_0);
    }

    public String toCode() {
        return code;
    }

    public ItemStack toItemStack() {
        ItemStack s = new ItemStack(mat, 1);
        if ("Lapis".equals(name)) {
            s = new ItemStack(mat, 1, (short) 0, (byte) 4);
        }
        String[] lore = {
                LuckCrystal.LORE_LINE_0,
                "Put this in a Luck Disk to receive a 4% boost for " + name.toLowerCase() + "!"
        };
        ItemMeta m = s.getItemMeta();
        m.setDisplayName(name);
        m.setLore(Arrays.asList(lore));
        m.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, false);
        s.setItemMeta(m);
        return s;
    }
}
