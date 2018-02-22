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
package de.mirkosertic.bytecoder.core;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class BytecodeLoader {

    private final BytecodeSignatureParser signatureParser;
    private final ClassLoader classLoader;
    private final BytecodeReplacer bytecodeReplacer;

    public BytecodeLoader(ClassLoader aClassLoader) {
        classLoader = aClassLoader;
        bytecodeReplacer = new BytecodeReplacer(this);
        signatureParser = new BytecodeSignatureParser();
    }

    public BytecodeSignatureParser getSignatureParser() {
        return signatureParser;
    }

    public BytecodeClass loadByteCode(BytecodeObjectTypeRef aTypeRef) throws IOException, ClassNotFoundException {

        String theResourceName = aTypeRef.name().replace(".", "/") + ".class";
        InputStream theStream = classLoader.getResourceAsStream("modules/java.base/classes/" + theResourceName);
        if (theStream == null) {
            theStream = classLoader.getResourceAsStream(theResourceName);
            if (theStream == null) {
                throw new ClassNotFoundException(theResourceName);
            }
        }
        try (DataInputStream dis = new DataInputStream(theStream)) {
            BytecodeClassParser parser = parseHeader(dis);
            return parser.parseBody(dis);
        }
    }

    private BytecodeClassParser parseHeader(DataInput aStream) throws IOException {
        int theMagic = aStream.readInt();
        if (!(theMagic == 0xCAFEBABE)) {
            throw new IllegalArgumentException("Wrong class file format : " + theMagic);
        }
        int theMinorVersion = aStream.readUnsignedShort();
        int theMajorVersion = aStream.readUnsignedShort();
        switch (theMajorVersion) {
            case 49:
                return new Bytecode5xClassParser(new Bytecode5XProgramParser(), signatureParser, bytecodeReplacer);
            case 50:
                return new Bytecode5xClassParser(new Bytecode5XProgramParser(), signatureParser, bytecodeReplacer);
            case 51:
                return new Bytecode5xClassParser(new Bytecode5XProgramParser(), signatureParser, bytecodeReplacer);
            case 52:
                return new Bytecode5xClassParser(new Bytecode5XProgramParser(), signatureParser, bytecodeReplacer);
            case 53:
                return new Bytecode5xClassParser(new Bytecode5XProgramParser(), signatureParser, bytecodeReplacer);
        }
        throw new IllegalArgumentException("Not Supported bytecode format : " + theMajorVersion);
    }
}