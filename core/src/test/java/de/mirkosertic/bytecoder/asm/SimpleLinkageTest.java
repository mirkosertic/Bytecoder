package de.mirkosertic.bytecoder.asm;

import de.mirkosertic.bytecoder.asm.test.UnitTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(UnitTestRunner.class)
public class SimpleLinkageTest {

    enum TestEnum {
        a
    }

    public static class Base {
        static int x = 100;
    }

    public interface Testinterface {

    }

    public interface Testinterface2 extends Testinterface {

    }

    public static class Testclass extends Base implements Testinterface, Testinterface2 {

        static int y = 200;

        int value;

        private Object value2;

        static int x;

        private void doit() {
        }

        public int doit2() {
            return 23;
        }

        private static void gnamph() {
            final int x = 10;
        }

        public static void gnamph2() {
            final int x = 20;
        }

        public static int getValue() {
            return 42;
        }

        private int getInt(final int value, final Object obj) {
            return 18;
        }

        private int orderTest(final int a, final int b) {
            return a / b;
        }

        public void testmethod() {
            int x = 10;
            synchronized (this) {
                x = x + 10;
            }
            //new String(new byte[] {65});
        }
    }

    @Test
    public void testLinkage() {
        final Testclass cl = new Testclass();
        cl.testmethod();
    }
}