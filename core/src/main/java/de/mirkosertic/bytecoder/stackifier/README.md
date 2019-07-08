# Prototype of Stackifier

## How and What

We try to do a hierarchical topologic ordering based on the following rules:

* Dominated CFG nodes of A must be in a deeper nesting level of A, so they can correctly branch out
* Blocks for forward jumps must end before the jump target
* Blocks for back edges must start at the jump target
* A block can only contain its dominated nodes

## Example CFG 1


```
    A
```

should result in JS:

```
function() {
    A
}
```

## Example CFG 2

```
    A -> B -> C
```

should result in JS:

```
function() {
    A
    B
    C
}
```

## Example CFG 3

```
   A -> B -> C
   \
    \-> D
```

should result in JS:

```
function() {
    a: {
        A with optional break a;
        D
    }
    B
    C
}
```

## Example CFG 4

```
    A -> B -> C--|
    \<-----------|
```

should result in JS:

```
function() {
    a: for (;;) {
        A
        B
        C
        continue a;
    }
}
```

## Example CFG 5

```
    A -> B -> C -> D -> E
         |<--------|    |
    |<------------------|        
```

should result in JS:

```
function() {
    a: for (;;) {
        A
        b: {
            B
            C
            D
            continue b;
        }
        E
        continue a;
    \
}
```