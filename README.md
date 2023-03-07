# Bytecoder

Bytecoder is a Rich Domain Model for Java Bytecode and Framework to interpret and transpile it to other 
languages such as JavaScript, OpenCL or WebAssembly.

Current Build status: ![Build Workflow](https://github.com/mirkosertic/Bytecoder/workflows/Build%20Workflow/badge.svg) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.mirkosertic.bytecoder/bytecoder-parent/badge.svg)](https://maven-badges.herokuapp.com/maven-central/de.mirkosertic.bytecoder/bytecoder-parent/badge.svg)

## High Level Goals

* Ability to cross-compile JVM Bytecode to JavaScript, WebAssembly, OpenCL and other languages
* Primary compile targets are JavaScript and WebAssembly
* Supports Java 8, 9, 10, 11, 12, 13, 14, 15, 16 and 17
* Work well with Debugger Toolchains and SourceMaps
* Allow integration with other UI-Frameworks such as vue.js
* Backed by OpenJDK 17 as JRE Classlib

## Compiling strategies

The JVM Bytecode is parsed and transformed into an intermediate representation. This intermediate representation is passed thru 
optimizer stages and sent to a backend implementation for target code generation.

The **JavaScript** backend transforms the intermediate representation into JavaScript.

The **WebAssembly** backend transforms the intermediate representation into WebAssembly text and binary code.

The **OpenCL** backend is used to compile single algorithms into OpenCL and execute them on the GPU. This backend is designed to enhance
existing programs running on the JVM to utilize the vast power of modern GPUs.

## User Manual

The Bytecoder User Manual and a full set of example applications is available [here](https://mirkosertic.github.io/Bytecoder/).
