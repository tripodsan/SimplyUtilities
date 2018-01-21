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

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedList;
import java.util.List;

/**
 * {@code Main}...
 */
public class Main extends JavaPlugin {

    private WorldGen gen;

    private List<PluginUtility> utils = new LinkedList<>();

    @Override
    public void onEnable() {
        super.onEnable();
        addUtil(new Lazers());
        addUtil(new Pads());
        addUtil(new Infusion());
        addUtil(new Alchemy());
        addUtil(new BlastFurnace());
        addUtil(gen = new WorldGen());
        addUtil(new Placers());
        addUtil(new Breakers());
        addUtil(new AdvancedEChest());
        addUtil(new EMC());

        createRecipes();

        this.getLogger().info("Simply Utilities plugin enabled.");
        this.getLogger().info("I'm in the system now! WHEEEEEEEEEEEEEEE!");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        for (PluginUtility util: utils) {
            util.disable();
        }
        utils.clear();
        this.getLogger().info("Simply Utilities plugin disabled.");
    }

    private void addUtil(PluginUtility util) {
        util.enable(this);
        utils.add(util);
    }

    private void createRecipes() {
        {
            ItemStack sticks = new ItemStack(Material.STICK, 16);
            ShapelessRecipe recp = new ShapelessRecipe(new NamespacedKey(this, "sticks"), sticks);
            recp.addIngredient(2, Material.LOG);
            this.getServer().addRecipe(recp);
            recp = new ShapelessRecipe(new NamespacedKey(this, "sticks1"), sticks);
            recp.addIngredient(2, Material.LOG_2);
            this.getServer().addRecipe(recp);
            recp = new ShapelessRecipe(new NamespacedKey(this, "sticks2"), sticks);
            recp.addIngredient(1, Material.LOG);
            recp.addIngredient(1, Material.LOG_2);
            this.getServer().addRecipe(recp);
        }
    }

    void onResetChunks(Player p) {
        gen.resetChunks(p);
    }

}