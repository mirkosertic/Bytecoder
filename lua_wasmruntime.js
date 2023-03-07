const bytecoder = {

    instance: null,

    module: null,

    filehandles: [],

    toJSString: function(obj) {
        return bytecoder.instance.exports.java$lang$Object$getNativeObject(obj);
    },

    toBytecoderString: function(str) {
        return bytecoder.instance.exports.newBytecoderString(str);
    },

    setNativeObject: function(obj, value) {
        bytecoder.instance.exports.java$lang$Object$setNativeObject(obj, value);
    },

    getNativeObject: function(obj) {
        return bytecoder.instance.exports.java$lang$Object$getNativeObject(obj);
    },

    imports: {
        "java.lang.System": {
            J$currentTimeMillis$$: function () {
                return BigInt(Date.now());
            },
        },
        "java.lang.Object": {
            Ljava$lang$Class$$getClass$$: function (inst) {
                return inst.constructor.$rt;
            },
        },
        "java.lang.NullPointerException": {
            Ljava$lang$String$$getExtendedNPEMessage$$: function(inst) {
                return null;
            }
        },
        "jdk.internal.misc.ScopedMemoryAccess": {
            V$registerNatives$$: function () {
            },
        },
        "java.lang.Float": {
            I$floatToRawIntBits$F: function (unused, value) {
                let fl = new Float32Array(1);
                fl[0] = value;
                let br = new Int32Array(fl.buffer);
                return br[0];
            },
            Z$isNaN$D: function (unused, a) {
                return isNaN(a) ? 1 : 0
            },
            Z$isNaN$F: function (unused, a) {
                return isNaN(a) ? 1 : 0
            },
            Z$isInfinite$F: function (unused, a) {
                return (a === Number.POSITIVE_INFINITY || a === Number.NEGATIVE_INFINITY) ? 1 : 0
            },
            Ljava$lang$String$$toString$F: function (unused, value) {
                let str = value.toString();
                if (str.indexOf(".") < 0) {
                    str += '.0';
                }
                return bytecoder.toBytecoderString(str);
            },
            F$parseFloat$Ljava$lang$String$: function (unused, value) {
                return parseFloat(bytecoder.toJSString(value));
            },
        },
        "java.lang.Math": {
            I$min$I$I: function (unused, a, b) {
                return Math.min(a, b);
            },
            J$min$J$J: function (unused, a, b) {
                return Math.min(a, b);
            },
            F$min$F$F: function (unused, a, b) {
                return Math.min(a, b);
            },
            D$min$D$D: function (unused, a, b) {
                return Math.min(a, b);
            },
            I$max$I$I: function (unused, a, b) {
                return Math.max(a, b);
            },
            J$max$J$J: function (unused, a, b) {
                return Math.max(a, b);
            },
            D$max$D$D: function (unused, a, b) {
                return Math.max(a, b);
            },
            F$max$F$F: function (unused, a, b) {
                return Math.max(a, b);
            },
            D$floor$D: function (unused, a) {
                return Math.floor(a);
            },
            F$floor$F: function (unused, a) {
                return Math.floor(a);
            },
            D$ceil$D: function (unused, a) {
                return Math.ceil(a);
            },
            F$ceil$F: function (unused, a) {
                return Math.ceil(a);
            },
            D$toRadians$D: function (unused, a) {
                return a * (Math.PI / 180.0);
            },
            D$toDegrees$D: function (unused, a) {
                return a * (180. / Math.PI);
            },
            D$cos$D: function (unused, a) {
                return Math.cos(a);
            },
            D$sin$D: function (unused, a) {
                return Math.sin(a);
            },
            D$tan$D: function (unused, a) {
                return Math.tan(a);
            },
            D$sqrt$D: function (unused, a) {
                return Math.sqrt(a);
            },
            D$cbrt$D: function (unused, a) {
                return Math.cbrt(a);
            },
            D$log$D: function (unused, a) {
                return Math.log(a);
            },
            D$random$$: function (unused) {
                return Math.random(unused);
            },
        },
        "java.lang.StrictMath": {
            D$sqrt$D: function (unused, a) {
                return Math.sqrt(a);
            },
            I$round$F: function(unused, a) {
                return Math.round(a);
            },
            D$sin$D: function(unused, a) {
                return Math.sin(a);
            },
            D$cos$D: function(unused, a) {
                return Math.cos(a);
            }
        },
        "java.lang.reflect.Array": {
            Ljava$lang$Object$$newArray$Ljava$lang$Class$$I: function (unused, t, l) {
                return bytecoder.newarray(l, null);
            },
        },
        "jdk.internal.misc.CDS": {
            Z$isDumpingClassList0$$: function (unused) {
                return 0;
            },
            Z$isDumpingArchive0$$: function (unused) {
                return 0;
            },
            Z$isSharingEnabled0$$: function (unused) {
                return 0;
            },
            V$initializeFromArchive$Ljava$lang$Class$: function (cls) {
            },
            J$getRandomSeedForDumping$$: function(unused) {
                return Math.trunc(Math.random() * 10000000);
            },
        },
        "java.io.UnixFileSystem": {
            I$getBooleanAttributes0$Ljava$lang$String$: function (fsref, path) {
                let jsPath = bytecoder.toJSString(path);
                try {
                    let request = new XMLHttpRequest();
                    request.open('HEAD', jsPath, false);
                    request.send(null);
                    if (request.status === 200) {
                        let length = request.getResponseHeader('content-length');
                        return 0x01;
                    }
                    return 0;
                } catch (e) {
                    return 0;
                }
            },
        },
        "java.lang.Class": {
            Ljava$lang$ClassLoader$$getClassLoader$$: function (classRef) {
                return null;
            },
            Ljava$lang$Class$$forName$Ljava$lang$String$$Z$Ljava$lang$ClassLoader$: function(className, initialize, classLoader) {
                throw 'Not supported class for reflective access';
            },
        },
        "java.io.FileInputStream": {
            I$open0$Ljava$io$FileDescriptor$$Ljava$lang$String$: function (fis, fdd, name) {
                let fd = bytecoder.openForRead(bytecoder.toJSString(name));
                if (fd >= 0) {
                    bytecoder.instance.exports.setFileDescriptorHandle(fdd, fd);
                }
                return fd;
            },
            J$skip0$Ljava$io$FileDescriptor$$I: function (fis, fdd, amount) {
                let fd = bytecoder.instance.exports.getFileDescriptorHandle(fdd);
                let x = bytecoder.filehandles[fd];
                return BigInt(x.J$skip0$I(fd, amount));
            },
            I$available0$Ljava$io$FileDescriptor$: function (fis, fdd) {
                let fd = bytecoder.instance.exports.getFileDescriptorHandle(fdd);
                let x = bytecoder.filehandles[fd];
                return x.I$available0$$(fd);
            },
            I$read0$Ljava$io$FileDescriptor$: function (fis, fdd) {
                let fd = bytecoder.instance.exports.getFileDescriptorHandle(fdd);
                let x = bytecoder.filehandles[fd];
                return x.I$read0$$(fd);
            },
            I$readBytes$Ljava$io$FileDescriptor$$$B$I$I: function (fis, fdd, b, off, len) {
                let fd = bytecoder.instance.exports.getFileDescriptorHandle(fdd);
                let x = bytecoder.filehandles[fd];
                return x.I$readBytes$$B$I$I(fd, b, off, len);
            },
        },
        "java.io.FileOutputStream": {
            V$writeBytes$Ljava$io$FileDescriptor$$$B$I$I: function (fis, fdd, b, off, len) {
                let fd = bytecoder.instance.exports.getFileDescriptorHandle(fdd);
                let x = bytecoder.filehandles[fd];
                x.V$writeBytes$$B$I$I(fd, b, off, len);
            },
            V$writeInt$Ljava$io$FileDescriptor$$I: function (fis, fdd, cp) {
                let fd = bytecoder.instance.exports.getFileDescriptorHandle(fdd);
                let x = bytecoder.filehandles[fd];
                x.V$writeInt$I(fd, cp);
            },
            V$close0$Ljava$io$FileDescriptor$: function(fis, fdd) {
                bytecoder.filehandles[fd] = null;
            }
        },
        "java.lang.invoke.LambdaMetafactory": {
            Ljava$lang$invoke$CallSite$$metafactory$Ljava$lang$invoke$MethodHandles$Lookup$$Ljava$lang$String$$Ljava$lang$invoke$MethodType$$Ljava$lang$invoke$MethodType$$Ljava$lang$invoke$MethodHandle$$Ljava$lang$invoke$MethodType$: function (unused, lookups, methodName, invokedType, samMethodType, implMethod, aInstantiatedMethodType) {
            }
        },
        "de.mirkosertic.bytecoder.classlib.BytecoderCharsetDecoder": {
            $C$decodeFromBytes$Ljava$lang$String$$$B: function (decoder, charsetName, data) {
                let targetCharacterSet = bytecoder.toJSString(charsetName);

                let length = bytecoder.instance.exports.byteArrayLength(null, data);
                let byteData = new Uint8Array(length);
                for (let i = 0; i < length; i++) {
                    byteData[i] = bytecoder.instance.exports.getByteArrayEntry(null, data, i);
                }

                let dec = new TextDecoder(targetCharacterSet);

                let str = dec.decode(byteData);

                let charArray = bytecoder.instance.exports.newCharArray(null, str.length);
                for (let i = 0; i < str.length; i++) {
                    bytecoder.instance.exports.setCharArrayEntry(null, charArray, i, str.codePointAt(i));
                }
                return charArray;
            },
        },
        "de.mirkosertic.bytecoder.classlib.BytecoderCharsetEncoder": {
            $B$encodeToBytes$Ljava$lang$String$$$C: function (encoder, charsetName, data) {
                var length = bytecoder.instance.exports.charArrayLength(null, data);
                let str = '';
                for (var i = 0; i < length; i++) {
                    str += String.fromCodePoint(bytecoder.instance.exports.getCharArrayEntry(null, data, i));
                }
                let targetCharacterSet = bytecoder.toJSString(charsetName);
                if (targetCharacterSet !== 'UTF-8') {
                    throw 'Not supported character set!';
                }

                let enc = new TextEncoder();
                let byteData = enc.encode(str);

                let bytes = bytecoder.instance.exports.newByteArray(null, byteData.length);
                for (var i = 0; i < byteData.length; i++) {
                    bytecoder.instance.exports.setByteArrayEntry(null, bytes, i, byteData[i]);
                }

                return bytes;
            },
        },
        "java.lang.StringBuffer": {
            V$initializeWith$I: function (buffer, size) {
                bytecoder.setNativeObject(buffer, "");
            },
            Ljava$lang$StringBuffer$$append$Ljava$lang$String$: function (buffer, str) {
                let x = bytecoder.getNativeObject(buffer);
                x+= bytecoder.toJSString(str);
                bytecoder.setNativeObject(buffer, x);
                return buffer;
            },
            Ljava$lang$String$$toString$$: function (buffer) {
                let x = bytecoder.getNativeObject(buffer);
                return bytecoder.toBytecoderString(x);
            },
        },
        "java.lang.StringBuilder": {
            V$initializeWith$I: function (builder, size) {
                bytecoder.setNativeObject(builder, "");
            },
            Ljava$lang$StringBuilder$$append$Ljava$lang$String$: function (builder, str) {
                let x = bytecoder.getNativeObject(builder);
                x+= bytecoder.toJSString(str);
                bytecoder.setNativeObject(builder, x);
                return builder;
            },
            Ljava$lang$StringBuilder$$append$Ljava$lang$CharSequence$$I$I: function (builder, str, start, end) {
                let x = bytecoder.getNativeObject(builder);
                x+= bytecoder.toJSString(str).substring(start, end);
                bytecoder.setNativeObject(builder, x);
                return builder;
            },
            Ljava$lang$String$$toString$$: function (builder) {
                let x = bytecoder.getNativeObject(builder);
                return bytecoder.toBytecoderString(x);
            },
            I$length$$: function (builder) {
                let x = bytecoder.getNativeObject(builder);
                return x.length;
            },
            V$setLength$I: function (builder, size) {
            },
            Ljava$lang$StringBuilder$$append$$C$I$I: function (builder, chars, offset, count) {
                for (let i = offset; i < offset + count; i++) {
                    builder.nativeObject += String.fromCodePoint(chars.data[i]);
                }
                return builder;
            },
        },
        "java.lang.String": {
            C$charAt$I: function (str, index) {
                const a = bytecoder.toJSString(str);
                return a.codePointAt(index);
            },
            I$length$$: function (str) {
                const a = bytecoder.toJSString(str);
                return a.length;
            },
            Ljava$lang$String$$format$Ljava$lang$String$$$Ljava$lang$Object$: function(pattern,values) {
                return pattern;
            },
            V$getChars$I$I$$C$I: function (str, srcBegin, srcEnd, dst, dstBegin) {
                const s = bytecoder.toJSString(str);
                let dstOffset = dstBegin;
                for (let i = srcBegin; i < srcEnd; i++) {
                    bytecoder.instance.exports.setCharArrayEntry(null, dst, dstOffset++, s.codePointAt(i));
                }
            },
            I$indexOf$I: function (str, cp) {
                if (cp >= 0) {
                    const a = bytecoder.toJSString(str);
                    return a.indexOf(String.fromCodePoint(cp));
                }
                return -1;
            },
            I$lastIndexOf$I: function (str, cp) {
                if (cp >= 0) {
                    const a = bytecoder.toJSString(str);
                    return a.lastIndexOf(String.fromCodePoint(cp));
                }
                return -1;
            },
            Z$startsWith$Ljava$lang$String$: function (str, otherstr) {
                const a = bytecoder.toJSString(str);
                const b = bytecoder.toJSString(otherstr);
                if (a.startsWith(b)) {
                    return 1;
                }
                return 0;
            },
            I$lastIndexOf$Ljava$lang$String$: function (str, s) {
                const a = bytecoder.toJSString(str);
                return a.lastIndexOf(bytecoder.toJSString(s));
            },
            Ljava$lang$String$$trim$$: function (str) {
                return bytecoder.toBytecoderString(bytecoder.toJSString(str).trim());
            },
            Ljava$lang$String$$substring$I: function(str, i) {
                return bytecoder.toBytecoderString(bytecoder.toJSString(str).substring(i));
            },
            Ljava$lang$String$$substring$I$I: function(str, i, b) {
                return bytecoder.toBytecoderString(bytecoder.toJSString(str).substring(i, b));
            },
            Ljava$lang$String$$repeat$I: function (str, amount) {
                return bytecoder.toBytecoderString(bytecoder.toJSString(str).repeat(amount));
            },
            Z$equals0$Ljava$lang$String$: function (str, otherstr) {
                const a = bytecoder.toJSString(str);
                const b = bytecoder.toJSString(otherstr);
                if (a === b) {
                    return 1;
                }
                return 0;
            },
            Z$equalsIgnoreCase$Ljava$lang$String$: function (str, otherstr) {
                if (str == null) {
                    return 0;
                }
                if (otherstr == null) {
                    return 0;
                }
                const a = bytecoder.toJSString(str);
                const b = bytecoder.toJSString(otherstr);

                if (a.toUpperCase() === b.toUpperCase()) {
                    return 1;
                }
                return 0;
            },
            V$initializeWith$$C$I$I: function (str, chars, offset, count) {
                let x = '';
                for (let i = offset; i < offset + count; i++) {
                    x += String.fromCodePoint(bytecoder.instance.exports.getByteArrayEntry(null, chars, i));
                }
                bytecoder.setNativeObject(str, x);
            },
            V$initializeWith$$B$I$I$B: function (str, bytes, offset, count) {
                // TODO
            },
            $C$toCharArray$$: function (str) {
                let no = bytecoder.getNativeObject(str);
                let arr = bytecoder.instance.exports.newCharArray(null, no.length);
                for (let i = 0; i < no.length; i++) {
                    bytecoder.instance.exports.setByteArrayEntry(null, arr, i, no.codePointAt(i));
                }
                return arr;
            },
            V$initializeWith$Ljava$lang$String$: function(str, otherstr) {
                bytecoder.setNativeObject(str, bytecoder.getNativeObject(otherstr));
            }
        },
        "java.lang.Character": {
            Z$isDigit$C: function (unused, char) {
                if ("0123456789".indexOf(String.fromCodePoint(char)) >= 0) {
                    return 1;
                }
                return 0;
            },
            Z$isLowerCase$C: function (unused, char) {
                let str = String.fromCodePoint(char);
                if (str.toLowerCase() === str) {
                    return 1;
                }
                return 0;
            },
            Z$isUpperCase$C: function (unused, char) {
                let str = String.fromCodePoint(char);
                if (str.toUpperCase() === str) {
                    return 1;
                }
                return 0;
            },
            C$toLowerCase$C: function (unused, char) {
                let str = String.fromCodePoint(char).toLowerCase();
                return str.codePointAt(0);
            },
            C$toUpperCase$C: function (unused, char) {
                let str = String.fromCodePoint(char).toUpperCase();
                return str.codePointAt(0);
            },
            I$digit$C$I: function (unused, char, radix) {
                let str = String.fromCodePoint(char).toUpperCase();
                if ('0' === str) {
                    return 0;
                }
                if ('1' === str) {
                    return 1;
                }
                if ('2' === str) {
                    return 2;
                }
                if ('3' === str) {
                    return 3;
                }
                if ('4' === str) {
                    return 4;
                }
                if ('5' === str) {
                    return 5;
                }
                if ('6' === str) {
                    return 6;
                }
                if ('7' === str) {
                    return 7;
                }
                if ('8' === str) {
                    return 8;
                }
                if ('9' === str) {
                    return 9;
                }
                if ('A' === str) {
                    return 10;
                }
                if ('B' === str) {
                    return 11;
                }
                if ('C' === str) {
                    return 12;
                }
                if ('D' === str) {
                    return 13;
                }
                if ('E' === str) {
                    return 14;
                }
                if ('15' === str) {
                    return 15;
                }
                return -1;
            },
            Ljava$lang$String$$toString$C: function (unused, char) {
                return bytecoder.toBytecoderString(String.fromCodePoint(char));
            },
        },
        "java.lang.Byte": {
            Ljava$lang$String$$toString$B$I: function (unused, value, radix) {
                return bytecoder.toBytecoderString(value.toString(radix));
            },
            B$parseByte$Ljava$lang$String$: function (unused, str) {
                return parseInt(bytecoder.toJSString(str));
            },
        },
        "java.lang.Short": {
            Ljava$lang$String$$toString$S$I: function (unused, value, radix) {
                return bytecoder.toBytecoderString(value.toString(radix));
            },
            S$parseShort$Ljava$lang$String$$I: function (unused, value, radix) {
                const str = bytecoder.toJSString(value);
                return parseInt(str, radix);
            },
        },
        "java.lang.Integer": {
            Ljava$lang$String$$toString$I$I: function (unused, value, radix) {
                return bytecoder.toBytecoderString(value.toString(radix));
            },
            Ljava$lang$String$$toHexString$I: function (unused, value) {
                return bytecoder.toBytecoderString(value.toString(16));
            },
            I$parseInt$Ljava$lang$String$$I: function (unused, value, radix) {
                return parseInt(bytecoder.toJSString(value), radix);
            },
        },
        "java.lang.Long": {
            Ljava$lang$String$$toString$J$I: function (unused, value, radix) {
                return bytecoder.toBytecoderString(value.toString(radix));
            },
            J$parseLong$Ljava$lang$String$$I: function (unused, value, radix) {
                return BigInt(parseInt(bytecoder.toJSString(value), radix));
            },
        },
        "java.lang.Double": {
            Ljava$lang$String$$toString$D: function (unused, value) {
                let str = value.toString();
                if (str.indexOf(".") < 0) {
                    str += '.0';
                }
                return bytecoder.toBytecoderString(str);
            },
            D$parseDouble$Ljava$lang$String$: function (unused, str) {
                return parseFloat(bytecoder.toJSString(str));
            },
            Z$isNaN$D: function (unused, d) {
                return isNaN(d) ? 1 : 0;
            },
            Z$isInfinite$D: function (unused, d) {
                return isFinite(d) ? 1 : 0;
            }
        },
        "runtime": {
            nativeconsole: function() {
                return console;
            },
            nativewindow: function() {
                return window;
            },
            nativedocument: function() {
                return document;
            }
        },
        "de.mirkosertic.bytecoder.api.web.OpaqueArrays": {
            Lde$mirkosertic$bytecoder$api$web$FloatArray$$createFloatArray$I: function(length) {
                return new Float32Array(length);
            },
            'Lde$mirkosertic$bytecoder$api$web$IntArray$$createIntArray$I': function(length) {
                return new Int32Array(length);
            },
            'Lde$mirkosertic$bytecoder$api$web$Int16Array$$createInt16Array$I': function(length) {
                return new Int16Array(length);
            },
            'Lde$mirkosertic$bytecoder$api$web$Int8Array$$createInt8Array$I': function(length) {
                return new Int8Array(length);
            },
            'Lde$mirkosertic$bytecoder$api$web$OpaqueReferenceArray$$createObjectArray$$': function() {
                return [];
            }
        },
        "bytecoder": {
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
                            bytecoder.instance.exports.setByteArrayEntry(null, target, offset++, this.data[this.currentpos++]);
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

bytecoder.filehandles[1] = {
    V$writeBytes$$B$I$I: function(fd, b, off, len) {
        let decoder = new TextDecoder();
        let d = [];
        for (let i = off; i < off + len; i++) {
            d.push(bytecoder.instance.exports.getByteArrayEntry(null, b, i));
        }
        let arr = new Uint8Array(d);
        let str = decoder.decode(arr).replace('\n', '').replace('\f', '');
        if (str.length > 0) {
            console.log(str);
        }
    },
    V$writeInt$I: function(fd, cp) {
        let decoder = new TextDecoder();
        let arr = new Uint8Array([cp]);
        let str = decoder.decode(arr).replace('\n', '').replace('\f', '');
        if (str.length > 0) {
            console.log(str);
        }
    },
};


bytecoder.imports["bytecoder"].resolveStringConstant = function(index) {
  switch(index) {
      case 0: return 'nil';
      case 1: return 'boolean';
      case 2: return 'lightuserdata';
      case 3: return 'number';
      case 4: return 'string';
      case 5: return 'table';
      case 6: return 'function';
      case 7: return 'userdata';
      case 8: return 'thread';
      case 9: return 'value';
      case 10: return '_ENV';
      case 11: return '__index';
      case 12: return '__newindex';
      case 13: return '__call';
      case 14: return '__mode';
      case 15: return '__metatable';
      case 16: return '__add';
      case 17: return '__sub';
      case 18: return '__div';
      case 19: return '__mul';
      case 20: return '__pow';
      case 21: return '__mod';
      case 22: return '__unm';
      case 23: return '__len';
      case 24: return '__eq';
      case 25: return '__lt';
      case 26: return '__le';
      case 27: return '__tostring';
      case 28: return '__concat';
      case 29: return '';
      case 30: return 'n';
      case 31: return 'void';
      case 32: return 'byte';
      case 33: return 'char';
      case 34: return 'short';
      case 35: return 'int';
      case 36: return 'float';
      case 37: return 'double';
      case 38: return 'long';
      case 39: return 'Illegal initial capacity: ';
      case 40: return 'Illegal load factor: ';
      case 41: return 'java.awt.graphicsenv';
      case 42: return 'de.mirkosertic.bytecoder.classlib.BytecoderGraphicsEnvironment';
      case 43: return 'Null output stream';
      case 44: return 'UTF-8';
      case 45: return 'RESET';
      case 46: return 'CODING';
      case 47: return 'CODING_END';
      case 48: return 'FLUSHED';
      case 49: return 'IGNORE';
      case 50: return 'REPLACE';
      case 51: return 'REPORT';
      case 52: return 'Null action';
      case 53: return 'capacity expected to be negative';
      case 54: return 'null';
      case 55: return 'capacity < 0: (';
      case 56: return ' < 0)';
      case 57: return 'newLimit > capacity: (';
      case 58: return ' > ';
      case 59: return ')';
      case 60: return 'newLimit expected to be negative';
      case 61: return 'newLimit < 0: (';
      case 62: return 'newPosition > limit: (';
      case 63: return 'newPosition expected to be negative';
      case 64: return 'newPosition < 0: (';
      case 65: return 'mark > position: (';
      case 66: return 'BIG_ENDIAN';
      case 67: return 'LITTLE_ENDIAN';
      case 68: return 'Buffer size <= 0';
      case 69: return 'suspended';
      case 70: return 'running';
      case 71: return 'normal';
      case 72: return 'dead';
      case 73: return 'luainput';
      case 74: return 'luaoutput';
      case 75: return 'runlua';
      case 76: return 'click';
      case 77: return 'Non-positive averageBytesPerChar';
      case 78: return 'Non-positive maxBytesPerChar';
      case 79: return 'averageBytesPerChar exceeds maxBytesPerChar';
      case 80: return 'Null replacement';
      case 81: return 'Empty replacement';
      case 82: return 'Replacement too long';
      case 83: return 'UNDERFLOW';
      case 84: return 'OVERFLOW';
      case 85: return 'MALFORMED';
      case 86: return 'UNMAPPABLE';
      case 87: return 'Current state = ';
      case 88: return ', new state = ';
      case 89: return 'Non-positive length';
      case 90: return 'apply';
      case 91: return 'checkFromIndexSize';
      case 92: return '-XX:DumpLoadedClassList=';
      case 93: return '-XX:+DumpSharedSpaces';
      case 94: return '-XX:+DynamicDumpSharedSpaces';
      case 95: return '-XX:+RecordDynamicDumpInfo';
      case 96: return '-Xshare:';
      case 97: return '-XX:SharedClassListFile=';
      case 98: return '-XX:SharedArchiveFile=';
      case 99: return '-XX:ArchiveClassesAtExit=';
      case 100: return '-XX:+UseSharedSpaces';
      case 101: return '-XX:+RequireSharedSpaces';
      case 102: return 'duplicate element: ';
      case 103: return 'length is odd';
      case 104: return 'duplicate key: ';
      case 105: return 'Range check failed';
      case 106: return 'Range check failed: %s';
      case 107: return 'Range [%s, %<s + %s) out of bounds for length %s';
      case 108: return 'Range [%s, %s) out of bounds for length %s';
      case 109: return 'Index %s out of bounds for length %s';
      case 110: return 'Range check failed: %s %s';
      case 111: return 'checkFromToIndex';
      case 112: return 'checkIndex';
      case 113: return '[';
      case 114: return ']';
      case 115: return 'Illegal replacement';
      case 116: return '\uFFFD';
      case 117: return 'Non-positive averageCharsPerByte';
      case 118: return 'Non-positive maxCharsPerByte';
      case 119: return 'averageCharsPerByte exceeds maxCharsPerByte';
      case 120: return 't';
      case 121: return 'load ';
      case 122: return ': ';
      case 123: return 'No undumper.';
      case 124: return 'mark\/reset not supported';
      case 125: return 'No compiler.';
      case 126: return 'Failed to load prototype ';
      case 127: return ' using mode \'';
      case 128: return '\'';
      case 129: return 'attempt to call ';
      case 130: return 'rawget';
      case 131: return '\' not implemented for ';
      case 132: return 'strValue';
      case 133: return ' expected, got ';
      case 134: return 'Index: ';
      case 135: return ' Size: ';
      case 136: return 'vm error: ';
      case 137: return 'data type scale not a power of two';
      case 138: return 'Attempt to access an already released memory resource';
      case 139: return 'This segment is already closed';
      case 140: return '\/';
      case 141: return ' ';
      case 142: return 'No java.util.Objects instances for you!';
      case 143: return '(';
      case 144: return ',';
      case 145: return ':';
      case 146: return '-';
      case 147: return '[pos=';
      case 148: return ' lim=';
      case 149: return ' cap=';
      case 150: return '=';
      case 151: return 'none';
      case 152: return 'true';
      case 153: return 'false';
      case 154: return 'function: ';
      case 155: return 'Lua';
      case 156: return '?';
      case 157: return 'call';
      case 158: return 'line';
      case 159: return 'count';
      case 160: return 'return';
      case 161: return 'func';
      case 162: return 'istailcall';
      case 163: return 'isvararg';
      case 164: return 'nups';
      case 165: return 'nparams';
      case 166: return 'name';
      case 167: return 'namewhat';
      case 168: return 'what';
      case 169: return 'source';
      case 170: return 'short_src';
      case 171: return 'linedefined';
      case 172: return 'lastlinedefined';
      case 173: return 'currentline';
      case 174: return 'activelines';
      case 175: return 'TRACE';
      case 176: return 'CALLS';
      case 177: return '\n';
      case 178: return 'stack traceback:';
      case 179: return '\n\t';
      case 180: return 'closure';
      case 181: return 'bad argument: ';
      case 182: return '@';
      case 183: return '\u001B';
      case 184: return 'binary string';
      case 185: return '[Java]';
      case 186: return ' in ';
      case 187: return 'Java';
      case 188: return '(for iterator)';
      case 189: return '(for iterator';
      case 190: return 'local';
      case 191: return 'method';
      case 192: return 'global';
      case 193: return 'field';
      case 194: return 'upvalue';
      case 195: return 'constant';
      case 196: return 'metamethod';
      case 197: return 'main';
      case 198: return '=?';
      case 199: return '=[Java]';
      case 200: return 'main chunk';
      case 201: return 'function \'';
      case 202: return 'function <';
      case 203: return '>';
      case 204: return '\n\t[Java]: in ?';
      case 205: return 'error in error handling';
      case 206: return 'MOVE';
      case 207: return 'LOADK';
      case 208: return 'LOADKX';
      case 209: return 'LOADBOOL';
      case 210: return 'LOADNIL';
      case 211: return 'GETUPVAL';
      case 212: return 'GETTABUP';
      case 213: return 'GETTABLE';
      case 214: return 'SETTABUP';
      case 215: return 'SETUPVAL';
      case 216: return 'SETTABLE';
      case 217: return 'NEWTABLE';
      case 218: return 'SELF';
      case 219: return 'ADD';
      case 220: return 'SUB';
      case 221: return 'MUL';
      case 222: return 'DIV';
      case 223: return 'MOD';
      case 224: return 'POW';
      case 225: return 'UNM';
      case 226: return 'NOT';
      case 227: return 'LEN';
      case 228: return 'CONCAT';
      case 229: return 'JMP';
      case 230: return 'EQ';
      case 231: return 'LT';
      case 232: return 'LE';
      case 233: return 'TEST';
      case 234: return 'TESTSET';
      case 235: return 'CALL';
      case 236: return 'TAILCALL';
      case 237: return 'RETURN';
      case 238: return 'FORLOOP';
      case 239: return 'FORPREP';
      case 240: return 'TFORCALL';
      case 241: return 'TFORLOOP';
      case 242: return 'SETLIST';
      case 243: return 'CLOSURE';
      case 244: return 'VARARG';
      case 245: return 'EXTRAARG';
      case 246: return 'Negative initial size: ';
      case 247: return '  ';
      case 248: return 'system';
      case 249: return 'Stream closed';
      case 250: return ']  ';
      case 251: return '  ; is_vararg=';
      case 252: return '  ; ';
      case 253: return '  ; to ';
      case 254: return '\\\\';
      case 255: return '\\\"';
      case 256: return '\\r';
      case 257: return '\\f';
      case 258: return '\\v';
      case 259: return '\\n';
      case 260: return '\\t';
      case 261: return '\\b';
      case 262: return '\\a';
      case 263: return '[-]  ';
      case 264: return ' | ';
      case 265: return '...+';
      case 266: return 'b';
      case 267: return 'Uexecutable opcode: OP_EXTRAARG';
      case 268: return 'No space for upvalue';
      case 269: return 'rawset';
      case 270: return 'loop in settable';
      case 271: return 'index';
      case 272: return '\'for\' initial value must be a number';
      case 273: return '\'for\' limit must be a number';
      case 274: return '\'for\' step must be a number';
      case 275: return 'attempt to perform arithmetic ';
      case 276: return ' on ';
      case 277: return ' and ';
      case 278: return 'attempt to compare ';
      case 279: return ' with ';
      case 280: return 'attempt to concatenate ';
      case 281: return 'attempt to get length of ';
      case 282: return 'attempt to perform arithmetic on ';
      case 283: return 'loop in gettable';
      case 284: return 'attempt to index ? (a ';
      case 285: return ' value)';
      case 286: return 'Illegal opcode: ';
      case 287: return ' on number and ';
      case 288: return 'table index';
      case 289: return 'Illegal Capacity: ';
      case 290: return 'Illegal Load: ';
      case 291: return '(for control)';
      case 292: return '(for generator)';
      case 293: return '(for index)';
      case 294: return '(for limit)';
      case 295: return '(for state)';
      case 296: return '(for step)';
      case 297: return 'and';
      case 298: return 'break';
      case 299: return 'do';
      case 300: return 'else';
      case 301: return 'elseif';
      case 302: return 'end';
      case 303: return 'for';
      case 304: return 'goto';
      case 305: return 'if';
      case 306: return 'in';
      case 307: return 'not';
      case 308: return 'or';
      case 309: return 'repeat';
      case 310: return 'then';
      case 311: return 'until';
      case 312: return 'while';
      case 313: return '..';
      case 314: return '...';
      case 315: return '==';
      case 316: return '>=';
      case 317: return '<=';
      case 318: return '~=';
      case 319: return '::';
      case 320: return '<eos>';
      case 321: return '<number>';
      case 322: return '<name>';
      case 323: return '<string>';
      case 324: return '<eof>';
      case 325: return 'compiler assert failed';
      case 326: return 'upvalues';
      case 327: return 'main function has more than ';
      case 328: return '[string \"';
      case 329: return '\"]';
      case 330: return 'syntax error: ';
      case 331: return ' near ';
      case 332: return 'char(';
      case 333: return 'function at line ';
      case 334: return ' has more than ';
      case 335: return 'Ee';
      case 336: return 'Xx';
      case 337: return 'Pp';
      case 338: return '+-';
      case 339: return 'chunk has too many lines';
      case 340: return 'nesting of [[...]] is deprecated';
      case 341: return 'unfinished long string';
      case 342: return 'unfinished long comment';
      case 343: return 'invalid long string delimiter';
      case 344: return '.';
      case 345: return 'escape sequence too large';
      case 346: return 'hexadecimal digit expected \'x';
      case 347: return 'unfinished string';
      case 348: return 'chunk has too many syntax levels';
      case 349: return 'unexpected symbol ';
      case 350: return ' (';
      case 351: return ' expected';
      case 352: return 'control structure too long';
      case 353: return 'function or expression too complex';
      case 354: return 'cannot use ';
      case 355: return ' outside a vararg function';
      case 356: return 'self';
      case 357: return 'local variables';
      case 358: return '<name> or ';
      case 359: return ' expected ';
      case 360: return '(to close ';
      case 361: return ' at line ';
      case 362: return '<goto ';
      case 363: return '> at line ';
      case 364: return ' jumps into the scope of local \'';
      case 365: return '<';
      case 366: return ' not inside a loop';
      case 367: return 'no visible label \'';
      case 368: return '\' for <goto> at line ';
      case 369: return 'items in a constructor';
      case 370: return 'function arguments expected';
      case 371: return 'syntax error';
      case 372: return 'label \'';
      case 373: return ' already defined on line ';
      case 374: return ' or ';
      case 375: return 'Required array length ';
      case 376: return ' + ';
      case 377: return ' is too large';
      case 378: return ' instack ';
      case 379: return ' closed ';
      case 380: return 'UNKNOWN';
      case 381: return 'start must be > 0';
      case 382: return 'bad argument #';
      case 383: return 'nan';
      case 384: return '-inf';
      case 385: return 'inf';
      case 386: return 'attempt to compare string with number';
      case 387: return 'abs';
      case 388: return 'ceil';
      case 389: return 'cos';
      case 390: return 'deg';
      case 391: return 'exp';
      case 392: return 'floor';
      case 393: return 'fmod';
      case 394: return 'frexp';
      case 395: return 'huge';
      case 396: return 'ldexp';
      case 397: return 'max';
      case 398: return 'min';
      case 399: return 'modf';
      case 400: return 'pi';
      case 401: return 'pow';
      case 402: return 'random';
      case 403: return 'randomseed';
      case 404: return 'rad';
      case 405: return 'sin';
      case 406: return 'sqrt';
      case 407: return 'tan';
      case 408: return 'math';
      case 409: return 'package';
      case 410: return 'loaded';
      case 411: return 'attempt to compare number with string';
      case 412: return 'globals';
      case 413: return 'debug';
      case 414: return 'gethook';
      case 415: return 'getinfo';
      case 416: return 'getlocal';
      case 417: return 'getmetatable';
      case 418: return 'getregistry';
      case 419: return 'getupvalue';
      case 420: return 'getuservalue';
      case 421: return 'sethook';
      case 422: return 'setlocal';
      case 423: return 'setmetatable';
      case 424: return 'setupvalue';
      case 425: return 'setuservalue';
      case 426: return 'traceback';
      case 427: return 'upvalueid';
      case 428: return 'upvaluejoin';
      case 429: return 'table or string';
      case 430: return 'Input length = ';
      case 431: return '; ';
      case 432: return '<dead';
      case 433: return 'value expected';
      case 434: return 'index out of range';
      case 435: return 'String';
      case 436: return 'flnStu';
      case 437: return 'function or level';
      case 438: return 'interval is empty';
      case 439: return 'c';
      case 440: return 'l';
      case 441: return 'r';
      case 442: return 'weak<';
      case 443: return 'typename';
      case 444: return 'weak value';
      case 445: return 'illegal operation \'';
      case 446: return '\' for ';
      case 447: return 'type';
      case 448: return 'cannot set ';
      case 449: return ' for userdata';
  }
  throw 'Unknown string index ' + index;
};
bytecoder.imports["de.mirkosertic.bytecoder.api.web.ParentNode_generated"] = {
    Lde$mirkosertic$bytecoder$api$web$Element$$getElementById$Ljava$lang$String$ : function(thisref, arg0) {
        return (thisref.getElementById(arg0));
    },
};
bytecoder.imports["de.mirkosertic.bytecoder.api.web.Window_generated"] = {
    Lde$mirkosertic$bytecoder$api$web$HTMLDocument$$document$$ : function(thisref) {
        return (thisref.document);
    },
};
bytecoder.imports["de.mirkosertic.bytecoder.api.web.Element_generated"] = {
    V$innerHTML$Ljava$lang$String$ : function(thisref, arg0) {
        (thisref.innerHTML = arg0);
    },
};
bytecoder.imports["de.mirkosertic.bytecoder.api.web.EventTarget_generated"] = {
    V$addEventListener$Ljava$lang$String$$Lde$mirkosertic$bytecoder$api$web$EventListener$ : function(thisref, arg0, arg1) {
        (thisref.addEventListener(arg0, function(evt) {bytecoder.instance.exports['de.mirkosertic.bytecoder.api.web.EventListener_callback'](arg1,evt);}));
    },
};
bytecoder.imports["de.mirkosertic.bytecoder.api.web.HTMLTextAreaElement_generated"] = {
    Ljava$lang$String$$value$$ : function(thisref) {
        return (thisref.value);
    },
};
