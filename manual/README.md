# Bytecoder User Manual

## Introduction

Bytecoder is a Rich Domain Model for Java Bytecode and Framework to interpret and transpile it to other 
languages such as JavaScript, OpenCL or WebAssembly.

* Ability to cross-compile JVM Bytecode to JavaScript, WebAssembly, OpenCL and other languages
* Primary compile targets are JavaScript and WebAssembly
* Work well with Debugger Toolchains and SourceMaps
* Use OpenJDK 12 as Java Classlib

The JVM Bytecode is parsed and transformed into an intermediate representation. This intermediate representation is passed thru 
[optimizer stages](OPTIMIZER.md) and sent to a backend implementation for target code generation.

The *JavaScript* backend transforms the intermediate representation into JavaScript.

The *WebAssembly* backend transforms the intermediate representation into WebAssembly text and binary code.

The *OpenCL* backend is used to compile single algorithms into OpenCL and execute them on the GPU. This backend is designed to enhance
existing programs running on the JVM to utilize the vast power of modern GPUs.

## Jump-Start using the Command-Line Interface

Lets assume we have the following Java class, and we want to compile it to JavaScript and run it in the Browser:

```
package bytecodertest;

public class HelloWorld {

    public static void main(String[] args) {
        System.out.println("Hello World!");
    }
}
```

First of all, we need to compile the Java sources to a JVM class file. This is done by using the `javac` tool from the JDK:

```
javac HelloWorld.java
```

Now, we have the compiled .class files. Now we can use the Bytecoder CLI to compile it to JavaScript!

Step 1: Download the CLI from Maven central:

``` 
wget http://central.maven.org/maven2/de/mirkosertic/bytecoder/bytecoder-cli/2019-08-03/bytecoder-cli-2019-08-03-executable.jar
```

Step 2: Invoke the CLI:

```
java -jar bytecoder-cli-2019-08-03-executable.jar -classpath=. -mainclass=bytecodertest.HelloWorld -builddirectory=. -backend=js -minify=false
```

Step 3: Create an embedding HTML document

Now we have a `bytecoder.js` file, which needs to be embedded into a HTML document. Here is a sample
HTML file embedding the Bytecoder JavaScript and invoking it:

```
<html>
    <head>
        <title>Bytecoder Test</title>
    </head>
    <body>
        <script type="text/javascript" src="bytecoder.js"></script>
        <script type="text/javascript">

            console.log("Init");
            bytecoder.bootstrap();

            bytecoder.exports.main();
            console.log("Done");
        </script>
    </body>
</html>
```

After opening this HTML file in a Browser you will see the "Hello World!" output in the JavaScript console log.

## Hello World, extended edition!

Let's see how a simple Java program can be written that changes the document title in the Browser. Here it is:

```
import de.mirkosertic.bytecoder.api.web.Event;
import de.mirkosertic.bytecoder.api.web.EventListener;
import de.mirkosertic.bytecoder.api.web.HTMLDocument;
import de.mirkosertic.bytecoder.api.web.Window;

public class OpaqueReferenceTest {

    public static void main(String[] args) {
        final Window w = Window.window();
        w.document().addEventListener("click", new EventListener<ClickEvent>() {
            @Override
            public void run(final ClickEvent aValue) {
                w.document().title("clicked!");
            }
        });
    }
}
```

I'll try to explain the basics behind this and how it can be compiled to JavaScript or WebAssembly in the following sections.

### Interoperability and runtime linkage

Programs do not live on their own, they need to communicate with their environment.
This communication can be tricky if done on multiple environments. Bytecoder supports
JavaScript, WebAssembly and OpenCL as target platforms. How does this wok?

#### OpaqueReferenceTypes API

Bytecoder allows transparent usage of APIs not implemented by Bytecoder itself. Such APIs are
provided by the host environment, for instance the DOM API or interaction with the browser window.
Bytecoder support such APIs by so called OpaqueReferenceTypes. For every external API, a new OpaqueRecerenceType
in form of a JVM interface class needs to be created. Bytecoder already comes with implementations for the
browser window and the DOM.

See the following example, which demonstrates calls from Bytecoder to the HTML Canvas API:

```
Window window = Window.window();
Document document = window.document();
final HTMLCanvasElement theCanvas = document.getElementById("benchmark-canvas");
CanvasRenderingContext2D renderingContext2D = theCanvas.getContext("2d");

renderingContext2D.moveTo(10, 10);
renderingContext2D.lineTo(20, 20);
```

Bytecoder also supports event listeners, as seen in the following example:

```
final HTMLElement button = document.getElementById("button");
button.addEventListener("click", new EventListener<ClickEvent>() {
    @Override
    public void run(ClickEvent aValue) {
        button.disabled = true;
    }
});
```

The OpaqueReferenceType API allows the following types for Bytecoder-Host communication:

* Primitives(int,float,double,long,char,byte,short)
* `java.lang.String`
* `de.mirkosertic.bytecoder.api.OpaqueReferenceType` and sub classes of it
* `de.mirkosertic.bytecoder.api.Callback` and sub classes of it

