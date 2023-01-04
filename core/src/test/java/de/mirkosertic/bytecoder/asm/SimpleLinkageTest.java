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

        private int getInt() {
            return 18;
        }

        public void testmethod() {
            final Testclass k = new Testclass();
            //k.doit()
            k.doit2();
            getValue();
            getInt();
        }
    }

    @Test
    public void testLinkage() {
        final Testclass cl = new Testclass();
        cl.testmethod();
    }
}