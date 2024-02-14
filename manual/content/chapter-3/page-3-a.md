---
title: "Intermediate Representation"
date: 2019-10-25T14:49:24+02:00
draft: false
weight: 1
---

The Bytecoder internal intermediate representation is basically a directed graph. The key idea behind this is described 
[in this paper](https://github.com/mirkosertic/Bytecoder/blob/master/core/src/main/java/de/mirkosertic/bytecoder/core/ir/c2-ir95-150110.pdf).

Given this Java source code:

```
@Test
public void testSimpleLoop() {
    int x = 0;
    for (int i = 0; i < 100; i++) {
        x = x + i;
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

