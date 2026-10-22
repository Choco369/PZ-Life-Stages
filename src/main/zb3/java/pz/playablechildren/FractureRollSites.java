package pz.playablechildren;

import java.io.InputStream;
import java.util.*;
import net.bytebuddy.jar.asm.*;

/** Discover native fracture integer rolls; missing/ambiguous debug metadata fails closed.
 * The mixed Neck/Groin crash branch is additionally guarded by ElderFractureScope. */
public final class FractureRollSites {
    private record Site(String owner, String method, int line) {}
    private record Insn(int opcode, int line, String owner, String name, String desc, Label label) {}
    private static final Set<Site> SITES = discover();
    private FractureRollSites() {}

    public static boolean contains(StackWalker.StackFrame frame) {
        return SITES.contains(new Site(frame.getClassName(), frame.getMethodName(), frame.getLineNumber()));
    }

    private static Set<Site> discover() {
        Set<Site> result=new HashSet<>();
        read(result,"zombie.characters.IsoGameCharacter","handleLandingImpact","(Lzombie/characters/FallDamage;)V",2);
        read(result,"zombie.characters.IsoPlayer","applyDamageFromVehicleHit","(Lzombie/vehicles/BaseVehicle;FF)V",3);
        read(result,"zombie.vehicles.BaseVehicle","addRandomDamageFromCrash","(Lzombie/characters/IsoGameCharacter;F)V",1);
        return Set.copyOf(result);
    }

    private static boolean rand(Insn i) {
        return i.opcode()==Opcodes.INVOKESTATIC && "zombie/core/random/Rand".equals(i.owner())
            && "Next".equals(i.name()) && "(I)I".equals(i.desc());
    }

    private static void read(Set<Site> out,String owner,String method,String desc,int expected) {
        Set<Site> found=new HashSet<>();
        try (InputStream input=FractureRollSites.class.getClassLoader().getResourceAsStream(owner.replace('.','/')+".class")) {
            if (input==null) throw new IllegalStateException("Missing native class metadata");
            new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override public MethodVisitor visitMethod(int access,String name,String descriptor,String signature,String[] exceptions) {
                    if (!name.equals(method) || !descriptor.equals(desc)) return null;
                    return new MethodVisitor(Opcodes.ASM9) {
                        final List<Insn> code=new ArrayList<>();
                        int line=-1;
                        void add(int opcode) { code.add(new Insn(opcode,line,null,null,null,null)); }
                        @Override public void visitLineNumber(int value,Label label) { line=value; }
                        @Override public void visitLabel(Label label) { code.add(new Insn(-1,line,null,null,null,label)); }
                        @Override public void visitInsn(int opcode) { add(opcode); }
                        @Override public void visitIntInsn(int opcode,int operand) { add(opcode); }
                        @Override public void visitVarInsn(int opcode,int var) { add(opcode); }
                        @Override public void visitTypeInsn(int opcode,String type) { add(opcode); }
                        @Override public void visitFieldInsn(int opcode,String o,String n,String d) { add(opcode); }
                        @Override public void visitLdcInsn(Object value) { add(Opcodes.LDC); }
                        @Override public void visitIincInsn(int var,int increment) { add(Opcodes.IINC); }
                        @Override public void visitMethodInsn(int opcode,String o,String n,String d,boolean itf) { code.add(new Insn(opcode,line,o,n,d,null)); }
                        @Override public void visitJumpInsn(int opcode,Label label) { code.add(new Insn(opcode,line,null,null,null,label)); }
                        @Override public void visitEnd() {
                            Map<Integer,Integer> draws=new HashMap<>();
                            for (Insn i:code) if (rand(i)) draws.merge(i.line(),1,Integer::sum);
                            for (int p=0;p<code.size();p++) {
                                Insn draw=code.get(p);
                                if (!rand(draw) || draw.line()<0 || draws.get(draw.line())!=1) continue;
                                Insn branch=null;
                                for (int q=p+1;q<Math.min(code.size(),p+12);q++) {
                                    Insn i=code.get(q);
                                    if (i.opcode()==-1) continue;
                                    if (i.label()!=null) { branch=i; break; }
                                }
                                if (branch==null || (branch.opcode()!=Opcodes.IF_ICMPGE && branch.opcode()!=Opcodes.IF_ICMPGT && branch.opcode()!=Opcodes.IFNE)) continue;
                                boolean accepted=false;
                                for (int q=code.indexOf(branch)+1;q<code.size();q++) {
                                    Insn i=code.get(q);
                                    if (i.opcode()==-1 && i.label()==branch.label()) break;
                                    if ("zombie/characters/BodyDamage/BodyPart".equals(i.owner()) && "(F)V".equals(i.desc())
                                        && ("generateFracture".equals(i.name()) || "generateFractureNew".equals(i.name()))) { accepted=true; break; }
                                }
                                if (accepted) found.add(new Site(owner,method,draw.line()));
                            }
                        }
                    };
                }
            },ClassReader.SKIP_FRAMES);
            if (found.size()!=expected) throw new IllegalStateException("Expected "+expected+" unique fracture rolls, found "+found.size());
            out.addAll(found);
        } catch (Exception unavailable) {
            System.out.println("[PZLifeStages] ELDER fracture risk disabled for "+owner+"."+method+": "+unavailable.getMessage());
        }
    }
}
