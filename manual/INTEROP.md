# Interoperability and runtime linkage

Programs do now live on their own, they need to communicate with their environment.
This communication can be tricky if done on multiple environments. Bytecoder supports
JavaScript, WebAssembly and OpenCL as target platforms. How does this wok?

## OpaqueReferenceTypes API

Bytecoder allows transparent usage of APIs not implemented by Bytecoder itself. Such APIs are
provided by the host environment, for instance the DOM API or interaction with the browser window.
Bytecoder support such APIs by so called OpaqueReferenceTypes. For every external API, a new OpaqueRecerenceType
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
button.addEventListener("click", new Callback<ClickEvent>() {
    @Override
    public void run(ClickEvent aValue) {
        button.style().setProperty("disabled", "true");
    }
});
```

## Importing functionality from the host environment

Using host environment functionality is quite common. This can be either simply
logging or more complex code like painting on a HTML5 Canvas.

Java has a built-in language feature for importing functionality. The `native` keyword!

for instance, we take a look at the `TMath` runtime class:

```
public class TMath extends TObject {

    public static native double sqrt(double aValue);
}
```

The `native` keyword instructs the JVM to link the implementation code from somewhere else.
This linking is done when bootstrapping the Bytecoder runtime. By default, Bytecoder will
import the implementation using a `modulename` and an `importname`. The `modulename` is
derived from the classname in lowercase, the `importname` is the method name to import.

### Native-Linking in JavaScript

At startup, the following code must be provided:

```
bytecoder.imports.tmath = {
    sqrt: function(p1) {
        return Math.sqrt(p1);
    },
};
```

### Native-Linking in WebAssembly

At startup, the following code must be provided:

```
// Our module for dynamic linking. This is passed to the WASM instantiate method.
var importObject = {
    tmath: {
        sqrt: function(thisref, p1) {return Math.sqrt(p1);},
    },
};
```

### Customizing the module names

Sometimes you want to provide your own `modulename` and `importname`. This can be
done by adding an `@de.mirkosertic.bytecoder.api.Import` annotation to the native method:

```
public class CanvasRenderingContext2D {

    @Import(module = "canvas", name = "canvasClear")
    public native void clear();
    
}
```

## Exporting functionality to the host environment

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

### Calling exported functionality from JavaScript

Just call an exported method using the Bytecoder module API:

```
bytecoder.exports.proceedSimulation();
```

### Calling exported functionality from WebAssembly

Easy, just call the method:

```
runningInstance.exports.proceedSimulation(0);
```

The WebAssembly runtime only makes `@de.mirkosertic.bytecoder.api.Export` annotated 
methods available as exports.