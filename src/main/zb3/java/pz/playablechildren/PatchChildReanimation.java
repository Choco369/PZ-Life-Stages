package pz.playablechildren;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import me.zed_0xff.zombie_buddy.annotations.Patch;
import me.zed_0xff.zombie_buddy.annotations.Patch.Argument;
import me.zed_0xff.zombie_buddy.annotations.Patch.OnEnter;
import me.zed_0xff.zombie_buddy.annotations.Patch.OnExit;
import me.zed_0xff.zombie_buddy.annotations.Patch.Return;
import me.zed_0xff.zombie_buddy.annotations.Patch.This;
import zombie.SharedDescriptors;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.iso.objects.IsoDeadBody;

/** Preserves an exact life-stage field across corpse -> reanimated player zombie. */
public final class PatchChildReanimation {
    public static final int STAGE_MASK = StageBits.MASK;
    public static final int TEEN_BITS = StageBits.TEEN;
    public static final int CHILD_BITS = StageBits.CHILD;
    public static final int ELDER_BITS = StageBits.ELDER;

    private static final ThreadLocal<Deque<LifeStage>> REANIMATING_STAGES =
        ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Deque<LifeStage>> CREATING_PLAYER_ZOMBIE_STAGES =
        ThreadLocal.withInitial(ArrayDeque::new);

    private PatchChildReanimation() {}

    public static void pushReanimatingStage(LifeStage stage) {
        REANIMATING_STAGES.get().push(stage == null ? LifeStage.ADULT : stage);
    }
    public static void popReanimatingStage() {
        Deque<LifeStage> stack = REANIMATING_STAGES.get();
        if (!stack.isEmpty()) stack.pop();
        if (stack.isEmpty()) REANIMATING_STAGES.remove();
    }
    public static void pushCreatingStage(LifeStage stage) {
        CREATING_PLAYER_ZOMBIE_STAGES.get().push(stage == null ? LifeStage.ADULT : stage);
    }
    public static void popCreatingStage() {
        Deque<LifeStage> stack = CREATING_PLAYER_ZOMBIE_STAGES.get();
        if (!stack.isEmpty()) stack.pop();
        if (stack.isEmpty()) CREATING_PLAYER_ZOMBIE_STAGES.remove();
    }
    public static LifeStage peek(ThreadLocal<Deque<LifeStage>> local) {
        Deque<LifeStage> stack = local.get();
        return stack.isEmpty() ? LifeStage.ADULT : stack.peek();
    }
    public static LifeStage currentReanimatingStage() {
        return peek(REANIMATING_STAGES);
    }
    public static LifeStage currentCreatingStage() {
        return peek(CREATING_PLAYER_ZOMBIE_STAGES);
    }
    public static LifeStage activeReanimationStage() {
        LifeStage creating = currentCreatingStage();
        return creating != LifeStage.ADULT ? creating : currentReanimatingStage();
    }
    public static int addStageBit(int id, LifeStage stage) { return StageBits.withStage(id, stage); }
    public static LifeStage stageFromBits(int id) { return StageBits.stageOf(id); }
    public static int stripStageBits(int id) { return StageBits.strip(id); }
    public static int copyStageBits(int cleanResult, int markedSource) { return StageBits.copy(cleanResult, markedSource); }
    public static void markCorpse(IsoDeadBody body, LifeStage stage) {
        if (body == null || stage == LifeStage.ADULT) return;
        Integer old = (Integer)ReflectionAccess.tryGet(body, "persistentOutfitId", Integer.valueOf(0));
        int id = old == null ? 0 : old.intValue();
        ReflectionAccess.trySet(body, "persistentOutfitId", Integer.valueOf(addStageBit(id, stage)));
    }
    public static void markStoredSharedDescriptor(IsoZombie zombie, LifeStage stage) {
        if (zombie == null || zombie.getDescriptor() == null || stage == LifeStage.ADULT) return;
        try {
            int index = zombie.getDescriptor().getID() - 1000;
            if (index < 0) return;
            SharedDescriptors.Descriptor[] array = SharedDescriptors.getPlayerZombieDescriptors();
            if (array == null || index >= array.length || array[index] == null) return;
            Object stored = array[index];
            Field outfitField = ReflectionAccess.field(stored.getClass(), "persistentOutfitId");
            if (outfitField != null) outfitField.setInt(stored, addStageBit(outfitField.getInt(stored), stage));
        } catch (Throwable ignored) {}
    }

