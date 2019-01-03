/*
 * Copyright 2018 Mirko Sertic
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
package de.mirkosertic.bytecoder.core;

import de.mirkosertic.bytecoder.api.IsObject;
import de.mirkosertic.bytecoder.api.Substitutes;
import de.mirkosertic.bytecoder.api.SubstitutesInClass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class BytecodeShadowReplacer extends BytecodeReplacer {

    private final BytecodeReplacer defaultReplacer;

    public BytecodeShadowReplacer(BytecodeLoader aLoader, BytecodeReplacer aDefaultReplacer) {
        super(aLoader);
        defaultReplacer = aDefaultReplacer;
    }

    private BytecodeMethod replaceMethodFrom(BytecodeMethod aMethod, BytecodeClass aShadowType) {
        for (BytecodeMethod theShadowMethod : aShadowType.getMethods()) {
            BytecodeAnnotation theAnnotation = theShadowMethod.getAttributes().getAnnotationByType(Substitutes.class.getName());
            if (theAnnotation != null) {
                String theMethodName = theAnnotation.getElementValueByName("value").stringValue();
                if (Objects.equals(theMethodName, aMethod.getName().stringValue()) && theShadowMethod.getSignature().matchesExactlyTo(aMethod.getSignature())) {
                    return aMethod.replaceAndFlagsFrom(theShadowMethod);
                }
            }
        }
        return aMethod;
    }

    @Override
    public MergeResult replace(
            BytecodeClassinfoConstant aClass, BytecodeMethod[] aMethods, BytecodeField[] aFields,
            BytecodeClassinfoConstant aSuperClass,
            BytecodeInterface[] aInterfaces) {

        BytecodeObjectTypeRef theObjectType = BytecodeObjectTypeRef.fromUtf8Constant(aClass.getConstant());
        StringBuilder theShadowName = new StringBuilder("de.mirkosertic.bytecoder.classlib.").append(theObjectType.name());
        int p = theShadowName.lastIndexOf(".");
        if (p>0) {
            theShadowName.insert(p+1, "T");
        }
        try {
            String theShadowNameStr = theShadowName.toString();
            defaultReplacer.addTypeMap(theShadowNameStr, theObjectType.name());
            BytecodeClass theShadowType = loader.loadByteCode(new BytecodeObjectTypeRef(theShadowNameStr), defaultReplacer);

            BytecodeClassinfoConstant theSuperClass = theShadowType.getSuperClass();
            if (theShadowType.getAttributes().getAnnotationByType(IsObject.class.getName()) != null) {
                theSuperClass = BytecodeClassinfoConstant.OBJECT_CLASS;
            }

            BytecodeAnnotation theClassAnnotation = theShadowType.getAttributes().getAnnotationByType(SubstitutesInClass.class.getName());
            if (theClassAnnotation == null) {
                // No valid shadow type
                return new MergeResult(
                        aMethods,
                        aFields,
                        aSuperClass,
                        aInterfaces
                );
            }

            if (Objects.equals(theClassAnnotation.getElementValueByName("completeReplace").stringValue(), "true")) {

                List<BytecodeMethod> theMethods = new ArrayList<>();
                for (BytecodeMethod aMethod : theShadowType.getMethods()) {
                    theMethods.add(replaceMethodFrom(aMethod, theShadowType));
                }

                return new MergeResult(
                        theMethods.toArray(new BytecodeMethod[theMethods.size()]),
                        theShadowType.fields(),
                        theSuperClass,
                        theShadowType.getInterfaces()
                );
            }

            List<BytecodeField> theFields = new ArrayList<>();
            // Import fields from shadow type
            theFields.addAll(Arrays.asList(theShadowType.fields()));
            theFields.addAll(Arrays.asList(aFields));

            List<BytecodeMethod> theMethods = new ArrayList<>();
            for (BytecodeMethod aMethod : aMethods) {
                theMethods.add(replaceMethodFrom(aMethod, theShadowType));
            }

            return new MergeResult(
                    theMethods.toArray(new BytecodeMethod[theMethods.size()]),
                    theFields.toArray(new BytecodeField[theFields.size()]),
                    theSuperClass,
                    aInterfaces
            );
        } catch (Exception  e) {
            // No shadow type found
            return new MergeResult(
                    aMethods,
                    aFields,
                    aSuperClass,
                    aInterfaces
            );
        }
    }
}