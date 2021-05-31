package de.mirkosertic.bytecoder.asm;

public class Testclass implements Testinterface {

    final static int staticValue;

    static {
        int k = 0;
        for (int i = 0; i < 10; i++) {
            k += i;
        }
        staticValue = k;
    }

    public Testclass(final int i) {
    }

    public static void main(final String[] args) {
        int k = 10;
        for (int i=0;i<10;i++) {
            k = i + 10;
        }
        System.out.println(k);
    }
}
