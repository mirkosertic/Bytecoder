function decodeUtf16(w) {
    var i = 0;
    var len = w.length;
    var w1, w2;
    var charCodes = [];
    while (i < len) {
        var w1 = w[i++];
        if ((w1 & 0xF800) !== 0xD800) { // w1 < 0xD800 || w1 > 0xDFFF
            if (w1 != 0) charCodes.push(w1);
            continue;
        }
        if ((w1 & 0xFC00) === 0xD800) { // w1 >= 0xD800 && w1 <= 0xDBFF
            throw new RangeError('Invalid octet 0x' + w1.toString(16) + ' at offset ' + (i - 1));
        }
        if (i === len) {
            throw new RangeError('Expected additional octet');
        }
        w2 = w[i++];
        if ((w2 & 0xFC00) !== 0xDC00) { // w2 < 0xDC00 || w2 > 0xDFFF)
            throw new RangeError('Invalid octet 0x' + w2.toString(16) + ' at offset ' + (i - 1));
        }
        charCodes.push(((w1 & 0x3ff) << 10) + (w2 & 0x3ff) + 0x10000);
    }
    return String.fromCharCode.apply(String, charCodes);
}

const bytecoder = {
    imports: {
        "java.lang.Object.Ljava$lang$Class$$getClass$$": function(inst) {
            return inst.constructor.$rt;
        },
        "de.mirkosertic.bytecoder.classlib.VM.Ljava$lang$Class$$bytePrimitiveClass$$": function() {
            return bytecoder.primitives.byte;
        },
        "de.mirkosertic.bytecoder.classlib.VM.Ljava$lang$Class$$charPrimitiveClass$$": function() {
            return bytecoder.primitives.char;
        },
        "de.mirkosertic.bytecoder.classlib.VM.Ljava$lang$Class$$shortPrimitiveClass$$": function() {
            return bytecoder.primitives.short;
        },
        "de.mirkosertic.bytecoder.classlib.VM.Ljava$lang$Class$$intPrimitiveClass$$": function() {
            return bytecoder.primitives.int;
        },
        "de.mirkosertic.bytecoder.classlib.VM.Ljava$lang$Class$$floatPrimitiveClass$$": function() {
            return bytecoder.primitives.float;
        },
        "de.mirkosertic.bytecoder.classlib.VM.Ljava$lang$Class$$doublePrimitiveClass$$": function() {
            return bytecoder.primitives.double;
        },
        "de.mirkosertic.bytecoder.classlib.VM.Ljava$lang$Class$$longPrimitiveClass$$": function() {
            return bytecoder.primitives.long;
        },
        "de.mirkosertic.bytecoder.classlib.VM.Ljava$lang$Class$$booleanPrimitiveClass$$": function() {
            return bytecoder.primitives.boolean;
        },
        "jdk.internal.misc.ScopedMemoryAccess.V$registerNatives$$": function() {
        },
        "java.lang.Float.I$floatToRawIntBits$F": function(value) {
            let fl = new Float32Array(1);
            fl[0] = value;
            let br = new Int32Array(fl.buffer);
            return br[0];
        },
        "java.lang.Double.Z$isNaN$D": function(a) {
            return isNaN(a) ? 1 : 0
        },
        "java.lang.Float.Z$isNaN$F": function(a) {
            return isNaN(a) ? 1 : 0
        },
        "java.lang.Math.I$min$I$I": function(a,b) {
            return Math.min(a,b);
        },
        "java.lang.Math.F$min$F$F": function(a,b) {
            return Math.min(a,b);
        },
        "java.lang.Math.I$max$I$I": function(a,b) {
            return Math.max(a,b);
        },
        "java.lang.Math.D$floor$D": function(a) {
            return Math.floor(a);
        },
        "java.lang.Math.F$floor$F": function(a) {
            return Math.floor(a);
        },
        "java.lang.Math.D$ceil$D": function(a) {
            return Math.ceil(a);
        },
        "java.lang.Math.F$ceil$F": function(a) {
            return Math.ceil(a);
        },
        "java.lang.Math.D$toRadians$D": function(a) {
            return a * (Math.PI/180.0);
        },
        "java.lang.Math.D$cos$D": function(a) {
            return Math.cos(a);
        },
        "java.lang.Math.D$sin$D": function(a) {
            return Math.sin(a);
        },
        "java.lang.Math.D$sqrt$D": function(a) {
            return Math.sqrt(a);
        },
        "java.lang.Math.D$cbrt$D": function(a) {
            return Math.cbrt(a);
        },
        "java.lang.Math.D$log$D": function(a) {
            return Math.log(a);
        },
        "java.lang.Math.D$random$$": function() {
            return Math.random();
        },
        "java.lang.StringUTF16.Z$isBigEndian$$": function() {
            return 1;
        },
        "java.lang.reflect.Array.Ljava$lang$Object$$newArray$Ljava$lang$Class$$I": function(t, l) {
            return bytecoder.newarray(l, null);
        },
        "jdk.internal.misc.CDS.Z$isDumpingClassList0$$": function() {
            return 0;
        },
        "jdk.internal.misc.CDS.Z$isDumpingArchive0$$": function() {
            return 0;
        },
        "jdk.internal.misc.CDS.Z$isSharingEnabled0$$": function() {
            return 0;
        },
        "jdk.internal.misc.CDS.V$initializeFromArchive$Ljava$lang$Class$": function(cls) {
        },
        "java.io.UnixFileSystem.I$getBooleanAttributes0$Ljava$lang$String$": function(fsref, path) {
            let jsPath = bytecoder.toJSString(path);
            try {
                let request = new XMLHttpRequest();
                request.open('HEAD',jsPath,false);
                request.send(null);
                if (request.status === 200) {
                    let length = request.getResponseHeader('content-length');
                    return 0x01;
                }
                return 0;
            } catch(e) {
                return 0;
            }
        },
        "java.lang.Class.Ljava$lang$ClassLoader$$getClassLoader$$": function(classRef) {
            return null;
        },
        "java.io.FileInputStream.I$open0$Ljava$lang$String$": function(fis, name) {
            let fd = bytecoder.openForRead(bytecoder.toJSString(name));
            if (fd >= 0) fis.fd.fd = fd;
            return fd;
        },
        "java.io.FileInputStream.J$skip0$I": function(fis, amount) {
            let fd = fis.fd.fd;
            let x = bytecoder.filehandles[fd];
            return x.J$skip0$I(fd, amount);
        },
        "java.io.FileInputStream.I$available0$$": function(fis) {
            let fd = fis.fd.fd;
            let x = bytecoder.filehandles[fd];
            return x.I$available0$$(fd);
        },
        "java.io.FileInputStream.I$read0$$": function(fis) {
            let fd = fis.fd.fd;
            let x = bytecoder.filehandles[fd];
            return x.I$read0$$(fd);
        },
        "java.io.FileInputStream.I$readBytes$$B$I$I": function(fis, b, off, len) {
            let fd = fis.fd.fd;
            let x = bytecoder.filehandles[fd];
            return x.I$readBytes$$B$I$I(fd, b, off, len);
        },
        "java.io.FileOutputStream.V$writeBytes$$B$I$I": function(fis, b, off, len) {
            let fd = fis.fd.fd;
            let x = bytecoder.filehandles[fd];
            x.V$writeBytes$$B$I$I(fd, b, off, len);
        },
        "java.io.FileOutputStream.V$writeInt$I": function(fis, cp) {
            let fd = fis.fd.fd;
            let x = bytecoder.filehandles[fd];
            x.V$writeInt$I(fd, cp);
        },
        "java.lang.Class.Ljava$lang$Class$$forName$Ljava$lang$String$$Z$Ljava$lang$ClassLoader$": function(className, initialize, classLoader) {
            throw 'Not supported class for reflective access';
        },
        "java.lang.invoke.LambdaMetafactory.Ljava$lang$invoke$CallSite$$metafactory$Ljava$lang$invoke$MethodHandles$Lookup$$Ljava$lang$String$$Ljava$lang$invoke$MethodType$$Ljava$lang$invoke$MethodType$$Ljava$lang$invoke$MethodHandle$$Ljava$lang$invoke$MethodType$": function(lookups, methodName, invokedType, samMethodType, implMethod, aInstantiatedMethodType) {
            return {
                Ljava$lang$invoke$MethodHandle$$getTarget$$: function() {
                    return {
                        lambdaref: this,
                        invokeExact: function() {
                            let linkingArguments = Array.from(arguments);
                            let instType = invokedType[0];
                            let inst = null;
                            if ((instType.$modifiers & 512) > 0) {
                                let LambdaClass = class extends instType(java$lang$Object) {
                                    static #rt = undefined;
                                    static get $rt() {
                                        if (!this.#rt) {
                                            this.#rt = bytecoder.newRuntimeClassFor(this,[java$lang$Object,instType]);
                                        }
                                        return this.#rt;
                                    }
                                };
                                inst = new LambdaClass();
                            } else {
                                inst = new instType();
                            }
                            switch (implMethod.kind) {
                                case 1:
                                    // Invoke static
                                    inst.$lambdaimpl = function() {
                                        let captureArguments = Array.from(arguments);
                                        return implMethod.owner[implMethod.methodName].apply(inst, linkingArguments.concat(captureArguments));
                                    };
                                    break;
                                case 2:
                                    // Invoke virtual
                                    inst.$lambdaimpl = function() {
                                        let captureArguments = Array.from(arguments);
                                        let allargs = linkingArguments.concat(captureArguments)
                                        return implMethod.owner.prototype[implMethod.methodName].apply(allargs[0], allargs.splice(1));
                                    };
                                    break;
                                case 3:
                                    // Invoke interface
                                    inst.$lambdaimpl = function() {
                                        let captureArguments = Array.from(arguments);
                                        let allargs = linkingArguments.concat(captureArguments)
                                        return allargs[0][implMethod.methodName].apply(allargs[0], allargs.splice(1));
                                    };
                                    break;
                                case 4:
                                    // Invoke constructor
                                    inst.$lambdaimpl = function() {
                                        let captureArguments = Array.from(arguments);
                                        let obj = new implMethod.owner();
                                        implMethod.owner.prototype[implMethod.methodName].apply(obj, linkingArguments.concat(captureArguments));
                                        return obj;
                                    };
                                    break;
                                case 5:
                                    // Invoke special
                                    inst.$lambdaimpl = function() {
                                        let captureArguments = Array.from(arguments);
                                        let allargs = linkingArguments.concat(captureArguments)
                                        return allargs[0][implMethod.methodName].apply(allargs[0], allargs.splice(1));
                                    };
                                    break;
                                default:
                                    throw 'Not supported method kind : ' + implMethod.kind;
                            }
                            return inst;
                        }
                    };
                }
            };
        }
    },
    exports: {},
    filehandles : [],
    stringconstants: [],
    cmp: function(a,b) {
        if (a > b) return 1;
        if (a < b) return -1;
        return 0;
    },
    instanceOf: function(a,b) {
        if (a) {
            let rt = a.constructor.$rt;
            return rt.instanceOf(a, b);
        }
        return 0;
    },
    registerStack: function(exception, stack) {
        exception.stack = stack;
        return exception;
    },
    toJSString: function(str) {
        if (str && str.value.data) {return decodeUtf16(str.value.data);} else return '';
    },
    newarray: function(len, defaultvalue) {
        let x = new de$mirkosertic$bytecoder$classlib$Array();
        x.data = new Array(len);
        x.data.fill(defaultvalue);
        return x;
    },
    methodHandle: function(owner, methodName, kind) {
        return {
            owner: owner,
            methodName: methodName,
            kind: kind,
            invokeExact: function() {
                //throw 'not implemented';
            }
        };
    },
    primitives: {
        byte: {
        },
        char: {
        },
        short: {
        },
        int: {
        },
        float: {
        },
        double: {
        },
        long: {
        },
        boolean: {
        },
        void: {
        }
    },
    openForRead :  function(path) {
        try {
            let request = new XMLHttpRequest();
            request.open('GET',path,false);
            request.overrideMimeType('text/plain; charset=x-user-defined');
            request.send(null);
            if (request.status === 200) {
                let length = request.getResponseHeader('content-length');
                let responsetext = request.response;
                let buf = new ArrayBuffer(responsetext.length);
                let bufView = new Uint8Array(buf);
                let i = 0;
                const strLen = responsetext.length;
                for (; i < strLen; i++) {
                    bufView[i] = responsetext.charCodeAt(i) & 0xff;
                }
                let handle = bytecoder.filehandles.length;
                bytecoder.filehandles[handle] = {
                    currentpos: 0,
                    data: bufView,
                    size: length,
                    J$skip0$I: function(fd, amount) {
                        let remaining = this.size - this.currentpos;
                        let possible = Math.min(remaining, amount);
                        this.currentpos += possible;
                        return possible;
                    },
                    I$available0$$: function(fd) {
                        return this.size - this.currentpos;
                    },
                    I$read0$$: function(fd) {
                        return this.data[this.currentpos++];
                    },
                    I$readBytes$$B$I$I: function(fd, target, offset, length) {
                        if (length === 0) {return 0;}
                        let remaining = this.size - this.currentpos;
                        let possible = Math.min(remaining, length);
                        if (possible === 0) {return -1;}
                        for (let j=0; j<possible; j++) {
                            target.data[offset++] = this.data[this.currentpos++];
                        }
                        return possible;
                    }
                };
                return handle;
            }
            return -1;
        } catch(e) {
            return -1;
        }
    },
    newRuntimeClassFor: function(type,supportedtypes) {
        return {
            Ljava$lang$ClassLoader$$getClassLoader$$: function() {
                return null;
            },
            Ljava$lang$ClassLoader$$getClassLoader0$$: function() {
                return null;
            },
            Z$desiredAssertionStatus$$: function() {
                return false;
            },
            Ljava$lang$Object$$newInstance$$: function() {
                const x = new type.$i();
                x.V$$init$$$();
                return x;
            },
            $Ljava$lang$Object$$getEnumConstants$$: function() {
                return type.$i.$VALUES;
            },
            instanceOf: function(a, b) {
                if (supportedtypes.includes(b)) {
                    return 1;
                }
                return 0;
            },
        };
    }
};

bytecoder.filehandles[1] = {
    V$writeBytes$$B$I$I: function(fd, b, off, len) {
        let decoder = new TextDecoder();
        let arr = new Uint8Array(b.data.slice(off, off +  len));
        console.log(decoder.decode(arr).replace('\n', ''));
    },
    V$writeInt$I: function(fd, cp) {
        let decoder = new TextDecoder();
        let arr = new Uint8Array([cp]);
        console.log(decoder.decode(arr).replace('\n', ''));
    },
};
