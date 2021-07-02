package de.mirkosertic.bytecoder.api.web.webgl;

import de.mirkosertic.bytecoder.api.OpaqueReferenceType;
import de.mirkosertic.bytecoder.api.web.FloatArray;

public interface WebMat4 extends OpaqueReferenceType {
    FloatArray create();

    void perspective(FloatArray out, float fovy, float aspect, float near, float far);

    void translate(FloatArray out, FloatArray a, FloatArray v);
}