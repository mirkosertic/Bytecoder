---
title: "JRE Emulation"
date: 2019-10-25T14:49:24+02:00
draft: false
weight: 3
---

## JRE Emulation

Bytecoder comes with a JRE emulation layer which is based on OpenJDK and also a set of
additional libraries for **browser interaction**, **vue.js** and **OpenCL** integration.

{{% notice note %}}
Bytecoder comes with a set of JPMS modules like `java.base` etc. Please note that **Bytecoder
does not support the JPMS in general**. From the compiler and runtime view, all classes
are exported to the `ALL-UNNAMED` module by default, `module-info` declarations are
completely ignored.
{{% /notice %}}


### bytecoder-core

Bytecoder-core includes the compiler Logic and the JUnit Testrunner. Most of the time
you won't need this module directly, as it is included in the CLI and the Maven Plugin 
by default.

```
<dependencies>
    <dependency>
        <groupId>de.mirkosertic.bytecoder</groupId>
        <artifactId>bytecoder-core</artifactId>
        <version>{{% siteparam "bytecoderversion" %}}</version>
    </dependency>
</dependency>
```

### bytecoder.api

Bytecoder.api provides a set of Classes and Annotations which enables the Compiler
do its job. 

```
<dependencies>
    <dependency>
        <groupId>de.mirkosertic.bytecoder</groupId>
        <artifactId>bytecoder.api</artifactId>
        <version>{{% siteparam "bytecoderversion" %}}</version>
    </dependency>
</dependency>
```

### java.base

Java.base contains the `java.base` JPMS Module.

```
<dependencies>
    <dependency>
        <groupId>de.mirkosertic.bytecoder</groupId>
        <artifactId>java.base</artifactId>
        <version>{{% siteparam "bytecoderversion" %}}</version>
    </dependency>
</dependency>
```

### java.xml

Java.xml contains the `java.xml` JPMS Module.

```
<dependencies>
    <dependency>
        <groupId>de.mirkosertic.bytecoder</groupId>
        <artifactId>java.xml</artifactId>
        <version>{{% siteparam "bytecoderversion" %}}</version>
    </dependency>
</dependency>
```

### java.datatransfer

Java.datatransfer contains the `java.datatransfer` JPMS Module.


```
<dependencies>
    <dependency>
        <groupId>de.mirkosertic.bytecoder</groupId>
        <artifactId>java.datatransfer</artifactId>
        <version>{{% siteparam "bytecoderversion" %}}</version>
    </dependency>
</dependency>
```

### java.desktop

Java.desktop contains the `java.desktop` JPMS Module.

```
<dependencies>
    <dependency>
        <groupId>de.mirkosertic.bytecoder</groupId>
        <artifactId>java.desktop</artifactId>
        <version>{{% siteparam "bytecoderversion" %}}</version>
    </dependency>
</dependency>
```

### bytecoder.web

Bytecoder.web contains APIs to interact with the Browser.

```
<dependencies>
    <dependency>
        <groupId>de.mirkosertic.bytecoder</groupId>
        <artifactId>bytecoder.web</artifactId>
        <version>{{% siteparam "bytecoderversion" %}}</version>
    </dependency>
</dependency>
```

### bytecoder.vue

Bytecoder.vue allows vue.js enabled frontends using Bytecoder.

```
<dependencies>
    <dependency>
        <groupId>de.mirkosertic.bytecoder</groupId>
        <artifactId>bytecoder.vue</artifactId>
        <version>{{% siteparam "bytecoderversion" %}}</version>
    </dependency>
</dependency>
```

### bytecoder.opencl

Bytecoder.opencl is required if you want to use the `OpenCL` integration.

```
<dependencies>
    <dependency>
        <groupId>de.mirkosertic.bytecoder</groupId>
        <artifactId>bytecoder.opencl</artifactId>
        <version>{{% siteparam "bytecoderversion" %}}</version>
    </dependency>
</dependency>
```
