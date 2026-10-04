package dev.valorantcraft;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public class ValorantCraft implements ModInitializer {
    public static final String MOD_ID = "valorantcraft";

    @Override
    public void onInitialize() {
        for (Weapon weapon : Weapon.ALL) {
            Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, weapon.id());
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
            Item item = new WeaponItem(new Item.Properties().setId(key).stacksTo(1), weapon);
            Registry.register(BuiltInRegistries.ITEM, key, item);
        }
    }
}
