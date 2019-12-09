---
title: "Intermediate Representation"
date: 2019-10-25T14:49:24+02:00
draft: false
weight: 1
---

The Bytecoder internal intermediate representation is basically a directed graph. The key idea behind this is described 
[in this paper](https://github.com/mirkosertic/Bytecoder/tree/master/core/src/main/java/de/mirkosertic/bytecoder/graph/c2-ir95-150110.pdf).

Given this Java source code:

```
@Test
public void testSimpleLoop() {
    for (int i=0;i<10;i++) {

    }
}
```

the following intermediate representation graph is generated (in its first, unoptimized form):

![Intermediate representation graph](/Bytecoder/docassets/ir_loopexample.svg)

This graph combines data flow analysis and control flow into one big graph. Using this graph makes data and
control flow dependencies explicit and lays foundation for a variety of optimizations that can be performed on it to
either reduce code size or improve execution speed. Optimizing the program simply becomes an optimizing
the graph problem.

The following graph shows the further optimized version of the previous loop:

![Intermediate representation graph optimized](/Bytecoder/docassets/ir_loopexample_optimized.svg) 

There are two different output styles available for generated code:

* Relooper

    The [Relooper output generator](https://github.com/mirkosertic/Bytecoder/tree/master/core/src/main/java/de/mirkosertic/bytecoder/relooper/paper.pdf)
    tries to recover high level control flow constructs from the intermediate representation. This step eliminates
    the needs of GOTO statements and thus allows generation of more natural source code, which in turn can be easier read
    and optimized by Web Browsers or other tools. The Relooper supports all styles of control flows and also supports
    exception handling.
    
* Stackifier

   The Stackifier is based on [this paper](https://github.com/mirkosertic/Bytecoder/tree/master/core/src/main/java/de/mirkosertic/bytecoder/stackifier/SRC-RR-4.pdf). It
   tries to remove all GOTO statements and replaces them with structured control flow elements and multi level break
   and continues. The Stackifier does only work for reducible control flows and also does not support 
   exception handling. The generated output is smaller and in some cases faster compared to the Relooper output.

Relooper output is enabled by default for `JS` and `Wasm` backends. The Stackifier can be enabled for `CLI` or `Maven` by setting
`preferStackifier` to `true` as a configuration parameter. If Stackifier is enabled and Bytecoder detects an
irreducible control flow Relooper is used as a fallback.

Stackifier is used as the default by the `OpenCL` backend.
