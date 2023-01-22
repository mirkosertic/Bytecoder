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
package de.mirkosertic.bytecoder.asm.backend.js;

import de.mirkosertic.bytecoder.api.ClassLibProvider;
import de.mirkosertic.bytecoder.asm.AnnotationUtils;
import de.mirkosertic.bytecoder.asm.Graph;
import de.mirkosertic.bytecoder.asm.ResolvedClass;
import de.mirkosertic.bytecoder.asm.ResolvedField;
import de.mirkosertic.bytecoder.asm.ResolvedMethod;
import de.mirkosertic.bytecoder.asm.optimizer.Optimizer;
import de.mirkosertic.bytecoder.asm.parser.CompileUnit;
import de.mirkosertic.bytecoder.asm.parser.ConstantPool;
import de.mirkosertic.bytecoder.asm.sequencer.DominatorTree;
import de.mirkosertic.bytecoder.asm.sequencer.Sequencer;
import de.mirkosertic.bytecoder.backend.CompileResult;
import de.mirkosertic.bytecoder.core.ReflectionConfiguration;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static de.mirkosertic.bytecoder.asm.backend.js.JSHelpers.generateClassName;
import static de.mirkosertic.bytecoder.asm.backend.js.JSHelpers.generateFieldName;
import static de.mirkosertic.bytecoder.asm.backend.js.JSHelpers.generateMethodName;

public class JSBackend {

