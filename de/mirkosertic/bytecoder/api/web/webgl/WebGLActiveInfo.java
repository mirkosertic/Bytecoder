package de.mirkosertic.bytecoder.api.web.webgl;

import de.mirkosertic.bytecoder.api.OpaqueProperty;
import de.mirkosertic.bytecoder.api.OpaqueReferenceType;

public abstract class WebGLActiveInfo implements OpaqueReferenceType {
    @OpaqueProperty("size")
    abstract int getSize();

    @OpaqueProperty("name")
    abstract String getName();

    @OpaqueProperty("type")
    abstract int getType();
}