# Bytecoder

Rich Domain Model for Java Bytecode and Framework to interpret and transpile it to other languages.

## High Level Goals

* Ability to cross-compile JVM Bytecode to JavaScript and other languages
* Primary compile targets are JavaScript and WebAssembly
* Use other toolchains such as clang or emscripten to further optimize generated code
* Reuse or implement cross-compileable Java Classlib

## Dead Code removal and brute force optimizations

Before compiling to the target language, a dead code removal is done to reduce the amount of generated code. Starting
from an application entry point, the referenced classes, fields, methods and interfaces are searched. Only detected used
items are then compiled by a language specific backend. The gathered class and method dependency information are later 
used for further optimizations such as optimizing virtual method invocation if there is in fact only
one implementation available.

## Compiling strategies

*JVM* Bytecode is basically a typed stack machine. For JavaScript compile target, a stack machine is emulated in the first step. Later other
optimizations will be applied to generate faster and more efficient code.

*WebAssembly* is also a stack maschine. The idea is to translate the JVM stack machine into a WebAssemvly stack machine for the WebAssembly compile target.

There are currently no plans to implement Bytecode optimization strategies. The reasoning is that the current status of the project is MVP, the goal is to create a minimul viable product. The WebAssembly Browser Runtime also includes optimization strategies, so for the MVP we will rely on them. This also holds true for optimizations build into the C compiler like emscripten.

## Memory management

*JVM Bytecode* relies on the garbage collection mechanism provided by the Java Runtime. Webassembly has no GC support on the current MVP. Also plain C has no garbage collection build in. So the WebAssembly and C compile targets must include garbage collection code for memory management. The first implementation of such a GC will be a Mark-And-Sweep based.

The JavaScript compile target will rely on JavaScript garbage collection provided by the browser.

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

There is Maven plugin available. It currently supports only the JavaScript compile target. Basically it can be used as follows:


```
    <build>
        <plugins>
            <plugin>
                <groupId>de.mirkosertic.bytecoder</groupId>
                <artifactId>bytecoder-mavenplugin</artifactId>
                <version>${project.version}</version>
                <configuration>
                    <mainClass>de.mirkosertic.bytecoder.integrationtest.SimpleMainClass</mainClass>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>compile</goal>
                        </goals>
                    </execution>
                </executions>
                <dependencies>
                    <!-- Include all bytecode dependencies here !!-->
                </dependencies>
            </plugin>
        </plugins>
    </build>
```

You have to set a main class with a valid "public static void main(String[] args)" method as an entry point. 
The plugin will invoke the JSCompileTarget which will do all the heavy lifting. The generated
JavaScript will be placed in the Maven "target/bytecoder" directory.