    private void generateHeader(final CompileUnit compileUnit, final PrintWriter pw) {

        pw.println("function decodeUtf16(w) {\n" +
                "    var i = 0;\n" +
                "    var len = w.length;\n" +
                "    var w1, w2;\n" +
                "    var charCodes = [];\n" +
                "    while (i < len) {\n" +
                "        var w1 = w[i++];\n" +
                "        if ((w1 & 0xF800) !== 0xD800) { // w1 < 0xD800 || w1 > 0xDFFF\n" +
                "            if (w1 != 0) charCodes.push(w1);\n" +
                "            continue;\n" +
                "        }\n" +
                "        if ((w1 & 0xFC00) === 0xD800) { // w1 >= 0xD800 && w1 <= 0xDBFF\n" +
                "            throw new RangeError('Invalid octet 0x' + w1.toString(16) + ' at offset ' + (i - 1));\n" +
                "        }\n" +
                "        if (i === len) {\n" +
                "            throw new RangeError('Expected additional octet');\n" +
                "        }\n" +
                "        w2 = w[i++];\n" +
                "        if ((w2 & 0xFC00) !== 0xDC00) { // w2 < 0xDC00 || w2 > 0xDFFF)\n" +
                "            throw new RangeError('Invalid octet 0x' + w2.toString(16) + ' at offset ' + (i - 1));\n" +
                "        }\n" +
                "        charCodes.push(((w1 & 0x3ff) << 10) + (w2 & 0x3ff) + 0x10000);\n" +
                "    }\n" +
                "    return String.fromCharCode.apply(String, charCodes);\n" +
                "};");
        pw.println("const bytecoder = {");
        pw.println("  imports: {");
        pw.println("    \"java.lang.Object.Ljava$lang$Class$$getClass$$\": function(inst) {");
        pw.println("        return inst.constructor.$rt;");
        pw.println("    },");
        pw.println("    \"de.mirkosertic.bytecoder.classlib.VM.Ljava$lang$Class$$bytePrimitiveClass$$\": function() {");
        pw.println("        return bytecoder.primitives.byte;");
        pw.println("    },");
        pw.println("    \"de.mirkosertic.bytecoder.classlib.VM.Ljava$lang$Class$$charPrimitiveClass$$\": function() {");
        pw.println("        return bytecoder.primitives.char;");
        pw.println("    },");
        pw.println("    \"de.mirkosertic.bytecoder.classlib.VM.Ljava$lang$Class$$shortPrimitiveClass$$\": function() {");
        pw.println("        return bytecoder.primitives.short;");
        pw.println("    },");
        pw.println("    \"de.mirkosertic.bytecoder.classlib.VM.Ljava$lang$Class$$intPrimitiveClass$$\": function() {");
        pw.println("        return bytecoder.primitives.int;");
        pw.println("    },");
        pw.println("    \"de.mirkosertic.bytecoder.classlib.VM.Ljava$lang$Class$$floatPrimitiveClass$$\": function() {");
        pw.println("        return bytecoder.primitives.float;");
        pw.println("    },");
        pw.println("    \"de.mirkosertic.bytecoder.classlib.VM.Ljava$lang$Class$$doublePrimitiveClass$$\": function() {");
        pw.println("        return bytecoder.primitives.double;");
        pw.println("    },");
        pw.println("    \"de.mirkosertic.bytecoder.classlib.VM.Ljava$lang$Class$$longPrimitiveClass$$\": function() {");
        pw.println("        return bytecoder.primitives.long;");
        pw.println("    },");
        pw.println("    \"de.mirkosertic.bytecoder.classlib.VM.Ljava$lang$Class$$booleanPrimitiveClass$$\": function() {");
        pw.println("        return bytecoder.primitives.boolean;");
        pw.println("    },");
        pw.println("    \"jdk.internal.misc.ScopedMemoryAccess.V$registerNatives$$\": function() {");
        pw.println("    },");
        pw.println("    \"java.lang.Float.I$floatToRawIntBits$F\": function(value) {");
        pw.println("        let fl = new Float32Array(1);");
        pw.println("        fl[0] = value;");
        pw.println("        let br = new Int32Array(fl.buffer);");
        pw.println("        return br[0];");
        pw.println("    },");
        pw.println("    \"java.lang.Double.Z$isNaN$D\": function(a) {");
        pw.println("        return isNaN(a) ? 1 : 0");
        pw.println("    },");
        pw.println("    \"java.lang.Float.Z$isNaN$F\": function(a) {");
        pw.println("        return isNaN(a) ? 1 : 0");
        pw.println("    },");
        pw.println("    \"java.lang.Math.I$min$I$I\": function(a,b) {");
        pw.println("        return Math.min(a,b);");
        pw.println("    },");
        pw.println("    \"java.lang.Math.F$min$F$F\": function(a,b) {");
        pw.println("        return Math.min(a,b);");
        pw.println("    },");
        pw.println("    \"java.lang.Math.I$max$I$I\": function(a,b) {");
        pw.println("        return Math.max(a,b);");
        pw.println("    },");
        pw.println("    \"java.lang.Math.D$floor$D\": function(a) {");
        pw.println("        return Math.floor(a);");
        pw.println("    },");
        pw.println("    \"java.lang.Math.F$floor$F\": function(a) {");
        pw.println("        return Math.floor(a);");
        pw.println("    },");
        pw.println("    \"java.lang.Math.D$ceil$D\": function(a) {");
        pw.println("        return Math.ceil(a);");
        pw.println("    },");
        pw.println("    \"java.lang.Math.F$ceil$F\": function(a) {");
        pw.println("        return Math.ceil(a);");
        pw.println("    },");
        pw.println("    \"java.lang.Math.D$toRadians$D\": function(a) {");
        pw.println("        return a * (Math.PI/180.0);");
        pw.println("    },");
        pw.println("    \"java.lang.Math.D$cos$D\": function(a) {");
        pw.println("        return Math.cos(a);");
        pw.println("    },");
        pw.println("    \"java.lang.Math.D$sin$D\": function(a) {");
        pw.println("        return Math.sin(a);");
        pw.println("    },");
        pw.println("    \"java.lang.StringUTF16.Z$isBigEndian$$\": function() {");
        pw.println("        return 1;");
        pw.println("    },");
        pw.println("    \"java.lang.reflect.Array.Ljava$lang$Object$$newArray$Ljava$lang$Class$$I\": function(t, l) {");
        pw.println("        return bytecoder.newarray(l, null);");
        pw.println("    },");
        pw.println("    \"jdk.internal.misc.CDS.Z$isDumpingClassList0$$\": function() {");
        pw.println("        return 0;");
        pw.println("    },");
        pw.println("    \"jdk.internal.misc.CDS.Z$isDumpingArchive0$$\": function() {");
        pw.println("        return 0;");
        pw.println("    },");
        pw.println("    \"jdk.internal.misc.CDS.Z$isSharingEnabled0$$\": function() {");
        pw.println("        return 0;");
        pw.println("    },");
        pw.println("    \"jdk.internal.misc.CDS.V$initializeFromArchive$Ljava$lang$Class$\": function(cls) {");
        pw.println("    },");
        pw.println("    \"java.io.UnixFileSystem.I$getBooleanAttributes0$Ljava$lang$String$\": function(fsref, path) {");
        pw.println("        let jsPath = bytecoder.toJSString(path);");
        pw.println("        try {");
        pw.println("          let request = new XMLHttpRequest();");
        pw.println("          request.open('HEAD',jsPath,false);");
        pw.println("          request.send(null);");
        pw.println("          if (request.status == 200) {");
        pw.println("            let length = request.getResponseHeader('content-length');");
        pw.println("            return 0x01;");
        pw.println("          }");
        pw.println("          return 0;");
        pw.println("        } catch(e) {");
        pw.println("          return 0;");
        pw.println("        }");
        pw.println("    },");
        pw.println("    \"java.lang.Class.Ljava$lang$ClassLoader$$getClassLoader$$\": function(classRef) {");
        pw.println("        return null;");
        pw.println("    },");
        pw.println("    \"java.io.FileInputStream.I$open0$Ljava$lang$String$\": function(fis, name) {");
        pw.println("        let fd = bytecoder.openForRead(bytecoder.toJSString(name));");
        pw.println("        if (fd >= 0) fis.fd.fd = fd;");
        pw.println("        return fd;");
        pw.println("    },");
        pw.println("    \"java.io.FileInputStream.J$skip0$I\": function(fis, amount) {");
        pw.println("        let fd = fis.fd.fd;");
        pw.println("        let x = bytecoder.filehandles[fd];");
        pw.println("        return x.J$skip0$I(fd, amount);");
        pw.println("    },");
        pw.println("    \"java.io.FileInputStream.I$available0$$\": function(fis) {");
        pw.println("        let fd = fis.fd.fd;");
        pw.println("        let x = bytecoder.filehandles[fd];");
        pw.println("        return x.I$available0$$(fd);");
        pw.println("    },");
        pw.println("    \"java.io.FileInputStream.I$read0$$\": function(fis) {");
        pw.println("        let fd = fis.fd.fd;");
        pw.println("        let x = bytecoder.filehandles[fd];");
        pw.println("        return x.I$read0$$(fd);");
        pw.println("    },");
        pw.println("    \"java.io.FileInputStream.I$readBytes$$B$I$I\": function(fis, b, off, len) {");
        pw.println("        let fd = fis.fd.fd;");
        pw.println("        let x = bytecoder.filehandles[fd];");
        pw.println("        return x.I$readBytes$$B$I$I(fd, b, off, len);");
        pw.println("    },");
        pw.println("    \"java.io.FileOutputStream.V$writeBytes$$B$I$I\": function(fis, b, off, len) {");
        pw.println("        let fd = fis.fd.fd;");
        pw.println("        let x = bytecoder.filehandles[fd];");
        pw.println("        x.V$writeBytes$$B$I$I(fd, b, off, len);");
        pw.println("    },");
        pw.println("    \"java.io.FileOutputStream.V$writeInt$I\": function(fis, cp) {");
        pw.println("        let fd = fis.fd.fd;");
        pw.println("        let x = bytecoder.filehandles[fd];");
        pw.println("        x.V$writeInt$I(fd, cp);");
        pw.println("    },");
        pw.println("    \"java.lang.Class.Ljava$lang$Class$$forName$Ljava$lang$String$$Z$Ljava$lang$ClassLoader$\": function(className, initialize, classLoader) {");

        for (final ReflectionConfiguration.ReflectiveClass rc : compileUnit.getReflectionConfiguration().configuredClasses()) {
            if (rc.supportsClassForName()) {
                final Type cl = Type.getObjectType(rc.getName().replace('.', '/'));
                final int idx = compileUnit.getConstantPool().getPooledStrings().indexOf(rc.getName());
                pw.print("        if (bytecoder.stringconstants[");
                pw.print(idx);
                pw.println("].Z$equals$Ljava$lang$Object$(className)) {");
                pw.print("          // ");
                pw.println(compileUnit.getConstantPool().getPooledStrings().get(idx));
                pw.print("          return ");
                pw.print(generateClassName(cl));
                pw.println(".$rt;");
                pw.println("        }");
            }
        }

        pw.println("        throw 'Not supported class for reflective access';");
        pw.println("    },");
        pw.println("    \"java.lang.invoke.LambdaMetafactory.Ljava$lang$invoke$CallSite$$metafactory$Ljava$lang$invoke$MethodHandles$Lookup$$Ljava$lang$String$$Ljava$lang$invoke$MethodType$$Ljava$lang$invoke$MethodType$$Ljava$lang$invoke$MethodHandle$$Ljava$lang$invoke$MethodType$\": function(lookups, methodName, invokedType, samMethodType,implMethod, aInstantiatedMethodType) {");
        pw.println("      return {");
        pw.println("        lookups: lookups,");
        pw.println("        methodName: methodName,");
        pw.println("        invokedType: invokedType,");
        pw.println("        samMethodType: samMethodType,");
        pw.println("        implMethod: implMethod,");
        pw.println("        aInstantiatedMethodType: aInstantiatedMethodType,");
        pw.println("        Ljava$lang$invoke$MethodHandle$$getTarget$$: function() {");
        pw.println("          return {");
        pw.println("            lambdaref: this,");
        pw.println("            invokeExact: function() {");
        pw.println("              let staticargs = arguments;");
        pw.println("              let implmethod = this.lambdaref.implMethod;");
        pw.println("              let instType = this.lambdaref.invokedType[0];");
        pw.println("              let inst = null;");
        pw.println("              if (instType.$modifiers & 512 > 0) {");
        pw.println("                let cl = class extends instType(java$lang$Object) {");
        pw.println("                };");
        pw.println("                inst = new cl();");
        pw.println("              } else {");
        pw.println("                inst = new instType();");
        pw.println("              }");
        pw.println("              inst.$lambdaimpl = function() {");
        pw.println("                let dynamicargs = arguments;");
        pw.println("                let statargs = staticargs;");
        pw.println("                let impl = implmethod;");
        pw.println("                impl.impl();");
        pw.println("              };");
        pw.println("              return inst;");
        pw.println("            }");
        pw.println("          };");
        pw.println("        }");
        pw.println("      };");
        pw.println("    }");
        pw.println("  },");
        pw.println("  exports: {},");
        pw.println("  filehandles : [],");
        pw.println("  stringconstants: [],");
        pw.println("  cmp: function(a,b) {");
        pw.println("    if (a > b) return 1;");
        pw.println("    if (a < b) return -1;");
        pw.println("    return 0;");
        pw.println("  },");
        pw.println("  instanceOf: function(a,b) {");
        pw.println("    if (a) {");
        pw.println("        let rt = a.constructor.$rt;");
        pw.println("        return rt.instanceOf(a, b);");
        pw.println("    }");
        pw.println("    return 0;");
        pw.println("  },");
        pw.println("  registerStack: function(exception, stack) {");
        pw.println("    exception.stack = stack;");
        pw.println("    return exception;");
        pw.println("  },");
        pw.println("  toJSString: function(str) {");
        pw.println("    if (str && str.value.data) {return decodeUtf16(str.value.data);} else return '';");
        pw.println("  },");
        pw.println("  newarray: function(len, defaultvalue) {");
        pw.println("    let x = new de$mirkosertic$bytecoder$classlib$Array();");
        pw.println("    x.data = new Array(len);");
        pw.println("    x.data.fill(defaultvalue);");
        pw.println("    return x;");
        pw.println("  },");
        pw.println("  methodHandle: function(method, access) {");
        pw.println("    return {");
        pw.println("        impl: method,");
        pw.println("        access: access,");
        pw.println("        invokeExact: function() {");
        pw.println("          //throw 'not implemented';");
        pw.println("        }");
        pw.println("    };");
        pw.println("  },");
        pw.println("  primitives: {");
        pw.println("    byte: {");
        pw.println("    },");
        pw.println("    char: {");
        pw.println("    },");
        pw.println("    short: {");
        pw.println("    },");
        pw.println("    int: {");
        pw.println("    },");
        pw.println("    float: {");
        pw.println("    },");
        pw.println("    double: {");
        pw.println("    },");
        pw.println("    long: {");
        pw.println("    },");
        pw.println("    boolean: {");
        pw.println("    },");
        pw.println("    void: {");
        pw.println("    }");
        pw.println("  },");
        pw.println("  openForRead :  function(path) {");
        pw.println("    try {");
        pw.println("      let request = new XMLHttpRequest();");
        pw.println("      request.open('GET',path,false);");
        pw.println("      request.overrideMimeType('text/plain; charset=x-user-defined');");
        pw.println("      request.send(null);");
        pw.println("      if (request.status == 200) {");
        pw.println("        let length = request.getResponseHeader('content-length');");
        pw.println("        let responsetext = request.response;");
        pw.println("        let buf = new ArrayBuffer(responsetext.length);");
        pw.println("        let bufView = new Uint8Array(buf);");
        pw.println("        for (var i=0, strLen = responsetext.length; i < strLen; i++) {");
        pw.println("          bufView[i] = responsetext.charCodeAt(i) & 0xff;");
        pw.println("        }");
        pw.println("        let handle = bytecoder.filehandles.length;");
        pw.println("        bytecoder.filehandles[handle] = {");
        pw.println("          currentpos: 0,");
        pw.println("          data: bufView,");
        pw.println("          size: length,");
        pw.println("          J$skip0$I: function(fd, amount) {");
        pw.println("            let remaining = this.size - this.currentpos;");
        pw.println("            let possible = Math.min(remaining, amount);");
        pw.println("            this.currentpos += possible;");
        pw.println("            return possible;");
        pw.println("          },");
        pw.println("          I$available0$$: function(fd) {");
        pw.println("            return this.size - this.currentpos;");
        pw.println("          },");
        pw.println("          I$read0$$: function(fd) {");
        pw.println("            return this.data[this.currentpos++];");
        pw.println("          },");
        pw.println("          I$readBytes$$B$I$I: function(fd, target, offset, length) {");
        pw.println("            if (length === 0) {return 0;}");
        pw.println("            let remaining = this.size - this.currentpos;");
        pw.println("            let possible = Math.min(remaining, length);");
        pw.println("            if (possible === 0) {return -1;}");
        pw.println("            for (var j=0;j<possible;j++) {");
        pw.println("              target.data[offset++] = this.data[this.currentpos++];");
        pw.println("            }");
        pw.println("            return possible;");
        pw.println("          }");
        pw.println("        };");
        pw.println("        return handle;");
        pw.println("      }");
        pw.println("      return -1;");
        pw.println("    } catch(e) {");
        pw.println("      return -1;");
        pw.println("    }");
        pw.println("  },");
        pw.println("  newRuntimeClassFor: function(type,supportedtypes) {");
        pw.println("    return {");
        pw.println("      Ljava$lang$ClassLoader$$getClassLoader$$: function() {");
        pw.println("         return null;");
        pw.println("      },");
        pw.println("      Ljava$lang$ClassLoader$$getClassLoader0$$: function() {");
        pw.println("         return null;");
        pw.println("      },");
        pw.println("      Z$desiredAssertionStatus$$: function() {");
        pw.println("         return false;");
        pw.println("      },");
        pw.println("      Ljava$lang$Object$$newInstance$$: function() {");
        pw.println("         const x = new type.$i();");
        pw.println("         x.V$$init$$$();");
        pw.println("         return x;");
        pw.println("      },");
        pw.println("      $Ljava$lang$Object$$getEnumConstants$$: function() {");
        pw.println("         return type.$i.$VALUES;");
        pw.println("      },");
        pw.println("      instanceOf: function(a, b) {");
        pw.println("         if (supportedtypes.includes(b)) {");
        pw.println("           return 1;");
        pw.println("         }");
        pw.println("         return 0;");
        pw.println("      },");
        pw.println("    };");
        pw.println("  }");
        pw.println("};");
        pw.println();

        pw.println("bytecoder.filehandles[1] = {");
        pw.println("  V$writeBytes$$B$I$I: function(fd, b, off, len) {");
        pw.println("    let decoder = new TextDecoder();");
        pw.println("    let arr = new Uint8Array(b.data.slice(off, off +  len));");
        pw.println("    console.log(decoder.decode(arr).replace('\\n', ''));");
        pw.println("  },");
        pw.println("  V$writeInt$I: function(fd, cp) {");
        pw.println("    let decoder = new TextDecoder();");
        pw.println("    let arr = new Uint8Array([cp]);");
        pw.println("    console.log(decoder.decode(arr).replace('\\n', ''));");
        pw.println("  },");
        pw.println("};");
    }

