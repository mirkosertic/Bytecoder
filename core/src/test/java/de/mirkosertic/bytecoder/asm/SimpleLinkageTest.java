package de.mirkosertic.bytecoder.asm;

import de.mirkosertic.bytecoder.asm.test.UnitTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(UnitTestRunner.class)
public class SimpleLinkageTest {

    public interface Testinterface {

    }

    public interface Testinterface2 extends Testinterface {

    }

    public static class Testclass implements Testinterface, Testinterface2 {

        int value;

        private Object value2;

        private void doit() {
        }

        public int doit2() {
            return 23;
        }

        private static void gnamph() {
            int x = 10;
        }

        public static void gnamph2() {
            int x = 20;
        }

        public static int getValue() {
            return 42;
        }

        private int getInt(final int value, Object obj) {
            return 18;
        }

        private int orderTest(final int a, final int b) {
            return a / b;
        }

        public void testmethod() {
            final Testclass k = new Testclass();
            //k.doit()
            k.doit2();
            getValue();
            getInt(10, k);

            int temp = k.value;
            k.value = temp;

            Object temp2 = k.value2;
            k.value2 = temp2;

            int[] x = new int[10];
            x[0] = 1;
            int a = x[0];
            Object[] y = new Object[10];
            Object o = y[0];
            y[0] = o;

            orderTest(10, 20);
        }
    }

    @Test
    public void testLinkage() {
        final Testclass cl = new Testclass();
        cl.testmethod();
    }
}