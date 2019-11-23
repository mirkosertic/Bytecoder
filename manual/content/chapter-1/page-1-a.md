---
title: "The Command-Line Interface (CLI)"
date: 2019-11-22T14:49:24+02:00
draft: false
weight: 1
---

## Jump-Start

Lets assume we have the following Java class, and we want to compile it to JavaScript and run it in the Browser:

```
package bytecodertest;

public class HelloWorld {

    public static void main(String[] args) {
        System.out.println("Hello World!");
    }
}
```

First of all, we need to compile the Java sources to a JVM class file. This is done by using the `javac` tool from the JDK:

```
javac HelloWorld.java
```

Now, we have the compiled .class files. Now we can use the Bytecoder CLI to compile it to JavaScript!

**Step 1: Download the CLI from Maven central**:

``` 
wget http://central.maven.org/maven2/de/mirkosertic/bytecoder/bytecoder-cli/{{% siteparam "bytecoderversion" %}}/bytecoder-cli-{{% siteparam "bytecoderversion" %}}-executable.jar
```

**Step 2: Invoke the CLI and compile to JavaScript**:

```
java -jar bytecoder-cli-{{% siteparam "bytecoderversion" %}}-executable.jar -classpath=. -mainclass=bytecodertest.HelloWorld -builddirectory=. -backend=js -minify=false
```

**Step 3: Create an embedding HTML document**:

Now we have a `bytecoder.js` file, which needs to be embedded into a HTML document. Here is a sample
HTML file embedding the Bytecoder JavaScript and invoking it:

```
<html>
    <head>
        <title>Bytecoder Test</title>
    </head>
    <body>
        <script type="text/javascript" src="bytecoder.js"></script>
        <script type="text/javascript">

            console.log("Init");
            bytecoder.bootstrap();
            bytecocer.initializeFileIO();

            bytecoder.exports.main();
            console.log("Done");
        </script>
    </body>
</html>
```

After opening this HTML file in a Browser you will see the "Hello World!" output in the JavaScript console log.

**Bonus: Compiling to WebAssembly**

Generating WebAssembly output is just a command line switch. 

```
java -jar bytecoder-cli-{{% siteparam "bytecoderversion" %}}-executable.jar -classpath=. -mainclass=bytecodertest.HelloWorld -builddirectory=. -backend=wasm -minify=false
```

However, you need a different style of
HTML embedding, which is shown here:

```
<html>
    <meta charset="UTF-8">
    <script src="bytecoder_wasmbindings.js"></script>
    <script>
        var bytecoderWasmFile = 'bytecoder.wasm';
        var instantiated = function(result) {
            bytecoder.init(result.instance);
            bytecoder.exports.initMemory(0);
            bytecoder.exports.bootstrap(0);
            bytecoder.initializeFileIO();

            // We have to activate the garbage collector!
            var gcInterval = 10000;
            var gcRunning = false,
            setInterval(function() {
                if (!gcRunning) {
                    gcRunning = true;
                    bytecoder.exports.GC(0);
                    gcRunning = false;
                }
            }, gcInterval);

            bytecoder.exports.main(0);

        };
        WebAssembly.instantiateStreaming(fetch(bytecoderWasmFile), bytecoder.imports).then(instantiated).catch(function(error) {
            var request = new XMLHttpRequest();
            request.open('GET', bytecoderWasmFile);
            request.responseType = 'arraybuffer';
            request.send();
            request.onload = function() {
                var bytes = request.response;
                WebAssembly.instantiate(bytes, bytecoder.imports).then(instantiated);
            };
        });
    </script>
</html>
```

{{% notice warning %}}
The WebAssembly backend includes a very simple mark and sweep garbage collector.
When GC runs, the application halts ( stop the world ) and memory is scanned and
freed. However, this is a **very** expensive process, so you maybe want to configure
the garbage collector interval by hand depending an your use case.

The Bytecoder GC is will be removed as soon as there is a Wasm built-in garbage collector
available.
{{% /notice %}}