package me.matsubara.realisticvillagers.event;

import me.matsubara.realisticvillagers.entity.IVillagerNPC;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class VillagerRemoveEvent extends VillagerEvent {

    private final RemovalReason reason;

    private static final HandlerList handlers = new HandlerList();

    public VillagerRemoveEvent(IVillagerNPC npc, RemovalReason reason) {
        super(npc);
        this.reason = reason;
    }

    public RemovalReason getReason() {
        return reason;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    @SuppressWarnings("unused")
    public static @NotNull HandlerList getHandlerList() {
        return handlers;
    }

    @SuppressWarnings("unused")
    public enum RemovalReason {
        KILLED(true, false),
        DISCARDED(true, false),
        UNLOADED_TO_CHUNK(false, true),
        UNLOADED_WITH_PLAYER(false, false),
        CHANGED_DIMENSION(false, false);

        private final boolean destroy;
        private final boolean save;

        RemovalReason(boolean destroy, boolean save) {
            this.destroy = destroy;
            this.save = save;
        }

        public boolean shouldDestroy() {
            return destroy;
        }

        public boolean shouldSave() {
            return save;
        }
    }
}