package dev.valorantcraft;

import java.util.LinkedHashMap;
import java.util.Map;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public class ValorantCraft implements ModInitializer {
    public static final String MOD_ID = "valorantcraft";
    public static final Map<String, Item> ITEMS = new LinkedHashMap<>();

    @Override
    public void onInitialize() {
        for (Weapon weapon : Weapon.ALL) {
            Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, weapon.id());
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
            Item item = new WeaponItem(new Item.Properties().setId(key).stacksTo(1), weapon);
            Registry.register(BuiltInRegistries.ITEM, key, item);
            ITEMS.put(weapon.id(), item);
        }

        // Waffen im Kreativ-Inventar (Reiter "Kampf")
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(tab -> {
            for (Item item : ITEMS.values()) {
                tab.accept(item);
            }
        });

        // Befehle: /buy (Kaufmenue) und /credits
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("buy").executes(ctx -> {
                Shop.open(ctx.getSource().getPlayerOrException());
                return 1;
            }));
            dispatcher.register(Commands.literal("credits")
                    .executes(ctx -> {
                        ServerPlayer p = ctx.getSource().getPlayerOrException();
                        int c = Shop.get(p);
                        ctx.getSource().sendSuccess(() -> Component.literal("Credits: " + c), false);
                        return c;
                    })
                    .then(Commands.literal("reset").executes(ctx -> {
                        ServerPlayer p = ctx.getSource().getPlayerOrException();
                        Shop.set(p, Shop.START_CREDITS);
                        ctx.getSource().sendSuccess(() -> Component.literal("Credits zurueckgesetzt auf " + Shop.START_CREDITS), false);
                        return 1;
                    })));
        });
    }
}
