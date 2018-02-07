# Bytecoder

Rich Domain Model for Java Bytecode and Framework to interpret and transpile it to other languages such as JavaScript or WebAssembly.

Current travis-ci build status: [![Build Status](https://travis-ci.org/mirkosertic/Bytecoder.svg?branch=master)](https://travis-ci.org/mirkosertic/Bytecoder) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.mirkosertic.bytecoder/bytecoder-parent/badge.svg)](https://maven-badges.herokuapp.com/maven-central/de.mirkosertic.bytecoder/bytecoder-parent/badge.svg)

## High Level Goals

* Ability to cross-compile JVM Bytecode to JavaScript, WebAssembly, OpenCL and other languages
* Primary compile targets are JavaScript and WebAssembly
* Use other toolchains such as Google Closure Compiler or Binaryen to further optimize generated code
* Reuse or implement cross-compileable Java Classlib

## Compiling strategies

The JVM Bytecode is parsed and transformed into an [intermediate representation](IR.md). This intermediate representation is passed thru 
optimizer stages and sent to a backend implementation for target code generation.

The *JavaScript* backend transforms the intermediate representation into JavaScript.

The *WebAssembly* backend transforms the intermediate representation into WebAssembly text format code, which can easily compiled 
into WebAssembly binary code using the WABT toolchain.

The *OpenCL* backend is used to compile single algorithms into OpenCL and execute them on the GPU. This backend is designed to enhance
existing programs running on the JVM to utilizy the vast power of modern GPUs.

## Using OpenCL

The following program demonstrates the use of the Bytecoder `OpenCL` backend embedded into a JVM program:

```
PlatformFactory theFactory = new PlatformFactory();
Platform thePlatform = theFactory.createPlatform();

final float[] theA = {10f, 20f, 30f, 40f};
final float[] theB = {100f, 200f, 300f, 400f};
final float[] theResult = new float[4];

try (Context theContext = thePlatform.createContext()) {
    theContext.compute(4, new Kernel() {
        public void processWorkItem() {
            int id = get_global_id(0);
            float a = theA[id];
            float b = theB[id];
            theResult[id] = a + b;
        }
    });
}

for (int i=0; i<theResult.length;i++) {
    System.out.println(theResult[i]);
}
```

The interesting part is the `Kernel` class. At runtime, the JVM bytecode is translated into an `OpenCL` program and executed
heavily parallel on the GPU. Writing OpenCL Kernels in Java keeps developer productivity up and allows to write efficient 
algorithms that can transparently executed on the `GPU`.

Details about OpenCL and its usage with Bytecoder are documented [here](OPENCL.md).

## Unit testing

Bytecoder comes with built in JUnit Testing support using a specialized test runner. This test runner compiles the body of the test method to a target language
and executes this code. For instance, the following JUnit Test

```
@RunWith(BytecoderUnitTestRunner.class)
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

Is compiled to JavaScript and WebAssembly and executed using a Selenium Chrome driver. This testrunner also supports comparison of original Java code and its crosscompiled counterpart. This mechanism is the core tool to test the compiler and the Classlib.

## Maven Plugin

There is Bytecoder Maven Plugin available.

## Compiling to JavaScript

```
<build>
    <plugins>
        <plugin>
            <groupId>de.mirkosertic.bytecoder</groupId>
            <artifactId>bytecoder-mavenplugin</artifactId>
            <version>2018-02-01</version>
            <configuration>
                <mainClass>de.mirkosertic.bytecoder.integrationtest.SimpleMainClass</mainClass>
                <backend>js</backend>                    
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>compile</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

You have to set a main class with a valid `public static void main(String[] args)` method as an entry point. 
The plugin will invoke the JavaScript compiler which will do all the heavy lifting. The generated
JavaScript will be placed in the Maven `target/bytecoder` directory.

## Compiling to WebAssembly

```
<build>
    <plugins>
        <plugin>
            <groupId>de.mirkosertic.bytecoder</groupId>
            <artifactId>bytecoder-mavenplugin</artifactId>
            <version>2018-02-01</version>
            <configuration>
                <mainClass>de.mirkosertic.bytecoder.integrationtest.SimpleMainClass</mainClass>
                <backend>wasm</backend>                    
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>compile</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

You have to set a main class with a valid `public static void main(String[] args)` method as an entry point. 
The plugin will invoke the WebAssembly compiler which will do all the heavy lifting. The generated
WebAssembly text file and compiled binaries will be placed in the Maven `target/bytecoder` directory. 

Bytecoder relies for WebAssemvly compilation on the WABT toolchain. Compilation is done by invoking the JS version of 
the WABT tools using Selenium and Chrome. To make everything working, you have to add `CHROMEDRIVER_BINARY` 
environment variable pointing to an installed Selenium Chrome WebDriver binary. You can get the latest release
of WebDriver here: https://sites.google.com/a/chromium.org/chromedriver.  

## Internals

### Integrated compiler optimizations

#### Dead Code removal and brute force optimizations

Before compiling to the target language, a dead code removal is done to reduce the amount of generated code. Starting
from an application entry point, the referenced classes, fields, methods and interfaces are searched. Only detected used
items are then compiled by a target language specific backend. The gathered class and method dependency information are later 
used for further optimizations such as optimizing virtual method invocation if there is in fact only
one implementation available.

#### Memory management

*JVM Bytecode* relies on the garbage collection mechanism provided by the Java Runtime. Webassembly has currently no GC support in version 1.0.

The WebAssembly backend must include garbage collection runtime code for memory management. The first implementation of such a GC will be a Mark-And-Sweep based.
Details about WebAssembly are documented [here](WASM.md) 

The JavaScript backend relies on JavaScript garbage collection provided by the browser.

