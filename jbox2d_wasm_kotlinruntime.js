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
                return BigInt(Math.trunc(Math.random() * 10000000));
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
            Ljava$lang$StringBuilder$$deleteCharAt$I: function(builder, index) {
                const current = bytecoder.getNativeObject(builder);
                bytecoder.setNativeObject(builder, current.slice(0, index) + current.slice(index + 1));
                return builder;
            },
            V$setCharAt$I$C: function(builder, index, char) {
                const current = bytecoder.getNativeObject(builder);
                bytecoder.setNativeObject(builder, current.slice(0, index) + String.fromCodePoint(char) + current.slice(index + 1));
            },
            Ljava$lang$StringBuilder$$insert$I$C: function(builder, index, char) {
                const current = bytecoder.getNativeObject(builder);
                bytecoder.setNativeObject(builder, current.slice(0, index) + String.fromCodePoint(char) + current.slice(index));
                return builder
            },
            C$charAt$I: function(builder, index) {
                const current = bytecoder.getNativeObject(builder);
                return current.codePointAt(index);
            }
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
            Z$endsWith$Ljava$lang$String$: function (str, otherstr) {
                const a = bytecoder.toJSString(str);
                const b = bytecoder.toJSString(otherstr);
                if (a.endsWith(b)) {
                    return 1;
                }
                return 0;
            },
            Ljava$lang$String$$replaceAll$Ljava$lang$String$$Ljava$lang$String$: function(str, regex, replacement) {
                return str;
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
            V$initializeWith$$I$I$I: function (str, points, offset, count) {
                let x = '';
                for (let i = offset; i < offset + count; i++) {
                    x += String.fromCodePoint(bytecoder.instance.exports.getIntArrayEntry(null, points, i));
                }
                bytecoder.setNativeObject(str, x);
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
            },
            V$initializeWith$$B$I$I$B: function(str, bytes, index, count, coder) {
                const arr = new Uint8Array(bytecoder.instance.exports.byteArrayLength(null, bytes))
                for (var i = index; i < index + count; i++) {
                    arr[i - index] = bytecoder.instance.exports.getByteArrayEntry(null, bytes, i);
                }
                const jsstr = new TextDecoder().decode(arr);
                bytecoder.setNativeObject(str, jsstr);
            },
            $B$getBytes$$: function(str) {
                const jsstr = bytecoder.getNativeObject(str);
                const bytes = new TextEncoder().encode(jsstr);

                const arr = bytecoder.instance.exports.newByteArray(null, bytes.length);
                for (var i = 0; i < bytes.length; i++) {
                    bytecoder.instance.exports.setByteArrayEntry(null, arr, i, bytes[i]);
                }

                return arr;
            },
            Ljava$lang$String$$toUpperCase$$: function(str) {
                return bytecoder.toBytecoderString(bytecoder.getNativeObject(str).toUpperCase());
            },
            Ljava$lang$String$$toLowerCase$$: function(str) {
                return bytecoder.toBytecoderString(bytecoder.getNativeObject(str).toLowerCase());
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
      case 0: return 'null';
      case 1: return '';
      case 2: return 'void';
      case 3: return 'byte';
      case 4: return 'char';
      case 5: return 'short';
      case 6: return 'int';
      case 7: return 'float';
      case 8: return 'double';
      case 9: return 'long';
      case 10: return 'boolean';
      case 11: return 'Illegal initial capacity: ';
      case 12: return 'Illegal load factor: ';
      case 13: return 'java.awt.graphicsenv';
      case 14: return 'de.mirkosertic.bytecoder.classlib.BytecoderGraphicsEnvironment';
      case 15: return 'Null output stream';
      case 16: return 'charset';
      case 17: return 'jdk.io.useMonitors';
      case 18: return 'true';
      case 19: return 'RESET';
      case 20: return 'CODING';
      case 21: return 'CODING_END';
      case 22: return 'FLUSHED';
      case 23: return 'IGNORE';
      case 24: return 'REPLACE';
      case 25: return 'REPORT';
      case 26: return 'Null action';
      case 27: return 'capacity expected to be negative';
      case 28: return 'capacity < 0: (';
      case 29: return ' < 0)';
      case 30: return 'newLimit > capacity: (';
      case 31: return ' > ';
      case 32: return ')';
      case 33: return 'newLimit expected to be negative';
      case 34: return 'newLimit < 0: (';
      case 35: return 'newPosition > limit: (';
      case 36: return 'newPosition expected to be negative';
      case 37: return 'newPosition < 0: (';
      case 38: return 'mark > position: (';
      case 39: return 'BIG_ENDIAN';
      case 40: return 'LITTLE_ENDIAN';
      case 41: return 'Wrong init level';
      case 42: return 'system';
      case 43: return 'main';
      case 44: return 'Buffer size <= 0';
      case 45: return 'UTF-8';
      case 46: return 'CIRCLE';
      case 47: return 'EDGE';
      case 48: return 'POLYGON';
      case 49: return 'CHAIN';
      case 50: return 'STATIC';
      case 51: return 'KINEMATIC';
      case 52: return 'DYNAMIC';
      case 53: return 'Array not built of correct length';
      case 54: return 'UNKNOWN';
      case 55: return 'REVOLUTE';
      case 56: return 'PRISMATIC';
      case 57: return 'DISTANCE';
      case 58: return 'PULLEY';
      case 59: return 'MOUSE';
      case 60: return 'GEAR';
      case 61: return 'WHEEL';
      case 62: return 'WELD';
      case 63: return 'FRICTION';
      case 64: return 'ROPE';
      case 65: return 'CONSTANT_VOLUME';
      case 66: return 'INACTIVE';
      case 67: return 'AT_LOWER';
      case 68: return 'AT_UPPER';
      case 69: return 'EQUAL';
      case 70: return 'You cannot create a constant volume joint with less than three bodies.';
      case 71: return 'Incorrect joint definition.  Joints have to correspond to the bodies';
      case 72: return 'benchmark-canvas';
      case 73: return '2d';
      case 74: return 'run';
      case 75: return 'CIRCLES';
      case 76: return 'FACE_A';
      case 77: return 'FACE_B';
      case 78: return 'FAILED';
      case 79: return 'OVERLAPPED';
      case 80: return 'TOUCHING';
      case 81: return 'SEPARATED';
      case 82: return 'POINTS';
      case 83: return 'white';
      case 84: return 'black';
      case 85: return 'null cannot be cast to non-null type org.jbox2d.collision.shapes.CircleShape';
      case 86: return 'null cannot be cast to non-null type org.jbox2d.collision.shapes.PolygonShape';
      case 87: return 'button';
      case 88: return 'click';
      case 89: return 'Non-positive averageBytesPerChar';
      case 90: return 'Non-positive maxBytesPerChar';
      case 91: return 'averageBytesPerChar exceeds maxBytesPerChar';
      case 92: return 'Null replacement';
      case 93: return 'Empty replacement';
      case 94: return 'Replacement too long';
      case 95: return 'UNDERFLOW';
      case 96: return 'OVERFLOW';
      case 97: return 'MALFORMED';
      case 98: return 'UNMAPPABLE';
      case 99: return 'Current state = ';
      case 100: return ', new state = ';
      case 101: return 'Non-positive length';
      case 102: return 'apply';
      case 103: return 'checkFromIndexSize';
      case 104: return '-XX:DumpLoadedClassList=';
      case 105: return '-XX:+RecordDynamicDumpInfo';
      case 106: return '-Xshare:';
      case 107: return '-XX:SharedClassListFile=';
      case 108: return '-XX:SharedArchiveFile=';
      case 109: return '-XX:ArchiveClassesAtExit=';
      case 110: return 'duplicate element: ';
      case 111: return 'length is odd';
      case 112: return 'duplicate key: ';
      case 113: return 'Range check failed';
      case 114: return 'Range check failed: %s';
      case 115: return 'Range [%s, %<s + %s) out of bounds for length %s';
      case 116: return 'Range [%s, %s) out of bounds for length %s';
      case 117: return 'Index %s out of bounds for length %s';
      case 118: return 'Range check failed: %s %s';
      case 119: return 'checkFromToIndex';
      case 120: return 'checkIndex';
      case 121: return '[';
      case 122: return ']';
      case 123: return 'Illegal replacement';
      case 124: return '\uFFFD';
      case 125: return 'Non-positive averageCharsPerByte';
      case 126: return 'Non-positive maxCharsPerByte';
      case 127: return 'averageCharsPerByte exceeds maxCharsPerByte';
      case 128: return 'Index: ';
      case 129: return ' Size: ';
      case 130: return 'Vertices of chain shape are too close together';
      case 131: return 'End of stack reached, there is probably a leak somewhere';
      case 132: return 'Beginning of stack reached, push\/pops are unmatched';
      case 133: return 'Array not built with correct length';
      case 134: return 'data type scale not a power of two';
      case 135: return 'No java.util.Objects instances for you!';
      case 136: return '=';
      case 137: return '(';
      case 138: return ',';
      case 139: return '[]';
      case 140: return '(this Collection)';
      case 141: return '[pos=';
      case 142: return ' lim=';
      case 143: return ' cap=';
      case 144: return '{}';
      case 145: return '(this Map)';
      case 146: return 'Sweep:\nlocalCenter: ';
      case 147: return '\n';
      case 148: return 'c0: ';
      case 149: return ', c: ';
      case 150: return 'a0: ';
      case 151: return ', a: ';
      case 152: return 'Rot(s:';
      case 153: return ', c:';
      case 154: return 'AABB[';
      case 155: return ' . ';
      case 156: return 'Unknown Source';
      case 157: return 'Native Method';
      case 158: return ']\n';
      case 159: return 'XForm:\n';
      case 160: return 'Position: ';
      case 161: return 'R: \n';
      case 162: return 'EDGE_A';
      case 163: return 'EDGE_B';
      case 164: return 'VERTEX';
      case 165: return 'FACE';
      case 166: return ', Size: ';
  }
  throw 'Unknown string index ' + index;
};
bytecoder.imports["de.mirkosertic.bytecoder.api.web.EventTarget_generated"] = {
    V$addEventListener$Ljava$lang$String$$Lde$mirkosertic$bytecoder$api$web$EventListener$ : function(thisref, arg0, arg1) {
        (thisref.addEventListener(arg0, function(evt) {bytecoder.instance.exports['de.mirkosertic.bytecoder.api.web.EventListener_callback'](arg1,evt);}));
    },
};
bytecoder.imports["de.mirkosertic.bytecoder.api.web.Window_generated"] = {
    Lde$mirkosertic$bytecoder$api$web$HTMLDocument$$document$$ : function(thisref) {
        return (thisref.document);
    },
    V$requestAnimationFrame$Lde$mirkosertic$bytecoder$api$web$AnimationFrameCallback$ : function(thisref, arg0) {
        (thisref.requestAnimationFrame(function(evt) {bytecoder.instance.exports['de.mirkosertic.bytecoder.api.web.AnimationFrameCallback_callback'](arg0,evt);}));
    },
};
bytecoder.imports["de.mirkosertic.bytecoder.api.web.HTMLCanvasElement_generated"] = {
    Lde$mirkosertic$bytecoder$api$web$CanvasRenderingContext2D$$getContext$Ljava$lang$String$ : function(thisref, arg0) {
        return (thisref.getContext(arg0));
    },
};
bytecoder.imports["de.mirkosertic.bytecoder.api.web.ParentNode_generated"] = {
    Lde$mirkosertic$bytecoder$api$web$Element$$getElementById$Ljava$lang$String$ : function(thisref, arg0) {
        return (thisref.getElementById(arg0));
    },
};
bytecoder.imports["de.mirkosertic.bytecoder.api.web.CanvasRenderingContext2D_generated"] = {
    V$setFillStyle$Ljava$lang$String$ : function(thisref, arg0) {
        (thisref.fillStyle = arg0);
    },
    V$setStrokeStyle$Ljava$lang$String$ : function(thisref, arg0) {
        (thisref.strokeStyle = arg0);
    },
    V$fillRect$F$F$F$F : function(thisref, arg0, arg1, arg2, arg3) {
        (thisref.fillRect(arg0, arg1, arg2, arg3));
    },
    V$save$$ : function(thisref) {
        (thisref.save());
    },
    V$translate$F$F : function(thisref, arg0, arg1) {
        (thisref.translate(arg0, arg1));
    },
    V$scale$F$F : function(thisref, arg0, arg1) {
        (thisref.scale(arg0, arg1));
    },
    V$setLineWidth$F : function(thisref, arg0) {
        (thisref.lineWidth = arg0);
    },
    V$rotate$F : function(thisref, arg0) {
        (thisref.rotate(arg0));
    },
    V$beginPath$$ : function(thisref) {
        (thisref.beginPath());
    },
    V$arc$D$D$D$D$D$Z : function(thisref, arg0, arg1, arg2, arg3, arg4, arg5) {
        (thisref.arc(arg0, arg1, arg2, arg3, arg4, (arg5 === 1 ? true : false)));
    },
    V$closePath$$ : function(thisref) {
        (thisref.closePath());
    },
    V$stroke$$ : function(thisref) {
        (thisref.stroke());
    },
    V$moveTo$F$F : function(thisref, arg0, arg1) {
        (thisref.moveTo(arg0, arg1));
    },
    V$lineTo$F$F : function(thisref, arg0, arg1) {
        (thisref.lineTo(arg0, arg1));
    },
    V$restore$$ : function(thisref) {
        (thisref.restore());
    },
};
bytecoder.imports["de.mirkosertic.bytecoder.api.web.HTMLButton_generated"] = {
    V$disabled$Z : function(thisref, arg0) {
        (thisref.disabled = (arg0 === 1 ? true : false));
    },
};
