package de.mirkosertic.bytecoder.api.web.webgl;

import de.mirkosertic.bytecoder.api.OpaqueProperty;
import de.mirkosertic.bytecoder.api.OpaqueReferenceType;

public abstract class WebGLActiveInfo implements OpaqueReferenceType {
    @OpaqueProperty("size")
    public abstract int getSize();

    @OpaqueProperty("name")
    public abstract String getName();

    @OpaqueProperty("type")
    public abstract int getType();
}