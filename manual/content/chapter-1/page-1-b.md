---
title: "The Maven Plugin"
date: 2023-01-30T00:00:00+02:00
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

* `backend`: The Backend to be used. Can be `js` or `wasm`. Defaults to `js`.

* `debugOutput`: Shall debug output be generated? Defaults to `false`.

* `optimizationLevel`: Which kind of optimization should be applied? Can be `DISABLED`, `DEFAULT` or `ALL`. Defaults to `DEFAULT`.

* `filenamePrefix`: Prefix of the generated files. Defaults to `bytecoder`.

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


