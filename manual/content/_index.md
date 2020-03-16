+++
title = "Bytecoder User Manual"
date = 2019-10-25T14:42:16+02:00
weight = 2
+++

# Bytecoder User Manual

## Introduction

**Bytecoder** is a Rich Domain Model for Java Bytecode and Framework to interpret and transpile it to other 
languages such as **JavaScript**, **OpenCL** or **WebAssembly**.

Its key features are:

* Ability to **cross-compile JVM Bytecode** to JavaScript, WebAssembly, OpenCL and other languages
* Primary compile targets are **JavaScript** and **WebAssembly**
* Act as a **JVM Bytecode frontend for LLVM**
* **Work well with Debugger Toolchains and SourceMaps**
* **Use OpenJDK 14 as Java Classlib**

The JVM Bytecode is parsed and transformed into an intermediate representation. This intermediate representation is passed thru 
optimizer stages and sent to a backend implementation for target code generation.

The **JavaScript** backend transforms the intermediate representation into JavaScript.

The **WebAssembly** backend transforms the intermediate representation into WebAssembly text and binary code.

The **OpenCL** backend is used to compile single algorithms into OpenCL and execute them on the GPU. This backend is designed to enhance
existing programs running on the JVM to utilize the vast power of modern GPUs.
