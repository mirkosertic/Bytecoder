---
title: "WebAssembly Memory Management"
date: 2019-10-25T14:49:24+02:00
draft: false
weight: 2
---

The WebAssembly backend emulates high level data types using WebAssembly primitives. 
At the moment only `i32` and `f32` types. All other data types are composed using 
data blocks in the `linear memory` and pointers, which are basically also `i32`. This 
Backend does not use `Wasm64`. 

### Memory

The memory is managed. It is dynamically allocated and automatically freed using a
Mark-and-Sweep garbage collector. All memory is directly mapped to the Wasm linear memory.

Memory is split into `Heap` and `Stack`. The `Heap` is used for allocated objects. The `Stack` 
is used to hold `activation records` used during method invocation.

`Heap` and `Stack` grow in opposite directions. `Heap` grows from bottom to top of the 
linear memory. `Stack` grows from top to bottom. The heap contains a lot of live and dead
objects and tends to be fragmented. Stack grows and shrinks with a compile time detected
rates, and cannot fragment, as it is basically a stack of `activation records`.

### Objects

Objects are a pointer to an allocated memory block. This memory contains an `object header` and
an `object body`. The `object header` contains the following fields:

 Field       | Type | Description                                   
-------------|------|-----------------------------------------------
 type        | i32  | Reference to the runtime class of this object 
 vtable      | i32  | The virtual method resolver function          

The `object body` contains the raw data of the object depending on its type.

### Runtime classes

Runtime classes are objects of type `runtime class` and a fixed `virtual table`.
Their `object body` contains the following fields:

 Field       | Type | Description                                    
-------------|------|------------------------------------------------
 initstatus  | i32  | The initialization status of the runtime class 
 enumvals    | i32  | Pointer to optional enum values array          
 classname   | i32  | Pointer to the class name string object
 typeid      | i32  | Internal unique id of this runtime class          

Static class attributes are added to the field list on demand.

### Arrays

Arrays are objects of type `array` and a fixed `virtual table`.
Their `object body` contains the following fields:

 Field       | Type | Description                                    
-------------|------|------------------------------------------------
 length      | i32  | The length of the array                        
 1..length   | i32  | Pointer for every array element                

### Regular object instances

The `object body` is a list of instance members. The `object body` is
constructed as written above.
