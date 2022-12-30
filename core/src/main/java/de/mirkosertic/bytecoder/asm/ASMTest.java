/*
 * Copyright 2022 Mirko Sertic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.mirkosertic.bytecoder.asm;

import de.mirkosertic.bytecoder.api.ClassLibProvider;
import de.mirkosertic.bytecoder.asm.interpreter.Interpreter;
import de.mirkosertic.bytecoder.asm.optimizer.Optimizations;
import de.mirkosertic.bytecoder.asm.optimizer.Optimizer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ASMTest {

    private static void loadFromStream(final String typeName, final InputStream is) throws IOException {
        final ClassReader reader = new ClassReader(is);
        final ClassNode classNode = new ClassNode();
        reader.accept(classNode, ClassReader.EXPAND_FRAMES);

        for (int i = 0; i < classNode.methods.size(); i++) {
            final MethodNode methodNode = classNode.methods.get(i);
            final GraphParser p = new GraphParser(methodNode);
            final Graph g = p.graph();
            g.writeDebugTo(Files.newOutputStream(Paths.get("debug.dot")));

            final Optimizer o = Optimizations.DEFAULT;
            while (o.optimize(g)) {
            }

            g.writeDebugTo(Files.newOutputStream(Paths.get("debug_optimized.dot")));

            final Interpreter interpreter = new Interpreter("Temp",  g);
        }
    }

    private static void loadClass(final String typeName, final ClassLoader classLoader) throws IOException, ClassNotFoundException {
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

    public static void main(final String[] args) throws IOException, ClassNotFoundException {
        loadClass(Testclass.class.getName(), ASMTest.class.getClassLoader());
    }
}
