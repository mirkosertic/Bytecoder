package de.mirkosertic.bytecoder.asm;

public class Testclass {

    public Testclass() {
        for (int i = 0; i < 10; i++) {
            if (i == 5) {
                return;
            }
        }
    }
}
