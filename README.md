# Bytecoder

Rich Domain Model for Java Bytecode and Framework to interpret and transpile it.

## High Level Goals

* Ability to cross-compile JVM Bytecode to C or other languages
* Primary compile targets are C and WebAssembly
* Use other toolchains such as clang or emscripten to further optimize generated code
* Reuse or implement cross-compileable Java Classlib

## Compiling strategies

*JVM* Bytecode is basically a typed stack machine. *WebAssembly* is also a stack maschine. So these
two stack machines can be translated to each other.
*C* on the other hand is a high level language, and its compile target can either be a register- or stack based machine.To keep translation process simple, a stack machine is emulated in C code in a very efficient way.

So for *WebAssembly* the most performant code will be a direct translation between JVM Bytecode and WebAssembly Bytecode. Compiling JVM ByteCode to C and then compiling to WebAssembly would be a emulated stack machine running on a stack maschine,
which will of course be a performance penality.

## Memory management

*JVM Bytecode* relies on the garbage collection mechanism provided by the Java Runtime. Webassembly has no GC support on the current MVP. Also plain C has no garbage collection build in. So the WebAssembly and C compile targets must include garbage collection code for memory management.

