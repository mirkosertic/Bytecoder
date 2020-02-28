---
title: "The Maven Plugin"
date: 2019-10-25T14:49:24+02:00
draft: false
weight: 2
---

## Maven Plugin usage

Bytecoder comes with a handy Maven plugin. This plugins supports the JavaScript and WebAssembly backends 
and can compile JVM Bytecode as part of the Maven project lifecycle without any third party or command-line
tools.

### Configuration options

The following configuration options are available:

* `buildDirectory': The build target directory. Defaults to `${project.build.outputDirectory}`

* `mainClass` The Classname with the main class to be compiled. Required.

* `backend`: The Backend to be used. Can be `js`, `wasm`  or `wasm_llvm`. Defaults to `js`.

* `debugOutput`: Shall debug output be generated? Defaults to `false`.

* `enableExceptionHandling`: Shall Exception-Handling be activated? Defaults to `false`.

* `optimizationLevel`: Which kind of optimization should be applied? Can be `NONE`, `ALL` or `EXPERIMENTAL`. Defaults to `ALL`.

* `filenamePrefix`: Prefix of the generated files. Defaults to `bytecoder`.

* `wasmInitialPages`: Minimum number of pages for WASM memory. Defaults to `512`.

* `wasmMaximumPages`: Maximum number of pages for WASM memory. Defaults to `1024`.

* `minifyCompileResult`: Shall the compile result be minified? Defaults to `true`.

* `preferStackifier`: Shall the Stackifier be used and the Relooper as fallback? Defaults to `false`.

* `registerAllocator`: Which register allocator should be used? Can be `linear` or `passthru`? Defaults to `linear`.

* `additionalClassesToLink`: List of full qualified class names to be linked beside the statically referenced ones to make them available by reflection API. Optional

### Compiling to JavaScript

```
<build>
    <plugins>
        <plugin>
            <groupId>de.mirkosertic.bytecoder</groupId>
            <artifactId>bytecoder-mavenplugin</artifactId>
            <version>{{% siteparam "bytecoderversion" %}}</version>
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
            <version>{{% siteparam "bytecoderversion" %}}</version>
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


