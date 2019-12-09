---
title: "Interoperability"
date: 2019-10-25T14:49:24+02:00
draft: false
weight: 5
---

## Hello World, extended edition!

Let's see how a simple Java program can be written that changes the document title in the Browser. Here it is:

```
import de.mirkosertic.bytecoder.api.web.Event;
import de.mirkosertic.bytecoder.api.web.EventListener;
import de.mirkosertic.bytecoder.api.web.HTMLDocument;
import de.mirkosertic.bytecoder.api.web.Window;

public class OpaqueReferenceTest {

    public static void main(String[] args) {
        final Window w = Window.window();
        w.document().addEventListener("click", new EventListener<ClickEvent>() {
            @Override
            public void run(final ClickEvent aValue) {
                w.document().title("clicked!");
            }
        });
    }
}
```

I'll try to explain the basics behind this and how it can be compiled to JavaScript or WebAssembly in the following sections.

### Interoperability and runtime linkage

Programs do not live on their own, they need to communicate with their environment.
This communication can be tricky if done on multiple environments. Bytecoder supports
JavaScript, WebAssembly and OpenCL as target platforms. How does this wok?

#### OpaqueReferenceTypes API

Bytecoder allows transparent usage of APIs not implemented by Bytecoder itself. Such APIs are
provided by the host environment, for instance the DOM API or interaction with the browser window.
Bytecoder support such APIs by so called OpaqueReferenceTypes. For every external API, a new OpaqueReferenceType
in form of a JVM interface class needs to be created. Bytecoder already comes with implementations for the
browser window and the DOM.

See the following example, which demonstrates calls from Bytecoder to the HTML Canvas API:

```
Window window = Window.window();
Document document = window.document();
final HTMLCanvasElement theCanvas = document.getElementById("benchmark-canvas");
CanvasRenderingContext2D renderingContext2D = theCanvas.getContext("2d");

renderingContext2D.moveTo(10, 10);
renderingContext2D.lineTo(20, 20);
```

Bytecoder also supports event listeners, as seen in the following example:

```
final HTMLElement button = document.getElementById("button");
button.addEventListener("click", new EventListener<ClickEvent>() {
    @Override
    public void run(ClickEvent aValue) {
        button.disabled = true;
    }
});
```

The OpaqueReferenceType API allows the following types for Bytecoder-Host communication:

* Primitives(int,float,double,long,char,byte,short)
* `java.lang.String`
* `de.mirkosertic.bytecoder.api.OpaqueReferenceType` and sub classes of it
* `de.mirkosertic.bytecoder.api.Callback` and sub classes of it

{{% notice note %}}
`java.lang.String` references are a special case. They are objects in the sense of the JVM,
but they are not automatically converted to JavaScript String instances on host side due to
the expensive conversion operation and its potential performance impact. However, there are
handy conversion operations available to do it if its really needed.
The conversion functions are part of the global `bytecoder` object as 
`bytecoder.toJSString(aBytecoderString)` and `bytecoder.toBytecoderString(aJSString)` respectively.
{{% /notice %}}

{{% notice warning %}}
The JVM long datatype is currently only in a limited form available in Bytecoder. There is currently 
no native 64-bit integer datatype available in JavaScript. Bytecoder is limited
to a 53-bit range due to JavaScript's IEEE 754 double precision number type. However, once JavaScript BigInt 
will be supported by all major browsers, Bytecoder will use BigInt as a substitute for the 
JVM long datatype. For now, there is no full emulation of long datatype planned.
{{% /notice %}}

#### Importing functionality from the host environment

Using host environment functionality is quite common. This can be either simply
logging or more complex code. Basically something that cannot be archived by plain OpaqueReferenceTypes.

Java has a built-in language feature for importing functionality. The `native` keyword!

For instance, we take a look at the `TMath` runtime class:

```
public class TMath extends TObject {

    public static native double sqrt(double aValue);
}
```

The `native` keyword instructs the JVM to link the implementation code from somewhere else.
This linking is done when bootstrapping the Bytecoder runtime. By default, Bytecoder will
import the implementation using a `modulename` and an `importname`. The `modulename` is
derived from the classname in lowercase, the `importname` is the method name to import.

##### Native-Linking in JavaScript

At startup, the following code must be provided:

```
bytecoder.imports.math = {
    sqrtDOUBLE: function(p1) {
        return Math.sqrt(p1);
    },
};
```

##### Native-Linking in WebAssembly

At startup, the following code must be provided:

```
bytecoder.imports.math = {
    sqrtDOUBLE: function(thisref, p1) {
        return Math.sqrt(p1);
    },
};
```

##### Customizing the module names

Sometimes you want to provide your own `modulename` and `importname`. This can be
done by adding an `@de.mirkosertic.bytecoder.api.Import` annotation to the native method:

```
public class CanvasRenderingContext2D {

    @Import(module = "canvas", name = "canvasClear")
    public native void clear();
    
}
```

#### Exporting functionality to the host environment

We also want to make functionality be callable from the host environment. The most 
important use case for this is to call out program! So, how can this be done? Java
has no keyword that could mimic this behavior, so we have to provide our own. Method
that should be callable from the host environment needs to be annotated with 
`@de.mirkosertic.bytecoder.api.Export`, as seen in the following example:

```
public class JBox2DSimulation {
    @Export("proceedSimulation")
    public static void proceedSimulation() {
    }
}
```

##### Calling exported functionality from JavaScript

Just call an exported method using the Bytecoder module API:

```
bytecoder.exports.proceedSimulation();
```

##### Calling exported functionality from WebAssembly

Easy, just call the method:

```
runningInstance.exports.proceedSimulation(0);
```

The WebAssembly runtime only makes `@de.mirkosertic.bytecoder.api.Export` annotated 
methods available as exports.
