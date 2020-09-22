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

import de.mirkosertic.bytecoder.api.ClassLibProvider;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class BytecodeLoader {

    private final BytecodeSignatureParser signatureParser;
    private final ClassLoader classLoader;
    private final BytecodeReplacer bytecodeReplacer;
    private final BytecodeShadowReplacer shadowReplacer;

    public BytecodeLoader(final ClassLoader aClassLoader) {
        classLoader = aClassLoader;
        bytecodeReplacer = new BytecodeReplacer(this);
        shadowReplacer = new BytecodeShadowReplacer(this, bytecodeReplacer);
        signatureParser = new BytecodeSignatureParser(bytecodeReplacer);
    }

    public BytecodeSignatureParser getSignatureParser() {
        return signatureParser;
    }

    public BytecodeClass loadByteCode(final BytecodeObjectTypeRef aTypeRef) throws IOException, ClassNotFoundException {
        return loadByteCode(aTypeRef, bytecodeReplacer);
    }

    public BytecodeClass loadByteCode(final BytecodeObjectTypeRef aTypeRef, final BytecodeReplacer aDefaultReplacer) throws IOException, ClassNotFoundException {
        final String theResourceName = aTypeRef.name().replace(".", "/") + ".class";

        for (final ClassLibProvider theProvider : ClassLibProvider.availableProviders()) {
            final InputStream theStream = classLoader.getResourceAsStream(theProvider.getResourceBase() + "/" + theResourceName);
            if (theStream != null) {
                try (final DataInputStream dis = new DataInputStream(theStream)) {
                    final BytecodeClassParser parser = parseHeader(dis, shadowReplacer);
                    return parser.parseBody(dis);
                }
            }
        }

        if (aTypeRef.name().startsWith("java.") || aTypeRef.name().startsWith("javax.")) {
            throw new IllegalArgumentException("Not supported Java module for class " + aTypeRef.name());
        }

        final InputStream theStream = classLoader.getResourceAsStream(theResourceName);
        if (theStream == null) {
            throw new ClassNotFoundException(theResourceName);
        }
        try (final DataInputStream dis = new DataInputStream(theStream)) {
            final BytecodeClassParser parser = parseHeader(dis, aDefaultReplacer);
            return parser.parseBody(dis);
        }
    }

    private BytecodeClassParser parseHeader(final DataInput aStream, final BytecodeReplacer aReplacer) throws IOException {
        final int theMagic = aStream.readInt();
        if (!(theMagic == 0xCAFEBABE)) {
            throw new IllegalArgumentException("Wrong class file format : " + theMagic);
        }
        final int theMinorVersion = aStream.readUnsignedShort();
        final int theMajorVersion = aStream.readUnsignedShort();
        switch (theMajorVersion) {
            case 47:
                return new Bytecode5xClassParser(new Bytecode5XProgramParser(), signatureParser, aReplacer);
            case 48:
                return new Bytecode5xClassParser(new Bytecode5XProgramParser(), signatureParser, aReplacer);
            case 49:
                return new Bytecode5xClassParser(new Bytecode5XProgramParser(), signatureParser, aReplacer);
            case 50:
                return new Bytecode5xClassParser(new Bytecode5XProgramParser(), signatureParser, aReplacer);
            case 51:
                return new Bytecode5xClassParser(new Bytecode5XProgramParser(), signatureParser, aReplacer);
            case 52:
                return new Bytecode5xClassParser(new Bytecode5XProgramParser(), signatureParser, aReplacer);
            case 53:
                return new Bytecode5xClassParser(new Bytecode5XProgramParser(), signatureParser, aReplacer);
            case 54:
                return new Bytecode5xClassParser(new Bytecode5XProgramParser(), signatureParser, aReplacer);
            case 55:
                return new Bytecode5xClassParser(new Bytecode5XProgramParser(), signatureParser, aReplacer);
            case 56:
                return new Bytecode5xClassParser(new Bytecode5XProgramParser(), signatureParser, aReplacer);
            case 57:
                return new Bytecode5xClassParser(new Bytecode5XProgramParser(), signatureParser, aReplacer);
            case 58:
                return new Bytecode5xClassParser(new Bytecode5XProgramParser(), signatureParser, aReplacer);
            case 59:
                return new Bytecode5xClassParser(new Bytecode5XProgramParser(), signatureParser, aReplacer);
        }
        throw new IllegalArgumentException("Not Supported bytecode format : " + theMajorVersion);
    }
}