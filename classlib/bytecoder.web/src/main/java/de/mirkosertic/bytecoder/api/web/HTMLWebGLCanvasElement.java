package de.mirkosertic.bytecoder.api.web;

import de.mirkosertic.bytecoder.api.OpaqueProperty;
import de.mirkosertic.bytecoder.api.web.webgl.WebGLRenderingContext;

public interface HTMLWebGLCanvasElement extends HTMLElement {

    @OpaqueProperty
    int width();

    @OpaqueProperty
    int height();

    WebGLRenderingContext getContext(String context);

}
