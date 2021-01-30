var bytecoder = {

     runningInstance: undefined,
     runningInstanceMemory: undefined,
     exports: undefined,
     referenceTable: ['EMPTY'],
     callbacks: [],
     filehandles: [],

     openForRead: function(path) {
         try {
             var request = new XMLHttpRequest();
             request.open('GET',path,false);
             request.overrideMimeType('text\/plain; charset=x-user-defined');
             request.send(null);
             if (request.status == 200) {
                var length = request.getResponseHeader('content-length');
                var responsetext = request.response;
                var buf = new ArrayBuffer(responsetext.length);
                var bufView = new Uint8Array(buf);
                for (var i=0, strLen=responsetext.length; i<strLen; i++) {
                    bufView[i] = responsetext.charCodeAt(i) & 0xff;
                }
                var handle = bytecoder.filehandles.length;
                bytecoder.filehandles[handle] = {
                    currentpos: 0,
                    data: bufView,
                    size: length,
                    skip0INTINT: function(handle,amount) {
                        var remaining = this.size - this.currentpos;
                        var possible = Math.min(remaining, amount);
                        this.currentpos+=possible;
                        return possible;
                    },
                    available0INT: function(handle) {
                        return this.size - this.currentpos;
                    },
                    read0INT: function(handle) {
                        return this.data[this.currentpos++];
                    },
                    readBytesINTL1BYTEINTINT: function(handle,target,offset,length) {
                        if (length === 0) {
                            return 0;
                        }
                        var remaining = this.size - this.currentpos;
                        var possible = Math.min(remaining, length);
                        if (possible === 0) {
                            return -1;
                        }
                        for (var j=0;j<possible;j++) {
                            bytecoder.runningInstanceMemory[target + 20 + offset * 8]=this.data[this.currentpos++];
                            offset++;
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

     init: function(instance) {
         bytecoder.runningInstance = instance;
         bytecoder.runningInstanceMemory = new Uint8Array(instance.exports.memory.buffer);
         bytecoder.exports = instance.exports;
     },

     initializeFileIO: function() {
         var stddin = {
         };
         var stdout = {
             buffer: "",
             writeBytesINTL1BYTEINTINT: function(handle, data, offset, length) {
                 if (length > 0) {
                     var array = new Uint8Array(length);
                     data+=20;
                     for (var i = 0; i < length; i++) {
                         array[i] = bytecoder.intInMemory(data);
                         data+=8;
                     }
                     var asstring = String.fromCharCode.apply(null, array);
                     for (var i=0;i<asstring.length;i++) {
                         var c = asstring.charAt(i);
                         if (c == '\n') {
                             console.log(stdout.buffer);
                             stdout.buffer="";
                         } else {
                             stdout.buffer = stdout.buffer.concat(c);
                         }
                     }
                 }
             },
             close0INT: function(handle) {
             },
             writeIntINTINT: function(handle,value) {
                 var c = String.fromCharCode(value);
                 if (c == '\n') {
                     console.log(stdout.buffer);
                     stdout.buffer="";
                 } else {
                     stdout.buffer = stdout.buffer.concat(c);
                 }
             }
         };
         bytecoder.filehandles[0] = stddin;
         bytecoder.filehandles[1] = stdout;
         bytecoder.filehandles[2] = stdout;
         bytecoder.exports.initDefaultFileHandles(-1, 0,1,2);
     },

     intInMemory: function(value) {
         return bytecoder.runningInstanceMemory[value]
                + (bytecoder.runningInstanceMemory[value + 1] * 256)
                + (bytecoder.runningInstanceMemory[value + 2] * 256 * 256)
                + (bytecoder.runningInstanceMemory[value + 3] * 256 * 256 * 256);
     },

     toJSString: function(value) {
         var theByteArray = bytecoder.intInMemory(value + 16);
         var theData = bytecoder.byteArraytoJSString(theByteArray);
         return theData;
     },

     byteArraytoJSString: function(value) {
         var theLength = bytecoder.intInMemory(value + 16);
         var theData = '';
         value = value + 20;
         for (var i=0;i<theLength;i++) {
             var theCharCode = bytecoder.intInMemory(value);
             value = value + 8;
             theData+= String.fromCharCode(theCharCode);
         }
         return theData;
     },

     toBytecoderReference: function(value) {
         var index = bytecoder.referenceTable.indexOf(value);
         if (index>=0) {
             return index;
         }
         bytecoder.referenceTable.push(value);
         return bytecoder.referenceTable.length - 1;
     },

     toJSReference: function(value) {
         return bytecoder.referenceTable[value];
     },

     toBytecoderString: function(value) {
         var newArray = bytecoder.exports.newByteArray(0, value.length);
         for (var i=0;i<value.length;i++) {
             bytecoder.exports.setByteArrayEntry(0,newArray,i,value.charCodeAt(i));
         }
         return bytecoder.exports.newStringUTF8(0, newArray);
     },

     registerCallback: function(ptr,callback) {
         bytecoder.callbacks.push(ptr);
         return callback;
     },

     imports: {
         stringutf16: {
             isBigEndian: function() {return 1;},
         },
         env: {
             fmodf: function(f1,f2) {return f1 % f2;},
             fmod: function(f1,f2) {return f1 % f2;},
             debug: function(thisref, f1) {console.log(f1);}
         },
         system: {
             currentTimeMillis: function() {return Date.now();},
             nanoTime: function() {return Date.now() * 1000000;},
         },
         vm: {
             newLambdaStaticInvocationStringMethodTypeMethodHandleObject: function() {},
             newLambdaConstructorInvocationMethodTypeMethodHandleObject: function() {},
             newLambdaInterfaceInvocationMethodTypeMethodHandleObject: function() {},
             newLambdaVirtualInvocationMethodTypeMethodHandleObject: function() {},
             newLambdaSpecialInvocationMethodTypeMethodHandleObject: function() {},
         },
         memorymanager: {
             logINT: function(thisref, value) {
                     console.log('Log : ' + value);
             },
             logAllocationDetailsINTINTINT: function(thisref, current, prev, next) {
                     if (prev != 0) console.log('m_' + current + ' -> m_' + prev + '[label="Prev"]');
                     if (next != 0) console.log('m_' + current + ' -> m_' + next + '[label="Next"]');
             },
             isUsedAsCallbackINT : function(thisref, ptr) {
                 return bytecoder.callbacks.includes(ptr);
             },
             printObjectDebugInternalObjectINTINTBOOLEANBOOLEAN: function(thisref, ptr, indexAlloc, indexFree, usedByStack, usedByHeap) {
                 console.log('Memory debug for ' + ptr);
                 var theAllocatedBlock = ptr - 16;
                 var theSize = bytecoder.intInMemory(theAllocatedBlock);
                 var theNext = bytecoder.intInMemory(theAllocatedBlock +  4);
                 var theSurvivorCount = bytecoder.intInMemory(theAllocatedBlock +  8);
                 console.log(' Allocation starts at '+ theAllocatedBlock);
                 console.log(' Size = ' + theSize + ', Next = ' + theNext);
                 console.log(' GC survivor count        : ' + theSurvivorCount);
                 console.log(' Index in allocation list : ' + indexAlloc);
                 console.log(' Index in free list       : ' + indexFree);
                 console.log(' Used by STACK            : ' + usedByStack);
                 console.log(' Used by HEAP             : ' + usedByHeap);
                 for (var i=0;i<theSize;i+=4) {
                     console.log(' Memory offset +' + i + ' = ' + bytecoder.intInMemory( theAllocatedBlock + i));
                 }
             }
         },
         opaquearrays : {
             createIntArrayINT: function(thisref, p1) {
                 return bytecoder.toBytecoderReference(new Int32Array(p1));
             },
             createFloatArrayINT: function(thisref, p1) {
                 return bytecoder.toBytecoderReference(new Float32Array(p1));
             },
             createObjectArray: function(thisref) {
                 return bytecoder.toBytecoderReference([]);
             },
             createInt8ArrayINT: function(thisref, p1) {
                 return bytecoder.toBytecoderReference(new Int8Array(p1));
             },
             createInt16ArrayINT: function(thisref, p1) {
                 return bytecoder.toBytecoderReference(new Int16Array(p1));
             },
         },
         float : {
             floatToRawIntBitsFLOAT : function(thisref,value) {
                 var fl = new Float32Array(1);
                 fl[0] = value;
                 var br = new Int32Array(fl.buffer);
                 return br[0];
             },
             intBitsToFloatINT : function(thisref,value) {
                 var fl = new Int32Array(1);
                 fl[0] = value;
                 var br = new Float32Array(fl.buffer);
                 return br[0];
             },
         },
         double : {
             doubleToRawLongBitsDOUBLE : function(thisref, value) {
                 var fl = new Float64Array(1);
                 fl[0] = value;
                 var br = new BigInt64Array(fl.buffer);
                 return br[0];
             },
             longBitsToDoubleLONG : function(thisref, value) {
                 var fl = new BigInt64Array(1);
                 fl[0] = value;
                 var br = new Float64Array(fl.buffer);
                 return br[0];
             },
         },
         math: {
             floorDOUBLE: function (thisref, p1) {return Math.floor(p1);},
             ceilDOUBLE: function (thisref, p1) {return Math.ceil(p1);},
             sinDOUBLE: function (thisref, p1) {return Math.sin(p1);},
             cosDOUBLE: function  (thisref, p1) {return Math.cos(p1);},
             tanDOUBLE: function  (thisref, p1) {return Math.tan(p1);},
             roundDOUBLE: function  (thisref, p1) {return Math.round(p1);},
             sqrtDOUBLE: function(thisref, p1) {return Math.sqrt(p1);},
             cbrtDOUBLE: function(thisref, p1) {return Math.cbrt(p1);},
             add: function(thisref, p1, p2) {return p1 + p2;},
             maxLONGLONG: function(thisref, p1, p2) { return Math.max(p1, p2);},
             maxDOUBLEDOUBLE: function(thisref, p1, p2) { return Math.max(p1, p2);},
             maxINTINT: function(thisref, p1, p2) { return Math.max(p1, p2);},
             maxFLOATFLOAT: function(thisref, p1, p2) { return Math.max(p1, p2);},
             minFLOATFLOAT: function(thisref, p1, p2) { return Math.min(p1, p2);},
             minINTINT: function(thisref, p1, p2) { return Math.min(p1, p2);},
             minLONGLONG: function(thisref, p1, p2) { return Math.min(p1, p2);},
             minDOUBLEDOUBLE: function(thisref, p1, p2) { return Math.min(p1, p2);},
             toRadiansDOUBLE: function(thisref, p1) {
                 return p1 * (Math.PI / 180);
             },
             toDegreesDOUBLE: function(thisref, p1) {
                 return p1 * (180 / Math.PI);
             },
             random: function(thisref) { return Math.random();},
             logDOUBLE: function (thisref, p1) {return Math.log(p1);},
             powDOUBLEDOUBLE: function (thisref, p1, p2) {return Math.pow(p1, p2);},
             acosDOUBLE: function (thisref, p1, p2) {return Math.acos(p1);},
             atan2DOUBLE: function (thisref, p1, p2) {return Math.atan2(p1);},
         },
         strictmath: {
             floorDOUBLE: function (thisref, p1) {return Math.floor(p1);},
             ceilDOUBLE: function (thisref, p1) {return Math.ceil(p1);},
             sinDOUBLE: function (thisref, p1) {return Math.sin(p1);},
             cosDOUBLE: function  (thisref, p1) {return Math.cos(p1);},
             roundFLOAT: function  (thisref, p1) {return Math.round(p1);},
             sqrtDOUBLE: function(thisref, p1) {return Math.sqrt(p1);},
             atan2DOUBLEDOUBLE: function(thisref, p1) {return Math.sqrt(p1);},
         },
         runtime: {
             nativewindow: function(caller) {return bytecoder.toBytecoderReference(window);},
             nativeconsole: function(caller) {return bytecoder.toBytecoderReference(console);},
         },
         unixfilesystem :{
             getBooleanAttributes0String : function(thisref,path) {
                 var jsPath = bytecoder.toJSString(path);
                 try {
                     var request = new XMLHttpRequest();
                     request.open('HEAD',jsPath,false);
                     request.send(null);
                     if (request.status == 200) {
                         var length = request.getResponseHeader('content-length');
                         return 0x01;
                     }
                     return 0;
                 } catch(e) {
                     return 0;
                 }
             },
         },
         nullpointerexception : {
             getExtendedNPEMessage : function(thisref) {
                 return 0;
             },
         },
         fileoutputstream : {
             writeBytesINTL1BYTEINTINT : function(thisref, handle, data, offset, length) {
                 bytecoder.filehandles[handle].writeBytesINTL1BYTEINTINT(handle,data,offset,length);
             },
             writeIntINTINT : function(thisref, handle, intvalue) {
                 bytecoder.filehandles[handle].writeIntINTINT(handle,intvalue);
             },
             close0INT : function(thisref,handle) {
                 bytecoder.filehandles[handle].close0INT(handle);
             },
         },
         fileinputstream : {
             open0String : function(thisref,name) {
                 return bytecoder.openForRead(bytecoder.toJSString(name));
             },
             read0INT : function(thisref,handle) {
                 return bytecoder.filehandles[handle].read0INT(handle);
             },
             readBytesINTL1BYTEINTINT : function(thisref,handle,data,offset,length) {
                 return bytecoder.filehandles[handle].readBytesINTL1BYTEINTINT(handle,data,offset,length);
             },
             skip0INTINT : function(thisref,handle,amount) {
                 return bytecoder.filehandles[handle].skip0INTINT(handle,amount);
             },
             available0INT : function(thisref,handle) {
                 return bytecoder.filehandles[handle].available0INT(handle);
             },
             close0INT : function(thisref,handle) {
                 bytecoder.filehandles[handle].close0INT(handle);
             },
         },
         inflater : {
             initIDs : function(thisref) {
             },
             initBOOLEAN : function(thisref,nowrap) {
             },
             inflateBytesBytesLONGL1BYTEINTINTL1BYTEINTINT : function(thisref,addr,inputArray,inputOff,inputLen,outputArray,outputOff,outputLen) {
             },
             inflateBufferBytesLONGLONGINTL1BYTEINTINT : function(thisref,addr,inputAddress,inputLen,outputArray,outputOff,outputLen) {
             },
             endLONG : function(thisref,addr) {
             },
         },
         canvasrenderingcontext2d: {
             setFillStyleString: function(target,arg0) {
               bytecoder.referenceTable[target].fillStyle=bytecoder.toJSString(arg0);
             },
             setStrokeStyleString: function(target,arg0) {
               bytecoder.referenceTable[target].strokeStyle=bytecoder.toJSString(arg0);
             },
             fillRectFLOATFLOATFLOATFLOAT: function(target,arg0,arg1,arg2,arg3) {
               bytecoder.referenceTable[target].fillRect(arg0,arg1,arg2,arg3);
             },
             save: function(target) {
               bytecoder.referenceTable[target].save();
             },
             translateFLOATFLOAT: function(target,arg0,arg1) {
               bytecoder.referenceTable[target].translate(arg0,arg1);
             },
             scaleFLOATFLOAT: function(target,arg0,arg1) {
               bytecoder.referenceTable[target].scale(arg0,arg1);
             },
             setLineWidthFLOAT: function(target,arg0) {
               bytecoder.referenceTable[target].lineWidth=arg0;
             },
             rotateFLOAT: function(target,arg0) {
               bytecoder.referenceTable[target].rotate(arg0);
             },
             beginPath: function(target) {
               bytecoder.referenceTable[target].beginPath();
             },
             arcDOUBLEDOUBLEDOUBLEDOUBLEDOUBLEBOOLEAN: function(target,arg0,arg1,arg2,arg3,arg4,arg5) {
               bytecoder.referenceTable[target].arc(arg0,arg1,arg2,arg3,arg4,arg5);
             },
             closePath: function(target) {
               bytecoder.referenceTable[target].closePath();
             },
             stroke: function(target) {
               bytecoder.referenceTable[target].stroke();
             },
             moveToFLOATFLOAT: function(target,arg0,arg1) {
               bytecoder.referenceTable[target].moveTo(arg0,arg1);
             },
             lineToFLOATFLOAT: function(target,arg0,arg1) {
               bytecoder.referenceTable[target].lineTo(arg0,arg1);
             },
             restore: function(target) {
               bytecoder.referenceTable[target].restore();
             },
         },
         htmlcanvaselement: {
             getContextString: function(target,arg0) {
               return bytecoder.toBytecoderReference(bytecoder.referenceTable[target].getContext(bytecoder.toJSString(arg0)));
             },
         },
         htmlbutton: {
             disabledBOOLEAN: function(target,arg0) {
               bytecoder.referenceTable[target].disabled=arg0;
             },
         },
         promise: {
             thenPromise$Handler: function(target,arg0) {
               bytecoder.referenceTable[target].then(bytecoder.registerCallback(arg0,function (farg0) {var marg0=bytecoder.toBytecoderReference(farg0);bytecoder.exports.dmbawPromise$Handler_VOIDhandleObjectdmbaOpaqueReferenceType(arg0,marg0);}));
             },
         },
         parentnode: {
             getElementByIdString: function(target,arg0) {
               return bytecoder.toBytecoderReference(bytecoder.referenceTable[target].getElementById(bytecoder.toJSString(arg0)));
             },
         },
         window: {
             document: function(target) {
               return bytecoder.toBytecoderReference(bytecoder.referenceTable[target].document);
             },
             requestAnimationFrameAnimationFrameCallback: function(target,arg0) {
               bytecoder.referenceTable[target].requestAnimationFrame(bytecoder.registerCallback(arg0,function (farg0) {var marg0=farg0;bytecoder.exports.dmbawAnimationFrameCallback_VOIDrunINT(arg0,marg0);}));
             },
         },
         body: {
             text: function(target) {
               return bytecoder.toBytecoderReference(bytecoder.referenceTable[target].text());
             },
         },
         eventtarget: {
             addEventListenerStringEventListener: function(target,arg0,arg1) {
               bytecoder.referenceTable[target].addEventListener(bytecoder.toJSString(arg0),bytecoder.registerCallback(arg1,function (farg0) {var marg0=bytecoder.toBytecoderReference(farg0);bytecoder.exports.dmbawEventListener_VOIDrundmbawEvent(arg1,marg0);delete bytecoder.referenceTable[marg0];}));
             },
         },
         windoworworkerglobalscope: {
             fetchString: function(target,arg0) {
               return bytecoder.toBytecoderReference(bytecoder.referenceTable[target].fetch(bytecoder.toJSString(arg0)));
             },
         },
         stringpromise: {
             thenStringPromise$Handler: function(target,arg0) {
               bytecoder.referenceTable[target].then(bytecoder.registerCallback(arg0,function (farg0) {var marg0=bytecoder.toBytecoderString(farg0);bytecoder.exports.dmbawStringPromise$Handler_VOIDhandleStringjlString(arg0,marg0);}));
             },
         },
         element: {
             innerHTMLString: function(target,arg0) {
               bytecoder.referenceTable[target].innerHTML=bytecoder.toJSString(arg0);
             },
         },
     },
};
