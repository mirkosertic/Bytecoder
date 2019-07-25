/*
 * Copyright 2018 Mirko Sertic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.mirkosertic.bytecoder.core;

import de.mirkosertic.bytecoder.backend.CompileTarget;
import de.mirkosertic.bytecoder.unittest.BytecoderTestOption;
import de.mirkosertic.bytecoder.unittest.BytecoderTestOptions;
import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.OutputStream;

@RunWith(BytecoderUnitTestRunner.class)
@BytecoderTestOptions(value = {
        @BytecoderTestOption(backend = CompileTarget.BackendType.js, minify = false, exceptionsEnabled = true, preferStackifier = false),
        @BytecoderTestOption(backend = CompileTarget.BackendType.js, minify = true, exceptionsEnabled = true, preferStackifier = false)
})
public class ExceptionTest {

    private static void throwException(final int aValue) {
        throw new RuntimeException();
    }

    @Test
    public void testControlFlow() {
        int i = 10;
        try {
            i++;
            throwException(i);
            i++;
        } catch (final Exception e) {
            for (int j=0; j< 5;j++) {
                i++;
            }
        } finally {
            i+=3;
        }
        Assert.assertEquals(19, i, 0);
    }

    @Test
    public void testForLoop() {
        int i = 10;
        for (int j = 0; j<20;j++) {
            i = i + 3;
        }
        Assert.assertEquals(70, i, 0);
    }

    public void complexWithArgument(final String aMessage) {
        final int i;
        try {
            i = 1;

            if (false != (i!=0) );

            return;
        } catch ( final Exception e ) {
            final RuntimeException le = new RuntimeException(aMessage);
            throw le;
        }
    }

    @Test
    public void testComplexFlow() {
        complexWithArgument("Hello!");
    }

    @Test
    public void testSimpleTry() {
        int x = 10;
        try {
            x = x * 2;
        } catch (final Exception e) {
            x = 20;
            System.out.println("Exceptions happened!");
        }
        Assert.assertEquals(20, x, 0);
    }

    private boolean value1;
    private boolean value2;

    @Test
    public void testExceptionWithCondition() {
        boolean value1 = false;
        try {
            value1 = null != System.getProperty("CALLS");
        } catch (final Exception e) {

        }
        try {
            value2 = null != System.getProperty("LALA");
        } catch (final Exception e) {

        }
        value1 = true;
        value2 = true;
    }

    @Test
    public void testExceptionWithForLoop() {
        final int x = 1;
        try {
            for (int y = 0; y < 10; y++) {
                if (y > 0) {
                    return;
                }
            }
        } catch (final NullPointerException e) {
            return;
        } catch (final Exception e) {
            return;
        }
        return;
    }

    public void writeExceptionTestMethod(final int b) {
        try {
            synchronized (this) {
            }
        }
        catch (final RuntimeException x) {
        }
    }

    private boolean autoFlush;
    private OutputStream out;

    @Test
    public void testExceptionWithNestedSynchronized() {
        writeExceptionTestMethod(10);
    }

    private boolean computeMultiple() {
        try {
            for (int x=0;x<10;x++) {
                if (x > 2) {
                    return true;
                }
                if (x < 2) {
                    return true;
                }
            }
        } catch (final Exception e) {
            return true;
        }
        return false;
    }

    @Test
    public void testExceptionMultipleExit() {
        Assert.assertTrue(computeMultiple());
    }

    @Test
    public void testNestedExceptionHandling() {
        int x = 0;
        try {
            x = 1;
            try {
                x = 3;
            } catch (final RuntimeException e) {
                return;
            }
        } catch (final Exception e) {
            return;
        }
    }

    @Test
    public void testNestedWithSwitchCase() {
        int x = 0;
        try {
            switch (x) {
                case 1:
                case 0: {
                    try {
                        x = 99;
                    } catch (final RuntimeException e) {
                        x = 101;
                    }
                    break;
                }
                default: {
                    try {
                        x = 2;
                    } catch (final RuntimeException e) {
                        x = 3;
                    }
                    break;
                }
            }
        } catch (final Exception e) {

        }
    }

    public static class EXA extends RuntimeException {

    }

    public static class EXB extends RuntimeException {

    }

    @Test
    public void testMultiCatch() {
        int x = 0;
        try {
             x = 1;
        } catch (final EXA | EXB e) {
            x = 2;
            System.out.println(e.getMessage());
        }
        x = 13;
    }

    @Test
    public void testSimpleCatch() {
        int x = 0;
        try {
            x = 1;
        } catch (final EXA e) {
            x = 2;
        }
        x = 13;
    }

    @Test
    public void testSimpleCatchFinally() {
        int x = 0;
        try {
            x = 2;
            throw new NullPointerException();
        } catch (final NullPointerException e) {
            x = 17;
        } finally {
            x = x + 1;
        }
        Assert.assertEquals(18, x, 0);
    }

    private void doSomething() {
    }

    @Test
    public void testTryWithSwitch() {
        int x = 0;
        int y = 0;
        int z = 0;
        try {
            switch (x) {
                case 0: {
                    y++;
                }
                case 1: {
                    y+=3;
                }
            }
        } catch (final RuntimeException e) {
            if (x > 0) {
                y = 10;
                doSomething();
                y = 15;
            } else {
                y = 20;
                doSomething();
                y = 25;
            }
            x = -1;
        } finally {
            z++;
            if (z>0) {
                z = 12;
            } else {
                z = 13;
            }
            doSomething();
        }
        Assert.assertEquals(4, y, 0);
    }
}
