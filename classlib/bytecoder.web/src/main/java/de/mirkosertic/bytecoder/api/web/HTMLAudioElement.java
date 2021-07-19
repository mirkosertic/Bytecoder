package de.mirkosertic.bytecoder.api.web;

import de.mirkosertic.bytecoder.api.OpaqueProperty;

public interface HTMLAudioElement extends HTMLElement {

    @OpaqueProperty("loop")
    void setLoop(boolean loop);

    @OpaqueProperty("loop")
    boolean isLoop();

    @OpaqueProperty("volume")
    void setVolume(float volume);

    @OpaqueProperty("volume")
    float getVolume();

    @OpaqueProperty("currentTime")
    void setCurrentTime(int currentTime);

    @OpaqueProperty("currentTime")
    int getCurrentTime();

    @OpaqueProperty("paused")
    boolean isPaused();

    @OpaqueProperty("ended")
    boolean isEnded();

    void play();

    void pause();

    void stop();

    @OpaqueProperty("src")
    void setSrc(String url);

    @OpaqueProperty("crossOrigin")
    void setCrossOrigin(String crossOrigin);
}
