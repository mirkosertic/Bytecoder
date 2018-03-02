/*
 * Copyright 2017 Mirko Sertic
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
package de.mirkosertic.bytecoder.backend.wasm;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import de.mirkosertic.bytecoder.api.EmulatedByRuntime;
import de.mirkosertic.bytecoder.api.Export;
import de.mirkosertic.bytecoder.backend.CompileBackend;
import de.mirkosertic.bytecoder.backend.CompileOptions;
import de.mirkosertic.bytecoder.backend.ConstantPool;
import de.mirkosertic.bytecoder.classlib.Address;
import de.mirkosertic.bytecoder.classlib.MemoryManager;
import de.mirkosertic.bytecoder.core.BytecodeAnnotation;
import de.mirkosertic.bytecoder.core.BytecodeArrayTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeClass;
import de.mirkosertic.bytecoder.core.BytecodeImportedLink;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeMethod;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeResolvedFields;
import de.mirkosertic.bytecoder.core.BytecodeResolvedMethods;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeVirtualMethodIdentifier;
import de.mirkosertic.bytecoder.relooper.Relooper;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.ProgramGenerator;
import de.mirkosertic.bytecoder.ssa.ProgramGeneratorFactory;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.StringValue;
import de.mirkosertic.bytecoder.ssa.TypeRef;
import de.mirkosertic.bytecoder.ssa.Variable;

public class WASMSSACompilerBackend implements CompileBackend<WASMCompileResult> {

    private static class CallSite {
        private final Program program;
        private final RegionNode bootstrapMethod;

        private CallSite(Program aProgram, RegionNode aBootstrapMethod) {
            program = aProgram;
            bootstrapMethod = aBootstrapMethod;
        }
    }

    private final ProgramGeneratorFactory programGeneratorFactory;

    public WASMSSACompilerBackend(ProgramGeneratorFactory aProgramGeneratorFactory) {
        programGeneratorFactory = aProgramGeneratorFactory;
    }

    @Override
    public WASMCompileResult generateCodeFor(CompileOptions aOptions, BytecodeLinkerContext aLinkerContext, Class aEntryPointClass, String aEntryPointMethodName, BytecodeMethodSignature aEntryPointSignatue) {

        // Link required mamory management code
        BytecodeLinkedClass theArrayClass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Array.class));

        BytecodeLinkedClass theManagerClass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(MemoryManager.class));

        theManagerClass.resolveStaticMethod("freeMem", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.LONG, new BytecodeTypeRef[0]));
        theManagerClass.resolveStaticMethod("usedMem", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.LONG, new BytecodeTypeRef[0]));

        theManagerClass.resolveStaticMethod("free", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[] {BytecodeObjectTypeRef.fromRuntimeClass(
                Address.class)}));
        theManagerClass.resolveStaticMethod("malloc", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(
                Address.class), new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT}));
        theManagerClass.resolveStaticMethod("newObject", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(
                Address.class), new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));
        theManagerClass.resolveStaticMethod("newArray", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(
                Address.class), new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));
        theManagerClass.resolveStaticMethod("newArray", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(
                Address.class), new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));

        BytecodeLinkedClass theStringClass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(String.class));
        if (!theStringClass.resolveConstructorInvocation(new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[] {new BytecodeArrayTypeRef(BytecodePrimitiveTypeRef.BYTE, 1)}))) {
            throw new IllegalStateException("No matching constructor!");
        }

        StringWriter theStringWriter = new StringWriter();
        PrintWriter theWriter = new PrintWriter(theStringWriter);

        theWriter.println("(module");

        theWriter.println("   (func $float_remainder (import \"math\" \"float_rem\") (param $p1 f32) (param $p2 f32) (result f32))\n");

        // Print imported functions first
        aLinkerContext.linkedClasses().forEach(aEntry -> {

            if (aEntry.targetNode().getBytecodeClass().getAccessFlags().isInterface()) {
                return;
            }
            if (Objects.equals(aEntry.edgeType().objectTypeRef(), BytecodeObjectTypeRef.fromRuntimeClass(Address.class))) {
                return;
            }

            BytecodeResolvedMethods theMethodMap = aEntry.targetNode().resolvedMethods();
            theMethodMap.stream().forEach(aMethodMapEntry -> {

                // Only add implementation methods
                if (!(aMethodMapEntry.getProvidingClass() == aEntry.targetNode())) {
                    return;
                }

                BytecodeMethod t = aMethodMapEntry.getValue();
                BytecodeMethodSignature theSignature = t.getSignature();

                if (t.getAccessFlags().isNative()) {
                    if (aMethodMapEntry.getProvidingClass().getBytecodeClass().getAttributes().getAnnotationByType(EmulatedByRuntime.class.getName()) != null) {
                        return;
                    }

                    BytecodeImportedLink theLink = aMethodMapEntry.getProvidingClass().linkfor(t);

                    // Imported function

                    theWriter.print("   (func ");
                    theWriter.print("$");
                    theWriter.print(WASMWriterUtils.toMethodName(aMethodMapEntry.getProvidingClass().getClassName(), t.getName(), theSignature));
                    theWriter.print(" (import \"");
                    theWriter.print(theLink.getModuleName());
                    theWriter.print("\" \"");
                    theWriter.print(theLink.getLinkName());
                    theWriter.print("\") ");

                    theWriter.print("(param $thisRef");
                    theWriter.print(" ");
                    theWriter.print(WASMWriterUtils.toType(TypeRef.Native.REFERENCE));
                    theWriter.print(") ");

                    for (int i = 0; i < theSignature.getArguments().length; i++) {
                        BytecodeTypeRef theParamType = theSignature.getArguments()[i];
                        theWriter.print("(param $p");
                        theWriter.print((i + 1));
                        theWriter.print(" ");
                        theWriter.print(WASMWriterUtils.toType(TypeRef.toType(theParamType)));
                        theWriter.print(") ");
                    }

                    if (!theSignature.getReturnType().isVoid()) {
                        theWriter.print("(result "); // result
                        theWriter.print(WASMWriterUtils.toType(TypeRef.toType(theSignature.getReturnType())));
                        theWriter.print(")");
                    }
                    theWriter.println(")");
                }
            });
        });


        Map<String, String> theGlobalTypes = new HashMap<>();
        theGlobalTypes.put("RESOLVEMETHOD", "(func (param i32) (param i32) (result i32))");
        theGlobalTypes.put("INSTANCEOF", "(func (param i32) (param i32) (result i32))");

        theWriter.println();

        List<String> theGeneratedFunctions = new ArrayList<>();
        theGeneratedFunctions.add("LAMBDA__resolvevtableindex");
        theGeneratedFunctions.add("RUNTIMECLASS__resolvevtableindex");
        theGeneratedFunctions.add("jlClass_A1ObjectgetEnumConstants");
        theGeneratedFunctions.add("jlClass_BOOLEANdesiredAssertionStatus");

        List<BytecodeLinkedClass> theLinkedClasses = new ArrayList<>();
        ConstantPool theConstantPool = new ConstantPool();

        Map<String, CallSite> theCallsites = new HashMap<>();

        WASMSSAWriter.IDResolver theResolver = new WASMSSAWriter.IDResolver() {

            @Override
            public int resolveVTableMethodByType(BytecodeObjectTypeRef aObjectType) {
                String theClassName = WASMWriterUtils.toClassName(aObjectType);

                String theMethodName = theClassName + "__resolvevtableindex";
                int theIndex = theGeneratedFunctions.indexOf(theMethodName);
                if (theIndex < 0) {
                    throw new IllegalStateException("Cannot resolve vtable method for " + theClassName);
                }
                return theIndex;
            }

            @Override
            public String resolveStringPoolFunctionName(StringValue aValue) {
                return "stringPool" + theConstantPool.register(aValue);
            }

            @Override
            public String resolveCallsiteBootstrapFor(BytecodeClass aOwningClass, String aCallsiteId, Program aProgram,
                    RegionNode aBootstrapMethod) {
                String theID = "callsite_" + aCallsiteId.replace("/","_");
                CallSite theCallsite = theCallsites.computeIfAbsent(theID, k -> new CallSite(aProgram, aBootstrapMethod));
                return theID;
            }

            @Override
            public int resolveMethodIDByName(String aMethodName) {
                int theIndex = theGeneratedFunctions.indexOf(aMethodName);
                if (theIndex < 0) {
                    throw new IllegalStateException("Cannot resolve method " + aMethodName);
                }
                return theIndex;
            }

            @Override
            public void registerGlobalType(BytecodeMethodSignature aSignature, boolean aStatic) {
                String theMethodSignature = WASMWriterUtils.toMethodSignature(aSignature, aStatic);
                if (!theGlobalTypes.containsKey(theMethodSignature)) {
                    String theTypeDefinition = WASMWriterUtils.toWASMMethodSignature(aSignature);

                    theGlobalTypes.put(theMethodSignature, theTypeDefinition);
                }
            }
        };

        aLinkerContext.linkedClasses().forEach(aEntry -> {

            if (Objects.equals(aEntry.edgeType().objectTypeRef(), BytecodeObjectTypeRef.fromRuntimeClass(Address.class))) {
                return;
            }

            theLinkedClasses.add(aEntry.targetNode());

            String theClassName = WASMWriterUtils.toClassName(aEntry.edgeType().objectTypeRef());

            if (!aEntry.targetNode().getBytecodeClass().getAccessFlags().isInterface()) {
                theGeneratedFunctions.add(theClassName + "__resolvevtableindex");
                theGeneratedFunctions.add(theClassName + "__instanceof");
            }

            BytecodeResolvedMethods theMethodMap = aEntry.targetNode().resolvedMethods();
            theMethodMap.stream().forEach(aMapEntry -> {
                BytecodeMethod t = aMapEntry.getValue();

                // If the method is provided by the runtime, we do not need to generate the implementation
                if (t.getAttributes().getAnnotationByType(EmulatedByRuntime.class.getName()) != null) {
                    return;
                }
                // Do not generate code for abstract methods
                if (t.getAccessFlags().isAbstract()) {
                    return;
                }
                // Constructors for the same reason
                if (t.isConstructor()) {
                    return;
                }
                // Class initializer also
                if (t.isClassInitializer()) {
                    return;
                }
                // Only write real methods
                if (!(aMapEntry.getProvidingClass() == aEntry.targetNode())) {
                    return;
                }

                BytecodeMethodSignature theSignature = t.getSignature();
                theResolver.registerGlobalType(theSignature, t.getAccessFlags().isStatic());

                if (aEntry.targetNode().getBytecodeClass().getAttributes().getAnnotationByType(EmulatedByRuntime.class.getName()) != null) {
                    return;
                }

                String theMethodName = WASMWriterUtils.toMethodName(aEntry.edgeType().objectTypeRef(), t.getName(), theSignature);
                if (!theGeneratedFunctions.contains(theMethodName)) {
                    theGeneratedFunctions.add(theMethodName);
                }
            });
        });

        theWriter.println("   (memory (export \"memory\") 512 512)");

        // Write virtual method table
        if (!theGeneratedFunctions.isEmpty()) {
            theWriter.println();
            theWriter.print("   (table ");
            theWriter.print(theGeneratedFunctions.size());
            theWriter.println(" anyfunc)");

            for (int i=0;i<theGeneratedFunctions.size();i++) {
                theWriter.print("   (elem (i32.const ");
                theWriter.print(i);
                theWriter.print(") $");
                theWriter.print(theGeneratedFunctions.get(i));
                theWriter.println(")");
            }

            theWriter.println();
        }

        // Initialize memory layout for classes and instances
        WASMMemoryLayouter theMemoryLayout = new WASMMemoryLayouter(aLinkerContext);

        // Now everything else
        aLinkerContext.linkedClasses().forEach(aEntry -> {

            if (Objects.equals(aEntry.edgeType().objectTypeRef(), BytecodeObjectTypeRef.fromRuntimeClass(Address.class))) {
                return;
            }
            if (aEntry.targetNode().getBytecodeClass().getAttributes().getAnnotationByType(EmulatedByRuntime.class.getName()) != null) {
                return;
            }

            Set<BytecodeObjectTypeRef> theStaticReferences = new HashSet<>();

            BytecodeResolvedMethods theMethodMap = aEntry.targetNode().resolvedMethods();
            theMethodMap.stream().forEach(aMethodMapEntry -> {

                BytecodeMethod t = aMethodMapEntry.getValue();

                // If the method is provided by the runtime, we do not need to generate the implementation
                if (t.getAttributes().getAnnotationByType(EmulatedByRuntime.class.getName()) != null) {
                    return;
                }

                // Do not generate code for abstract methods
                if (t.getAccessFlags().isAbstract()) {
                    return;
                }

                if (t.getAccessFlags().isNative()) {
                    // Already written
                    return;
                }

                if (!(aMethodMapEntry.getProvidingClass() == aEntry.targetNode())) {
                    // Skip methods not implemented here
                    return;
                }

                BytecodeMethodSignature theSignature = t.getSignature();

                ProgramGenerator theGenerator = programGeneratorFactory.createFor(aLinkerContext);
                Program theSSAProgram = theGenerator.generateFrom(aMethodMapEntry.getProvidingClass().getBytecodeClass(), t);

                //Run optimizer
                aOptions.getOptimizer().optimize(theSSAProgram.getControlFlowGraph(), aLinkerContext);

                theWriter.print("   (func ");
                theWriter.print("$");
                theWriter.print(WASMWriterUtils.toMethodName(aEntry.targetNode().getClassName(), t.getName(), theSignature));
                theWriter.print(" ");

                if (t.getAccessFlags().isStatic()) {
                    theWriter.print("(param $UNUSED");
                    theWriter.print(" ");
                    theWriter.print(WASMWriterUtils.toType(TypeRef.Native.REFERENCE));
                    theWriter.print(") ");
                }

                for (Program.Argument theArgument : theSSAProgram.getArguments()) {

                    Variable theVariable = theArgument.getVariable();

                    theWriter.print("(param $");
                    theWriter.print(theVariable.getName());
                    theWriter.print(" ");
                    theWriter.print(WASMWriterUtils.toType(theVariable.resolveType()));
                    theWriter.print(") ");
                }

                if (!theSignature.getReturnType().isVoid()) {
                    theWriter.print("(result "); // result
                    theWriter.print(WASMWriterUtils.toType(TypeRef.toType(theSignature.getReturnType())));
                    theWriter.print(")");
                }
                theWriter.println();

                theStaticReferences.addAll(theSSAProgram.getStaticReferences());

                WASMSSAWriter theSSAWriter = new WASMSSAWriter(aOptions, theSSAProgram, "         ", theWriter, aLinkerContext, theResolver, theMemoryLayout);

                for (Variable theVariable : theSSAProgram.getVariables()) {

                    if (!(theVariable.isSynthetic()) &&
                        !theSSAWriter.isStackVariable(theVariable)) {

                        theSSAWriter.print("(local $");
                        theSSAWriter.print(theVariable.getName());
                        theSSAWriter.print(" ");
                        theSSAWriter.print(WASMWriterUtils.toType(theVariable.resolveType()));
                        theSSAWriter.print(") ;; ");
                        theSSAWriter.println(theVariable.resolveType().resolve().name());
                    }
                }

                // Try to reloop it!
                try {
                    Relooper theRelooper = new Relooper();
                    Relooper.Block theReloopedBlock = theRelooper.reloop(theSSAProgram.getControlFlowGraph());

                    theSSAWriter.writeRelooped(theReloopedBlock);
                } catch (Exception e) {
                    throw new IllegalStateException("Error relooping cfg", e);
                }

                theWriter.println("   )");
                theWriter.println();
            });

            String theClassName = WASMWriterUtils.toClassName(aEntry.edgeType().objectTypeRef());

            if (!aEntry.targetNode().getBytecodeClass().getAccessFlags().isInterface()) {

                theWriter.print("   (func ");
                theWriter.print("$");
                theWriter.print(theClassName);
                theWriter.println("__resolvevtableindex (param $thisRef i32) (param $p1 i32) (result i32)");

                theMethodMap.stream().forEach(aMethodMapEntry -> {

                    BytecodeMethod theMethod = aMethodMapEntry.getValue();
                    if (theMethod.getAccessFlags().isStatic()) {
                        return;
                    }
                    if (theMethod.getAccessFlags().isPrivate()) {
                        return;
                    }
                    if (theMethod.isConstructor()) {
                        return;
                    }
                    if (theMethod.getAccessFlags().isAbstract()) {
                        return;
                    }

                    if (theMethod == BytecodeLinkedClass.GET_CLASS_PLACEHOLDER) {
                        // This method cannot be called as it is handled by TypeOfValue
                        return;
                    }

                    BytecodeVirtualMethodIdentifier theMethodIdentifier = aLinkerContext.getMethodCollection()
                            .identifierFor(theMethod);

                    theWriter.println("         (block $b");
                    theWriter.print("             (br_if $b (i32.ne (get_local $p1) (i32.const ");
                    theWriter.print(theMethodIdentifier.getIdentifier());
                    theWriter.println(")))");

                    String theFullMethodName = WASMWriterUtils.toMethodName(aMethodMapEntry.getProvidingClass().getClassName(),
                            theMethod.getName(),
                            theMethod.getSignature());

                    int theIndex = theGeneratedFunctions.indexOf(theFullMethodName);
                    if (theIndex < 0) {
                        throw new IllegalStateException("Unknown index : " + theFullMethodName);
                    }

                    theWriter.print("             (return (i32.const ");
                    theWriter.print(theIndex);
                    theWriter.println("))");
                    theWriter.println("         )");
                });

                theWriter.println("         (block $b");
                theWriter.print("             (br_if $b (i32.ne (get_local $p1) (i32.const ");
                theWriter.print(WASMSSAWriter.GENERATED_INSTANCEOF_METHOD_ID);
                theWriter.println(")))");

                String theFullMethodName = theClassName + "__instanceof";

                int theIndex = theGeneratedFunctions.indexOf(theFullMethodName);
                if (theIndex < 0) {
                    throw new IllegalStateException("Unknown index : " + theFullMethodName);
                }

                theWriter.print("             (return (i32.const ");
                theWriter.print(theIndex);
                theWriter.println("))");
                theWriter.println("         )");

                theWriter.println("         (unreachable)");
                theWriter.println("   )");
                theWriter.println();

                // Instanceof method
                theWriter.print("   (func ");
                theWriter.print("$");
                theWriter.print(theClassName);
                theWriter.println("__instanceof (param $thisRef i32) (param $p1 i32) (result i32)");

                for (BytecodeLinkedClass theType : aEntry.targetNode().getImplementingTypes()) {

                    theWriter.print("         (block $block");
                    theWriter.print(theType.getUniqueId());
                    theWriter.println();

                    theWriter.print("             (br_if $block");
                    theWriter.print(theType.getUniqueId());
                    theWriter.print(" (i32.ne (get_local $p1) (i32.const ");
                    theWriter.print(theType.getUniqueId());
                    theWriter.println(")))");

                    theWriter.println("             (return (i32.const 1))");

                    theWriter.println("         )");
                }

                theWriter.println("         (return (i32.const 0))");
                theWriter.println("   )");
                theWriter.println();
            }


            theWriter.print("   (func ");
            theWriter.print("$");
            theWriter.print(theClassName);
            theWriter.println("__classinitcheck");

            theWriter.println("      (block $check");
            theWriter.print("         (br_if $check (i32.eq (i32.load offset=8 (get_global $");
            theWriter.print(theClassName);
            theWriter.println("__runtimeClass)) (i32.const 1)))");

            theWriter.print("         (i32.store offset=8 (get_global $");
            theWriter.print(theClassName);
            theWriter.println("__runtimeClass) (i32.const 1))");

            for (BytecodeObjectTypeRef theRef : theStaticReferences) {
                if (!Objects.equals(theRef, aEntry.edgeType().objectTypeRef())) {
                    theWriter.print("         (call $");
                    theWriter.print(WASMWriterUtils.toClassName(theRef));
                    theWriter.println("__classinitcheck)");
                }
            }


            if (aEntry.targetNode().hasClassInitializer()) {
                theWriter.print("         (call $");
                theWriter.print(theClassName);
                theWriter.println("_VOIDclinit (i32.const 0))");
            }

            theWriter.println("      )");
            theWriter.println("   )");
            theWriter.println();
        });

        // Render callsites
        for (Map.Entry<String, CallSite> theEntry : theCallsites.entrySet()) {

            theWriter.print("   (func ");
            theWriter.print("$");
            theWriter.print(theEntry.getKey());
            theWriter.print(" ");

            theWriter.print("(result "); // result
            theWriter.print(WASMWriterUtils.toType(TypeRef.Native.REFERENCE));
            theWriter.print(")");
            theWriter.println();

            Program theSSAProgram = theEntry.getValue().program;

            WASMSSAWriter theSSAWriter = new WASMSSAWriter(aOptions, theSSAProgram, "         ", theWriter, aLinkerContext, theResolver, theMemoryLayout);

            for (Variable theVariable : theSSAProgram.getVariables()) {

                if (!(theVariable.isSynthetic()) &&
                    !(theSSAWriter.isStackVariable(theVariable))) {

                    theSSAWriter.print("(local $");
                    theSSAWriter.print(theVariable.getName());
                    theSSAWriter.print(" ");
                    theSSAWriter.print(WASMWriterUtils.toType(theVariable.resolveType()));
                    theSSAWriter.print(") ;; ");
                    theSSAWriter.println(theVariable.resolveType().resolve().name());
                }
            }

            theSSAWriter.printStackEnter();
            theSSAWriter.writeExpressionList(theEntry.getValue().bootstrapMethod.getExpressions());

            theWriter.println("   )");
            theWriter.println();
        }

        theWriter.println("   (func $newRuntimeClass (param $type i32) (param $staticSize i32) (param $enumValuesOffset i32) (result i32)");
        theWriter.println("         (local $newRef i32)");
        theWriter.println("         (set_local $newRef");
        theWriter.print("              (call $");
        theWriter.print(WASMWriterUtils.toClassName(theManagerClass.getClassName()));
        theWriter.print("_dmbcAddressnewObjectINTINTINT (i32.const 0) (get_local $staticSize) (i32.const -1) (i32.const ");
        theWriter.print(theGeneratedFunctions.indexOf("RUNTIMECLASS__resolvevtableindex"));
        theWriter.println("))");
        theWriter.println("         )");
        theWriter.println("         (i32.store offset=12 (get_local $newRef) (i32.add (get_local $newRef) (get_local $enumValuesOffset)))");
        theWriter.println("         (return (get_local $newRef))");
        theWriter.println("   )");
        theWriter.println();

        theWriter.println("   (func $LAMBDA__resolvevtableindex (param $thisRef i32) (param $methodId i32) (result i32)");
        theWriter.println("         (return (i32.load offset=8 (get_local $thisRef)))");
        theWriter.println("   )");
        theWriter.println();

        int theLambdaVTableResolveIndex = theGeneratedFunctions.indexOf("LAMBDA__resolvevtableindex");
        if (theLambdaVTableResolveIndex < 0) {
            throw new IllegalStateException("Cannot resolve LAMBDA__resolvevtableindex");
        }

        theWriter.println("   (func $newLambda (param $type i32) (param $implMethodNumber i32) (result i32)");
        theWriter.println("         (local $newRef i32)");
        theWriter.println("         (set_local $newRef");
        theWriter.print("            (call $");
        theWriter.print(WASMWriterUtils.toClassName(theManagerClass.getClassName()));
        theWriter.print("_dmbcAddressnewObjectINTINTINT (i32.const 0) (i32.const 12) (get_local $type) (i32.const ");
        theWriter.print(theLambdaVTableResolveIndex);
        theWriter.println("))");
        theWriter.println("         )");
        theWriter.println("         (i32.store offset=8 (get_local $newRef) (get_local $implMethodNumber))");
        theWriter.println("         (return (get_local $newRef))");
        theWriter.println("   )");
        theWriter.println();

        theWriter.println("   (func $compareValueI32 (param $p1 i32) (param $p2 i32) (result i32)");
        theWriter.println("     (block $b1");
        theWriter.println("         (br_if $b1");
        theWriter.println("             (i32.ne (get_local $p1) (get_local $p2))");
        theWriter.println("         )");
        theWriter.println("         (return (i32.const 0))");
        theWriter.println("     )");
        theWriter.println("     (block $b2");
        theWriter.println("         (br_if $b2");
        theWriter.println("             (i32.ge_s (get_local $p1) (get_local $p2))");
        theWriter.println("         )");
        theWriter.println("         (return (i32.const -1))");
        theWriter.println("     )");
        theWriter.println("     (return (i32.const 1))");
        theWriter.println("   )");
        theWriter.println();

        theWriter.println("   (func $compareValueF32 (param $p1 f32) (param $p2 f32) (result i32)");
        theWriter.println("     (block $b1");
        theWriter.println("         (br_if $b1");
        theWriter.println("             (f32.ne (get_local $p1) (get_local $p2))");
        theWriter.println("         )");
        theWriter.println("         (return (i32.const 0))");
        theWriter.println("     )");
        theWriter.println("     (block $b2");
        theWriter.println("         (br_if $b2");
        theWriter.println("             (f32.ge (get_local $p1) (get_local $p2))");
        theWriter.println("         )");
        theWriter.println("         (return (i32.const -1))");
        theWriter.println("     )");
        theWriter.println("     (return (i32.const 1))");
        theWriter.println("   )");
        theWriter.println();

        theWriter.println("   (func $INSTANCEOF_CHECK (param $thisRef i32) (param $type i32) (result i32)");
        theWriter.println("     (block $nullcheck");
        theWriter.println("         (br_if $nullcheck");
        theWriter.println("             (i32.ne (get_local $thisRef) (i32.const 0))");
        theWriter.println("         )");
        theWriter.println("         (return (i32.const 0))");
        theWriter.println("     )");
        theWriter.println("     (call_indirect $t_INSTANCEOF");
        theWriter.println("         (get_local $thisRef)");
        theWriter.println("         (get_local $type)");
        theWriter.println("         (call_indirect $t_RESOLVEMETHOD");
        theWriter.println("             (get_local $thisRef)");
        theWriter.print("             (i32.const ");
        theWriter.print(WASMSSAWriter.GENERATED_INSTANCEOF_METHOD_ID);
        theWriter.println(")");
        theWriter.println("             (i32.load offset=4 (get_local $thisRef))");
        theWriter.println("         )");
        theWriter.println("      )");
        theWriter.println("   )");
        theWriter.println();

        theWriter.println("   (func $jlClass_A1ObjectgetEnumConstants (param $thisRef i32) (result i32)");
        theWriter.println("     (return (i32.load (i32.load offset=12 (get_local $thisRef))))");
        theWriter.println("   )");
        theWriter.println();

        theWriter.println("   (func $jlClass_BOOLEANdesiredAssertionStatus (param $thisRef i32) (result i32)");
        theWriter.println("     (return (i32.const 0))");
        theWriter.println("   )");
        theWriter.println();

        theWriter.println("   (func $RUNTIMECLASS__resolvevtableindex (param $thisRef i32) (param $methodId i32) (result i32)");

        BytecodeLinkedClass theClassLinkedCass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Class.class));
        BytecodeResolvedMethods theRuntimeMethodMap = theClassLinkedCass.resolvedMethods();
        theRuntimeMethodMap.stream().forEach(aMethodMapEntry -> {
            BytecodeMethod theMethod = aMethodMapEntry.getValue();
            if (!theMethod.getAccessFlags().isStatic()) {

                BytecodeVirtualMethodIdentifier theMethodIdentifier = aLinkerContext.getMethodCollection().identifierFor(theMethod);

                theWriter.println("     (block $m" + theMethodIdentifier.getIdentifier());
                theWriter.println("         (br_if $m" + theMethodIdentifier.getIdentifier() + " (i32.ne (get_local $methodId) (i32.const " + theMethodIdentifier.getIdentifier() + ")))");
                if (Objects.equals("getClass", theMethod.getName().stringValue())) {
                    theWriter.println("         (unreachable)");
                } else if (Objects
                        .equals("toString", theMethod.getName().stringValue())) {
                    theWriter.println("         (unreachable)");
                } else if (Objects
                        .equals("equals", theMethod.getName().stringValue())) {
                    theWriter.println("         (unreachable)");
                } else if (Objects
                        .equals("hashCode", theMethod.getName().stringValue())) {
                    theWriter.println("         (unreachable)");
                } else if (Objects.equals("desiredAssertionStatus",
                        theMethod.getName().stringValue())) {
                    theWriter.println("         (return (i32.const " + theGeneratedFunctions.indexOf("Class_BOOLEANdesiredAssertionStatus") + "))");
                } else if (Objects
                        .equals("getEnumConstants",
                                theMethod.getName().stringValue())) {
                    theWriter.println("         (return (i32.const " + theGeneratedFunctions.indexOf("Class_A1TObjectgetEnumConstants") + "))");
                } else {
                    theWriter.println("         (unreachable)");
                }
                theWriter.println("     )");
            }
        });

        theWriter.println("     (unreachable)");
        theWriter.println("   )");
        theWriter.println();

        List<String> theGlobalVariables = new ArrayList<>();

        theWriter.println("   (func $bootstrap");

        theWriter.println("      (set_global $STACKTOP (i32.sub (i32.mul (current_memory) (i32.const 65536)) (i32.const 1)))");

        // Globals for static class data
        aLinkerContext.linkedClasses().forEach(aEntry -> {

            BytecodeLinkedClass theLinkedClass = aEntry.targetNode();

            if (Objects.equals(aEntry.edgeType().objectTypeRef(), BytecodeObjectTypeRef.fromRuntimeClass(Address.class))) {
                return;
            }
            if (theLinkedClass.getBytecodeClass().getAttributes().getAnnotationByType(EmulatedByRuntime.class.getName()) != null) {
                return;
            }

            theGlobalVariables.add(WASMWriterUtils.toClassName(aEntry.edgeType().objectTypeRef()) + "__runtimeClass");

            theWriter.print("      (set_global $");
            theWriter.print(WASMWriterUtils.toClassName(aEntry.edgeType().objectTypeRef()));
            theWriter.print("__runtimeClass (call $newRuntimeClass");

            theWriter.print(" (i32.const ");
            theWriter.print(theLinkedClass.getUniqueId());
            theWriter.print(")");

            WASMMemoryLayouter.MemoryLayout theLayout = theMemoryLayout.layoutFor(aEntry.edgeType().objectTypeRef());

            theWriter.print(" (i32.const ");
            theWriter.print(theLayout.classSize());
            theWriter.print(")");

            BytecodeResolvedFields theStaticFields = theLinkedClass.resolvedFields();
            if (theStaticFields.fieldByName("$VALUES") != null) {
                theWriter.print(" (i32.const ");
                theWriter.print(theLayout.offsetForClassMember("$VALUES"));
                theWriter.println(")");
            } else {
                theWriter.print(" (i32.const -1)");
            }

            theWriter.println("))");
        });

        WASMMemoryLayouter.MemoryLayout theStringMemoryLayout = theMemoryLayout.layoutFor(theStringClass.getClassName());

        aLinkerContext.linkedClasses().forEach(aEntry -> {

            if (aEntry.targetNode().getBytecodeClass().getAttributes().getAnnotationByType(EmulatedByRuntime.class.getName()) != null) {
                return;
            }

            if (!Objects.equals(aEntry.edgeType().objectTypeRef(), BytecodeObjectTypeRef.fromRuntimeClass(Address.class))) {
                theWriter.print("      (call $");
                theWriter.print(WASMWriterUtils.toClassName(aEntry.edgeType().objectTypeRef()));
                theWriter.println("__classinitcheck)");
            }
        });

        List<StringValue> thePoolValues = theConstantPool.stringValues();
        for (int i=0;i<thePoolValues.size();i++) {

            StringValue theConstantInPool = thePoolValues.get(i);
            String theData = theConstantInPool.getStringValue();
            byte[] theDataBytes = theData.getBytes();

            theGlobalVariables.add("stringPool" + i);
            theGlobalVariables.add("stringPool" + i + "__array");

            theWriter.print("      (set_global $stringPool");
            theWriter.print(i);
            theWriter.print("__array ");

            String theMethodName = WASMWriterUtils.toMethodName(
                    BytecodeObjectTypeRef.fromRuntimeClass(MemoryManager.class),
                    "newArray",
                    new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(
                            Address.class), new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));

            theWriter.print("(call $");
            theWriter.print(theMethodName);
            theWriter.print(" (i32.const 0) "); // UNUSED argument
            theWriter.print(" (i32.const "); // Length
            theWriter.print(theDataBytes.length);
            theWriter.print(")");

            // We also need the runtime class
            theWriter.print(" (get_global $");
            theWriter.print(WASMWriterUtils.toClassName(theArrayClass.getClassName()));
            theWriter.print("__runtimeClass");
            theWriter.print(")");
            // Plus the vtable index
            theWriter.print(" (i32.const ");
            theWriter.print(theResolver.resolveVTableMethodByType(BytecodeObjectTypeRef.fromRuntimeClass(Array.class)));
            theWriter.println(")))");

            // Set array value
            for (int j=0; j< theDataBytes.length;j++) {
                //
                int offset = 20 + j * 4;

                theWriter.print("      (i32.store ");
                theWriter.print("offset="+offset+" ");

                theWriter.print("(get_global $stringPool");
                theWriter.print(i);
                theWriter.print("__array) ");

                theWriter.print("(i32.const ");
                theWriter.print(theDataBytes[j]);
                theWriter.println("))");
            }

            theWriter.print("      (set_global $stringPool");
            theWriter.print(i);
            theWriter.print(" (call $");
            theWriter.print(WASMWriterUtils.toClassName(theManagerClass.getClassName()));
            theWriter.print("_dmbcAddressnewObjectINTINTINT");

            theWriter.print(" (i32.const 0)"); // Unused argument

            theWriter.print(" (i32.const ");
            theWriter.print(theStringMemoryLayout.instanceSize());
            theWriter.print(")");

            theWriter.print(" (i32.const ");
            theWriter.print(theStringClass.getUniqueId());
            theWriter.print(")");

            theWriter.print(" (i32.const ");
            theWriter.print(theResolver.resolveVTableMethodByType(theStringClass.getClassName()));
            theWriter.print(")");
            theWriter.println("))");

            theWriter.print("      (call $");
            theWriter.print(WASMWriterUtils.toClassName(theStringClass.getClassName()));
            theWriter.print("_VOIDinitA1BYTE ");
            theWriter.print(" (get_global $stringPool");
            theWriter.print(i);
            theWriter.print(")");
            theWriter.print(" (get_global $stringPool");
            theWriter.print(i);
            theWriter.println("__array))");
        }

        // After the Bootstrap, we need to all the static stuff on the stack, so it is not garbage collected
        theWriter.print("      (set_global $STACKTOP (i32.sub (get_global $STACKTOP) (i32.const ");
        theWriter.print(theGlobalVariables.size() * 4);
        theWriter.println(")))");
        for (int i=0;i<theGlobalVariables.size();i++) {
            theWriter.print("      (i32.store offset=");
            theWriter.print(i * 4);
            theWriter.print(" (get_global $STACKTOP) (get_global $");
            theWriter.print(theGlobalVariables.get(i));
            theWriter.println("))");
        }

        theWriter.println("   )");
        theWriter.println();

        for (int i=0;i<thePoolValues.size();i++) {
            theWriter.print("   (global $stringPool");
            theWriter.print(i);
            theWriter.println(" (mut i32) (i32.const 0))");
            theWriter.print("   (global $stringPool");
            theWriter.print(i);
            theWriter.println("__array (mut i32) (i32.const 0))");
        }

        theWriter.println("   (global $STACKTOP (mut i32) (i32.const 0))");

        // Globals for static class data
        aLinkerContext.linkedClasses().forEach(aEntry -> {

            if (Objects.equals(aEntry.edgeType().objectTypeRef(), BytecodeObjectTypeRef.fromRuntimeClass(Address.class))) {
                return;
            }
            if (aEntry.targetNode().getBytecodeClass().getAttributes().getAnnotationByType(EmulatedByRuntime.class.getName()) != null) {
                return;
            }

            theWriter.print("   (global $");
            theWriter.print(WASMWriterUtils.toClassName(aEntry.edgeType().objectTypeRef()));
            theWriter.println("__runtimeClass (mut i32) (i32.const 0))");
        });

        theWriter.println();

        theWriter.println("   (export \"bootstrap\" (func $bootstrap))");

        // Write exports
        aLinkerContext.linkedClasses().forEach(aEntry -> {

            BytecodeLinkedClass theLinkedClass = aEntry.targetNode();

            if (theLinkedClass.getBytecodeClass().getAccessFlags().isInterface()) {
                return;
            }

            BytecodeResolvedMethods theMethodMap = theLinkedClass.resolvedMethods();
            theMethodMap.stream().forEach(aMethodMapEntry -> {
                BytecodeMethod t = aMethodMapEntry.getValue();
                BytecodeAnnotation theExport = t.getAttributes().getAnnotationByType(Export.class.getName());
                if (theExport != null) {
                    theWriter.print("   (export \"");
                    theWriter.print(theExport.getElementValueByName("value").stringValue());
                    theWriter.print("\" (func $");
                    theWriter.print(WASMWriterUtils.toMethodName(aEntry.edgeType().objectTypeRef(), t.getName(), t.getSignature()));
                    theWriter.println("))");
                }
            });
        });

        theWriter.print("   (export \"main\" (func $");
        theWriter.print(WASMWriterUtils.toMethodName(BytecodeObjectTypeRef.fromRuntimeClass(aEntryPointClass), aEntryPointMethodName, aEntryPointSignatue));
        theWriter.println("))");

        theWriter.println();
        for (Map.Entry<String, String> theEntry : theGlobalTypes.entrySet()) {
            theWriter.print("   (type $t_");
            theWriter.print(theEntry.getKey());
            theWriter.print(" ");
            theWriter.print(theEntry.getValue());
            theWriter.println(")");
        }

        theWriter.println(")");
        theWriter.flush();

        return new WASMCompileResult(aLinkerContext, theGeneratedFunctions, theStringWriter.toString(), theMemoryLayout);
    }

    @Override
    public String generatedFileName() {
        return "bytecoder.wat";
    }
}