#### Importing functionality from the host environment

Using host environment functionality is quite common. This can be either simply
logging or more complex code. Basically something that cannot be archived by plain OpaqueReferenceTypes.

Java has a built-in language feature for importing functionality. The `native` keyword!

For instance, we take a look at the `TMath` runtime class:

```
public class TMath extends TObject {

    public static native double sqrt(double aValue);
}
```

The `native` keyword instructs the JVM to link the implementation code from somewhere else.
This linking is done when bootstrapping the Bytecoder runtime. By default, Bytecoder will
import the implementation using a `modulename` and an `importname`. The `modulename` is
derived from the classname in lowercase, the `importname` is the method name to import.

##### Native-Linking in JavaScript

At startup, the following code must be provided:

```
bytecoder.imports.math = {
    sqrtDOUBLE: function(p1) {
        return Math.sqrt(p1);
    },
};
```

##### Native-Linking in WebAssembly

At startup, the following code must be provided:

```
bytecoder.imports.math = {
    sqrtDOUBLE: function(thisref, p1) {
        return Math.sqrt(p1);
    },
};
```

##### Customizing the module names

Sometimes you want to provide your own `modulename` and `importname`. This can be
done by adding an `@de.mirkosertic.bytecoder.api.Import` annotation to the native method:

```
public class CanvasRenderingContext2D {

    @Import(module = "canvas", name = "canvasClear")
    public native void clear();
    
}
```

#### Exporting functionality to the host environment

We also want to make functionality be callable from the host environment. The most 
important use case for this is to call out program! So, how can this be done? Java
has no keyword that could mimic this behavior, so we have to provide our own. Method
that should be callable from the host environment needs to be annotated with 
`@de.mirkosertic.bytecoder.api.Export`, as seen in the following example:

```
public class JBox2DSimulation {
    @Export("proceedSimulation")
    public static void proceedSimulation() {
    }
}
```

##### Calling exported functionality from JavaScript

Just call an exported method using the Bytecoder module API:

```
bytecoder.exports.proceedSimulation();
```

##### Calling exported functionality from WebAssembly

Easy, just call the method:

```
runningInstance.exports.proceedSimulation(0);
```

The WebAssembly runtime only makes `@de.mirkosertic.bytecoder.api.Export` annotated 
methods available as exports.

## Hello OpenCL

**!!! OpenCL Integration is highly experimental !!!**

Before we can use the OpenCL integration, we have to setup the whole OpenCL environment.
An OpenCL environment can be obtained by the following code:

```
import de.mirkosertic.bytecoder.api.opencl.PlatformFactory;
import de.mirkosertic.bytecoder.api.opencl.Platform;

PlatformFactory theFactory = PlatformFactory.resolve();
Platform thePlatform = theFactory.createPlatform(logger,options);
```

The `Platform` instance must only be obtained once and can be cached.

If you have multiple GPUs or a system with NVIDIA Optimus technology, you
have multiple OpenCL platform available. One for the NVIDIA GPU, and another
for the embedded Intel GPU on your CPU. By default Bytecoder logs the available
platforms at startup and selects the OpenCL platform with the highest number. 
If this does not fit, you can always add a `OPENCL_PLATFORM=x` to your JVM properties 
to set the OpenCL platform to number x.

Every OpenCL computation is done within an OpenCL `Context`. This context
keeps references to compiled OpenCL programs and allocated memory per `Kernel`.
It definitely makes sense to cache a created context. Closing and reopening
of context instances forces the OpenCL runtime to recompile Kernels, which will
cause a huge performance impact. `Context` instances are auto-closable, so they
can be used with Java try-with-resource blocks.

OpenCL `Kernel` instances are the workhorse of the system. They can be created
by sub classing `de.mirkosertic.bytecoder.api.opencl.Kernel` and adding a single
method to it. Here is a simple example of the whole workflow:

```
PlatformFactory theFactory = PlatformFactory.resolve();
Platform thePlatform = theFactory.createPlatform(logger,options);

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

This will print the numbers 110,220,330 and 440 at command line.

Here is an example of the OpenCL C-Code generated for the Java Kernel from above:

```
__kernel void BytecoderKernel(__global const float* val$theA, 
                              __global const float* val$theB, 
                              __global float* val$theResult) {
    int var1 = get_global_id(0);
    float var4 = val$theA[var1];
    float var7 = val$theB[var1];
    float var9 = var4 + var7;
    val$theResult[var1] = var9;
    return;
}
```

## Usage with Maven

### Compiling to JavaScript

```
<build>
    <plugins>
        <plugin>
            <groupId>de.mirkosertic.bytecoder</groupId>
            <artifactId>bytecoder-mavenplugin</artifactId>
            <version>2019-08-03</version>
            <configuration>
                <mainClass>de.mirkosertic.bytecoder.integrationtest.SimpleMainClass</mainClass>
                <backend>js</backend>
                <enableExceptionHandling>true</enableExceptionHandling>
                <optimizationLevel>ALL</optimizationLevel>
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


### Compiling to WebAssembly

