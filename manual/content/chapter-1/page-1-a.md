---
title: "The Command-Line Interface (CLI)"
date: 2019-10-25T14:49:24+02:00
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

Step 1: Download the CLI from Maven central:

``` 
wget http://central.maven.org/maven2/de/mirkosertic/bytecoder/bytecoder-cli/{{% siteparam "bytecoderversion" %}}/bytecoder-cli-{{% siteparam "bytecoderversion" %}}-executable.jar
```

Step 2: Invoke the CLI:

```
java -jar bytecoder-cli-{{% siteparam "bytecoderversion" %}}-executable.jar -classpath=. -mainclass=bytecodertest.HelloWorld -builddirectory=. -backend=js -minify=false
```

Step 3: Create an embedding HTML document

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

            bytecoder.exports.main();
            console.log("Done");
        </script>
    </body>
</html>
```

After opening this HTML file in a Browser you will see the "Hello World!" output in the JavaScript console log.

