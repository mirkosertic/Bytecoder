package de.mirkosertic.bytecoder.core;

import de.mirkosertic.bytecoder.core.test.UnitTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.function.Consumer;

@RunWith(UnitTestRunner.class)
public class MethodReferenceTest {
    public static String transform(String input){
        consumed = input+input;
        return input+input;
    }
    static String consumed;
    public static void consume(String input){
        consumed = input;
    }

    @Test
    public void passMatchingReference(){
        consumed = "";
        useConsumer(MethodReferenceTest::consume);
        Assert.assertEquals(consumed,"test");
    }

    @Test
    public void passUnmatchingReturnType(){
        consumed = "";
        useConsumer(MethodReferenceTest::transform);
        Assert.assertEquals(consumed,"testtest");
    }

    public static void useConsumer(Consumer<String> consumer){
        consumer.accept("test");
    }
}
