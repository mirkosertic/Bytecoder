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

    public BytecodeShadowReplacer(final BytecodeLoader aLoader, final BytecodeReplacer aDefaultReplacer) {
        super(aLoader);
        defaultReplacer = aDefaultReplacer;
    }

    private BytecodeMethod replaceMethodFrom(final BytecodeMethod aMethod, final BytecodeClass aShadowType) {
        for (final BytecodeMethod theShadowMethod : aShadowType.getMethods()) {
            final BytecodeAnnotation theAnnotation = theShadowMethod.getAttributes().getAnnotationByType(Substitutes.class.getName());
            if (theAnnotation != null) {
                final String theMethodName = theAnnotation.getElementValueByName("value").stringValue();
                if (Objects.equals(theMethodName, aMethod.getName().stringValue()) && theShadowMethod.getSignature().matchesExactlyTo(aMethod.getSignature())) {
                    return aMethod.replaceAndFlagsFrom(theShadowMethod);
                }
            } else {
                if (!"<clinit>".equals(theShadowMethod.getName().stringValue()) && !"<init>".equals(theShadowMethod.getName().stringValue()) && Objects.equals(theShadowMethod.getName().stringValue(), aMethod.getName().stringValue()) && theShadowMethod.getSignature().matchesExactlyTo(aMethod.getSignature())) {
                    return aMethod.replaceAndFlagsFrom(theShadowMethod);
                }
            }
        }
        return aMethod;
    }

    @Override
    public MergeResult replace(
            final BytecodeClassinfoConstant aClass, final BytecodeMethod[] aMethods, final BytecodeField[] aFields,
            final BytecodeClassinfoConstant aSuperClass,
            final BytecodeInterface[] aInterfaces,
            final BytecodeAttributeInfo[] aClassAttributes) {

        final BytecodeObjectTypeRef theObjectType = BytecodeObjectTypeRef.fromUtf8Constant(aClass.getConstant());
        final StringBuilder theShadowName = new StringBuilder("de.mirkosertic.bytecoder.classlib.").append(theObjectType.name());
        final int p = theShadowName.lastIndexOf(".");
        if (p>0) {
            theShadowName.insert(p+1, "T");
        }
        try {
            final String theShadowNameStr = theShadowName.toString();
            defaultReplacer.addTypeMap(theShadowNameStr, theObjectType.name());
            final BytecodeClass theShadowType = loader.loadByteCode(new BytecodeObjectTypeRef(theShadowNameStr), defaultReplacer);

            BytecodeClassinfoConstant theSuperClass = theShadowType.getSuperClass();
            if (theShadowType.getAttributes().getAnnotationByType(IsObject.class.getName()) != null) {
                theSuperClass = BytecodeClassinfoConstant.OBJECT_CLASS;
            }

            final BytecodeAnnotation theClassAnnotation = theShadowType.getAttributes().getAnnotationByType(SubstitutesInClass.class.getName());
            if (theClassAnnotation == null) {
                // No valid shadow type
                return new MergeResult(
                        aMethods,
                        aFields,
                        aSuperClass,
                        aInterfaces,
                        aClassAttributes
                );
            }

            if (Objects.equals(theClassAnnotation.getElementValueByName("completeReplace").stringValue(), "true")) {

                final List<BytecodeMethod> theMethods = new ArrayList<>();
                for (final BytecodeMethod aMethod : theShadowType.getMethods()) {
                    final BytecodeMethod theReplacement = replaceMethodFrom(aMethod, theShadowType);
                    // If the replacement signature contains anyref, we take the original signature as granted
                    if (theReplacement.getSignature().containsAnyMatches()) {
                        for (final BytecodeMethod theOriginal : aMethods) {
                            if (theOriginal.getName().stringValue().equals(aMethod.getName().stringValue()) && theReplacement.getSignature().matchesExactlyTo(theOriginal.getSignature())) {
                                theMethods.add(theReplacement.replaceSignature(theOriginal));
                            }
                        }
                    } else {
                        theMethods.add(theReplacement);
                    }
                }

                return new MergeResult(
                        theMethods.toArray(new BytecodeMethod[0]),
                        theShadowType.fields(),
                        theSuperClass,
                        theShadowType.getInterfaces(),
                        theShadowType.getAttributesRaw()
                );
            }

            final List<BytecodeField> theFields = new ArrayList<>();
            // Import fields from shadow type
            theFields.addAll(Arrays.asList(theShadowType.fields()));
            theFields.addAll(Arrays.asList(aFields));

            final List<BytecodeMethod> theMethods = new ArrayList<>();
            for (final BytecodeMethod aMethod : aMethods) {
                theMethods.add(replaceMethodFrom(aMethod, theShadowType));
            }

            return new MergeResult(
                    theMethods.toArray(new BytecodeMethod[0]),
                    theFields.toArray(new BytecodeField[0]),
                    theSuperClass,
                    aInterfaces,
                    aClassAttributes
            );
        } catch (final Exception  e) {
            // No shadow type found
            return new MergeResult(
                    aMethods,
                    aFields,
                    aSuperClass,
                    aInterfaces,
                    aClassAttributes
            );
        }
    }
}