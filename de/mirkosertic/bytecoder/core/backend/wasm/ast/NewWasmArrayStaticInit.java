package de.mirkosertic.bytecoder.core.backend.wasm.ast;

import java.io.IOException;
import java.util.List;

public class NewWasmArrayStaticInit implements WasmValue {

    private final WasmType type;

    private final List<WasmValue> arguments;

    NewWasmArrayStaticInit(final WasmType type, final List<WasmValue> arguments) {
        this.type = type;
        this.arguments = arguments;
    }

    @Override
    public void writeTo(final TextWriter writer, final ExportContext context) throws IOException {
        writer.opening();
        writer.write("array.init_static ");
        type.writeRefTo(writer);
        for (final WasmValue arg : arguments) {
            writer.space();
            arg.writeTo(writer, context);
        }
        writer.closing();
    }

    @Override
    public void writeTo(final BinaryWriter.Writer binaryWriter, final ExportContext context) throws IOException {
        for (final WasmValue arg : arguments) {
            arg.writeTo(binaryWriter, context);
        }
        binaryWriter.writeByte((byte) 0xfb);
        binaryWriter.writeByte((byte) 0x1a);
        binaryWriter.writeUnsignedLeb128(type.index());
        binaryWriter.writeUnsignedLeb128(arguments.size());
    }
}