    public JSCompileResult generateCodeFor(final CompileUnit compileUnit, final CompileOptions compileOptions) {

        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);

        generateHeader(compileUnit, pw);

        for (final ResolvedClass cl : compileUnit.computeClassDependencies()) {
            final String className = generateClassName(cl.type);
            if (Modifier.isInterface(cl.classNode.access)) {
                pw.print("const ");
                pw.print(className);

                int bracesCounter = 0;
                final StringBuilder extendsClause = new StringBuilder();
                if (!cl.superClass.type.getClassName().equals(Object.class.getName())) {
                    extendsClause.append(generateClassName(cl.superClass.type));
                    extendsClause.append("(");
                    bracesCounter++;
                }
                for (int i = cl.interfaces.length - 1; i >= 0; i--) {
                    extendsClause.insert(0, generateClassName(cl.interfaces[i].type) + "(");
                    bracesCounter++;
                }

                pw.print(" = (superclass) => class extends ");
                if (extendsClause.length() > 0) {
                    pw.print(extendsClause);
                }
                pw.print("superclass");
                for (int i = 0; i < bracesCounter; i++) {
                    pw.print(")");
                }
                pw.println(" {");

                generateFieldsFor(pw, compileUnit, cl);

                generateClassInitFor(pw, compileUnit, cl);

                generateLambdaLogicFor(pw, compileUnit, cl);

                generateMethodsFor(pw, compileUnit, cl, compileOptions);

                pw.println("};");

                pw.print(className);
                pw.print(".$modifiers = ");
                pw.print(cl.classNode.access);
                pw.println(";");

                pw.println();
            } else {
                pw.print("class ");
                pw.print(className);

                int bracesCounter = 0;
                final StringBuilder extendsClause = new StringBuilder();
                for (int i = 0; i < cl.interfaces.length; i++) {
                    extendsClause.append(generateClassName(cl.interfaces[i].type)).append("(");
                    bracesCounter++;
                }
                if (cl.superClass != null) {
                    extendsClause.append(generateClassName(cl.superClass.type));
                }
                for (int i = 0; i < bracesCounter; i++) {
                    extendsClause.append(")");
                }

                if (extendsClause.length() > 0) {
                    pw.print(" extends ");
                    pw.print(extendsClause);
                }

                pw.print(" ");
                pw.println("{");

                pw.print("  static $modifiers = ");
                pw.print(cl.classNode.access);
                pw.println(";");

                generateFieldsFor(pw, compileUnit, cl);

                pw.println("  constructor() {");
                if (cl.superClass != null) {
                    pw.println("    super();");
                }
                pw.println("  }");

                generateClassInitFor(pw, compileUnit, cl);

                generateLambdaLogicFor(pw, compileUnit, cl);

                generateMethodsFor(pw, compileUnit, cl, compileOptions);

                pw.println("}");
                pw.println();
            }
        }

