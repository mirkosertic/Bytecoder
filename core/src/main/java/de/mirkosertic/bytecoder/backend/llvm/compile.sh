#!/bin/bash
$LLVM_HOME/bin/llc -O3 -filetype=obj ./codelayout.ll -o ./codelayout.o
$LLVM_HOME/bin/wasm-ld ./codelayout.o -o ./codelayout.wasm --export-dynamic -allow-undefined --no-entry
