---
title: "The Maven Plugin"
date: 2019-10-25T14:49:24+02:00
draft: false
weight: 2
---

## Usage with Maven Plugin

Bytecoder comes with a handy Maven plugin. This plugins supports the JavaScript and WebAssembly backends 
and can compile JVM bytecode as part of the Maven project lifecycle without any third party or command-line
tools.

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


