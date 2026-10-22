package pz.playablechildren;

import java.util.ArrayDeque;
import java.util.Deque;
import zombie.characters.IsoGameCharacter;
import zombie.characters.BodyDamage.BodyPart;
import zombie.characters.BodyDamage.BodyPartType;

/** Changes fracture probability, never impact damage, injury eligibility, or fracture duration. */
public final class ElderFractureScope {
    private static final class Frame {
        final String owner,method;
        final float factor;
        final IsoGameCharacter character;
        BodyPart crashPart;
        Frame(String owner,String method,float factor,IsoGameCharacter character) {
            this.owner=owner; this.method=method; this.factor=factor; this.character=character;
        }
    }
    private static final ThreadLocal<Deque<Frame>> SCOPES=new ThreadLocal<>();
    private static final StackWalker CALLERS=StackWalker.getInstance();
    private ElderFractureScope() {}

    public static void enter(IsoGameCharacter character,String owner,String method) {
        float factor=1f;
        if (character!=null && !character.isGodMod() && zombie.SandboxOptions.instance.boneFracture.getValue()
            && AgeGameplayApi.stageOf(character)==LifeStage.ELDER)
            factor=AgeGameplayApi.factor(character,AgeProfile.Effect.FRACTURE_CHANCE);
        Deque<Frame> stack=SCOPES.get();
        if (stack==null) { stack=new ArrayDeque<>(); SCOPES.set(stack); }
        stack.push(new Frame(owner,method,factor,character));
    }

    public static void exit() {
        Deque<Frame> stack=SCOPES.get();
        if (stack==null) return;
        stack.pop();
        if (stack.isEmpty()) SCOPES.remove();
    }

    public static int rollBound(int vanilla) {
        Deque<Frame> stack=SCOPES.get();
        if (stack==null || stack.isEmpty() || vanilla<=1) return vanilla;
        Frame scope=stack.peek();
        if (!Float.isFinite(scope.factor) || scope.factor<=1f) return vanilla;
        // The crash roll also causes deep wounds for Neck/Groin. Never increase that chance.
        if (scope.method.equals("addRandomDamageFromCrash") &&
            (scope.crashPart==null || scope.crashPart.getType()==BodyPartType.Neck || scope.crashPart.getType()==BodyPartType.Groin)) return vanilla;
        // Advice is inlined in Rand.Next. Its immediate caller must be the exact accident,
        // and its debug line must identify a fracture branch in the native bytecode.
        boolean allowed=CALLERS.walk(frames->frames.skip(2).findFirst().map(f->
            f.getClassName().equals(scope.owner) && f.getMethodName().equals(scope.method)
                && FractureRollSites.contains(f)).orElse(false));
        return allowed ? Math.max(1,Math.round(vanilla/scope.factor)) : vanilla;
    }

    /** Observes the native crash's AddDamage call without changing damage or selecting a part. */
    public static void rememberCrashPart(BodyPart part) {
        Deque<Frame> stack=SCOPES.get();
        if (stack==null || stack.isEmpty()) return;
        Frame scope=stack.peek();
        if (scope.factor<=1f || !scope.method.equals("addRandomDamageFromCrash")) return;
        boolean direct=CALLERS.walk(frames->frames.skip(2).findFirst().map(f->
            f.getClassName().equals(scope.owner) && f.getMethodName().equals(scope.method)).orElse(false));
        if (direct) scope.crashPart=part!=null && part.getParentChar()==scope.character ? part : null;
    }
}