        // Generate string pool
        final String stringInitConstructor = generateMethodName("<init>", Type.getMethodType("([BB)V"));
        final ConstantPool constantPool = compileUnit.getConstantPool();
        final List<String> pooledStrings = constantPool.getPooledStrings();
        for (int i = 0; i < pooledStrings.size(); i++) {
            pw.print("arr");
            pw.print(i);
            pw.println(" = bytecoder.newarray(0);");
            pw.print("arr");
            pw.print(i);
            pw.print(".data = [");
            final byte[] b = pooledStrings.get(i).getBytes(StandardCharsets.ISO_8859_1);
            for (int j = 0;j < b.length; j++) {
                if (j > 0) {
                    pw.print(",");
                }
                pw.print(b[j]);
            }
            pw.println("];");
            pw.print("bytecoder.stringconstants[");
            pw.print(i);
            pw.print("] = new ");
            pw.print(generateClassName(Type.getType(String.class)));
            pw.println(".$i();");
            pw.print("bytecoder.stringconstants[");
            pw.print(i);
            pw.print("].");
            pw.print(stringInitConstructor);
            pw.print("(arr");
            pw.print(i);
            pw.println(", 0);");

        }

        // Generate exports
        compileUnit.processExportedMethods((s, method) -> {
            pw.print("bytecoder.exports['");
            pw.print(s);
            pw.print("'] = ");
            pw.print(generateClassName(method.owner.type));
            pw.print(".");
            pw.print(generateMethodName(method.methodNode.name, Type.getMethodType(method.methodNode.desc)));
            pw.println(";");
        });

