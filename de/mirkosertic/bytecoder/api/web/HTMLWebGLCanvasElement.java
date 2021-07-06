package de.mirkosertic.bytecoder.api.web;

import de.mirkosertic.bytecoder.api.web.webgl.WebGLRenderingContext;

public interface HTMLWebGLCanvasElement extends HTMLElement {

    WebGLRenderingContext getContext(String context);

}
