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

import de.mirkosertic.bytecoder.api.Substitutes;

import java.util.Objects;

public class BytecodeReplacer {

    private final BytecodeLoader loader;

    public BytecodeReplacer(BytecodeLoader aLoader) {
        loader = aLoader;
    }

    private BytecodeMethod replaceMethodFrom(BytecodeMethod aMethod, BytecodeClass aShadowType) {
        for (BytecodeMethod theShadowMethod : aShadowType.getMethods()) {
            BytecodeAnnotation theAnnotation = theShadowMethod.getAttributes().getAnnotationByType(Substitutes.class.getName());
            if (theAnnotation != null) {
                String theMethodName = theAnnotation.getElementValueByName("value").stringValue();
                if (Objects.equals(theMethodName, aMethod.getName().stringValue()) && theShadowMethod.getSignature().metchesExactlyTo(aMethod.getSignature())) {
                    return aMethod.replaceAndFlagsFrom(theShadowMethod);
                }
            }
        }
        return aMethod;
    }

    public BytecodeMethod[] replace(
            BytecodeClassinfoConstant aClass, BytecodeMethod[] aMethods) {
        BytecodeObjectTypeRef theObjectType = BytecodeObjectTypeRef.fromUtf8Constant(aClass.getConstant());
        StringBuilder theShadowName = new StringBuilder("de.mirkosertic.bytecoder.classlib.shadow." + theObjectType.name());
        int p = theShadowName.lastIndexOf(".");
        if (p>0) {
            theShadowName.insert(p+1, "T");
        }
        try {
            BytecodeClass theShadowType = loader.loadByteCode(new BytecodeObjectTypeRef(theShadowName.toString()));
            BytecodeMethod[] theMethods = new BytecodeMethod[aMethods.length];
            for (int i=0;i<aMethods.length;i++) {
                theMethods[i] = replaceMethodFrom(aMethods[i], theShadowType);
            }
            return theMethods;
        } catch (Exception  e) {
            // No shadow type found
            return aMethods;
        }
    }
}
