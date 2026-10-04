package dev.valorantcraft;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Credits + Kaufmenue (Kiste mit Waffen). Oeffnen mit /buy. */
public final class Shop {
    public static final int START_CREDITS = 800;
    public static final int KILL_REWARD = 200;

    private static final Map<UUID, Integer> CREDITS = new HashMap<>();

    // Reihenfolge = Slots 10, 12, 14, 16
    private static final String[] IDS = { "classic", "sheriff", "phantom", "vandal" };
    private static final String[] NAMES = { "Classic", "Sheriff", "Phantom", "Vandal" };
    private static final int[] PRICES = { 0, 800, 2900, 2900 };
    private static final int[] SLOTS = { 10, 12, 14, 16 };
    private static final int CREDITS_SLOT = 22;

    private Shop() {}

    public static int get(Player p) { return CREDITS.computeIfAbsent(p.getUUID(), k -> START_CREDITS); }
    public static void set(Player p, int value) { CREDITS.put(p.getUUID(), Math.max(0, value)); }
    public static void add(Player p, int value) { set(p, get(p) + value); }

    public static void open(ServerPlayer player) {
        SimpleContainer container = new SimpleContainer(27);
        refresh(container, player);
        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new ShopMenu(id, inv, container),
                Component.literal("Kaufphase")));
    }

    private static void refresh(Container c, Player player) {
        for (int i = 0; i < 27; i++) {
            ItemStack pane = new ItemStack(Items.BLACK_STAINED_GLASS_PANE);
            pane.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
            c.setItem(i, pane);
        }
        for (int i = 0; i < IDS.length; i++) {
            ItemStack st = new ItemStack(ValorantCraft.ITEMS.get(IDS[i]));
            st.set(DataComponents.CUSTOM_NAME, Component.literal(NAMES[i] + " - " + PRICES[i] + " Credits"));
            c.setItem(SLOTS[i], st);
        }
        ItemStack gold = new ItemStack(Items.GOLD_NUGGET);
        gold.set(DataComponents.CUSTOM_NAME, Component.literal("Credits: " + get(player)));
        c.setItem(CREDITS_SLOT, gold);
    }

    private static void msg(ServerPlayer p, String text) {
        p.connection.send(new ClientboundSetActionBarTextPacket(Component.literal(text)));
    }

    private static void tryBuy(ServerPlayer player, int index, Container container) {
        int price = PRICES[index];
        if (get(player) < price) {
            msg(player, "Nicht genug Credits! (" + price + " noetig)");
            return;
        }
        Item item = ValorantCraft.ITEMS.get(IDS[index]);
        if (!player.getInventory().add(new ItemStack(item))) {
            msg(player, "Inventar voll!");
            return;
        }
        add(player, -price);
        refresh(container, player);
        msg(player, NAMES[index] + " gekauft! Credits: " + get(player));
    }

    /** Menue, in dem man nur auf Waffen klicken (kaufen) kann, aber nichts herausnehmen. */
    private static final class ShopMenu extends ChestMenu {
        private final Container shop;

        ShopMenu(int id, Inventory inv, Container container) {
            super(MenuType.GENERIC_9x3, id, inv, container, 3);
            this.shop = container;
        }

        @Override
        public void clicked(int slotId, int button, ClickType clickType, Player player) {
            if (!(player instanceof ServerPlayer sp) || clickType != ClickType.PICKUP) return;
            for (int i = 0; i < SLOTS.length; i++) {
                if (slotId == SLOTS[i]) {
                    tryBuy(sp, i, shop);
                    return;
                }
            }
        }
    }
}
