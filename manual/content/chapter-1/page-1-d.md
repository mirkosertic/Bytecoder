---
title: "Reflection API"
date: 2019-10-25T14:49:24+02:00
draft: false
weight: 4
---

## Overview

Bytecoder is an **AOT (Ahead-of-time) compiler**. As this, it has to determine the set
of classes at compile time. It does this by running a statical dependency analysis which
starts at a class implementing a `public static void main(String[] args)` method and
builds a dependency tree from there resulting in the final set of classes and methods
that must be included to make the program valid.

However, things start to get tricky once we use the Java Reflection API.

The most famous part of the Reflection API is the `Class.forName` method and its derivatives.
Here, the class to be resolved is defined as runtime. This is a problem for an AOT compiler.
Bytecoder tries so solve this problem using a set of heuristics and configuration.

Classes that are very likely to be resolved by reflection are automatically included by the compiler.
Some prominent examples are implementations of `java.nio.charset.Charset` or `java.lang.CharacterData`.
There is also a compiler option available by the CLI or Maven Plugin to add additional classes.

{{% notice note %}}
Only zero-arg constructors are supported yet.
{{% /notice %}}

## Support for the Java Reflection API

The following APIs are supported by Bytecoder:

```
Class runtimeClass = Class.forName("FullQualifiedClassNameHere");
Object instance = runtimeClass.newInstance(); // Method 1 to instantiate a class
cl.getConstructor(new Class[0]).newInstance(); // Method 2 to instantiate a class
```

{{% notice warning %}}
The ServiceLocator API is currently not supported!
{{% /notice %}}
