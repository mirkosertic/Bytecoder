---
title: "The Command-Line Interface (CLI)"
date: 2023-01-30T00:00:00+02:00
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
mkdir -p bytecodertest && cd bytecodertest  && javac HelloWorld.java
```
The directry structure
```
[root@host]# tree
.
├── bytecoder-cli-2021-11-02-executable.jar
└── bytecodertest
    └── HelloWorld.java

```

Now, we have the compiled .class files. Now we can use the Bytecoder CLI to compile it to JavaScript!

**Step 1: Download the CLI from Maven central**:

``` 
wget https://repo.maven.apache.org/maven2/de/mirkosertic/bytecoder/bytecoder-cli/{{% siteparam "bytecoderversion" %}}/bytecoder-cli-{{% siteparam "bytecoderversion" %}}-executable.jar
```

**Step 2: Invoke the CLI and compile to JavaScript**:

```
java -jar bytecoder-cli-{{% siteparam "bytecoderversion" %}}-executable.jar compile js -classpath=. -mainclass=bytecodertest.HelloWorld -builddirectory=.
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

{{% notice note %}}
Bytecoder's WebAssembly backend can generate Wasm binaries without further third party software like wabt or binaryen.
{{% /notice %}}

However, you need a different style of
HTML embedding, which is shown here:

```
<html>
    <meta charset="UTF-8">
    <script src="bytecoder.js"></script>
    <script>
        bytecoder.instantiate('lua_wasmwasmclasses.wasm').then(function() {

            console.log("Bootstrapped");
            bytecoder.instance.exports.main(null, bytecoder.instance.exports.newObjectArray(null, 0));
            console.log("Ready for action!");
        });
    </script>
</html>
```

{{% notice warning %}}
The WebAssembly backend generates Wasm bytecoded based on the Exception-Handling and GC proposal. These features
must be manually enabled in Chrome or Firefox as long as they have not been fully standardized.
{{% /notice %}}
