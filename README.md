# Bytecoder

Rich Domain Model for Java Bytecode and Framework to interpret it.

## High-Level Goals

* Ability to cross-compile JVM Bytecode to C or other languages
* Primary compile targets are C and WebAssembly
* Use other toolchains such as clang or emscripten to further optimize generated code
* Provide an runtime environment with GC to support cross compiling
* Reuse or implement cross-compileable Java Classlib
