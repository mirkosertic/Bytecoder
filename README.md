# Bytecoder

Bytecoder is a Rich Domain Model for Java Bytecode and Framework to interpret and transpile it to other 
languages such as JavaScript, OpenCL or WebAssembly.

Current travis-ci build status: [![Build Status](https://travis-ci.org/mirkosertic/Bytecoder.svg?branch=master)](https://travis-ci.org/mirkosertic/Bytecoder) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.mirkosertic.bytecoder/bytecoder-parent/badge.svg)](https://maven-badges.herokuapp.com/maven-central/de.mirkosertic.bytecoder/bytecoder-parent/badge.svg)

## High Level Goals

* Ability to cross-compile JVM Bytecode to JavaScript, WebAssembly, OpenCL and other languages
* Primary compile targets are JavaScript and WebAssembly
* Use other tool chains such as Google Closure Compiler to further optimize generated code
* Backed by OpenJDK 11 as JRE Classlib

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
 [JBox2D Demo compiled from Java to JavaScript](https://www.mirkosertic.de/examples/jbox2d/index.html)    |  
 [JBox2D Demo compiled from Java to WebAssembly](https://www.mirkosertic.de/examples/jbox2d/indexwasm.html)   |
 [JBox2D Demo compiled from Kotlin to JavaScript](https://www.mirkosertic.de/examples/jbox2d/index-kotlin.html)  |  
 [JBox2D Demo compiled from Kotlin to WebAssembly](https://www.mirkosertic.de/examples/jbox2d/indexwasm-kotlin.html) |

## User Manual

The Bytecoder User Manual is available [here](manual/README.md).