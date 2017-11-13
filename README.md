# Bytecoder

Rich Domain Model for Java Bytecode and Framework to interpret and transpile it to other languages.

## High Level Goals

* Ability to cross-compile JVM Bytecode to JavaScript and other languages
* Primary compile targets are JavaScript and WebAssembly
* Use other toolchains such as Google Closure Compiler or Clang to further optimize generated code
* Reuse or implement cross-compileable Java Classlib

## Dead Code removal and brute force optimizations

Before compiling to the target language, a dead code removal is done to reduce the amount of generated code. Starting
from an application entry point, the referenced classes, fields, methods and interfaces are searched. Only detected used
items are then compiled by a target language specific backend. The gathered class and method dependency information are later 
used for further optimizations such as optimizing virtual method invocation if there is in fact only
one implementation available.

## Compiling strategies

The JVM Bytecode is parsed and transformed into an intermediate representation which has the single static assignment property.
This intermediate representation is passed thru optimizer stages and sent to a backend implementation for target code generation.

The *JavaScript* backend transforms the intermediate representation into JavaScript.

The *WebAssembly* backend transforms the intermediate representation into C code, which can easily compiled into WebAssembly.

## Memory management

*JVM Bytecode* relies on the garbage collection mechanism provided by the Java Runtime. Webassembly has no GC support on the current MVP. Also plain C has no garbage collection build in. So the WebAssembly and C backends must include garbage collection code for memory management. The first implementation of such a GC will be a Mark-And-Sweep based.

The JavaScript backend relies on JavaScript garbage collection provided by the browser.

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

Is compiled to JavaScript and executed on a PhantomJS engine. This testrunner will also support comparison of original Java code and its crosscompiled
counterpart. This mechanism is the core tool to test the compiler and the Classlib.

## Maven Plugin

There is Maven plugin available. It currently supports only the JavaScript backend. Basically it can be used as follows:


```
    <build>
        <plugins>
            <plugin>
                <groupId>de.mirkosertic.bytecoder</groupId>
                <artifactId>bytecoder-mavenplugin</artifactId>
                <version>${project.version}</version>
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
The plugin will invoke the JSCompileTarget which will do all the heavy lifting. The generated
JavaScript will be placed in the Maven `target/bytecoder` directory.