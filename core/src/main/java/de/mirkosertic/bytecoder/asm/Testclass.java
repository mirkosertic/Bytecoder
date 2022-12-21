package de.mirkosertic.bytecoder.asm;

public class Testclass {

    public Testclass() {
        //final int x = 10;
        int x = 0;
        for (int y = 0; y < 100; y++) {
            x = x + y;
        }
    }
}
