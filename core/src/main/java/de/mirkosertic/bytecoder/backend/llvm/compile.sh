#!/bin/bash
llc-10 -O3 -filetype=obj ./codelayout.ll -o ./codelayout.o
wasm-ld-10 ./codelayout.o -o ./codelayout.wasm --export-dynamic -allow-undefined --no-entry
