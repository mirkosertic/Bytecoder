#!/bin/bash
/home/sertic/clang+llvm-9.0.1-x86_64-linux-gnu-ubuntu-16.04/bin/llc -O3 -filetype=obj ./codelayout.ll -o ./codelayout.o
/home/sertic/clang+llvm-9.0.1-x86_64-linux-gnu-ubuntu-16.04/bin//wasm-ld ./codelayout.o -o ./codelayout.wasm --export-dynamic -allow-undefined --no-entry
