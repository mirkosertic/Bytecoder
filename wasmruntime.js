const bytecoder = {

    instance: null,

    module: null,

    imports: {
        "jdk.internal.misc.ScopedMemoryAccess": {
            V$registerNatives$$: function() {
            }
        },
        "java.lang.invoke.LambdaMetafactory": {
            Ljava$lang$invoke$CallSite$$metafactory$Ljava$lang$invoke$MethodHandles$Lookup$$Ljava$lang$String$$Ljava$lang$invoke$MethodType$$Ljava$lang$invoke$MethodType$$Ljava$lang$invoke$MethodHandle$$Ljava$lang$invoke$MethodType$: function() {
            }
        },
        "jdk.internal.misc.CDS": {
            Z$isDumpingClassList0$$: function() {
                return false;
            },
            Z$isDumpingArchive0$$: function() {
                return false;
            },
            Z$isSharingEnabled0$$: function() {
                return false;
            },
            J$getRandomSeedForDumping$$: function() {
                return Math.trunc(Math.random() * 10000000);
            },
            V$initializeFromArchive$Ljava$lang$Class$: function() {
            }
        },
        "java.lang.Math": {
            I$min$I$I: function(a,b) {
                return Math.min(a,b);
            }
        },
        "java.lang.System": {
            J$currentTimeMillis$$: function() {
                return Date.now();
            }
        },
        "java.lang.StringBuilder": {
            V$initializeWith$I: function() {
            },
            Ljava$lang$StringBuilder$$append$Ljava$lang$String$: function() {
            },
            Ljava$lang$String$$toString$$: function() {
            }
        },
        "java.lang.StringBuffer": {
            V$initializeWith$I: function() {
            },
            Ljava$lang$StringBuffer$$append$Ljava$lang$String$: function() {
            },
            Ljava$lang$String$$toString$$: function() {
            }
        },
        "java.lang.String": {
            Z$equals0$Ljava$lang$String$: function() {
            },
            I$length$$: function() {
            },
            Ljava$lang$String$$format$Ljava$lang$String$$$Ljava$lang$Object$: function() {
            },
            C$charAt$I: function() {
            },
            $C$toCharArray$$: function() {
            },
            V$getChars$I$I$$C$I: function() {
            },
            V$initializeWith$$C$I$I: function() {
            }
        },
        "java.lang.Byte": {
            Ljava$lang$String$$toString$B$I: function() {
            }
        },
        "java.lang.Long": {
            Ljava$lang$String$$toString$J$I: function() {
            }
        },
        "java.lang.Integer": {
            Ljava$lang$String$$toString$I$I: function() {
            }
        },
        "java.lang.Float": {
            Z$isNaN$F: function() {
            },
            Ljava$lang$String$$toString$F: function() {
            },
        },
        "de.mirkosertic.bytecoder.classlib.BytecoderCharsetDecoder": {
            $C$decodeFromBytes$Ljava$nio$charset$Charset$$$B: function() {
            }
        },
        "bytecoder": {
        }
    },

    init: function(module, instance) {
        this.module = module;
        this.instance = instance;
    },

    initializeFileIO: function() {
    },

    bootstrap: function() {
        this.instance.exports.bootstrap();
    },

    instantiate: function(wasmUri) {
        return WebAssembly.instantiateStreaming(fetch(wasmUri), bytecoder.imports).then(function (resolved) {
            bytecoder.init(resolved.module, resolved.instance);
            bytecoder.bootstrap();
            bytecoder.initializeFileIO();
            console.log("Bootstrapped");
        }, function (rejected) {
            console.log("Error instantiating webassembly");
            console.log(rejected);
        });
    }
};
