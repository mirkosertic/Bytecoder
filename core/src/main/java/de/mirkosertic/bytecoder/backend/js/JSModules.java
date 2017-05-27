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
package de.mirkosertic.bytecoder.backend.js;

import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;

import java.util.HashMap;
import java.util.Map;

public class JSModules {

    private final Map<String, JSModule> modules;

    public JSModules() {
        modules = new HashMap<>();
    }

    public JSModule resolveModule(final String aModuleName) {
        JSModule theModule = modules.get(aModuleName);
        if (theModule == null) {
            // Nothing found, hence we create a pass-thru module assuming that the defined
            // variable is somewhere
            return new JSModule() {
                @Override
                public JSFunction resolveFunction(String aName) {
                    return new JSFunction(aName) {
                        @Override
                        public String generateCode(BytecodeMethodSignature aSignature) {

                            StringBuilder theResult = new StringBuilder("return ");
                            theResult.append(aModuleName);
                            theResult.append(".");
                            theResult.append(aName);
                            theResult.append("(");
                            BytecodeTypeRef[] theArguments = aSignature.getArguments();
                            for (int i=0;i<theArguments.length;i++) {
                                if (i>0) {
                                    theResult.append(",");
                                }
                                theResult.append("p" + (i + 1));
                            }
                            theResult.append(")");
                            return theResult.toString();
                        }
                    };
                }
            };
        }
        return theModule;
    }

    public void register(String aName, JSModule aModule) {
        modules.put(aName, aModule);
    }
}
