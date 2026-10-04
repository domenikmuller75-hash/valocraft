package dev.valorantcraft;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Rechtsklick = schiessen, Shift + Rechtsklick = nachladen.
 * Hitscan: der Schuss trifft sofort, wie in Valorant.
 */
public class WeaponItem extends Item {

    private static final class State {
        int ammo;
        long nextShot;
        long reloadDone = -1;
        State(int ammo) { this.ammo = ammo; }
    }

    // Prototyp: Munition liegt im Speicher (pro Spieler + Waffe), nicht im Item.
    private static final Map<String, State> STATES = new HashMap<>();

    private static final ResourceKey<DamageType> BULLET = ResourceKey.create(Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath("valorantcraft", "bullet"));

    private final Weapon weapon;

    private static void actionBar(ServerPlayer player, String text) {
        player.connection.send(new ClientboundSetActionBarTextPacket(Component.literal(text)));
    }

    public WeaponItem(Properties properties, Weapon weapon) {
        super(properties);
        this.weapon = weapon;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer sp)) {
            return InteractionResult.SUCCESS;
        }

        long now = serverLevel.getGameTime();
        State s = STATES.computeIfAbsent(player.getUUID() + ":" + weapon.id(), k -> new State(weapon.magSize()));

        // Laufendes Nachladen pruefen
        if (s.reloadDone >= 0) {
            if (now >= s.reloadDone) {
                s.ammo = weapon.magSize();
                s.reloadDone = -1;
            } else {
                actionBar(sp, "Nachladen...");
                return InteractionResult.SUCCESS;
            }
        }

        // Nachladen starten (Shift + Rechtsklick oder Magazin leer)
        if (player.isShiftKeyDown() || s.ammo <= 0) {
            if (s.ammo < weapon.magSize()) {
                s.reloadDone = now + weapon.reloadTicks();
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.CROSSBOW_LOADING_MIDDLE.value(), SoundSource.PLAYERS, 1.0f, 1.0f);
                actionBar(sp, "Nachladen...");
            }
            return InteractionResult.SUCCESS;
        }

        if (now < s.nextShot) {
            return InteractionResult.SUCCESS;
        }
        s.nextShot = now + weapon.fireDelayTicks();
        s.ammo--;

        shoot(serverLevel, sp);
        actionBar(sp, weapon.id().toUpperCase() + "  " + s.ammo + " / " + weapon.magSize());
        return InteractionResult.SUCCESS;
    }

    private void shoot(ServerLevel level, ServerPlayer player) {
        // Streuung: ruhig stehen = genau, laufen/springen = ungenau (wie in Valorant)
        double spread = weapon.spread();
        if (!player.onGround()) spread *= 6.0;
        else if (player.getDeltaMovement().horizontalDistanceSqr() > 0.0025) spread *= 3.0;
        else if (player.isCrouching()) spread *= 0.6;

        RandomSource r = player.getRandom();
        Vec3 from = player.getEyePosition();
        Vec3 dir = player.getLookAngle()
                .add(r.nextGaussian() * spread, r.nextGaussian() * spread, r.nextGaussian() * spread)
                .normalize();
        Vec3 to = from.add(dir.scale(weapon.range()));

        // Wand zwischen Schuetze und Ziel?
        BlockHitResult blockHit = level.clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double bestDist = blockHit.getType() == HitResult.Type.MISS
                ? weapon.range() * weapon.range()
                : blockHit.getLocation().distanceToSqr(from);
        Vec3 endPoint = blockHit.getType() == HitResult.Type.MISS ? to : blockHit.getLocation();

        // Naechstes lebendes Ziel auf der Schusslinie
        Entity target = null;
        Vec3 targetPos = null;
        AABB search = player.getBoundingBox().expandTowards(dir.scale(weapon.range())).inflate(1.0);
        for (Entity e : level.getEntities(player, search,
                en -> en instanceof LivingEntity && en.isAlive() && !en.isSpectator())) {
            AABB box = e.getBoundingBox().inflate(e.getPickRadius());
            Optional<Vec3> hit = box.clip(from, to);
            if (hit.isPresent()) {
                double d = from.distanceToSqr(hit.get());
                if (d < bestDist) {
                    bestDist = d;
                    target = e;
                    targetPos = hit.get();
                }
            }
        }
        if (target != null) endPoint = targetPos;

        // Schuss-Sound und Tracer
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1.2f, 1.7f);
        Vec3 step = endPoint.subtract(from);
        int points = (int) Math.min(40, step.length() * 1.5);
        for (int i = 2; i < points; i++) {
            Vec3 p = from.add(step.scale(i / (double) points));
            level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }

        if (target instanceof LivingEntity living) {
            boolean headshot = targetPos.y >= living.getEyeY() - 0.25;
            float damage = headshot ? weapon.headDamage() : weapon.bodyDamage();
            // Eigener Schadenstyp "bullet" umgeht Minecrafts Treffer-Cooldown (siehe data/-Ordner)
            living.hurtServer(level, level.damageSources().source(BULLET, player), damage);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    headshot ? SoundEvents.PLAYER_LEVELUP : SoundEvents.ARROW_HIT_PLAYER,
                    SoundSource.PLAYERS, 0.8f, headshot ? 2.0f : 1.0f);
        }
    }
}
