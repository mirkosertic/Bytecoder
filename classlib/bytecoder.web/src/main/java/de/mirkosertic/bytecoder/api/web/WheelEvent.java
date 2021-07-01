package de.mirkosertic.bytecoder.api.web;

import de.mirkosertic.bytecoder.api.OpaqueProperty;

public interface WheelEvent extends MouseEvent {

    @OpaqueProperty
    int deltaX();

    @OpaqueProperty
    int deltaY();

    @OpaqueProperty
    int deltaZ();

    @OpaqueProperty
    int deltaMode();
}
