package local.jmquicktp;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/**
 * Adds guarded calls to the fullscreen map only: right-click menu opening, menu drawing,
 * Escape handling and menu cleanup. The original JourneyMap jar and other screens stay intact.
 */
public final class MapTransformer implements IClassTransformer {
    public static final String TARGET = "journeymap.client.ui.fullscreen.Fullscreen";
    private static final String OWNER = "local/jmquicktp/MapMenu";

    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !(TARGET.equals(name) || TARGET.equals(transformedName))) return bytes;
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        for (Object entry : node.methods) {
            MethodNode method = (MethodNode)entry;
            for (AbstractInsnNode insn : method.instructions.toArray()) {
                if (insn instanceof MethodInsnNode && OWNER.equals(((MethodInsnNode)insn).owner)) return bytes;
            }
        }
        int hooks = 0;
        for (Object entry : node.methods) {
            MethodNode method = (MethodNode)entry;
            if ("(III)V".equals(method.desc) && matches(method, "func_73864_a", "mouseClicked")) {
                // if (MapMenu.mouseClicked(this, mouseX, mouseY, mouseButton)) return;
                InsnList prefix = new InsnList();
                LabelNode original = new LabelNode();
                prefix.add(new VarInsnNode(Opcodes.ALOAD, 0));
                prefix.add(new VarInsnNode(Opcodes.ILOAD, 1));
                prefix.add(new VarInsnNode(Opcodes.ILOAD, 2));
                prefix.add(new VarInsnNode(Opcodes.ILOAD, 3));
                prefix.add(new MethodInsnNode(Opcodes.INVOKESTATIC, OWNER, "mouseClicked",
                    "(Ljava/lang/Object;III)Z", false));
                prefix.add(new JumpInsnNode(Opcodes.IFEQ, original));
                prefix.add(new InsnNode(Opcodes.RETURN));
                prefix.add(original);
                // Original locals at entry; no hierarchy loading or frame recomputation required.
                prefix.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
                method.instructions.insert(prefix);
                hooks++;
            } else if ("(CI)V".equals(method.desc) && matches(method, "func_73869_a", "keyTyped")) {
                // if (MapMenu.keyTyped(this, keyCode)) return;
                InsnList prefix = new InsnList();
                LabelNode original = new LabelNode();
                prefix.add(new VarInsnNode(Opcodes.ALOAD, 0));
                prefix.add(new VarInsnNode(Opcodes.ILOAD, 2));
                prefix.add(new MethodInsnNode(Opcodes.INVOKESTATIC, OWNER, "keyTyped", "(Ljava/lang/Object;I)Z", false));
                prefix.add(new JumpInsnNode(Opcodes.IFEQ, original));
                prefix.add(new InsnNode(Opcodes.RETURN));
                prefix.add(original);
                prefix.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
                method.instructions.insert(prefix);
                hooks++;
            } else if ("(IIF)V".equals(method.desc) && matches(method, "func_73863_a", "drawScreen")) {
                // MapMenu.draw(this); on every exit, so the menu stays above map and buttons.
                int inserted = 0;
                for (AbstractInsnNode insn : method.instructions.toArray()) {
                    if (insn.getOpcode() == Opcodes.RETURN) {
                        InsnList suffix = new InsnList();
                        suffix.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        suffix.add(new MethodInsnNode(Opcodes.INVOKESTATIC, OWNER, "draw", "(Ljava/lang/Object;)V", false));
                        method.instructions.insertBefore(insn, suffix);
                        inserted++;
                    }
                }
                if (inserted == 0) throw new IllegalStateException("Fullscreen.drawScreen has no RETURN: " + inserted);
                hooks++;
            } else if ("()V".equals(method.desc) && matches(method, "func_146281_b", "onGuiClosed")) {
                // MapMenu.close(this);
                InsnList prefix = new InsnList();
                prefix.add(new VarInsnNode(Opcodes.ALOAD, 0));
                prefix.add(new MethodInsnNode(Opcodes.INVOKESTATIC, OWNER, "close", "(Ljava/lang/Object;)V", false));
                method.instructions.insert(prefix);
                hooks++;
            }
        }
        if (hooks != 4) throw new IllegalStateException("Unexpected JourneyMap fullscreen layout: " + hooks + " hooks");
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        System.out.println("[JMQuickTP] Installed fullscreen right-click teleport menu");
        return writer.toByteArray();
    }

    private static boolean matches(MethodNode method, String srg, String mcp) {
        return srg.equals(method.name) || mcp.equals(method.name);
    }
}
