package de.mirkosertic.bytecoder.asm;

import de.mirkosertic.bytecoder.unittest.Slf4JLogger;
import org.junit.Test;
import org.objectweb.asm.Type;

public class IRTest {

    @Test
    public void testLoad() {

        final CompilationUnit compilationUnit = new CompilationUnit(new Slf4JLogger());
        compilationUnit.enqueue(new ResolveStaticMethodInvocationCommand(Type.getType(Testclass.class),
            "main", Type.getMethodType(Type.VOID_TYPE, Type.getType(String[].class)).getDescriptor()));
        compilationUnit.process();

        System.out.println(compilationUnit);
/*
        for (final MethodNode method : classNode.methods) {
            System.out.println(method.name + " " + method.desc);
            final ControlFlowAnalyzer a = new ControlFlowAnalyzer();
            a.analyze("Testclass", method);

            final List<Frame<BasicValue>> frames = Arrays.asList(a.getFrames());
            for (final Frame<BasicValue> frame : frames) {
                if (frame != null) {
                    final FrameWithControlFlow frameWithControlFlow = (FrameWithControlFlow) frame;
                    System.out.println("Frame # " + frames.indexOf(frame));
                    for (final FrameWithControlFlow succ : frameWithControlFlow.successors) {
                        System.out.println(" -> Frame #" + frames.indexOf(succ));
                    }
                    for (int i = 0; i < frame.getLocals(); i++) {
                        System.out.println("   Local " + i + " : " + frame.getLocal(i));
                    }
                    System.out.println(method.instructions.get(frames.indexOf(frame)));
                }
            }
        }*/
    }
}
