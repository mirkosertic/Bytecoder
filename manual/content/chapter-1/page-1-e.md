---
title: "Unit Testing"
date: 2019-12-16T14:49:24+02:00
draft: false
weight: 6
---

## Testing code generation

Bytecoder comes with built in JUnit Testing support using a specialized test runner. This test runner compiles the body of the test method to a target language
and executes this code. For instance, the following JUnit Test

```
@RunWith(de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner.class)
public class SimpleMathTest {

    public static int sum(int a, int b) {
        return a + b;
    }

    @Test
    public void testAdd() throws Exception {
        int c = sum(10, 20);
        Assert.assertEquals(20, c, 0);
    }
}
```

is compiled to JavaScript and WebAssembly and executed using [Selenium](https://selenium.dev/). 
The test runner also supports comparison of original Java code and its cross compiled counterpart. 
This mechanism is the core tool to test the compiler and the Classlib.

You don't need to install the right Chrome version and Selenium drivers as Bytecoder 
uses [Testcontainers](https://www.testcontainers.org/) to run everything.
All you need is a working `Docker` environment as described [here](https://www.testcontainers.org/supported_docker_environment/).  

Please make sure to include the following dependency to make the test runner working:

```
<dependencies>
    <dependency>
        <groupId>de.mirkosertic.bytecoder</groupId>
        <artifactId>bytecoder-core</artifactId>
        <version>{{% siteparam "bytecoderversion" %}}</version>
        <scope>test</scope>
    </dependency>
</dependency>
```
