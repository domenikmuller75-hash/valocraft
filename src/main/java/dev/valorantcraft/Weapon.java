package dev.valorantcraft;

/**
 * Waffenwerte. Schaden = Valorant-Schaden * 0.2 (100 HP in Valorant = 20 HP in Minecraft).
 * Zeiten in Ticks (20 Ticks = 1 Sekunde). Spread in Block-Einheiten pro Block Distanz.
 */
public record Weapon(String id, float bodyDamage, float headDamage, int magSize,
                     int fireDelayTicks, int reloadTicks, double spread, double range) {

    public static final Weapon CLASSIC = new Weapon("classic", 5.2f, 15.6f, 12, 3, 35, 0.015, 60);
    public static final Weapon SHERIFF = new Weapon("sheriff", 11.0f, 31.8f, 6, 5, 45, 0.008, 90);
    public static final Weapon PHANTOM = new Weapon("phantom", 7.8f, 31.2f, 30, 2, 45, 0.012, 100);
    public static final Weapon VANDAL  = new Weapon("vandal", 8.0f, 32.0f, 25, 2, 50, 0.010, 120);

    public static final Weapon[] ALL = { CLASSIC, SHERIFF, PHANTOM, VANDAL };
}
