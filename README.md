# Bytecoder

Bytecoder is a Rich Domain Model for Java Bytecode and Framework to interpret and transpile it to other 
languages such as JavaScript, OpenCL or WebAssembly.

Current travis-ci build status: [![Build Status](https://travis-ci.org/mirkosertic/Bytecoder.svg?branch=master)](https://travis-ci.org/mirkosertic/Bytecoder) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.mirkosertic.bytecoder/bytecoder-parent/badge.svg)](https://maven-badges.herokuapp.com/maven-central/de.mirkosertic.bytecoder/bytecoder-parent/badge.svg)

## High Level Goals

* Ability to cross-compile JVM Bytecode to JavaScript, WebAssembly, OpenCL and other languages
* Primary compile targets are JavaScript and WebAssembly
* Supports Java 8, 11, 12 and 13
* Work well with Debugger Toolchains and SourceMaps
* Allow integration with other UI-Frameworks such as vue.js
* Backed by OpenJDK 12 as JRE Classlib

## Compiling strategies

The JVM Bytecode is parsed and transformed into an intermediate representation. This intermediate representation is passed thru 
optimizer stages and sent to a backend implementation for target code generation.

The *JavaScript* backend transforms the intermediate representation into JavaScript.

The *WebAssembly* backend transforms the intermediate representation into WebAssembly text and binary code.

The *OpenCL* backend is used to compile single algorithms into OpenCL and execute them on the GPU. This backend is designed to enhance
existing programs running on the JVM to utilize the vast power of modern GPUs.

## Demos

![Demo screenshot](manual/docassets/jbox2ddemo.png)

 Demo                                            |                                   
-------------------------------------------------|
 JBox2D Demo compiled from Java to JavaScript [Relooper Codegen](https://mirkosertic.github.io/Bytecoder/index.html) vs. [Stackifier Codegen](https://mirkosertic.github.io/Bytecoder/index-stackified.html)    |  
 JBox2D Demo compiled from Java to WebAssembly [Relooper Codegen](https://mirkosertic.github.io/Bytecoder/indexwasm.html) vs. [Stackifier Codegen](https://mirkosertic.github.io/Bytecoder/indexwasm-stackified.html)  |
 [JBox2D Demo compiled from Kotlin to JavaScript](https://mirkosertic.github.io/Bytecoder/index-kotlin.html)  |  
 [JBox2D Demo compiled from Kotlin to WebAssembly](https://mirkosertic.github.io/Bytecoder/indexwasm-kotlin.html) |
 [vue.js integration Demo compiled to WebAssembly](https://mirkosertic.github.io/Bytecoder/vuewasm.html) |
 [Lua4J Demo compiled to JavaScript](https://mirkosertic.github.io/Bytecoder/luajs.html) |
 [Lua4J Demo compiled to WebAssembly](https://mirkosertic.github.io/Bytecoder/luawasm.html) |
 [GameComposer Physics Game Example compiled to WebAssembly](https://www.mirkosertic.de/examples/gameengine/index.html). GameComposer is available [here](https://github.com/mirkosertic/GameComposer)|
 [GameComposer Physics Game Example compiled to JavaScript](https://www.mirkosertic.de/examples/gameengine/indexjs.html). GameComposer is available [here](https://github.com/mirkosertic/GameComposer) |

## User Manual

The Bytecoder User Manual is available [here](manual/README.md).
