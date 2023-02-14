package de.mirkosertic.bytecoder.asm.backend.wasm.ast;

import java.io.IOException;

public class SetStruct implements WasmExpression {

    private final StructType structType;

    private final WasmValue source;

    private final String fieldName;

    private final WasmValue value;

    SetStruct(final StructType structType, final WasmValue source, final String fieldName, final WasmValue value) {
        this.structType = structType;
        this.source = source;
        this.fieldName = fieldName;
        this.value = value;
    }

    @Override
    public void writeTo(final TextWriter writer, final ExportContext context) throws IOException {
        writer.opening();
        writer.write("struct.set $");
        writer.write(structType.getName());
        writer.write(" $");
        writer.write(fieldName);
        writer.space();
        source.writeTo(writer, context);
        writer.space();
        value.writeTo(writer, context);
        writer.closing();
        writer.newLine();
    }

    @Override
    public void writeTo(final BinaryWriter.Writer binaryWriter, final ExportContext context) {
        //TODO
    }
}
