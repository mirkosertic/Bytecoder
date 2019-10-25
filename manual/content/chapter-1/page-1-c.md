---
title: "Unit Testing"
date: 2019-10-25T14:49:24+02:00
draft: false
weight: 3
---

## Testing code generation

Bytecoder comes with built in JUnit Testing support using a specialized test runner. This test runner compiles the body of the test method to a target language
and executes this code. For instance, the following JUnit Test

```
@RunWith(BytecoderUnitTestRunner.class)
public class SimpleMathTest {

    public static int sum(int a, int b) {
        return a + b;
    }

    @Test
    public void testAdd() throws Exception {
        int c = sum(10, 20);
        Assert.assertEquals(20, c, 0);
    }
}
```

is compiled to JavaScript and WebAssembly and executed using a Selenium Chrome driver. This test runner also supports comparison of original Java code and its cross compiled counterpart. This mechanism is the core tool to test the compiler and the Classlib.

Bytecoder relies for WebAssembly unit testing on Chrome. To make everything working, you have to add `CHROMEDRIVER_BINARY` 
environment variable pointing to an installed Selenium Chrome WebDriver binary. You can get the latest release
of WebDriver here: https://sites.google.com/a/chromium.org/chromedriver.  
