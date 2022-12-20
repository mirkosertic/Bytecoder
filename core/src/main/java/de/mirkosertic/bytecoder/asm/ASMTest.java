package de.mirkosertic.bytecoder.asm;

import de.mirkosertic.bytecoder.api.ClassLibProvider;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.BasicVerifier;
import org.objectweb.asm.tree.analysis.Frame;

import java.io.IOException;
import java.io.InputStream;

public class ASMTest {

    private static void loadFromStream(final String typeName, final InputStream is) throws IOException, AnalyzerException {
        final ClassReader reader = new ClassReader(is);
        final ClassNode classNode = new ClassNode();
        reader.accept(classNode, ClassReader.EXPAND_FRAMES);

        for (int i = 0; i < classNode.methods.size(); i++) {
            final MethodNode methodNode = classNode.methods.get(i);
            final ControlFlowAnalyzer<BasicValue> analyzer = new ControlFlowAnalyzer<BasicValue>(new BasicVerifier());
            Frame<BasicValue> frames[] = analyzer.analyze(classNode.name, methodNode);
            System.out.println(frames);
        }
    }

    private static void loadClass(final String typeName, final ClassLoader classLoader) throws IOException, ClassNotFoundException, AnalyzerException {
        final String theResourceName = typeName.replace(".", "/") + ".class";
        for (final ClassLibProvider theProvider : ClassLibProvider.availableProviders()) {
            final InputStream theStream = classLoader.getResourceAsStream(theProvider.getResourceBase() + "/" + theResourceName);
            if (theStream != null) {
                loadFromStream(typeName, theStream);
            }
        }
        final InputStream fromRoot = classLoader.getResourceAsStream(theResourceName);
        if (fromRoot != null) {
            loadFromStream(typeName, fromRoot);
            return;
        }
        throw new ClassNotFoundException(typeName);
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, AnalyzerException {
        loadClass(Testclass.class.getName(), ASMTest.class.getClassLoader());
    }
}
