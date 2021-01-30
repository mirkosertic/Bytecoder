/*
 * Copyright 2020 Mirko Sertic
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
package de.mirkosertic.bytecoder.backend.llvm;

import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.ssa.DebugPosition;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.Program;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LLVMDebugInformation {

    public static class Metadata {
        protected final int id;

        public Metadata(final int id) {
            this.id = id;
        }

        public void writeDebugSuffixTo(final PrintWriter pw) {
            pw.print("!dbg !");
            pw.print(id);
        }
    }

    public static class CompileFile extends Metadata {
        private final String fileName;

        public CompileFile(final int id, final String fileName) {
            super(id);
            this.fileName = fileName;
        }
    }

    public class SubProgram extends Metadata {

        private final Program program;
        private final String name;
        private final BytecodeMethodSignature methodSignature;
        private DebugPosition lastDebugPosition;
        private final Map<Integer, Location> locations;

        public SubProgram(final int id, final Program p, final String name, final BytecodeMethodSignature methodSignature) {
            super(id);
            this.locations = new HashMap<>();
            this.program = p;
            this.name = name;
            this.methodSignature = methodSignature;
            this.lastDebugPosition = null;
        }

        public void writeDebugSuffixFor(final Expression e, final PrintWriter pw) {
            DebugPosition p = program.getDebugInformation().debugPositionFor(e.getAddress());
            if (p == null) {
                 p = lastDebugPosition;
            }
            if (p == null) {
                // Nothing found,
                // We have to create artificial data here to make llvm happy
                p = new DebugPosition("unknown", 1);
            }

            Location l = locations.get(p.getLineNumber());
            if (l == null) {
                l = new Location(elementCounter++, p.getLineNumber(), 1);
                locations.put(p.getLineNumber(), l);
            }

            pw.print(", !dbg !");
            pw.print(l.id);

            lastDebugPosition = p;
        }
    }

    public static class Location extends Metadata {

        private final int lineNumber;
        private final int column;

        public Location(final int id, final int lineNumber, final int column) {
            super(id);
            this.lineNumber = lineNumber;
            this.column = column;
        }
    }

    public class CompileUnit extends Metadata {

        private final CompileFile compileFile;
        private final List<SubProgram> programs;

        public CompileUnit(final int id, final CompileFile compileFile) {
            super(id);
            this.compileFile = compileFile;
            this.programs = new ArrayList<>();
        }

        public SubProgram subProgram(final Program p, final String name, final BytecodeMethodSignature signature) {
            final SubProgram program = new SubProgram(elementCounter++, p, name, signature);
            programs.add(program);
            return program;
        }
    }

    private final Map<String, CompileUnit> compileUnits;
    private int elementCounter;

    public LLVMDebugInformation() {
        compileUnits = new HashMap<>();
        elementCounter = 10;
    }
    public CompileUnit compileUnitFor(final Program program) {
        String fileName = program.getDebugInformation().originalFileName();
        if (fileName == null) {
            fileName = "/unknown";
        }
        CompileUnit unit = compileUnits.get(fileName);
        if (unit == null) {
            final CompileFile compileFile = new CompileFile(elementCounter++, fileName);
            unit = new CompileUnit(elementCounter++, compileFile);
            compileUnits.put(fileName, unit);
        }
        return unit;
    }

    public CompileUnit compileUnitFor(final String fileName) {
        CompileUnit unit = compileUnits.get(fileName);
        if (unit == null) {
            final CompileFile compileFile = new CompileFile(elementCounter++, fileName);
            unit = new CompileUnit(elementCounter++, compileFile);
            compileUnits.put(fileName, unit);
        }
        return unit;
    }

    public void writeHeaderTo(final PrintWriter pw) {
        final List<CompileUnit> theUnits = new ArrayList<>(compileUnits.values());
        for (final CompileUnit compileUnit : theUnits) {
            final CompileFile file = compileUnit.compileFile;
            final String fileName = file.fileName;
            final int p = fileName.lastIndexOf('/');
            pw.print("!");
            pw.print(file.id);
            pw.print("= !DIFile(filename:\"");
            pw.print(file.fileName.substring(p + 1));
            pw.print("\", directory:\"");
            pw.print(file.fileName.substring(0, p));
            pw.println("\")");

            pw.print("!");
            pw.print(compileUnit.id);
            pw.print("= distinct !DICompileUnit(language: DW_LANG_Java, file: !");
            pw.print(file.id);
            pw.println(", isOptimized: true, runtimeVersion: 0, emissionKind: FullDebug, enums: !3)");

            for (final SubProgram prog : compileUnit.programs) {
                pw.print("!");
                pw.print(prog.id);
                pw.print(" = distinct !DISubprogram(name: \"");
                pw.print(prog.name);
                pw.print("\", unit: !");
                pw.print(compileUnit.id);
                pw.print(", file: !");
                pw.print(file.id);
                pw.print(", scope: !");
                pw.print(file.id);
                pw.println(", isDefinition: true, type: !4, isLocal: false, isOptimized: true,  retainedNodes: !3)");

                for (final Location l : prog.locations.values()) {
                    pw.print("!");
                    pw.print(l.id);
                    pw.print(" = !DILocation(line: ");
                    pw.print(l.lineNumber);
                    pw.print(", column: ");
                    pw.print(l.column);
                    pw.print(", scope: !");
                    pw.print(prog.id);
                    pw.println(")");
                }
            }
        }

        pw.print("!llvm.dbg.cu = !{");
        for (int i=0;i<theUnits.size();i++) {
            if (i>0) {
                pw.print(",");
            }
            pw.print("!");
            pw.print(theUnits.get(i).id);
        }
        pw.println("}");

        pw.println("!0 = !{i32 2, !\"Dwarf Version\", i32 4}");
        pw.println("!1 = !{i32 2, !\"Debug Info Version\", i32 3}");
        pw.println("!2 = !{!\"Bytecoder\"}");
        pw.println("!3 = !{}");
        pw.println("!4 = !DISubroutineType(types: !5)");
        pw.println("!5 = !{null}");

        pw.println("!llvm.module.flags = !{!0, !1}");
        pw.println("!llvm.ident = !{!2}");
    }
}