        pw.flush();

        final JSCompileResult result = new JSCompileResult();
        result.add(new CompileResult.StringContent("classes.js", sw.toString()));

        final List<String> resourcesToInclude = new ArrayList<>();
        for (final ClassLibProvider provider : ClassLibProvider.availableProviders()) {
            Collections.addAll(resourcesToInclude, provider.additionalResources());
        }
        Collections.addAll(resourcesToInclude, compileOptions.getAdditionalResources());

        // Finally, we add the list of additional resources to the result
        for (final String theResource : resourcesToInclude) {
            final URL theUrl = compileUnit.getLoader().getResource(theResource);
            if (theUrl != null) {
                result.add(new CompileResult.URLContent(theResource, theUrl));
            } else {
                //aOptions.getLogger().warn("Cannot find resource {}", theResource);
            }
        }

        return result;
    }

    private void generateLambdaLogicFor(final PrintWriter pw, final CompileUnit compileUnit, final ResolvedClass cl) {
        pw.println();
        pw.println("  set $lambdaimpl(impl) {");

        final Set<ResolvedMethod> abstractMethods = cl.abstractResolvedMethods();
        if (abstractMethods.size() == 1) {
            final ResolvedMethod m = abstractMethods.iterator().next();
            final String methodName = generateMethodName(m.methodNode.name, Type.getMethodType(m.methodNode.desc));
            pw.print("    this.");
            pw.print(methodName);
            pw.println(" = impl;");
        }
        pw.println("  }");
    }

    private void generateClassInitFor(final PrintWriter pw, final CompileUnit compileUnit, final ResolvedClass cl) {

        pw.println();
        pw.println("  static #rt = undefined;");
        pw.println("  static get $rt() {");
        pw.println("    if (!this.#rt) {");
        pw.print("      this.#rt = bytecoder.newRuntimeClassFor(");
        pw.print(generateClassName(cl.type));
        pw.print(",[");
        boolean f = true;
        for (final ResolvedClass type : cl.allTypesOf()) {
            if (f) {
                f = false;
            } else {
                pw.print(",");
            }
            pw.print(generateClassName(type.type));
        }
        pw.println("]);");

        pw.println("    }");
        pw.println("    return this.#rt;");
        pw.println("  }");

        if (cl.requiresClassInitializer()) {
            pw.println();
            pw.println("  static #iguard = false;");
            pw.println("  static get $i() {");
            pw.println("    if (!this.#iguard) {");
            pw.println("      this.#iguard = true;");
            if (cl.superClass != null && cl.superClass.requiresClassInitializer()) {
                pw.print("      ");
                pw.print(generateClassName(cl.superClass.type));
                pw.println(".$i;");
            }

            if (cl.classInitializer != null) {
                pw.print("      this.");
                pw.print(generateMethodName("<clinit>", Type.getMethodType(cl.classInitializer.methodNode.desc)));
                pw.println("();");
            }
            pw.println("    }");
            pw.println("    return this;");

            pw.println("  }");
        }
    }

    private void generateFieldsFor(final PrintWriter pw, final CompileUnit compileUnit, final ResolvedClass cl) {
        if (!cl.resolvedFields.isEmpty()) {
            pw.println();
            for (final ResolvedField f : cl.resolvedFields) {
                pw.print("  ");
                if (Modifier.isStatic(f.access)) {
                    pw.print("static ");
                }
                pw.print(generateFieldName(f.name));
                pw.print(" ");
                switch (f.type.getSort()) {
                    case Type.FLOAT:
                    case Type.DOUBLE:
                        pw.print(" = 0.0");
                        break;
                    case Type.BOOLEAN:
                        pw.print(" = false");
                        break;
                    case Type.ARRAY:
                    case Type.OBJECT:
                    case Type.METHOD:
                        pw.print(" = null");
                        break;
                    default:
                        pw.print(" = 0");
                        break;
                }
                pw.println(";");
            }

            pw.println();
        }
    }

    public void generateMethodsFor(final PrintWriter pw, final CompileUnit compileUnit, final ResolvedClass cl, final CompileOptions compileOptions) {
        for (final ResolvedMethod m : cl.resolvedMethods) {
            if (m.owner == cl) {
                if (Modifier.isNative(m.methodNode.access) || AnnotationUtils.hasAnnotation("Lde/mirkosertic/bytecoder/api/EmulatedByRuntime;", m.methodNode.visibleAnnotations)) {
                    generateNativeMethod(pw, compileUnit, cl, m);
                } else {
                    if (m.methodBody != null) {
                        generateMethod(pw, compileUnit, cl, m, compileOptions);
                    }
                }
            }
        }
    }

    public void generateNativeMethod(final PrintWriter pw, final CompileUnit compileUnit, final ResolvedClass cl, final ResolvedMethod m) {
        System.out.println("Writing native method for " + cl.type + " . " + m.methodNode.name + m.methodNode.desc);

        pw.println();
        pw.print("  ");
        if (Modifier.isStatic(m.methodNode.access)) {
            pw.print("static ");
        }
        final String methodName = generateMethodName(m.methodNode.name, Type.getMethodType(m.methodNode.desc));
        pw.print(methodName);

        final Type[] arguments = Type.getArgumentTypes(m.methodNode.desc);
        final Type returnType = Type.getReturnType(m.methodNode.desc);

        pw.print("(");
        for (int i = 0; i < arguments.length; i++) {
            if (i > 0) {
                pw.print(",");
            }
            pw.print("arg");
            pw.print(i);
        }
        pw.println(") {");

        if (!returnType.equals(Type.VOID_TYPE)) {
            pw.print("    return ");
        } else {
            pw.print("    ");
        }
        pw.print("bytecoder.imports['");
        pw.print(m.owner.type.getClassName());
        pw.print(".");
        pw.print(methodName);
        pw.print("'](");

        if (!Modifier.isStatic(m.methodNode.access)) {
            pw.print("this");
            for (int i = 0; i < arguments.length; i++) {
                pw.print(", arg");
                pw.print(i);
            }
        } else {
            for (int i = 0; i < arguments.length; i++) {
                if (i > 0) {
                    pw.print(", ");
                }
                pw.print("arg");
                pw.print(i);
            }
        }

        pw.println(");");

        pw.println("  }");
    }

    public void generateMethod(final PrintWriter pw, final CompileUnit compileUnit, final ResolvedClass cl, final ResolvedMethod m, final CompileOptions options) {
        System.out.println("Writing method for " + cl.type + " . " + m.methodNode.name + m.methodNode.desc);

        pw.println();
        pw.print("  ");
        if (Modifier.isStatic(m.methodNode.access)) {
            pw.print("static ");
        }
        final String methodName = generateMethodName(m.methodNode.name, Type.getMethodType(m.methodNode.desc));
        pw.print(methodName);

        final Type[] arguments = Type.getArgumentTypes(m.methodNode.desc);

        pw.print("(");
        for (int i = 0; i < arguments.length; i++) {
            if (i > 0) {
                pw.print(",");
            }
            pw.print("arg");
            pw.print(i);
        }
        pw.println(") {");

        final Graph g = m.methodBody;
        final Optimizer o = options.getOptimizer();


        while (o.optimize(m, g)) {
            //
        }

        final DominatorTree dt = new DominatorTree(g);

        try {
            if (cl.classNode.sourceFile != null) {
                pw.print("    // source file is ");
                pw.println(cl.classNode.sourceFile);
            }

            new Sequencer(g, dt, new JSStructuredControlflowCodeGenerator(compileUnit, cl, pw));

        } catch (final Exception ex) {

            try {
                g.writeDebugTo(Files.newOutputStream(Paths.get(generateClassName(cl.type) + "." + methodName + "_debug.dot")));
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }

            try {
                g.writeDebugTo(Files.newOutputStream(Paths.get(generateClassName(cl.type) + "." + methodName + "_debug_optimized.dot")));
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }

            try {
                dt.writeDebugTo(Files.newOutputStream(Paths.get(generateClassName(cl.type) + "." + methodName + "_dominatortree.dot")));
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }

            throw ex;
        }

        pw.println("  }");
    }
}
