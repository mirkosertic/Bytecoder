/*
 * Copyright 2021 Mirko Sertic
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

import de.mirkosertic.bytecoder.api.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class CompilationUnit {

    private final Map<String, ResolvedClass> resolvedClasses;
    private final Queue<Command> processingQueue;
    private final Logger logger;

    public CompilationUnit(final Logger logger) {
        this.logger = logger;
        this.resolvedClasses = new HashMap<>();
        this.processingQueue = new LinkedList<>();
    }

    public void enqueue(final Command command) {
        processingQueue.add(command);
    }

    public void process() {
        while (!processingQueue.isEmpty()) {
            while (!processingQueue.isEmpty()) {
                processingQueue.remove().execute(this);
            }
            // We know that at this point everything is clear
            // We now have to check if all inherited or abstract methods
            // are also resolved across the whole type system
            // (Implementation of abstract referenced methods, overwritten referenced methods in subtypes)
        }
    }

    protected ResolvedClass resolveType(final Type type) {
        if (type.getSort() == Type.OBJECT) {
            final String className = type.getClassName();
            return resolveClass(className);
        } else if (type.getSort() == Type.ARRAY) {
            return resolveType(type.getElementType());
        } else {
            return null;
        }
    }

    protected ClassNode loadClass(final String fullQualifiedClassName) throws IOException {
        final InputStream is = getClass().getResourceAsStream("/" + fullQualifiedClassName.replace(".", "/") + ".class");
        final ClassReader cr = new ClassReader(is);
        final ClassNode classNode = new ClassNode();
        cr.accept(classNode, 0);
        return classNode;
    }

    protected ResolvedClass resolveClass(final String fullQualifiedClassName) {
        ResolvedClass resolvedClass = resolvedClasses.get(fullQualifiedClassName);
        if (resolvedClass == null) {
            try {
                logger.info("Resolving class {}", fullQualifiedClassName);
                final ClassNode classNode = loadClass(fullQualifiedClassName);

                resolvedClass = new ResolvedClass(this, classNode);
                resolvedClasses.put(fullQualifiedClassName, resolvedClass);

                if (classNode.superName != null) {
                    final ResolvedClass superClass = resolveClass(Type.getObjectType(classNode.superName).getClassName());
                    superClass.subTypes.add(resolvedClass);
                    resolvedClass.superTypes.add(superClass);
                }

                for (final String interfaceName : classNode.interfaces) {
                    final ResolvedClass interfaceClass = resolveClass(Type.getObjectType(interfaceName).getClassName());
                    interfaceClass.subTypes.add(resolvedClass);
                    resolvedClass.superTypes.add(interfaceClass);
                }

                for (final MethodNode method : classNode.methods) {
                    if ("<clinit>".equals(method.name) && "()V".equals(method.desc) && Modifier.isStatic(method.access)) {
                        // We found a static class initializer
                        resolvedClass.flagWith(ResolvedClass.Flag.HAS_CLASS_INITIALIZER);
                        resolvedClass.resolveStaticMethod(method.name, method.desc);
                    }
                }

            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }
        return resolvedClass;
    }
}
