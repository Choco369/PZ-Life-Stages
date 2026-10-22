package pz.playablechildren;

import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.inventory.types.HandWeapon;

public final class ChildCombatPolicy {
    private static final StackWalker CALLERS = StackWalker.getInstance();

    private ChildCombatPolicy() {}

    public static float shoveFactor(IsoGameCharacter attacker, IsoGameCharacter target) {
        if (!(attacker instanceof IsoPlayer player) || !player.isDoShove() || player.isShoveStompAnim()
            || !(target instanceof IsoZombie) || target.isOnFloor()
            || AgeGameplayApi.stageOf(attacker) != LifeStage.CHILD
            || !AgeGameplayApi.effectsEnabled(LifeStage.CHILD)) return 1.0f;
        float progress = AgeGameplayApi.progressOf(attacker);
        if (!Float.isFinite(progress)) progress = .5f;
        return .75f + .15f * Math.clamp(progress, 0.0f, 1.0f);
    }

    public static int shoveChance(int vanilla, IsoPlayer attacker, IsoGameCharacter target) {
        if (vanilla <= 0) return vanilla;
        return Math.round(vanilla * shoveFactor(attacker, target));
    }

    public static void scaleShoveForce(IsoGameCharacter target, IsoGameCharacter attacker,
                                       HandWeapon weapon) {
        if (weapon == null || weapon.isRanged() || weapon.isExplosive()) return;
        float factor = shoveFactor(attacker, target);
        float force = target.getHitForce();
        if (factor != 1.0f && Float.isFinite(force) && force > 0.0f)
            target.setHitForce(force * factor);
    }

    public static int dragDownCount(IsoGameCharacter character, int vanilla) {
        if (vanilla < 2 || vanilla == Integer.MAX_VALUE
            || AgeGameplayApi.stageOf(character) != LifeStage.CHILD
            || !AgeGameplayApi.effectsEnabled(LifeStage.CHILD)) return vanilla;
        boolean dragDownRead = CALLERS.walk(frames -> frames
            .dropWhile(frame -> frame.getClassName().equals(ChildCombatPolicy.class.getName()))
            .skip(1)
            .findFirst()
            .map(frame -> frame.getClassName().equals("zombie.characters.BodyDamage.BodyDamage")
                && frame.getMethodName().equals("AddRandomDamageFromZombie"))
            .orElse(false));
        return dragDownRead ? vanilla + 1 : vanilla;
    }
}
