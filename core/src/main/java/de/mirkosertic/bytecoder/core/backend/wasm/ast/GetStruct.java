package de.mirkosertic.bytecoder.core.backend.wasm.ast;

import java.io.IOException;

public class GetStruct implements WasmValue {

    private final StructType structType;

    private final WasmValue source;

    private final String fieldName;


    GetStruct(final StructType structType, final WasmValue source, final String fieldName) {
        this.structType = structType;
        this.source = source;
        this.fieldName = fieldName;
    }

    @Override
    public void writeTo(final TextWriter writer, final ExportContext context) throws IOException {
        writer.opening();
        writer.write("struct.get $");
        writer.write(structType.getName());
        writer.write(" $");
        writer.write(fieldName);
        writer.space();
        source.writeTo(writer, context);
        writer.closing();

    }

    @Override
    public void writeTo(final BinaryWriter.Writer binaryWriter, final ExportContext context) throws IOException {
        source.writeTo(binaryWriter, context);
        binaryWriter.writeByte((byte) 0xfb);
        binaryWriter.writeByte((byte) 0x02);
        binaryWriter.writeUnsignedLeb128(structType.index());
        binaryWriter.writeUnsignedLeb128(structType.indexOfField(fieldName));
    }
}
