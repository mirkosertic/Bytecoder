---
title: "Extending the JRE"
date: 2019-10-25T14:49:24+02:00
draft: false
weight: 7
---

## Extending existing classes

Sometimes the methods or properties you want aren't there, but it's very simple to add them.

```
public abstract class CustomCanvas extends de.mirkosertic.bytecoder.api.web.HTMLCanvasElement {

    // The following two methods are setters and getters for the canvas.width property.

    @de.mirkosertic.bytecoder.api.OpaqueProperty
    public abstract void width(float value);

    @de.mirkosertic.bytecoder.api.OpaqueProperty
    public abstract float width();
}
```

To use your custom methods or properties simply cast a HTMLCanvasElement to CustomCanvas.

```
((CustomCanvas) Window.window().document().getElementById("canvas")).width(1000);
```

## Implementing bridges for JavaScript objects and classes

Explanation of JavaScript data types in this page:

* Objects are non-instantiatable (e.g. window.navigator)
* Classes are instantiable (e.g. new ArrayBuffer(...))

### Objects

```
public abstract class Navigator implements de.mirkosertic.bytecoder.api.OpaqueReferenceType {

    public static native Navigator navigator();

    @de.mirkosertic.bytecoder.api.OpaqueProperty
    public abstract String userAgent();

    @de.mirkosertic.bytecoder.api.OpaqueProperty
    public abstract boolean cookieEnabled();

    // If you want to have a different name in the Java code, you can
    // name the method for example beaconSend and annotate it with
    // @OpaqueMethod("sendBeacon")
    // This specific method has more types of possible arguments,
    // if you need to support those you just need to add more methods
    // but with the other `data` types
    public abstract void sendBeacon(String url, String data);
}
```

```
// Although the import would be created because this
// class is implementing OpaqueReferenceType AND we are
// calling one of it's abstract methods in our Java code,
// not calling one would cause JavaScript errors.
// So to be 100% sure it's better to define fallback to an
// empty object.
bytecoder.imports.navigator = bytecoder.imports.navigator || {};

// This method has no arguments so it's simply called navigator
bytecoder.imports.navigator.navigator = function (thisref) {
    return bytecoder.toBytecoderReference(navigator);
};
```

```
System.out.println(Navigator.navigator().userAgent());
```

### Classes

```
public abstract class ArrayBuffer implements OpaqueReferenceType {

    // The @Import annotation is completely optional and
    // it removes the need for having to include the
    // parameter types in the method name on the JavaScript side.
    @de.mirkosertic.bytecoder.api.Import(module = "arraybuffer", name = "create")
    public static native create(int size);

    @de.mirkosertic.bytecoder.api.OpaqueProperty
    public int byteLength();
}
```

```
// Read the comment on Objects in this section.
// Native methods are not abstract and if we for example
// only call ArrayBuffer.create(...) then an import would not
// be created. But if we also called ArrayBuffer.byteLength()
// it would, so it is much more secure to fallback to {}.
bytecoder.imports.arraybuffer = bytecoder.imports.arraybuffer || {};

// Passing around objects works by references, the Java code
// automatically converts this reference to an instance of
// ArrayBuffer but the JavaScript bindings have no such features.

// Warning: This method uses the @Import annotation which is why
// it is called `create` and not `createINT`.
bytecoder.imports.arraybuffer.create = function (thisref, size) {
    return bytecoder.toBytecoderReference(new ArrayBuffer(size));
};
```

```
System.out.println(ArrayBuffer.create(6).byteLength()); // 6
```

### Taking classes as method parameters

```
public abstract class DataView implements de.mirkosertic.bytecoder.api.OpaqueReferenceType {
    public static native create(ArrayBuffer arrayBuffer);

    // ... opaque methods and properties ...
}
```

```
bytecoder.imports.dataview = bytecoder.imports.dataview || {};

// create - method name, ArrayBuffer - parameter type
bytecoder.imports.dataview.createArrayBuffer = function (thisref, arraybufferref) {
    return bytecoder.toBytecoderReference(new DataView(bytecoder.toJSReference(arraybufferref)));
};
```

## Import and Export semantics

* Imports are methods imported from JavaScript/Host side and called from Java.
* Exports are methods exported from Java and called from JavaScript/Host side.

## Emulating classes and methods

Bytecoder is based on the OpenJDK JRE classlib. However, it is sometimes necessary to
patch existing classes to make them compatible with Bytecoder.

Bytecoder introduces a concept called shadow types for this purpose.

Take a look at the `java.lang.System` class. It needs some adaptation
for make it compatible with Bytecoder. Now, the shadow type called
`de.mirkosertic.bytecoder.classlib.java.lang.TSystem` is introduced.
Shadow types need the package prefix `de.mirkosertic.bytecoder.classlib`
and the `@de.mirkosertic.bytecoder.api.SubstitutesInClass` annotation.

`@SubstitutesInClass` toggles what should be adapted by the shadow
type. It can either override the whole class by setting `completeReplace=true`
or only specified methods by setting `completeReplace=false`.