    @Patch(className="zombie.iso.objects.IsoDeadBody", methodName="reanimate", strictMatch=false)
    public static final class Reanimate {
        @OnEnter public static void enter(@This IsoDeadBody body) {
            LifeStage stage = LifeStageResolver.stageOf(body);
            PatchChildReanimation.pushReanimatingStage(stage);
            if (stage != LifeStage.ADULT) PatchChildReanimation.markCorpse(body, stage);
        }
        @OnExit(onThrowable=Throwable.class)
        public static void exit(@This IsoDeadBody body, @Return IsoGameCharacter result) {
            try {
                LifeStage stage = PatchChildReanimation.currentReanimatingStage();
                if (result instanceof IsoZombie zombie) {
                    SavedZombieStages.remember(zombie, stage);
                    SavedZombieStages.restore(zombie);
                }
                if (stage != LifeStage.ADULT && result instanceof IsoZombie zombie) {
                    zombie.setPersistentOutfitID(PatchChildReanimation.addStageBit(zombie.getPersistentOutfitID(), stage));
                    if (stage == LifeStage.CHILD && !zombie.isFemale() && zombie.getHumanVisual() != null) {
                        zombie.getHumanVisual().setBeardModel(null);
                    }
                }
            } finally {
                PatchChildReanimation.popReanimatingStage();
            }
        }
    }

    @Patch(className="zombie.characters.IsoZombie", methodName="save", strictMatch=true)
    public static final class ZombieSave {
        @OnEnter public static void enter(@This IsoZombie zombie, @Argument(0) ByteBuffer output,
                @Argument(1) boolean debug) {
            if (zombie.isReanimatedPlayer()) SavedZombieStages.remember(zombie, LifeStageResolver.stageOf(zombie));
        }
    }

    @Patch(className="zombie.characters.IsoZombie", methodName="load", strictMatch=true)
    public static final class ZombieLoad {
        @OnExit public static void exit(@This IsoZombie zombie, @Argument(0) ByteBuffer input,
                @Argument(1) int worldVersion, @Argument(2) boolean debug) {
            SavedZombieStages.restore(zombie);
        }
    }

    @Patch(className="zombie.SharedDescriptors", methodName="createPlayerZombieDescriptor", strictMatch=true)
    public static final class SharedDescriptor {
        @OnEnter public static void enter(@Argument(0) IsoZombie zombie) {
            PatchChildReanimation.pushCreatingStage(LifeStageResolver.stageOf(zombie));
        }
        @OnExit(onThrowable=Throwable.class)
        public static void exit(@Argument(0) IsoZombie zombie) {
            try {
                LifeStage stage = PatchChildReanimation.currentCreatingStage();
                if (stage != LifeStage.ADULT && zombie != null) {
                    zombie.setPersistentOutfitID(PatchChildReanimation.addStageBit(zombie.getPersistentOutfitID(), stage));
                    PatchChildReanimation.markStoredSharedDescriptor(zombie, stage);
                }
            } finally {
                PatchChildReanimation.popCreatingStage();
            }
        }
    }

    @Patch(className="zombie.SharedDescriptors$Descriptor", methodName="save", strictMatch=true)
    public static final class SharedDescriptorSave {
        @OnEnter public static void enter(@This Object descriptor, @Argument(0) Object output) {
            LifeStage stage = PatchChildReanimation.activeReanimationStage();
            if (descriptor == null || stage == LifeStage.ADULT) return;
            Integer old = (Integer)ReflectionAccess.tryGet(descriptor, "persistentOutfitId", Integer.valueOf(0));
            int id = old == null ? 0 : old.intValue();
            ReflectionAccess.trySet(descriptor, "persistentOutfitId", Integer.valueOf(PatchChildReanimation.addStageBit(id, stage)));
        }
    }
}
