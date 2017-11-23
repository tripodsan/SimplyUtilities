/*************************************************************************
 *
 * ADOBE CONFIDENTIAL
 * ___________________
 *
 *  Copyright 2017 Adobe Systems Incorporated
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Adobe Systems Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Adobe Systems Incorporated and its
 * suppliers and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Adobe Systems Incorporated.
 **************************************************************************/
package ch.tripod.minecraft.simply_utilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

/**
 * {@code Verifier}...
 */
public class StructureVerifier {

    private static final int MAX_STRUCTURE_SIZE = 32*32*32;

    private final String name;

    private int dx;

    private int dy;

    private int dz;

    private BlockCondition[] filter;


    public StructureVerifier(String name) {
        this.name = name;
    }

    public StructureVerifier load(String json) {
        JsonParser p = new JsonParser();
        JsonObject obj = (JsonObject) p.parse(json);

        dx = obj.get("dx").getAsInt();
        dy = obj.get("dy").getAsInt();
        dz = obj.get("dz").getAsInt();
        Map<Character, BlockCondition> mapping = new HashMap<>();
        char[] matrix = obj.get("matrix").getAsString().toCharArray();
        JsonObject map = obj.get("map").getAsJsonObject();
        for (Map.Entry<String, JsonElement> e: map.entrySet()) {
            mapping.put(e.getKey().toCharArray()[0], new BlockCondition((JsonObject) e.getValue()));
        }

        List<BlockCondition> f = new ArrayList<>(matrix.length);
        for (char c: matrix) {
            f.add(mapping.get(c));
        }
        filter = f.toArray(new BlockCondition[f.size()]);
        return this;
    }

    private static class BlockCondition {

        private Material mat;

        private byte data;

        public BlockCondition(Material mat) {
            this.mat = mat;
        }

        public BlockCondition(Block block) {
            this.mat = block.getType();
            this.data = block.getData();
        }

        public BlockCondition(JsonObject value) {
            this.mat = Material.getMaterial(value.get("mat").getAsString());
        }

        public JsonObject toJson() {
            JsonObject obj = new JsonObject();
            obj.add("mat", new JsonPrimitive(mat.name()));
            obj.add("dat", new JsonPrimitive(data));
            return obj;
        }

        private boolean matches(Material blockMaterial) {
            if (mat == Material.AIR || mat == Material.BARRIER) {
                return true;
            }
            return mat == blockMaterial;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            BlockCondition that = (BlockCondition) o;

            return mat == that.mat;
        }

        @Override
        public int hashCode() {
            return mat.hashCode();
        }
    }

    public boolean verify(Player p, Location loc, int sy) {
        return verify(p, loc, sy, null);
    }

    public boolean verify(Player p, Location loc, int sy, Location forcedAirLocation) {
        int x0 = loc.getBlockX() - dx / 2;
        int y0 = loc.getBlockY() - sy;
        int z0 = loc.getBlockZ() - dz / 2;
        int i = 0;
        for (int y = y0; y <= y0 + dy; y++) {
            for (int x = x0; x <= x0 + dx; x++) {
                for (int z = z0; z <= z0 + dz; z++) {
                    Location l = new Location(loc.getWorld(), x, y, z);
                    Material mat = l.getBlock().getType();
                    if (forcedAirLocation != null && l.equals(forcedAirLocation)) {
                        mat = Material.AIR;
                    }
                    if (!filter[i++].matches(mat)) {
                        if (p != null) {
                            p.sendMessage(name + " structure not valid at " + l.getBlock() + " should be " + filter[i-1].toJson());
                        }
                        return false;
                    }
                }
            }
        }
        if (p != null) {
            p.sendMessage("Hooray. structure complete: " + name);
        }
        return true;
    }

    private static void swap(int[] v) {
        int t = v[0];
        v[0] = v[1];
        v[1] = t;
    }

    public void build(Location loc) {
        int x0 = loc.getBlockX();
        int y0 = loc.getBlockY();
        int z0 = loc.getBlockZ();
        int i = 0;
        for (int y = y0; y <= y0 + dy; y++) {
            for (int x = x0; x <= x0 + dx; x++) {
                for (int z = z0; z <= z0 + dz; z++) {
                    Location l = new Location(loc.getWorld(), x, y, z);
                    BlockCondition f = filter[i++];
                    l.getBlock().setType(f.mat);
                    if (f.data > 0) {
                        l.getBlock().setData(f.data);
                    }
                }
            }
        }
    }

    public static String scan(Player p, Location l0, Location l1) {
        p.sendMessage("scanning blocks....");
        int x[] = {l0.getBlockX(), l1.getBlockX()};
        int y[] = {l0.getBlockY(), l1.getBlockY()};
        int z[] = {l0.getBlockZ(), l1.getBlockZ()};
        int dx = x[1] - x[0];
        int dy = y[1] - y[0];
        int dz = z[1] - z[0];
        if (Math.abs(dx*dy*dz) > MAX_STRUCTURE_SIZE) {
            p.sendMessage("structure can't be larger than " + MAX_STRUCTURE_SIZE);
            return null;
        }
        if (dx < 0) {
            swap(x);
            dx = -dx;
        }
        if (dy < 0) {
            swap(y);
            dy = -dy;
        }
        if (dz < 0) {
            swap(z);
            dz = -dz;
        }
        JsonObject json = new JsonObject();
        json.add("dx", new JsonPrimitive(dx));
        json.add("dy", new JsonPrimitive(dy));
        json.add("dz", new JsonPrimitive(dz));
        Map<BlockCondition, String> map = new HashMap<>();
        map.put(new BlockCondition(Material.AIR), ".");
        char c = 'a';
        StringBuilder matrix = new StringBuilder();
        for (int yy = y[0]; yy <= y[1]; yy++) {
            for (int xx = x[0]; xx <= x[1]; xx++) {
                for (int zz = z[0]; zz <= z[1]; zz++) {
                    Location l = new Location(l0.getWorld(), xx, yy, zz);
                    BlockCondition b = new BlockCondition(l.getBlock());
                    String cc = map.get(b);
                    if (cc == null) {
                        cc = "" + c++;
                        map.put(b, cc);
                    }
                    matrix.append(cc);
                }
            }
        }
        json.add("matrix", new JsonPrimitive(matrix.toString()));
        JsonObject mapped = new JsonObject();
        for (Map.Entry<BlockCondition, String> e: map.entrySet()) {
            mapped.add(e.getValue(), e.getKey().toJson());
        }
        json.add("map", mapped);
        return json.toString();
    }

}