```
<build>
    <plugins>
        <plugin>
            <groupId>de.mirkosertic.bytecoder</groupId>
            <artifactId>bytecoder-mavenplugin</artifactId>
            <version>2019-08-03</version>
            <configuration>
                <mainClass>de.mirkosertic.bytecoder.integrationtest.SimpleMainClass</mainClass>
                <backend>wasm</backend>
                <enableExceptionHandling>false</enableExceptionHandling>
                <optimizationLevel>ALL</optimizationLevel>
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

is compiled to JavaScript and WebAssembly and executed using a Selenium Chrome driver. This test runner also supports comparison of original Java code and its cross compiled counterpart. This mechanism is the core tool to test the compiler and the Classlib.

Bytecoder relies for WebAssembly unit testing on Chrome. To make everything working, you have to add `CHROMEDRIVER_BINARY` 
environment variable pointing to an installed Selenium Chrome WebDriver binary. You can get the latest release
of WebDriver here: https://sites.google.com/a/chromium.org/chromedriver.  

## Internals

### Intermediate representation

The Bytecoder internal intermediate representation is basically a directed graph. The key idea behind this is described 
[in this paper](../core/src/main/java/de/mirkosertic/bytecoder/graph/c2-ir95-150110.pdf).

Given this Java source code:

```
@Test
public void testSimpleLoop() {
    for (int i=0;i<10;i++) {

    }
}
```

the following intermediate representation graph is generated (in its first, un optimized form):

![Intermediate representation graph](docassets/ir_loopexample.svg)

This graph combines data flow analysis and control flow into one big graph. Using this graph makes data and
control flow dependencies explicit and lays foundation for a variety of optimizations that can be performed on it to
either reduce code size or improve execution speed. Optimizing the program simply becomes an optimizing
the graph problem.

The following graph shows the further optimized version of the previous loop:

![Intermediate representation graph optimized](docassets/ir_loopexample_optimized.svg) 

There are two different output styles available for generated code:

* Relooper

    The [Relooper output generator](../core/src/main/java/de/mirkosertic/bytecoder/relooper/paper.pdf)
    tries to recover high level control flow constructs from the intermediate representation. This step eliminates
    the needs of GOTO statements and thus allows generation of more natural source code, which in turn can be easier read
    and optimized by Web Browsers or other tools. The Relooper supports all styles of control flows and also supports
    exception handling.
    
* Stackifier

   The Stackifier is based on [this paper](../core/src/main/java/de/mirkosertic/bytecoder/relooper/SRC-RR-4.pdf). It
   tries to remove all GOTO statements and replaces them with structured control flow elements and multi level break
   and continues. The Stackifier does only work for reducible control flows and also does not support 
   exception handling. The generated output is smaller and in some cases faster compared to the Relooper output.

Relooper output is enabled by default for `JS` and `Wasm` backends. The Stackifier can be enabled for `CLI` or `Maven` by setting
`preferStackifier` to `true` as a configuration parameter. If Stackifier is enabled and Bytecoder detects an
irreducible control flow Relooper is used as a fallback.

Stackifier is used as the default by the `OpenCL` backend.

### WebAssembly internals

The WebAssembly backend emulates high level data types using WebAssembly primitives. 
At the moment only `i32` and `f32` types. All other data types are composed using 
data blocks in the `linear memory` and pointers, which are basically also `i32`. This 
Backend does not use `Wasm64`. 

#### Memory

The memory is managed. It is dynamically allocated and automatically freed using a
Mark-and-Sweep garbage collector. All memory is directly mapped to the Wasm linear memory.

Memory is split into `Heap` and `Stack`. The `Heap` is used for allocated objects. The `Stack` 
is used to hold `activation records` used during method invocation.

`Heap` and `Stack` grow in opposite directions. `Heap` grows from bottom to top of the 
linear memory. `Stack` grows from top to bottom. The heap contains a lot of live and dead
objects and tends to be fragmented. Stack grows and shrinks with a compile time detected
rates, and cannot fragment, as it is basically a stack of `activation records`.

#### Objects

Objects are a pointer to an allocated memory block. This memory contains an `object header` and
an `object body`. The `object header` contains the following fields:

 Field       | Type | Description                                   
-------------|------|-----------------------------------------------
 type        | i32  | Reference to the runtime class of this object 
 vtable      | i32  | The virtual method resolver function          

The `object body` contains the raw data of the object depending on its type.

#### Runtime classes

Runtime classes are objects of type `runtime class` and a fixed `virtual table`.
Their `object body` contains the following fields:

 Field       | Type | Description                                    
-------------|------|------------------------------------------------
 initstatus  | i32  | The initialization status of the runtime class 
 enumvals    | i32  | Pointer to optional enum values array          

Static class attributes are added to the field list on demand.

#### Arrays

Arrays are objects of type `array` and a fixed `virtual table`.
Their `object body` contains the following fields:

 Field       | Type | Description                                    
-------------|------|------------------------------------------------
 length      | i32  | The length of the array                        
 1..length   | i32  | Pointer for every array element                

#### Regular object instances

The `object body` is a list of instance members. The `object body` is
constructed as written above.
