/*
 * Copyright 2023 Mirko Sertic
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

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompileUnit {

    private final ClassLoader classLoader;

    private final Map<Type, ResolvedClass> resolvedClasses;

    public CompileUnit(final ClassLoader classLoader) {
        this.classLoader = classLoader;
        this.resolvedClasses = new HashMap<>();
    }

    public ResolvedClass resolveClass(final Type type, final AnalysisStack analysisStack) {
        return resolvedClasses.computeIfAbsent(type, key -> {
            final String theResourceName = key.getClassName().replace(".", "/") + ".class";
            for (final ClassLibProvider theProvider : ClassLibProvider.availableProviders()) {
                final InputStream is = theProvider.getClass().getClassLoader().getResourceAsStream(theProvider.getResourceBase() + "/" + theResourceName);
                if (is != null) {
                    try {
                        return loadClass(key, is, analysisStack);
                    } catch (final IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            final InputStream fromRoot = classLoader.getResourceAsStream(theResourceName);
            if (fromRoot != null) {
                try {
                    return loadClass(key, fromRoot, analysisStack);
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
            }
            throw new RuntimeException(new ClassNotFoundException(type.getClassName()));
        });
    }

    private ResolvedClass loadClass(final Type type, final InputStream is, final AnalysisStack analysisStack) throws IOException {
        final ClassReader reader = new ClassReader(is);
        final ClassNode classNode = new ClassNode();
        reader.accept(classNode, ClassReader.EXPAND_FRAMES);

        final AnalysisStack importedStack = analysisStack.addTypeImport(type);

        ResolvedClass superClass = null;
        if (classNode.superName != null) {
            superClass = resolveClass(Type.getObjectType(classNode.superName), importedStack);
        }
        final List<ResolvedClass> interfaces = new ArrayList<>();
        for (final String interf : classNode.interfaces) {
            interfaces.add(resolveClass(Type.getObjectType(interf), importedStack));
        }

        return new ResolvedClass(this, type, classNode, superClass, interfaces.toArray(new ResolvedClass[0]));
    }
}
