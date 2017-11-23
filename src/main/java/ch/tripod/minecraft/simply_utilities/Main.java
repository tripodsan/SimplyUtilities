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
import org.bukkit.plugin.java.JavaPlugin;

/**
 * {@code Main}...
 */
public class Main extends JavaPlugin {

    private Lazers lazers;

    private Pads pads;

    private Infusion infusion;

    private Alchemy alchemy;

    private BlastFurnace furnce;

    private WorldGen gen;

    @Override
    public void onEnable() {
        super.onEnable();
        lazers = new Lazers();
        lazers.enable(this);

        pads = new Pads();
        pads.enable(this);

        infusion = new Infusion();
        infusion.enable(this);

        alchemy = new Alchemy();
        alchemy.enable(this);

        furnce = new BlastFurnace();
        furnce.enable(this);

        gen = new WorldGen();
        gen.enable(this);

        createRecipes();


        this.getLogger().info("Simply Utilities plugin enabled.");
        this.getLogger().info("I'm in the system now! WHEEEEEEEEEEEEEEE!");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (lazers != null) {
            lazers.disable();
            lazers = null;
        }
        if (pads != null) {
            pads.disable();
            pads = null;
        }
        if (infusion != null) {
            infusion.disable();
            infusion = null;
        }
        if (alchemy != null) {
            alchemy.disable();
            alchemy = null;
        }
        if (furnce != null) {
            furnce.disable();
            furnce = null;
        }
        if (gen != null) {
            gen.disable();
            gen = null;
        }
        this.getLogger().info("Simply Utilities plugin disabled.");
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