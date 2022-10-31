package me.matsubara.realisticvillagers.entity.v1_18_r2.villager.ai.behaviour.core;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import me.matsubara.realisticvillagers.entity.v1_18_r2.villager.VillagerNPC;
import me.matsubara.realisticvillagers.files.Config;
import me.matsubara.realisticvillagers.util.EntityHead;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;

import java.util.Optional;

public class VillagerPanicTrigger extends Behavior<Villager> {

    private static final ImmutableSet<Item> HALLOWEEN_MASKS = ImmutableSet.of(
            Items.DRAGON_HEAD,
            Items.WITHER_SKELETON_SKULL,
            Items.ZOMBIE_HEAD,
            Items.SKELETON_SKULL,
            Items.CREEPER_HEAD);

    public VillagerPanicTrigger() {
        super(ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT));
    }

    @Override
    public boolean canStillUse(ServerLevel level, Villager villager, long time) {
        return shouldPanic(villager) && (isHurt(villager) || hasHostile(villager));
    }

    @Override
    public void start(ServerLevel level, Villager villager, long time) {
        Brain<Villager> brain = villager.getBrain();

        LivingEntity target = getTarget(villager);
        if (target == null || target instanceof Villager) return;

        if (target instanceof ServerPlayer player) {
            if (player.isCreative()) return;
            if (villager instanceof VillagerNPC npc && npc.isPartner(player.getUUID())) return;

            boolean atRaid = level.getRaidAt(villager.blockPosition()) != null;
            if (!Config.VILLAGER_ATTACK_PLAYER_DURING_RAID.asBool() && atRaid) return;
        }

        // Use the same condition as canStillUse(), but the name doesn't mean anything.
        if (canStillUse(level, villager, time)) {
            handleNormalReaction(brain);
        } else if (!shouldPanic(villager) && (isHurt(villager) || hasHostile(villager))) {
            handleFightReaction(brain, target);
        }
    }

    @Override
    public void tick(ServerLevel level, Villager villager, long time) {
        if (time % 100L == 0L) villager.spawnGolemIfNeeded(level, time, 3);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private LivingEntity getTarget(Villager villager) {
        Brain<Villager> brain = villager.getBrain();

        if (hasHostile(villager)) {
            LivingEntity direct = brain.getMemory(MemoryModuleType.NEAREST_HOSTILE).get();

            if (direct instanceof ServerPlayer player && villager instanceof VillagerNPC npc) {
                if (ignorePlayer(npc, player)) return null;
            }

            return direct;
        }

        if (!isHurt(villager)) return null;

        Entity direct = villager.getBrain().getMemory(MemoryModuleType.HURT_BY).get().getEntity();
        if (direct instanceof Projectile projectile && projectile.getOwner() != null) {
            return (LivingEntity) projectile.getOwner();
        } else if (direct instanceof LivingEntity living) {
            return living;
        }

        return null;
    }

    private void handleNormalReaction(Brain<Villager> brain) {
        if (!brain.isActive(Activity.PANIC)) stopWhatWasDoing(brain);
        brain.setActiveActivityIfPossible(Activity.PANIC);
    }

    private void handleFightReaction(Brain<Villager> brain, LivingEntity target) {
        if (!brain.isActive(Activity.FIGHT)) stopWhatWasDoing(brain);
        brain.setMemory(MemoryModuleType.ATTACK_TARGET, target);
        brain.setDefaultActivity(Activity.FIGHT);
        brain.setActiveActivityIfPossible(Activity.FIGHT);
    }

    private static void stopWhatWasDoing(Brain<Villager> brain) {
        brain.eraseMemory(MemoryModuleType.PATH);
        brain.eraseMemory(MemoryModuleType.WALK_TARGET);
        brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
        brain.eraseMemory(MemoryModuleType.BREED_TARGET);
        brain.eraseMemory(MemoryModuleType.INTERACTION_TARGET);
    }

    private boolean shouldPanic(Villager villager) {
        return !(villager instanceof VillagerNPC npc) || !npc.canAttack();
    }

    private boolean hasHostile(LivingEntity entity) {
        return entity.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_HOSTILE);
    }

    private boolean isHurt(LivingEntity entity) {
        return entity.getBrain().hasMemoryValue(MemoryModuleType.HURT_BY);
    }

    public static boolean ignorePlayer(VillagerNPC npc, Player player) {
        return !isWearingMonsterHead(player)
                || !Config.ATTACK_PLAYER_WEARING_MONSTER_SKULL.asBool()
                || !getTypeBySkullType(player.getItemBySlot(EquipmentSlot.HEAD))
                .map(entityType -> npc.getTargetEntities().contains(entityType)).orElse(false);
    }

    private static Optional<EntityType<?>> getTypeBySkullType(ItemStack item) {
        for (EntityHead skull : EntityHead.values()) {
            if (CraftItemStack.asNMSCopy(skull.getHead()).is(item.getItem())) {
                return EntityType.byString(skull.name().toLowerCase());
            }
        }
        return Optional.empty();
    }

    private static boolean isWearingMonsterHead(LivingEntity entity) {
        ItemStack current = entity.getItemBySlot(EquipmentSlot.HEAD);
        for (Item head : HALLOWEEN_MASKS) {
            if (current.is(head)) return true;
        }
        return false;
    }
}